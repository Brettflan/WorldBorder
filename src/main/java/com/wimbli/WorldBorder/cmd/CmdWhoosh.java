package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdWhoosh extends WBCmd
{
	public CmdWhoosh()
	{
		name = permission = "whoosh";
		minParams = maxParams = 1;

		addCmdExample(nameEmphasized() + "<on|off> - turn knockback effect on or off.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		Config.setWhooshEffect(strAsBool(params.get(0)));

		if (player != null)
		{
			Config.log((Config.whooshEffect() ? "Enabled" : "Disabled") + " \"whoosh\" knockback effect at the command of player \"" + player.getName() + "\".");
			sender.sendMessage("\"Whoosh\" knockback effect " + enabledColored(Config.whooshEffect()) + ".");
		}
	}
}
