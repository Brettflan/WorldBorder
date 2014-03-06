package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdSetmsg extends WBCmd
{
	public CmdSetmsg()
	{
		name = permission = "setmsg";
		minParams = 1;

		addCmdExample(nameEmphasized() + "<text> - set border message.");
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

		Config.setMessage(message.toString());

		sender.sendMessage("Border message is now set to:");
		sender.sendMessage(Config.MessageRaw());
		sender.sendMessage("Formatted border message:");
		sender.sendMessage(Config.Message());
	}
}
