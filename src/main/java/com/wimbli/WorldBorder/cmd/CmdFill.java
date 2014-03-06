package com.wimbli.WorldBorder.cmd;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;


public class CmdFill extends WBCmd
{
	public CmdFill()
	{
		name = permission = "fill";
		hasWorldNameInput = true;
		consoleRequiresWorldName = false;
		minParams = 0;
		maxParams = 3;

		addCmdExample(nameEmphasizedW() + "[freq] [pad] [force] - fill world to border.");
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
				if (!makeSureFillIsRunning(sender))
					return;
				sender.sendMessage(clrHead + "Cancelling the world map generation task.");
				fillDefaults();
				Config.StopFillTask();
				return;
			}
			else if (check.equals("pause"))
			{
				if (!makeSureFillIsRunning(sender))
					return;
				Config.fillTask.pause();
				sender.sendMessage(clrHead + "The world map generation task is now " + (Config.fillTask.isPaused() ? "" : "un") + "paused.");
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

		// colorized "/wb fill "
		String cmd = ((player == null) ? cmdC : cmdP) + nameEmphasized() + clrCmd;

		// make sure Fill isn't already running
		if (Config.fillTask != null && Config.fillTask.valid())
		{
			sender.sendMessage(clrErr + "The world map generation task is already running.");
			sender.sendMessage(clrDesc + "You can cancel at any time with " + cmd + "cancel" + clrDesc + ", or pause/unpause with " + cmd + "pause" + clrDesc + ".");
			return;
		}

		// set frequency and/or padding if those were specified
		try
		{
			if (params.size() >= 1 && !confirm)
				fillFrequency = Math.abs(Integer.parseInt(params.get(0)));
			if (params.size() >= 2 && !confirm)
				fillPadding = Math.abs(Integer.parseInt(params.get(1)));
		}
		catch(NumberFormatException ex)
		{
			sendErrorAndHelp(sender, "The frequency and padding values must be integers.");
			fillDefaults();
			return;
		}
		if (fillFrequency <= 0)
		{
			sendErrorAndHelp(sender, "The frequency value must be greater than zero.");
			fillDefaults();
			return;
		}

		// see if the command specifies to load even chunks which should already be fully generated
		if (params.size() == 3)
			fillForceLoad = strAsBool(params.get(2));

		// set world if it was specified
		if (worldName != null)
			fillWorld = worldName;

		if (confirm)
		{	// command confirmed, go ahead with it
			if (fillWorld.isEmpty())
			{
				sendErrorAndHelp(sender, "You must first use this command successfully without confirming.");
				return;
			}

			if (player != null)
				Config.log("Filling out world to border at the command of player \"" + player.getName() + "\".");

			int ticks = 1, repeats = 1;
			if (fillFrequency > 20)
				repeats = fillFrequency / 20;
			else
				ticks = 20 / fillFrequency;

/*	*/		Config.log("world: " + fillWorld + "  padding: " + fillPadding + "  repeats: " + repeats + "  ticks: " + ticks);			
			Config.fillTask = new WorldFillTask(Bukkit.getServer(), player, fillWorld, fillPadding, repeats, ticks, fillForceLoad);
			if (Config.fillTask.valid())
			{
				int task = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(WorldBorder.plugin, Config.fillTask, ticks, ticks);
				Config.fillTask.setTaskID(task);
				sender.sendMessage("WorldBorder map generation task for world \"" + fillWorld + "\" started.");
			}
			else
				sender.sendMessage(clrErr + "The world map generation task failed to start.");

			fillDefaults();
		}
		else
		{
			if (fillWorld.isEmpty())
			{
				sendErrorAndHelp(sender, "You must first specify a valid world.");
				return;
			}

			sender.sendMessage(clrHead + "World generation task is ready for world \"" + fillWorld + "\", attempting to process up to " + fillFrequency + " chunks per second (default 20). The map will be padded out " + fillPadding + " blocks beyond the border (default " + defaultPadding + "). Parts of the world which are already fully generated will be " + (fillForceLoad ? "loaded anyway." : "skipped."));
			sender.sendMessage(clrHead + "This process can take a very long time depending on the world's border size. Also, depending on the chunk processing rate, players will likely experience severe lag for the duration.");
			sender.sendMessage(clrDesc + "You should now use " + cmd + "confirm" + clrDesc + " to start the process.");
			sender.sendMessage(clrDesc + "You can cancel at any time with " + cmd + "cancel" + clrDesc + ", or pause/unpause with " + cmd + "pause" + clrDesc + ".");
		}
	}
	

	/* with "view-distance=10" in server.properties on a fast VM test server and "Render Distance: Far" in client,
	 * hitting border during testing was loading 11+ chunks beyond the border in a couple of directions (10 chunks in
	 * the other two directions). This could be worse on a more loaded or worse server, so:
	 */
	private final int defaultPadding = CoordXZ.chunkToBlock(13);

	private String fillWorld = "";
	private int fillFrequency = 20;
	private int fillPadding = defaultPadding;
	private boolean fillForceLoad = false;

	private void fillDefaults()
	{
		fillWorld = "";
		fillFrequency = 20;
		fillPadding = defaultPadding;
		fillForceLoad = false;
	}

	private boolean makeSureFillIsRunning(CommandSender sender)
	{
		if (Config.fillTask != null && Config.fillTask.valid())
			return true;
		sendErrorAndHelp(sender, "The world map generation task is not currently running.");
		return false;
	}
}
