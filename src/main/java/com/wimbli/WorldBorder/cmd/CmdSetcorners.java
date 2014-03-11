package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.World;

import com.wimbli.WorldBorder.*;


public class CmdSetcorners extends WBCmd
{
	public CmdSetcorners()
	{
		name = "setcorners";
		permission = "set";
		hasWorldNameInput = true;
		minParams = maxParams = 4;

		addCmdExample(nameEmphasizedW() + "<x1> <z1> <x2> <z2> - corner coords.");
		helpText = "This is an alternate way to set a border, by specifying the X and Z coordinates of two opposite " +
			"corners of the border area ((x1, z1) to (x2, z2)). [world] is optional for players and defaults to the " +
			"world the player is in.";
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		if (worldName == null)
		{
			worldName = player.getWorld().getName();
		}
		else
		{
			World worldTest = sender.getServer().getWorld(worldName);
			if (worldTest == null)
				sender.sendMessage("The world you specified (\"" + worldName + "\") could not be found on the server, but data for it will be stored anyway.");
		}

		try
		{
			double x1 = Double.parseDouble(params.get(0));
			double z1 = Double.parseDouble(params.get(1));
			double x2 = Double.parseDouble(params.get(2));
			double z2 = Double.parseDouble(params.get(3));
			Config.setBorderCorners(worldName, x1, z1, x2, z2);
		}
		catch(NumberFormatException ex)
		{
			sendErrorAndHelp(sender, "The x1, z1, x2, and z2 coordinate values must be numerical.");
			return;
		}

		if(player != null)
			sender.sendMessage("Border has been set. " + Config.BorderDescription(worldName));
	}
}
