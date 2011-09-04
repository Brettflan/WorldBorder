package com.wimbli.WorldBorder;

import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.Server;
import org.bukkit.World;


public class WorldFillTask implements Runnable
{
	// general task-related reference data
	private transient Server server = null;
	private transient World world = null;
	private transient BorderData border = null;
	private transient boolean readyToGo = false;
	private transient boolean paused = false;
	private transient boolean pausedForMemory = false;
	private transient int taskID = -1;
	private transient Player notifyPlayer = null;
	private transient int chunksPerRun = 1;
	private transient boolean continueNotice = false;
	
	// these are only stored for saving task to config
	private transient int fillDistance = 176;
	private transient int tickFrequency = 1;
	private transient int refX = 0, lastLegX = 0;
	private transient int refZ = 0, lastLegZ = 0;
	private transient int refLength = -1;
	private transient int refTotal = 0, lastLegTotal = 0;

	// values for the spiral pattern check which fills out the map to the border
	private transient int x = 0;
	private transient int z = 0;
	private transient boolean isZLeg = false;
	private transient boolean isNeg = false;
	private transient int length = -1;
	private transient int current = 0;
	private transient boolean insideBorder = true;
	private List<CoordXZ> storedChunks = new LinkedList<CoordXZ>();
	private Set<CoordXZ> originalChunks = new HashSet<CoordXZ>();

	// for reporting progress back to user occasionally
	private transient long lastReport = Config.Now();
	private transient int reportTarget = 0;
	private transient int reportTotal = 0;
	private transient int reportNum = 0;


	public WorldFillTask(Server theServer, Player player, String worldName, int fillDistance, int chunksPerRun, int tickFrequency)
	{
		this.server = theServer;
		this.notifyPlayer = player;
		this.fillDistance = fillDistance;
		this.tickFrequency = tickFrequency;
		this.chunksPerRun = chunksPerRun;

		this.world = server.getWorld(worldName);
		if (this.world == null)
		{
			if (worldName.isEmpty())
				sendMessage("You must specify a world!");
			else
				sendMessage("World \"" + worldName + "\" not found!");
			this.stop();
			return;
		}

		this.border = (Config.Border(worldName) == null) ? null : Config.Border(worldName).copy();
		if (this.border == null)
		{
			sendMessage("No border found for world \"" + worldName + "\"!");
			this.stop();
			return;
		}

		this.border.setRadius(border.getRadius() + fillDistance);
		this.x = CoordXZ.blockToChunk((int)border.getX());
		this.z = CoordXZ.blockToChunk((int)border.getZ());

		int chunkWidth = (int) Math.ceil((double)((border.getRadius() + 16) * 2) / 16);
		this.reportTarget = (chunkWidth * chunkWidth) + chunkWidth + 1;

		// keep track of the chunks which are already loaded when the task starts, to not unload them
		Chunk[] originals = world.getLoadedChunks();
		for (Chunk original : originals)
		{
			originalChunks.add(new CoordXZ(original.getX(), original.getZ()));
		}

		this.readyToGo = true;
	}

	public void setTaskID(int ID)
	{	
		if (ID == -1) this.stop();
		this.taskID = ID;
	}


