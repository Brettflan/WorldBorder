package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdTrim extends WBCmd
{
	public CmdTrim()
	{
		name = permission = "trim";
		hasWorldNameInput = true;
		consoleRequiresWorldName = false;
		minParams = 0;
		maxParams = 2;

		addCmdExample(nameEmphasizedW() + "[freq] [pad] - trim world outside of border.");
		helpText = "This command will remove chunks which are outside the world's border. [freq] is the frequency " +
			"of chunks per second that will be checked (default 5000). [pad] is the number of blocks padding kept " +
			"beyond the border itself (default 208, to cover player visual range).";
	}

	@Override
	public void execute(CommandSender sender, Player player, List<String> params, String worldName)
	{
		boolean confirm = false;
		// check for "cancel", "pause", or "confirm"
		if (params.size() >= 1)
		{
			String check = params.get(0).toLowerCase();

			if (check.equals("cancel") || check.equals("stop"))
			{
				if (!makeSureTrimIsRunning(sender))
					return;
				sender.sendMessage(C_HEAD + "Cancelling the world map trimming task.");
				trimDefaults();
				Config.StopTrimTask();
				return;
			}
			else if (check.equals("pause"))
			{
				if (!makeSureTrimIsRunning(sender))
					return;
				Config.trimTask.pause();
				sender.sendMessage(C_HEAD + "The world map trimming task is now " + (Config.trimTask.isPaused() ? "" : "un") + "paused.");
				return;
			}

			confirm = check.equals("confirm");
		}

		// if not just confirming, make sure a world name is available
		if (worldName == null && !confirm)
		{
			if (player != null)
				worldName = player.getWorld().getName();
			else
			{
				sendErrorAndHelp(sender, "You must specify a world!");
				return;
			}
		}

		// colorized "/wb trim "
		String cmd = cmd(sender) + nameEmphasized() + C_CMD;

		// make sure Trim isn't already running
		if (Config.trimTask != null && Config.trimTask.valid())
		{
			sender.sendMessage(C_ERR + "The world map trimming task is already running.");
			sender.sendMessage(C_DESC + "You can cancel at any time with " + cmd + "cancel" + C_DESC + ", or pause/unpause with " + cmd + "pause" + C_DESC + ".");
			return;
		}

		// set frequency and/or padding if those were specified
		try
		{
			if (params.size() >= 1 && !confirm)
				trimFrequency = Math.abs(Integer.parseInt(params.get(0)));
			if (params.size() >= 2 && !confirm)
				trimPadding = Math.abs(Integer.parseInt(params.get(1)));
		}
		catch(NumberFormatException ex)
		{
			sendErrorAndHelp(sender, "The frequency and padding values must be integers.");
			trimDefaults();
			return;
		}
		if (trimFrequency <= 0)
		{
			sendErrorAndHelp(sender, "The frequency value must be greater than zero.");
			trimDefaults();
			return;
		}

		// set world if it was specified
		if (worldName != null)
			trimWorld = worldName;

		if (confirm)
		{	// command confirmed, go ahead with it
			if (trimWorld.isEmpty())
			{
				sendErrorAndHelp(sender, "You must first use this command successfully without confirming.");
				return;
			}

			if (player != null)
				Config.log("Trimming world beyond border at the command of player \"" + player.getName() + "\".");

			int ticks = 1, repeats = 1;
			if (trimFrequency > 20)
				repeats = trimFrequency / 20;
			else
				ticks = 20 / trimFrequency;

			Config.trimTask = new WorldTrimTask(Bukkit.getServer(), player, trimWorld, trimPadding, repeats);
			if (Config.trimTask.valid())
			{
				int task = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(WorldBorder.plugin, Config.trimTask, ticks, ticks);
				Config.trimTask.setTaskID(task);
				sender.sendMessage("WorldBorder map trimming task for world \"" + trimWorld + "\" started.");
			}
			else
				sender.sendMessage(C_ERR + "The world map trimming task failed to start.");

			trimDefaults();
		}
		else
		{
			if (trimWorld.isEmpty())
			{
				sendErrorAndHelp(sender, "You must first specify a valid world.");
				return;
			}

			sender.sendMessage(C_HEAD + "World trimming task is ready for world \"" + trimWorld + "\", attempting to process up to " + trimFrequency + " chunks per second (default 20). The map will be trimmed past " + trimPadding + " blocks beyond the border (default " + defaultPadding + ").");
			sender.sendMessage(C_HEAD + "This process can take a very long time depending on the world's overall size. Also, depending on the chunk processing rate, players may experience lag for the duration.");
			sender.sendMessage(C_DESC + "You should now use " + cmd + "confirm" + C_DESC + " to start the process.");
			sender.sendMessage(C_DESC + "You can cancel at any time with " + cmd + "cancel" + C_DESC + ", or pause/unpause with " + cmd + "pause" + C_DESC + ".");
		}
	}
	

	/* with "view-distance=10" in server.properties on a fast VM test server and "Render Distance: Far" in client,
	 * hitting border during testing was loading 11+ chunks beyond the border in a couple of directions (10 chunks in
	 * the other two directions). This could be worse on a more loaded or worse server, so:
	 */
	private final int defaultPadding = CoordXZ.chunkToBlock(13);

	private String trimWorld = "";
	private int trimFrequency = 5000;
	private int trimPadding = defaultPadding;

	private void trimDefaults()
	{
		trimWorld = "";
		trimFrequency = 5000;
		trimPadding = defaultPadding;
	}

	private boolean makeSureTrimIsRunning(CommandSender sender)
	{
		if (Config.trimTask != null && Config.trimTask.valid())
			return true;
		sendErrorAndHelp(sender, "The world map trimming task is not currently running.");
		return false;
	}
}
