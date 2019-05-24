package com.wimbli.WorldBorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Server;
import org.bukkit.World;

import com.wimbli.WorldBorder.Events.WorldBorderTrimFinishedEvent;
import com.wimbli.WorldBorder.Events.WorldBorderTrimStartEvent;


public class WorldTrimTask implements Runnable
{
	// general task-related reference data
	private transient Server server = null;
	private transient World world = null;
	private transient WorldFileData worldData = null;
	private transient BorderData border = null;
	private transient boolean readyToGo = false;
	private transient boolean paused = false;
	private transient int taskID = -1;
	private transient Player notifyPlayer = null;
	private transient int chunksPerRun = 1;
	
	// values for what chunk in the current region we're at
	private transient int currentRegion = -1;  // region(file) we're at in regionFiles
	private transient int regionX = 0;  // X location value of the current region
	private transient int regionZ = 0;  // X location value of the current region
	private transient int currentChunk = 0;  // chunk we've reached in the current region (regionChunks)
	private transient List<CoordXZ> regionChunks = new ArrayList<CoordXZ>(1024);
	private transient List<CoordXZ> trimChunks = new ArrayList<CoordXZ>(1024);
	private transient int counter = 0;

	// for reporting progress back to user occasionally
	private transient long lastReport = Config.Now();
	private transient int reportTarget = 0;
	private transient int reportTotal = 0;
	private transient int reportTrimmedRegions = 0;
	private transient int reportTrimmedChunks = 0;


	public WorldTrimTask(Server theServer, Player player, String worldName, int trimDistance, int chunksPerRun)
	{
		this.server = theServer;
		this.notifyPlayer = player;
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

		this.border.setRadiusX(border.getRadiusX() + trimDistance);
		this.border.setRadiusZ(border.getRadiusZ() + trimDistance);

		worldData = WorldFileData.create(world, notifyPlayer);
		if (worldData == null)
		{
			this.stop();
			return;
		}

		// each region file covers up to 1024 chunks; with all operations we might need to do, let's figure 3X that
		this.reportTarget = worldData.regionFileCount() * 3072;

		// queue up the first file
		if (!nextFile())
			return;

		this.readyToGo = true;
		Bukkit.getServer().getPluginManager().callEvent(new WorldBorderTrimStartEvent(this));
	}

	public void setTaskID(int ID)
	{
		this.taskID = ID;
	}


	public void run()
	{
		if (server == null || !readyToGo || paused)
			return;

		// this is set so it only does one iteration at a time, no matter how frequently the timer fires
		readyToGo = false;
		// and this is tracked to keep one iteration from dragging on too long and possibly choking the system if the user specified a really high frequency
		long loopStartTime = Config.Now();

		counter = 0;
		while (counter <= chunksPerRun)
		{
			// in case the task has been paused while we're repeating...
			if (paused)
				return;

			long now = Config.Now();

			// every 5 seconds or so, give basic progress report to let user know how it's going
			if (now > lastReport + 5000)
				reportProgress();

			// if this iteration has been running for 45ms (almost 1 tick) or more, stop to take a breather; shouldn't normally be possible with Trim, but just in case
			if (now > loopStartTime + 45)
			{
				readyToGo = true;
				return;
			}

			if (regionChunks.isEmpty())
				addCornerChunks();
			else if (currentChunk == 4)
			{	// determine if region is completely _inside_ border based on corner chunks
				if (trimChunks.isEmpty())
				{	// it is, so skip it and move on to next file
					counter += 4;
					nextFile();
					continue;
				}
				addEdgeChunks();
				addInnerChunks();
			}
			else if (currentChunk == 124 && trimChunks.size() == 124)
			{	// region is completely _outside_ border based on edge chunks, so delete file and move on to next
				counter += 16;
				trimChunks = regionChunks;
				unloadChunks();
				reportTrimmedRegions++;
				File regionFile = worldData.regionFile(currentRegion);
				if (!regionFile.delete())
				{
					sendMessage("Error! Region file which is outside the border could not be deleted: "+regionFile.getName());
					wipeChunks();
				}
				else
				{
					// if DynMap is installed, re-render the trimmed region ... disabled since it's not currently working, oh well
//					DynMapFeatures.renderRegion(world.getName(), new CoordXZ(regionX, regionZ));
				}

				nextFile();
				continue;
			}
			else if (currentChunk == 1024)
			{	// last chunk of the region has been checked, time to wipe out whichever chunks are outside the border
				counter += 32;
				unloadChunks();
				wipeChunks();
				nextFile();
				continue;
			}

			// check whether chunk is inside the border or not, add it to the "trim" list if not
			CoordXZ chunk = regionChunks.get(currentChunk);
			if (!isChunkInsideBorder(chunk))
				trimChunks.add(chunk);

			currentChunk++;
			counter++;
		}

		reportTotal += counter;

		// ready for the next iteration to run
		readyToGo = true;
	}