	public void run()
	{
		if (continueNotice)
		{	// notify user that task has continued automatically
			continueNotice = false;
			String clrCmd = ChatColor.AQUA.toString();
			String clrDesc = ChatColor.WHITE.toString();
			sendMessage("World map generation task automatically continuing.");
			sendMessage("Reminder: you can cancel at any time with " + clrCmd + "wb fill cancel" + clrDesc + ", or pause/unpause with " + clrCmd + "wb fill pause" + clrDesc + ".");
		}

		if (pausedForMemory)
		{	// if available memory gets too low, we automatically pause, so handle that
			if (Config.AvailableMemory() < 500)
				return;

			pausedForMemory = false;
			sendMessage("Available memory is sufficient, automatically continuing.");
		}

		if (server == null || !readyToGo || paused)
			return;

		// this is set so it only does one iteration at a time, no matter how frequently the timer fires
		readyToGo = false;

		for (int loop = 0; loop < chunksPerRun; loop++)
		{
			// in case the task has been paused while we're repeating...
			if (paused)
				return;

			// every 5 seconds or so, give basic progress report to let user know how it's going
			if (Config.Now() > lastReport + 5000)
				reportProgress();

			// if we've made it at least partly outside the border, skip past any such chunks
			while (!border.insideBorder(CoordXZ.chunkToBlock(x) + 8, CoordXZ.chunkToBlock(z) + 8))
			{
				if (!moveToNext())
					return;
			}
			insideBorder = true;

			// skip past any chunks which are currently loaded (they're definitely already generated)
			while (world.isChunkLoaded(x, z))
			{
				insideBorder = true;
				if (!moveToNext())
					return;
			}

			// load the target chunk and generate it if necessary (no way to check if chunk has been generated first, simply have to load it)
			world.loadChunk(x, z, true);

			// There need to be enough nearby chunks loaded to make the server populate a chunk with trees, snow, etc.
			// So, we keep the last few chunks loaded, and need to also temporarily load an extra inside chunk (neighbor closest to center of map)
			int popX = !isZLeg ? x : (x + (isNeg ? -1 : 1));
			int popZ = isZLeg ? z : (z + (!isNeg ? -1 : 1));
			world.loadChunk(popX, popZ, false);

			// Store the coordinates of these latest 2 chunks we just loaded, so we can unload them after a bit...
			storedChunks.add(new CoordXZ(x, z));
			storedChunks.add(new CoordXZ(popX, popZ));

			// If enough stored chunks are buffered in, go ahead and unload the oldest to free up memory
			if (storedChunks.size() > 6)
			{
				CoordXZ coord = storedChunks.remove(0);
				if (!originalChunks.contains(coord))
					world.unloadChunkRequest(coord.x, coord.z);
				coord = storedChunks.remove(0);
				if (!originalChunks.contains(coord))
					world.unloadChunkRequest(coord.x, coord.z);
			}

			// move on to next chunk
			if (!moveToNext())
				return;
		}

		// ready for the next iteration to run
		readyToGo = true;
	}

	// step through chunks in spiral pattern from center; returns false if we're done, otherwise returns true
	public boolean moveToNext()
	{
		reportNum++;

		// keep track of progress in case we need to save to config for restoring progress after server restart
		if (!isNeg && current == 0 && length > 3)
		{
			if (!isZLeg)
			{
				lastLegX = x;
				lastLegZ = z;
				lastLegTotal = reportTotal + reportNum;
			} else {
				refX = lastLegX;
				refZ = lastLegZ;
				refTotal = lastLegTotal;
				refLength = length - 1;
			}
		}

		// make sure of the direction we're moving (X or Z? negative or positive?)
		if (current < length)
			current++;
		else
		{	// one leg/side of the spiral down...
			current = 0;
			isZLeg ^= true;
			if (isZLeg)
			{	// every second leg (between X and Z legs, negative or positive), length increases
				isNeg ^= true;
				length++;
			}
		}

		// move one chunk further in the appropriate direction
		if (isZLeg)
			z += (isNeg) ? -1 : 1;
		else
			x += (isNeg) ? -1 : 1;

		// if we've been around one full loop (4 legs)...
		if (isZLeg && isNeg && current == 0)
		{	// see if we've been outside the border for the whole loop
			if (insideBorder == false)
			{	// and finish if so
				finish();
				return false;
			}	// otherwise, reset the "inside border" flag
			else
				insideBorder = false;
		}
		return true;

	/* reference diagram used, should move in this pattern:
	 *  8 [>][>][>][>][>] etc.
	 * [^][6][>][>][>][>][>][6]
	 * [^][^][4][>][>][>][4][v]
	 * [^][^][^][2][>][2][v][v]
	 * [^][^][^][^][0][v][v][v]
	 * [^][^][^][1][1][v][v][v]
	 * [^][^][3][<][<][3][v][v]
	 * [^][5][<][<][<][<][5][v]
	 * [7][<][<][<][<][<][<][7]
	 */
	}

