package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdClear extends WBCmd
{
	public CmdClear()
	{
		name = permission = "clear";
		hasWorldNameInput = true;
		consoleRequiresWorldName = false;
		minParams = 0;
		maxParams = 1;

		addCmdExample(nameEmphasizedW() + "- remove border for this world.");
		addCmdExample(nameEmphasized()  + "^all - remove border for all worlds.");
		helpText = "If run by an in-game player and [world] or \"all\" isn't specified, the world you are currently " +
			"in is used.";
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		// handle "clear all" command separately
		if (params.size() == 1 &&  params.get(0).equalsIgnoreCase("all"))
		{
			if (worldName != null)
			{
				sendErrorAndHelp(sender, "You should not specify a world with \"clear all\".");
				return;
			}

			Config.removeAllBorders();

			if (player != null)
				sender.sendMessage("All borders have been cleared for all worlds.");
			return;
		}

		if (worldName == null)
		{
			if (player == null)
			{
				sendErrorAndHelp(sender, "You must specify a world name from console if not using \"clear all\".");
				return;
			}
			worldName = player.getWorld().getName();
		}

		BorderData border = Config.Border(worldName);
		if (border == null)
		{
			sendErrorAndHelp(sender, "This world (\"" + worldName + "\") does not have a border set.");
			return;
		}

		Config.removeBorder(worldName);

		if (player != null)
			sender.sendMessage("Border cleared for world \"" + worldName + "\".");
	}
}
