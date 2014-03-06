package com.wimbli.WorldBorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.cmd.*;


public class WBCommand implements CommandExecutor
{
	// map of all sub-commands with the command name (string) for quick reference
	private Map<String, WBCmd> subCommands = new LinkedHashMap<String, WBCmd>();
	// ref. list of the commands which can have a world name in front of the command itself (ex. /wb _world_ radius 100)
	private Set<String> subCommandsWithWorldNames = new LinkedHashSet<String>();

	// constructor
	public WBCommand (WorldBorder plugin)
	{
		addCmd(new CmdSet());
		addCmd(new CmdSetcorners());
		addCmd(new CmdRadius());
		addCmd(new CmdShape());
		addCmd(new CmdClear());
		addCmd(new CmdList());
		addCmd(new CmdFill());
		addCmd(new CmdTrim());
		addCmd(new CmdBypass());
		addCmd(new CmdBypasslist());
		addCmd(new CmdKnockback());
		addCmd(new CmdWrap());
		addCmd(new CmdWhoosh());
		addCmd(new CmdGetmsg());
		addCmd(new CmdSetmsg());
		addCmd(new CmdDelay());
		addCmd(new CmdWshape());
		addCmd(new CmdDynmap());
		addCmd(new CmdDynmapmsg());
		addCmd(new CmdRemount());
		addCmd(new CmdFillautosave());
		addCmd(new CmdPortal());
		addCmd(new CmdDenypearl());
		addCmd(new CmdReload());
		addCmd(new CmdDebug());

		// this is the default command, which shows command example pages; should be last just in case
		addCmd(new CmdCommands());
	}


	private void addCmd(WBCmd cmd)
	{
		subCommands.put(cmd.name, cmd);
		if (cmd.hasWorldNameInput)
			subCommandsWithWorldNames.add(cmd.name);
	}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] split)
	{
		Player player = (sender instanceof Player) ? (Player)sender : null;

		// if world name is passed inside quotation marks, handle that, and get List<String> instead of String[]
		List<String> params = concatenateQuotedWorldName(split);

		String worldName = null;
		// is second parameter the command and first parameter a world name? definitely world name if it was in quotation marks
		if (wasWorldQuotation || (params.size() > 1 && !subCommands.containsKey(params.get(0)) && subCommandsWithWorldNames.contains(params.get(1))))
			worldName = params.get(0);

		// no command specified? show command examples / help
		if (params.isEmpty())
			params.add(0, "commands");

		// determined the command name
		String cmdName = (worldName == null) ? params.get(0).toLowerCase() : params.get(1).toLowerCase();

		// remove command name and (if there) world name from front of param array
		params.remove(0);
		if (worldName != null)
			params.remove(0);

		// make sure command is recognized, default to showing command examples / help if not; also check for specified page number
		if (!subCommands.containsKey(cmdName))
		{
			int page = (player == null) ? 0 : 1;
			try
			{
				page = Integer.parseInt(cmdName);
			}
			catch(NumberFormatException ignored)
			{
				sender.sendMessage(WBCmd.clrErr + "Command not recognized. Showing command list.");
			}
			cmdName = "commands";
			params.add(0, Integer.toString(page));
		}

		WBCmd subCommand = subCommands.get(cmdName);

		// check permission
		if (!Config.HasPermission(player, subCommand.permission))
			return true;

		// if command requires world name when run by console, make sure that's in place
		if (player == null && subCommand.hasWorldNameInput && subCommand.consoleRequiresWorldName && worldName == null)
		{
			sender.sendMessage(WBCmd.clrErr + "This command requires a world to be specified if run by the console.");
			subCommand.sendCmdHelp(sender);
			return true;
		}

		// make sure valid number of parameters has been provided
		if (params.size() < subCommand.minParams || params.size() > subCommand.maxParams)
		{
			sender.sendMessage(WBCmd.clrErr + "You have not provided a valid number of arguments.");
			subCommand.sendCmdHelp(sender);
			return true;
		}

		// execute command
		subCommand.execute(sender, player, params, worldName);

		return true;
	}


	private boolean wasWorldQuotation = false;

	// if world name is surrounded by quotation marks, combine it down and tag wasWorldQuotation if it's the first parameter
	// also return List<String> instead of input primitive String[]
	private List<String> concatenateQuotedWorldName(String[] split)
	{
		wasWorldQuotation = false;
		List<String> args = new ArrayList<String>(Arrays.asList(split));

		int startIndex = -1;
		for (int i = 0; i < args.size(); i++)
		{
			if (args.get(i).startsWith("\""))
			{
				startIndex = i;
				break;
			}
		}
		if (startIndex == -1)
			return args;

		if (args.get(startIndex).endsWith("\""))
		{
			args.set(startIndex, args.get(startIndex).substring(1, args.get(startIndex).length() - 1));
			if (startIndex == 0)
				wasWorldQuotation = true;
		}
		else
		{
			List<String> concat = new ArrayList<String>(args);
			Iterator<String> concatI = concat.iterator();

			// skip past any parameters in front of the one we're starting on
			for (int i = 1; i < startIndex + 1; i++)
			{
				concatI.next();
			}

			StringBuilder quote = new StringBuilder(concatI.next());
			while (concatI.hasNext())
			{
				String next = concatI.next();
				concatI.remove();
				quote.append(" ");
				quote.append(next);
				if (next.endsWith("\""))
				{
					concat.set(startIndex, quote.substring(1, quote.length() - 1));
					args = concat;
					if (startIndex == 0)
						wasWorldQuotation = true;
					break;
				}
			}
		}
		return args;
	}

}