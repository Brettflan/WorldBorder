package com.wimbli.WorldBorder.cmd;

import java.util.List;
import java.util.Set;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdList extends WBCmd
{
	public CmdList()
	{
		name = permission = "list";
		minParams = maxParams = 0;

		addCmdExample(nameEmphasized() + "- show border information for all worlds.");
		helpText = "This command will list full information for every border you have set including position, " +
			"radius, and shape. The default border shape will also be indicated.";
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		sender.sendMessage("Default border shape for all worlds is \"" + Config.ShapeName() + "\".");

		Set<String> list = Config.BorderDescriptions();

		if (list.isEmpty())
		{
			sender.sendMessage("There are no borders currently set.");
			return;
		}

		for(String borderDesc : list)
		{
			sender.sendMessage(borderDesc);
		}
	}
}
