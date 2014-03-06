package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdDynmapmsg extends WBCmd
{
	public CmdDynmapmsg()
	{
		name = permission = "dynmapmsg";
		minParams = 1;

		addCmdExample(nameEmphasized() + "<text> - DynMap border labels will show this.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		StringBuilder message = new StringBuilder();
		boolean first = true;
		for (String param : params)
		{
			if (!first)
				message.append(" ");
			message.append(param);
			first = false;
		}

		Config.setDynmapMessage(message.toString());

		if (player != null)
			sender.sendMessage("DynMap border label is now set to: " + clrErr + Config.DynmapMessage());
	}
}
