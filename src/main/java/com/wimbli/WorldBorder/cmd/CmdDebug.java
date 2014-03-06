package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdDebug extends WBCmd
{
	public CmdDebug()
	{
		name = permission = "debug";
		minParams = maxParams = 1;

		addCmdExample(nameEmphasized() + "<on|off> - turn console debug output on or off.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		Config.setDebug(strAsBool(params.get(0)));

		if (player != null)
		{
			Config.log((Config.Debug() ? "Enabled" : "Disabled") + " debug output at the command of player \"" + player.getName() + "\".");
			sender.sendMessage("Debug mode " + enabledColored(Config.Debug()) + ".");
		}
	}
}
