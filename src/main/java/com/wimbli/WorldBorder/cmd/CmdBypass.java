package com.wimbli.WorldBorder.cmd;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;
import com.wimbli.WorldBorder.UUID.UUIDFetcher;


public class CmdBypass extends WBCmd
{
	public CmdBypass()
	{
		name = permission = "bypass";
		minParams = 0;
		maxParams = 2;

		addCmdExample(nameEmphasized() + "{player} [on|off] - let player go beyond border.");
		helpText = "If [player] isn't specified, command sender is used. If [on|off] isn't specified, the value will " +
			"be toggled. Once bypass is enabled, the player will not be stopped by any borders until bypass is " +
			"disabled for them again. Use the " + commandEmphasized("bypasslist") + C_DESC + "command to list all " +
			"players with bypass enabled.";
	}

	@Override
	public void cmdStatus(CommandSender sender)
	{
		if (!(sender instanceof Player))
			return;

		boolean bypass = Config.isPlayerBypassing(((Player)sender).getUniqueId());
		sender.sendMessage(C_HEAD + "Border bypass is currently " + enabledColored(bypass) + C_HEAD + " for you.");
	}

	@Override
	public void execute(final CommandSender sender, final Player player, final List<String> params, String worldName)
	{
		if (player == null && params.isEmpty())
		{
			sendErrorAndHelp(sender, "When running this command from console, you must specify a player.");
			return;
		}

		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(WorldBorder.plugin, new Runnable()
		{
			@Override
			public void run()
			{
				final String sPlayer = (params.isEmpty()) ? player.getName() : params.get(0);
				UUID uPlayer = (params.isEmpty()) ? player.getUniqueId() : null;

				if (uPlayer == null)
				{
					Player p = Bukkit.getPlayer(sPlayer);
					if (p != null)
					{
						uPlayer = p.getUniqueId();
					}
					else
					{
						// only do UUID lookup using Mojang server if specified player isn't online
						try
						{
							uPlayer = UUIDFetcher.getUUID(sPlayer);
						}
						catch(Exception ex)
						{
							sendErrorAndHelp(sender, "Failed to look up UUID for the player name you specified. " + ex.getLocalizedMessage());
							return;
						}
					}
				}
				if (uPlayer == null)
				{
					sendErrorAndHelp(sender, "Failed to look up UUID for the player name you specified; null value returned.");
					return;
				}

				boolean bypassing = !Config.isPlayerBypassing(uPlayer);
				if (params.size() > 1)
					bypassing = strAsBool(params.get(1));

				Config.setPlayerBypass(uPlayer, bypassing);

				Player target = Bukkit.getPlayer(sPlayer);
				if (target != null && target.isOnline())
					target.sendMessage("Border bypass is now " + enabledColored(bypassing) + ".");

				Config.log("Border bypass for player \"" + sPlayer + "\" is " + (bypassing ? "enabled" : "disabled") +
						   (player != null ? " at the command of player \"" + player.getName() + "\"" : "") + ".");
				if (player != null && player != target)
					sender.sendMessage("Border bypass for player \"" + sPlayer + "\" is " + enabledColored(bypassing) + ".");
			}
		});
	}
}
