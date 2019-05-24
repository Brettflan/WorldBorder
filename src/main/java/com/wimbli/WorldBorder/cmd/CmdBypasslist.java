package com.wimbli.WorldBorder.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;
import com.wimbli.WorldBorder.UUID.UUIDFetcher;


public class CmdBypasslist extends WBCmd
{
	public CmdBypasslist()
	{
		name = permission = "bypasslist";
		minParams = maxParams = 0;

		addCmdExample(nameEmphasized() + "- list players with border bypass enabled.");
		helpText = "The bypass list will persist between server restarts, and applies to all worlds. Use the " +
			commandEmphasized("bypass") + C_DESC + "command to add or remove players.";
	}

	@Override
	public void execute(final CommandSender sender, Player player, List<String> params, String worldName)
	{
		final ArrayList<UUID> uuids = Config.getPlayerBypassList();
		if (uuids == null || uuids.isEmpty())
		{
			sender.sendMessage("Players with border bypass enabled: <none>");
			return;
		}

		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(WorldBorder.plugin, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Map<UUID, String> names = UUIDFetcher.getNameList(uuids);
					String nameString = names.values().toString();

					sender.sendMessage("Players with border bypass enabled: " + nameString.substring(1, nameString.length() - 1));
				}
				catch(Exception ex)
				{
					sendErrorAndHelp(sender, "Failed to look up names for the UUIDs in the border bypass list. " + ex.getLocalizedMessage());
					return;
				}
			}
		});
	}
}
