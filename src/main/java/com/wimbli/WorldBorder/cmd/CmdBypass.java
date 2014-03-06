package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdBypass extends WBCmd
{
	public CmdBypass()
	{
		name = permission = "bypass";
		minParams = 0;
		maxParams = 2;

		addCmdExample(nameEmphasized() + "{player} [on/off] - let player go beyond border.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		if (player == null && params.isEmpty())
		{
			sendErrorAndHelp(sender, "When running this command from console, you must specify a player.");
			return;
		}

		String sPlayer = (params.isEmpty()) ? player.getName() : params.get(0);

		boolean bypassing = !Config.isPlayerBypassing(sPlayer);
		if (params.size() > 1)
			bypassing = strAsBool(params.get(1));

		Config.setPlayerBypass(sPlayer, bypassing);

		Player target = Bukkit.getPlayer(sPlayer);
		if (target != null && target.isOnline())
			target.sendMessage("Border bypass is now " + enabledColored(bypassing) + ".");

		Config.log("Border bypass for player \"" + sPlayer + "\" is " + (bypassing ? "enabled" : "disabled") + (player != null ? " at the command of player \"" + player.getName() + "\"" : "") + ".");
		if (player != null && player != target)
			sender.sendMessage("Border bypass for player \"" + sPlayer + "\" is " + enabledColored(bypassing) + ".");
	}
}
