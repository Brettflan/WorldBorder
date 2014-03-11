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
		helpText = "Default value: 3.0 (blocks). Players who cross the border will be knocked back to this distance inside.";
	}

	@Override
	public void cmdStatus(CommandSender sender)
	{
		double kb = Config.KnockBack();
		if (kb < 1)
			sender.sendMessage(C_HEAD + "Knockback is set to 0, disabling border enforcement.");
		else
			sender.sendMessage(C_HEAD + "Knockback is set to " + kb + " blocks inside the border.");
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
			cmdStatus(sender);
	}
}
