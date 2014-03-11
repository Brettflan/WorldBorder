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
		helpText = "Default value: on. When enabled, this setting will directly cancel attempts to use an ender pearl to " +
			"get past the border rather than just knocking the player back. This should prevent usage of ender " +
			"pearls to glitch into areas otherwise inaccessible at the border edge.";
	}

	@Override
	public void cmdStatus(CommandSender sender)
	{
		sender.sendMessage(C_HEAD + "Direct cancellation of ender pearls thrown past the border is " +
						   enabledColored(Config.getDenyEnderpearl()) + C_HEAD + ".");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		Config.setDenyEnderpearl(strAsBool(params.get(0)));

		if (player != null)
		{
			Config.log((Config.getDenyEnderpearl() ? "Enabled" : "Disabled") + " direct cancellation of ender pearls thrown past the border at the command of player \"" + player.getName() + "\".");
			cmdStatus(sender);
		}
	}
}
