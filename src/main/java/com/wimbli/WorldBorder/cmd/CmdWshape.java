package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdWshape extends WBCmd
{
	public CmdWshape()
	{
		name = permission = "wshape";
		minParams = 1;
		maxParams = 2;

		addCmdExample(nameEmphasized() + "{world} <elliptic|rectangular|default> - shape");
		addCmdExample(C_DESC + "     override for a single world.", true, true, false);
		addCmdExample(nameEmphasized() + "{world} <round|square|default> - same as above.");
		helpText = "This will override the default border shape for a single world. The value \"default\" implies " +
			"a world is just using the default border shape. See the " + commandEmphasized("shape") + C_DESC +
			"command for more info and to set the default border shape.";
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		if (player == null && params.size() == 1)
		{
			sendErrorAndHelp(sender, "When running this command from console, you must specify a world.");
			return;
		}

		String shapeName = "";

		// world and shape specified
		if (params.size() == 2)
		{
			worldName = params.get(0);
			shapeName = params.get(1).toLowerCase();
		}
		// no world specified, just shape
		else
		{
			worldName = player.getWorld().getName();
			shapeName = params.get(0).toLowerCase();
		}

		BorderData border = Config.Border(worldName);
		if (border == null)
		{
			sendErrorAndHelp(sender, "This world (\"" + worldName + "\") does not have a border set.");
			return;
		}

		Boolean shape = null;
		if (shapeName.equals("rectangular") || shapeName.equals("square"))
			shape = false;
		else if (shapeName.equals("elliptic") || shapeName.equals("round"))
			shape = true;

		border.setShape(shape);
		Config.setBorder(worldName, border, false);

		sender.sendMessage("Border shape for world \"" + worldName + "\" is now set to \"" + Config.ShapeName(shape) + "\".");
	}
}
