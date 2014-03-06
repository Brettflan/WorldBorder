package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdDenypearl extends WBCmd
{
	public CmdDenypearl()
	{
		name = permission = "denypearl";
		minParams = maxParams = 1;

		addCmdExample(nameEmphasized() + "<on|off> - stop ender pearls past the border.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		Config.setDenyEnderpearl(strAsBool(params.get(0)));

		if (player != null)
		{
			Config.log((Config.getDenyEnderpearl() ? "Enabled" : "Disabled") + " direct cancellation of ender pearls thrown past the border at the command of player \"" + player.getName() + "\".");
			sender.sendMessage("Direct cancellation of ender pearls thrown past the border " + enabledColored(Config.getDenyEnderpearl()) + ".");
		}
	}
}
