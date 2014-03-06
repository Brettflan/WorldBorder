package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdDelay extends WBCmd
{
	public CmdDelay()
	{
		name = permission = "delay";
		minParams = maxParams = 1;

		addCmdExample(nameEmphasized() + "<amount> - time between border checks.");
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		int delay = 0;
		try
		{
			delay = Integer.parseInt(params.get(0));
			if (delay < 1)
				throw new NumberFormatException();
		}
		catch(NumberFormatException ex)
		{
			sendErrorAndHelp(sender, "The timer delay must be an integer of 1 or higher.");
			return;
		}

		Config.setTimerTicks(delay);

		if (player != null)
			sender.sendMessage("Timer delay set to " + delay + " tick(s). That is roughly " + (delay * 50) + "ms.");
	}
}
