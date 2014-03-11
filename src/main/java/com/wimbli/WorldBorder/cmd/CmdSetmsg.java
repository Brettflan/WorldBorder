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
		helpText = "Default value: \"&cYou have reached the edge of this world.\". This command lets you set the message shown to players who are knocked back from the border.";
	}

	@Override
	public void cmdStatus(CommandSender sender)
	{
		sender.sendMessage(C_HEAD + "Border message is set to:");
		sender.sendMessage(Config.MessageRaw());
		sender.sendMessage(C_HEAD + "Formatted border message:");
		sender.sendMessage(Config.Message());
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

		cmdStatus(sender);
	}
}
