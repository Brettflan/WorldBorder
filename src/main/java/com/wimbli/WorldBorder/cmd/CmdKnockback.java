package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdKnockback extends WBCmd
{
	public CmdKnockback()
	{
		name = permission = "knockback";
		minParams = maxParams = 1;

		addCmdExample(nameEmphasized() + "<distance> - how far to move the player back.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		double numBlocks = 0.0;
		try
		{
			numBlocks = Double.parseDouble(params.get(0));
			if (numBlocks < 0.0 || (numBlocks > 0.0 && numBlocks < 1.0))
				throw new NumberFormatException();
		}
		catch(NumberFormatException ex)
		{
			sendErrorAndHelp(sender, "The knockback must be a decimal value of at least 1.0, or it can be 0.");
			return;
		}

		Config.setKnockBack(numBlocks);

		if (player != null)
			sender.sendMessage("Knockback set to " + numBlocks + " blocks inside the border.");
	}
}
