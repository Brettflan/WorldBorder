package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdShape extends WBCmd
{
	public CmdShape()
	{
		name = permission = "shape";
		minParams = maxParams = 1;

		addCmdExample(nameEmphasized() + "<round|square> - set the default border shape.");
		addCmdExample(nameEmphasized() + "<elliptic|rectangular> - same as above.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		String shape = params.get(0).toLowerCase();
		if (shape.equals("rectangular") || shape.equals("square"))
			Config.setShape(false);
		else if (shape.equals("elliptic") || shape.equals("round"))
			Config.setShape(true);
		else
		{
			sendErrorAndHelp(sender, "You must specify one of the 4 valid shape names as indicated below.");
			return;
		}

		if (player != null)
			sender.sendMessage("Default border shape for all worlds is now set to \"" + Config.ShapeName() + "\".");
	}
}
