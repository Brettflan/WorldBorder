package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdRadius extends WBCmd
{
	public CmdRadius()
	{
		name = permission = "radius";
		hasWorldNameInput = true;
		minParams = 1;
		maxParams = 2;

		addCmdExample(nameEmphasizedW() + "<radiusX> [radiusZ] - change radius.");
		helpText = "Using this command you can adjust the radius of an existing border. If [radiusZ] is not " +
			"specified, the radiusX value will be used for both. You can also optionally specify + or - at the start " +
			"of <radiusX> and [radiusZ] to increase or decrease the existing radius rather than setting a new value.";
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		if (worldName == null)
			worldName = player.getWorld().getName();

		BorderData border = Config.Border(worldName);
		if (border == null)
		{
			sendErrorAndHelp(sender, "This world (\"" + worldName + "\") must first have a border set normally.");
			return;
		}

		double x = border.getX();
		double z = border.getZ();
		int radiusX;
		int radiusZ;
		try
		{
			if (params.get(0).startsWith("+"))
			{
				// Add to the current radius
				radiusX = border.getRadiusX();
				radiusX += Integer.parseInt(params.get(0).substring(1));
			}
			else if(params.get(0).startsWith("-"))
			{
				// Subtract from the current radius
				radiusX = border.getRadiusX();
				radiusX -= Integer.parseInt(params.get(0).substring(1));
			}
			else
				radiusX = Integer.parseInt(params.get(0));	

			if (params.size() == 2)
			{
				if (params.get(1).startsWith("+"))
				{
					// Add to the current radius
					radiusZ = border.getRadiusZ();
					radiusZ += Integer.parseInt(params.get(1).substring(1));
				}
				else if(params.get(1).startsWith("-"))
				{
					// Subtract from the current radius
					radiusZ = border.getRadiusZ();
					radiusZ -= Integer.parseInt(params.get(1).substring(1));
				}
				else
					radiusZ = Integer.parseInt(params.get(1));
			}
			else
				radiusZ = radiusX;
		}
		catch(NumberFormatException ex)
		{
			sendErrorAndHelp(sender, "The radius value(s) must be integers.");
			return;
		}

		Config.setBorder(worldName, radiusX, radiusZ, x, z);

		if (player != null)
			sender.sendMessage("Radius has been set. " + Config.BorderDescription(worldName));
	}
}
