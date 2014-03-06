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
			radiusX = Integer.parseInt(params.get(0));
			if (params.size() == 2)
				radiusZ = Integer.parseInt(params.get(1));
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
