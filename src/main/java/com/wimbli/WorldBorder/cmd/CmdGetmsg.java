package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdGetmsg extends WBCmd
{
	public CmdGetmsg()
	{
		name = permission = "getmsg";
		minParams = maxParams = 0;

		addCmdExample(nameEmphasized() + "- display border message.");
		helpText = "This command simply displays the message shown to players knocked back from the border.";
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		sender.sendMessage("Border message is currently set to:");
		sender.sendMessage(Config.MessageRaw());
		sender.sendMessage("Formatted border message:");
		sender.sendMessage(Config.Message());
	}
}
