package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdPortal extends WBCmd
{
	public CmdPortal()
	{
		name = permission = "portal";
		minParams = maxParams = 1;

		addCmdExample(nameEmphasized() + "<on|off> - turn portal redirection on or off.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		Config.setPortalRedirection(strAsBool(params.get(0)));

		if (player != null)
		{
			Config.log((Config.portalRedirection() ? "Enabled" : "Disabled") + " portal redirection at the command of player \"" + player.getName() + "\".");
			sender.sendMessage("Portal redirection " + enabledColored(Config.portalRedirection()) + ".");
		}
	}
}
