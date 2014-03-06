package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdDynmap extends WBCmd
{
	public CmdDynmap()
	{
		name = permission = "dynmap";
		minParams = maxParams = 1;

		addCmdExample(nameEmphasized() + "<on|off> - turn DynMap border display on or off.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		Config.setDynmapBorderEnabled(strAsBool(params.get(0)));

		if (player != null)
		{
			sender.sendMessage("DynMap border display " + (Config.DynmapBorderEnabled() ? "enabled" : "disabled") + ".");
			Config.log((Config.DynmapBorderEnabled() ? "Enabled" : "Disabled") + " DynMap border display at the command of player \"" + player.getName() + "\".");
		}
	}
}
