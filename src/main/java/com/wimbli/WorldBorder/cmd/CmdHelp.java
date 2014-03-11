package com.wimbli.WorldBorder.cmd;

import java.util.List;
import java.util.Set;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdHelp extends WBCmd
{
	public CmdHelp()
	{
		name = permission = "help";
		minParams = 0;
		maxParams = 10;

		addCmdExample(nameEmphasized() + "[command] - get help on command usage.");
//		helpText = "If [command] is specified, info for that particular command will be provided.";
	}

	@Override
	public void cmdStatus(CommandSender sender)
	{
		String commands = WorldBorder.wbCommand.getCommandNames().toString().replace(", ", C_DESC + ", " + C_CMD);
		sender.sendMessage(C_HEAD + "Commands: " + C_CMD + commands.substring(1, commands.length() - 1));
		sender.sendMessage("Example, for info on \"set\" command: " + cmd(sender) + nameEmphasized() + C_CMD + "set");
		sender.sendMessage(C_HEAD + "For a full command example list, simply run the root " + cmd(sender) + C_HEAD + "command by itself with nothing specified.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		if (params.isEmpty())
		{
			sendCmdHelp(sender);
			return;
		}

		Set<String> commands = WorldBorder.wbCommand.getCommandNames();
		for (String param : params)
		{
			if (commands.contains(param.toLowerCase()))
			{
				WorldBorder.wbCommand.subCommands.get(param.toLowerCase()).sendCmdHelp(sender);
				return;
			}
		}
		sendErrorAndHelp(sender, "No command recognized.");
	}
}
