package com.wimbli.WorldBorder.cmd;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;


public abstract class WBCmd
{
	/*
	 * Primary variables, should be set as needed in constructors for the subclassed commands
	 */

	// command name, command permission; normally the same thing
	public String name = "";
	public String permission = null;

	// whether command can accept a world name before itself
	public boolean hasWorldNameInput = false;
	public boolean consoleRequiresWorldName = true;

	// minimum and maximum number of accepted parameters
	public int minParams = 0;
	public int maxParams = 9999;

	/*
	 * The guts of the command run in here; needs to be overriden in the subclassed commands
	 */
	public abstract void execute(CommandSender sender, Player player, List<String> params, String worldName);


	/*
	 * Helper variables and methods
	 */

	// color values for strings
	public final static String clrCmd = ChatColor.AQUA.toString();			// main commands
	public final static String clrDesc = ChatColor.WHITE.toString();		// command descriptions
	public final static String clrErr = ChatColor.RED.toString();			// errors / notices
	public final static String clrHead = ChatColor.YELLOW.toString();		// command listing header
	public final static String clrOpt = ChatColor.DARK_GREEN.toString();	// optional values
	public final static String clrReq = ChatColor.GREEN.toString();			// required values

	// colorized root command, for console and for player
	public final static String cmdC = clrCmd + "wb ";
	public final static String cmdP = clrCmd + "/wb ";

	// list of command examples for this command to be displayed as usage reference, separate between players and console
	// ... these generally should be set indirectly using addCmdExample() within the constructor for each command class
	public List<String> cmdExamplePlayer = new ArrayList<String>();
	public List<String> cmdExampleConsole = new ArrayList<String>();

	// much like the above, but used for displaying command list from root /wb command, listing all commands
	public final static List<String> cmdExamplesConsole = new ArrayList<String>(48);  // 48 command capacity, 6 full pages
	public final static List<String> cmdExamplesPlayer = new ArrayList<String>(48);   // still, could need to increase later


	// add command examples for use the default "/wb" command list and for internal usage reference, formatted and colorized
	public void addCmdExample(String example)
	{
		addCmdExample(example, true, true, true);
	}
	public void addCmdExample(String example, boolean forPlayer, boolean forConsole, boolean prefix)
	{
		// go ahead and colorize required "<>" and optional "[]" parameters, extra command words, and description
		example = example.replace("<", clrReq+"<").replace("[", clrOpt+"[").replace("^", clrCmd).replace("- ", clrDesc+"- ");

		// all "{}" are replaced by "[]" (optional) for player, "<>" (required) for console
		if (forPlayer)
		{
			String exampleP = (prefix ? cmdP : "") + example.replace("{", clrOpt + "[").replace("}", "]");
			cmdExamplePlayer.add(exampleP);
			cmdExamplesPlayer.add(exampleP);
		}
		if (forConsole)
		{
			String exampleC = (prefix ? cmdC : "") + example.replace("{", clrReq + "<").replace("}", ">");
			cmdExampleConsole.add(exampleC);
			cmdExamplesConsole.add(exampleC);
		}
	}

	// formatted and colorized text, intended for marking command name
	public String commandEmphasized(String text)
	{
		return clrCmd + ChatColor.UNDERLINE + text + ChatColor.RESET + " ";
	}

	// returns green "enabled" or red "disabled" text
	public String enabledColored(boolean enabled)
	{
		return enabled ? clrReq+"enabled" : clrErr+"disabled";
	}

	// formatted and colorized command name, optionally prefixed with "[world]" (for player) / "<world>" (for console)
	public String nameEmphasized()
	{
		return commandEmphasized(name);
	}
	public String nameEmphasizedW()
	{
		return "{world} " + nameEmphasized();
	}

	// send command example message(s)
	public void sendCmdHelp(CommandSender sender)
	{
		for (String example : ((sender instanceof Player) ? cmdExamplePlayer : cmdExampleConsole))
		{
			sender.sendMessage(example);
		}
	}

	// send error message followed by command example message(s)
	public void sendErrorAndHelp(CommandSender sender, String error)
	{
		sender.sendMessage(clrErr + error);
		sendCmdHelp(sender);
	}

	// interpret string as boolean value (yes/no, true/false, on/off, +/-, 1/0)
	public boolean strAsBool(String str)
	{
		str = str.toLowerCase();
		return str.startsWith("y") || str.startsWith("t") || str.startsWith("on") || str.startsWith("+") || str.startsWith("1");
	}
}