	// Advance to the next region file. Returns true if successful, false if the next file isn't accessible for any reason
	private boolean nextFile()
	{
		reportTotal = currentRegion * 3072;
		currentRegion++;
		regionX = regionZ = currentChunk = 0;
		regionChunks = new ArrayList<CoordXZ>(1024);
		trimChunks = new ArrayList<CoordXZ>(1024);

		// have we already handled all region files?
		if (currentRegion >= worldData.regionFileCount())
		{	// hey, we're done
			paused = true;
			readyToGo = false;
			finish();
			return false;
		}

		counter += 16;

		// get the X and Z coordinates of the current region
		CoordXZ coord = worldData.regionFileCoordinates(currentRegion);
		if (coord == null)
			return false;

		regionX = coord.x;
		regionZ = coord.z;
		return true;
	}

	// add just the 4 corner chunks of the region; can determine if entire region is _inside_ the border 
	private void addCornerChunks()
	{
		regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX), CoordXZ.regionToChunk(regionZ)));
		regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX) + 31, CoordXZ.regionToChunk(regionZ)));
		regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX), CoordXZ.regionToChunk(regionZ) + 31));
		regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX) + 31, CoordXZ.regionToChunk(regionZ) + 31));
	}

	// add all chunks along the 4 edges of the region (minus the corners); can determine if entire region is _outside_ the border 
	private void addEdgeChunks()
	{
		int chunkX = 0, chunkZ;

		for (chunkZ = 1; chunkZ < 31; chunkZ++)
		{
			regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX)+chunkX, CoordXZ.regionToChunk(regionZ)+chunkZ));
		}
		chunkX = 31;
		for (chunkZ = 1; chunkZ < 31; chunkZ++)
		{
			regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX)+chunkX, CoordXZ.regionToChunk(regionZ)+chunkZ));
		}
		chunkZ = 0;
		for (chunkX = 1; chunkX < 31; chunkX++)
		{
			regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX)+chunkX, CoordXZ.regionToChunk(regionZ)+chunkZ));
		}
		chunkZ = 31;
		for (chunkX = 1; chunkX < 31; chunkX++)
		{
			regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX)+chunkX, CoordXZ.regionToChunk(regionZ)+chunkZ));
		}
		counter += 4;
	}

	// add the remaining interior chunks (after corners and edges)
	private void addInnerChunks()
	{
		for (int chunkX = 1; chunkX < 31; chunkX++)
		{
			for (int chunkZ = 1; chunkZ < 31; chunkZ++)
			{
				regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX)+chunkX, CoordXZ.regionToChunk(regionZ)+chunkZ));
			}
		}
		counter += 32;
	}

	// make sure chunks set to be trimmed are not currently loaded by the server
	private void unloadChunks()
	{
		for (CoordXZ unload : trimChunks)
		{
			if (world.isChunkLoaded(unload.x, unload.z))
				world.unloadChunk(unload.x, unload.z, false);
		}
		counter += trimChunks.size();
	}

	// edit region file to wipe all chunk pointers for chunks outside the border
	private void wipeChunks()
	{
		File regionFile = worldData.regionFile(currentRegion);
		if (!regionFile.canWrite())
		{
			if (!regionFile.setWritable(true))
				throw new RuntimeException();

			if (!regionFile.canWrite())
			{
				sendMessage("Error! region file is locked and can't be trimmed: "+regionFile.getName());
				return;
			}
		}

		// since our stored chunk positions are based on world, we need to offset those to positions in the region file
		int offsetX = CoordXZ.regionToChunk(regionX);
		int offsetZ = CoordXZ.regionToChunk(regionZ);
		long wipePos = 0;
		int chunkCount = 0;

		try
		{
			RandomAccessFile unChunk = new RandomAccessFile(regionFile, "rwd");
			for (CoordXZ wipe : trimChunks)
			{
				// if the chunk pointer is empty (chunk doesn't technically exist), no need to wipe the already empty pointer
				if (!worldData.doesChunkExist(wipe.x, wipe.z))
					continue;

				// wipe this extraneous chunk's pointer... note that this method isn't perfect since the actual chunk data is left orphaned,
				// but Minecraft will overwrite the orphaned data sector if/when another chunk is created in the region, so it's not so bad
				wipePos = 4 * ((wipe.x - offsetX) + ((wipe.z - offsetZ) * 32));
				unChunk.seek(wipePos);
				unChunk.writeInt(0);
				chunkCount++;
			}
			unChunk.close();

			// if DynMap is installed, re-render the trimmed chunks ... disabled since it's not currently working, oh well
//			DynMapFeatures.renderChunks(world.getName(), trimChunks);

			reportTrimmedChunks += chunkCount;
		}
		catch (FileNotFoundException ex)
		{
			sendMessage("Error! Could not open region file to wipe individual chunks: "+regionFile.getName());
		}
		catch (IOException ex)
		{
			sendMessage("Error! Could not modify region file to wipe individual chunks: "+regionFile.getName());
		}
		counter += trimChunks.size();
	}

	private boolean isChunkInsideBorder(CoordXZ chunk)
	{
		return border.insideBorder(CoordXZ.chunkToBlock(chunk.x) + 8, CoordXZ.chunkToBlock(chunk.z) + 8);
	}

	// for successful completion
	public void finish()
	{
		reportTotal = reportTarget;
		reportProgress();
		Bukkit.getServer().getPluginManager().callEvent(new WorldBorderTrimFinishedEvent(world, reportTotal));
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

		sendMessage("NOTICE: it is recommended that you restart your server after a Trim, to be on the safe side.");
		if (DynMapFeatures.renderEnabled())
			sendMessage("This especially true with DynMap. You should also run a fullrender in DynMap for the trimmed world after restarting, so trimmed chunks are updated on the map.");
	}

	// is this task still valid/workable?
	public boolean valid()
	{
		return this.server != null;
	}

	// handle pausing/unpausing the task
	public void pause()
	{
		pause(!this.paused);
	}
	public void pause(boolean pause)
	{
		this.paused = pause;
		if (pause)
			reportProgress();
	}
	public boolean isPaused()
	{
		return this.paused;
	}

	// let the user know how things are coming along
	private void reportProgress()
	{
		lastReport = Config.Now();
		double perc = getPercentageCompleted();
		sendMessage(reportTrimmedRegions + " entire region(s) and " + reportTrimmedChunks + " individual chunk(s) trimmed so far (" + Config.coord.format(perc) + "% done" + ")");
	}

	// send a message to the server console/log and possibly to an in-game player
	private void sendMessage(String text)
	{
		Config.log("[Trim] " + text);
		if (notifyPlayer != null)
			notifyPlayer.sendMessage("[Trim] " + text);
	}
	
	/**
	 * Get the percentage completed for the trim task.
	 * 
	 * @return Percentage
	 */
	public double getPercentageCompleted() {
		return ((double) (reportTotal) / (double) reportTarget) * 100;
	}

	/**
	 * Amount of chunks completed for the trim task.
	 * 
	 * @return Number of chunks processed.
	 */
	public int getChunksCompleted() {
		return reportTotal;
	}

	/**
	 * Total amount of chunks that need to be trimmed for the trim task.
	 * 
	 * @return Number of chunks that need to be processed.
	 */
	public int getChunksTotal() {
		return reportTarget;
	}
}