	// for successful completion
	public void finish()
	{
		this.paused = true;
		reportProgress();
		world.save();
		sendMessage("task successfully completed!");
		this.stop();
	}

	// for cancelling prematurely
	public void cancel()
	{
		this.stop();
	}

	// we're done, whether finished or cancelled
	private void stop()
	{
		if (server == null)
			return;

		readyToGo = false;
		if (taskID != -1)
			server.getScheduler().cancelTask(taskID);
		server = null;

		// go ahead and unload any chunks we still have loaded
		while(!storedChunks.isEmpty())
		{
			CoordXZ coord = storedChunks.remove(0);
			if (!originalChunks.contains(coord))
				world.unloadChunkRequest(coord.x, coord.z);
		}

	}

	// is this task still valid/workable?
	public boolean valid()
	{
		return this.server != null;
	}

	// handle pausing/unpausing the task
	public void pause()
	{
		if(this.pausedForMemory)
			pause(false);
		else
			pause(!this.paused);
	}
	public void pause(boolean pause)
	{
		if (this.pausedForMemory && !pause)
			this.pausedForMemory = false;
		else
			this.paused = pause;
		if (this.paused)
		{
			Config.StoreFillTask();
			reportProgress();
		}
		else
			Config.UnStoreFillTask();
	}
	public boolean isPaused()
	{
		return this.paused || this.pausedForMemory;
	}

	// let the user know how things are coming along
	private void reportProgress()
	{
		lastReport = Config.Now();
		double perc = ((double)(reportTotal + reportNum) / (double)reportTarget) * 100;
		sendMessage(reportNum + " more map chunks processed (" + (reportTotal + reportNum) + " total, " + ChatColor.GREEN.toString() + Config.coord.format(perc) + "%" + ChatColor.WHITE.toString() + ")");
		reportTotal += reportNum;
		reportNum = 0;

		// try to keep memory usage in check and keep things speedy as much as possible...
		world.save();
	}

	// send a message to the server console/log and possibly to an in-game player
	private void sendMessage(String text)
	{
		// Due to the apparent chunk generation memory leak, we need to track memory availability
		int availMem = Config.AvailableMemory();

		Config.Log("[Fill] " + text + ChatColor.GOLD.toString() + " (free mem: " + availMem + " MB)");
		if (notifyPlayer != null)
			notifyPlayer.sendMessage("[Fill] " + text);

		if (availMem < 200)
		{	// running low on memory, auto-pause
			pausedForMemory = true;
			Config.StoreFillTask();
			text = "Available memory is very low, task is pausing. Will automatically continue if/when sufficient memory is freed up.\n Alternately, if you restart the server, this task will automatically continue once the server is back up.";
			Config.Log("[Fill] " + text);
			if (notifyPlayer != null)
				notifyPlayer.sendMessage("[Fill] " + text);
		}
	}

	// stuff for saving / restoring progress
	public void continueProgress(int x, int z, int length, int totalDone)
	{
		this.x = x;
		this.z = z;
		this.length = length;
		this.reportTotal = totalDone;
		this.continueNotice = true;
	}
	public int refX()
	{
		return refX;
	}
	public int refZ()
	{
		return refZ;
	}
	public int refLength()
	{
		return refLength;
	}
	public int refTotal()
	{
		return refTotal;
	}
	public int refFillDistance()
	{
		return fillDistance;
	}
	public int refTickFrequency()
	{
		return tickFrequency;
	}
	public int refChunksPerRun()
	{
		return chunksPerRun;
	}
	public String refWorld()
	{
		return world.getName();
	}
}
