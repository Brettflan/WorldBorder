package com.wimbli.WorldBorder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.Server;
import org.bukkit.World;


public class WorldTrimTask implements Runnable
{
	// general task-related reference data
	private transient Server server = null;
	private transient World world = null;
	private transient File regionFolder = null;
	private transient File[] regionFiles = null;
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

		this.border.setRadius(border.getRadius() + trimDistance);

		regionFolder = new File(this.world.getWorldFolder(), "region");
		if (!regionFolder.exists() || !regionFolder.isDirectory())
		{
			String mainRegionFolder = regionFolder.getPath();
			regionFolder = new File(this.world.getWorldFolder(), "DIM-1"+File.separator+"region");  // nether worlds
			if (!regionFolder.exists() || !regionFolder.isDirectory())
			{
				String subRegionFolder = regionFolder.getPath();
				regionFolder = new File(this.world.getWorldFolder(), "DIM1"+File.separator+"region");  // "the end" worlds; not sure why "DIM1" vs "DIM-1", but that's how it is
				if (!regionFolder.exists() || !regionFolder.isDirectory())
				{
					sendMessage("Could not validate folder for world's region files. Looked in: "+mainRegionFolder+" -and- "+subRegionFolder+" -and- "+regionFolder.getPath());
					this.stop();
					return;
				}
			}
		}
		regionFiles = regionFolder.listFiles(new RegionFileFilter());

		if (regionFiles == null || regionFiles.length == 0)
		{
			sendMessage("Could not find any region files. Looked in: "+regionFolder.getPath());
			this.stop();
			return;
		}

		// each region file covers up to 1024 chunks; with all operations we might need to do, let's figure 3X that
		this.reportTarget = regionFiles.length * 3072;

		// queue up the first file
		if (!nextFile())
			return;

		this.readyToGo = true;
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

		counter = 0;
		while (counter <= chunksPerRun)
		{
			// in case the task has been paused while we're repeating...
			if (paused)
				return;

			// every 5 seconds or so, give basic progress report to let user know how it's going
			if (Config.Now() > lastReport + 5000)
				reportProgress();

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
				if (!regionFiles[currentRegion].delete())
				{
					sendMessage("Error! Region file which is outside the border could not be deleted: "+regionFiles[currentRegion].getName());
					wipeChunks();
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
		if (currentRegion >= regionFiles.length)
		{	// hey, we're done
			paused = true;
			readyToGo = false;
			finish();
			return false;
		}

		counter += 16;

		// get the X and Z coordinates of the current region from the filename
		String[] coords = regionFiles[currentRegion].getName().split("\\.");
		try
		{
			regionX = Integer.parseInt(coords[1]);
			regionZ = Integer.parseInt(coords[2]);
		}
		catch(Exception ex)
		{
			sendMessage("Error! Region file found with abnormal name: "+regionFiles[currentRegion].getName());
			return false;
		}

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
				world.unloadChunk(unload.x, unload.z, false, false);
		}
		counter += trimChunks.size();
	}

	// edit region file to wipe all chunk pointers for chunks outside the border
	// by the way, this method was created based on the divulged region file format: http://mojang.com/2011/02/16/minecraft-save-file-format-in-beta-1-3/
	private void wipeChunks()
	{
		if (!regionFiles[currentRegion].canWrite())
		{
			regionFiles[currentRegion].setWritable(true);
			if (!regionFiles[currentRegion].canWrite())
			{
				sendMessage("Error! region file is locked and can't be trimmed: "+regionFiles[currentRegion].getName());
				return;
			}
		}

		// since our stored chunk positions are based on world, we need to offset those to positions in the region file
		int offsetX = CoordXZ.regionToChunk(regionX);
		int offsetZ = CoordXZ.regionToChunk(regionZ);
		long wipePos = 0;

		try
		{
			RandomAccessFile unChunk = new RandomAccessFile(regionFiles[currentRegion], "rwd");
			for (CoordXZ wipe : trimChunks)
			{	// wipe this extraneous chunk's pointer... note that this method isn't perfect since the actual chunk data is left orphaned,
				// but Minecraft will overwrite the orphaned data sector if/when another chunk is created in the region, so it's not so bad
				wipePos = 4 * ((wipe.x - offsetX) + ((wipe.z - offsetZ) * 32));
				unChunk.seek(wipePos);
				unChunk.writeInt(0);
			}
			unChunk.close();
			reportTrimmedChunks += trimChunks.size();
		}
		catch (FileNotFoundException ex)
		{
			sendMessage("Error! Could not open region file to wipe individual chunks: "+regionFiles[currentRegion].getName());
		}
		catch (IOException ex)
		{
			sendMessage("Error! Could not modify region file to wipe individual chunks: "+regionFiles[currentRegion].getName());
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
		double perc = ((double)(reportTotal) / (double)reportTarget) * 100;
		sendMessage(reportTrimmedRegions + " entire region(s) and " + reportTrimmedChunks + " individual chunk(s) trimmed so far (" + Config.coord.format(perc) + "% done" + ")");
	}

	// send a message to the server console/log and possibly to an in-game player
	private void sendMessage(String text)
	{
		Config.Log("[Trim] " + text);
		if (notifyPlayer != null)
			notifyPlayer.sendMessage("[Trim] " + text);
	}

	// filter for region files
	private static class RegionFileFilter implements FileFilter
	{
		public boolean accept(File file)
		{
			return (
				   file.exists()
				&& file.isFile()
				&& file.getName().toLowerCase().endsWith(".mcr")
				);
		}
	}
}
