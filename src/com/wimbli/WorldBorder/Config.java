package com.wimbli.WorldBorder;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;


public class Config
{
	// private stuff used within this class
	private static WorldBorder plugin;
	private static FileConfiguration cfg = null;
	private static final Logger mcLog = Logger.getLogger("Minecraft");
	public static DecimalFormat coord = new DecimalFormat("0.0");
	private static int borderTask = -1;
	public static WorldFillTask fillTask;
	public static WorldTrimTask trimTask;
	private static Set<String> bypassPlayers = Collections.synchronizedSet(new LinkedHashSet<String>());
	private static Runtime rt = Runtime.getRuntime();
	
	// actual configuration values which can be changed
	private static boolean shapeRound = true;
	private static Map<String, BorderData> borders = Collections.synchronizedMap(new LinkedHashMap<String, BorderData>());
	private static String message;
	private static boolean DEBUG = false;
	private static double knockBack = 3.0;
	private static int timerTicks = 4;
	private static boolean whooshEffect = false;
	private static boolean dynmapEnable = true;
	private static String dynmapMessage;

	// for monitoring plugin efficiency
//	public static long timeUsed = 0;

	public static long Now()
	{
		return System.currentTimeMillis();
	}

	public static void setBorder(String world, BorderData border)
	{
		borders.put(world, border);
		Log("Border set. " + BorderDescription(world));
		save(true);
		DynMapFeatures.showBorder(world, border);
	}
	public static void setBorder(String world, int radius, double x, double z, Boolean shapeRound)
	{
		setBorder(world, new BorderData(x, z, radius, shapeRound));
	}
	public static void setBorder(String world, int radius, double x, double z)
	{
		BorderData old = Border(world);
		Boolean oldShape = (old == null) ? null : old.getShape();
		setBorder(world, new BorderData(x, z, radius, oldShape));
	}

	public static void removeBorder(String world)
	{
		borders.remove(world);
		Log("Removed border for world \"" + world + "\".");
		save(true);
		DynMapFeatures.removeBorder(world);
	}

	public static void removeAllBorders()
	{
		borders.clear();
		Log("Removed all borders for all worlds.");
		save(true);
		DynMapFeatures.removeAllBorders();
	}

	public static String BorderDescription(String world)
	{
		BorderData border = borders.get(world);
		if (border == null)
			return "No border was found for the world \"" + world + "\".";
		else
			return "World \"" + world + "\" has border " + border.toString();
	}

	public static Set<String> BorderDescriptions()
	{
		Set<String> output = new HashSet<String>();

		Iterator world = borders.keySet().iterator();
		while(world.hasNext())
		{
			output.add( BorderDescription((String)world.next()) );
		}

		return output;
	}

	public static BorderData Border(String world)
	{
		return borders.get(world);
	}

	public static Map<String, BorderData> getBorders()
	{
		return new LinkedHashMap<String, BorderData>(borders);
	}

	public static void setMessage(String msg)
	{
		message = msg;
		Log("Border message is now set to: " + msg);
		save(true);
	}

	public static String Message()
	{
		return message;
	}

	public static void setShape(boolean round)
	{
		shapeRound = round;
		Log("Set default border shape to " + (round ? "round" : "square") + ".");
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static boolean ShapeRound()
	{
		return shapeRound;
	}

	public static void setDebug(boolean debugMode)
	{
		DEBUG = debugMode;
		Log("Debug mode " + (DEBUG ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean Debug()
	{
		return DEBUG;
	}

	public static void setWhooshEffect(boolean enable)
	{
		whooshEffect = enable;
		Log("\"Whoosh\" knockback effect " + (whooshEffect ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean whooshEffect()
	{
		return whooshEffect;
	}

	public static void setKnockBack(double numBlocks)
	{
		knockBack = numBlocks;
		Log("Knockback set to " + knockBack + " blocks inside the border.");
		save(true);
	}

	public static double KnockBack()
	{
		return knockBack;
	}

	public static void setTimerTicks(int ticks)
	{
		timerTicks = ticks;
		Log("Timer delay set to " + timerTicks + " tick(s). That is roughly " + (timerTicks * 50) + "ms / " + (((double)timerTicks * 50.0) / 1000.0) + " seconds.");
		StartBorderTimer();
		save(true);
	}

	public static int TimerTicks()
	{
		return timerTicks;
	}


	public static void setDynmapBorderEnabled(boolean enable)
	{
		dynmapEnable = enable;
		Log("DynMap border display is now " + (enable ? "enabled" : "disabled") + ".");
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static boolean DynmapBorderEnabled()
	{
		return dynmapEnable;
	}

	public static void setDynmapMessage(String msg)
	{
		dynmapMessage = msg;
		Log("DynMap border label is now set to: " + msg);
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static String DynmapMessage()
	{
		return dynmapMessage;
	}

	public static void setPlayerBypass(String player, boolean bypass)
	{
		if (bypass)
			bypassPlayers.add(player.toLowerCase());
		else
			bypassPlayers.remove(player.toLowerCase());
	}

	public static boolean isPlayerBypassing(String player)
	{
		return bypassPlayers.contains(player.toLowerCase());
	}

	public static void togglePlayerBypass(String player)
	{
		setPlayerBypass(player, !isPlayerBypassing(player));
	}



	public static boolean isBorderTimerRunning()
	{
		if (borderTask == -1) return false;
		return (plugin.getServer().getScheduler().isQueued(borderTask) || plugin.getServer().getScheduler().isCurrentlyRunning(borderTask));
	}

	public static void StartBorderTimer()
	{
		StopBorderTimer();

		borderTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new BorderCheckTask(plugin.getServer()), timerTicks, timerTicks);

		if (borderTask == -1)
			LogWarn("Failed to start timed border-checking task! This will prevent the plugin from working. Try restarting Bukkit.");

		LogConfig("Border-checking timed task started.");
	}

	public static void StopBorderTimer()
	{
		if (borderTask == -1) return;

		plugin.getServer().getScheduler().cancelTask(borderTask);
		borderTask = -1;
		LogConfig("Border-checking timed task stopped.");
	}


	public static void StopFillTask()
	{
		if (fillTask != null && fillTask.valid())
			fillTask.cancel();
	}

	public static void StoreFillTask()
	{
		save(false, true);
	}
	public static void UnStoreFillTask()
	{
		save(false);
	}

	public static void RestoreFillTask(String world, int fillDistance, int chunksPerRun, int tickFrequency, int x, int z, int length, int total)
	{
		fillTask = new WorldFillTask(plugin.getServer(), null, world, fillDistance, chunksPerRun, tickFrequency);
		if (fillTask.valid())
		{
			fillTask.continueProgress(x, z, length, total);
			int task = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, fillTask, 20, tickFrequency);
			fillTask.setTaskID(task);
		}
	}


	public static void StopTrimTask()
	{
		if (trimTask != null && trimTask.valid())
			trimTask.cancel();
	}


	public static int AvailableMemory()
	{
		return (int)((rt.maxMemory() - rt.totalMemory() + rt.freeMemory()) / 1048576);  // 1024*1024 = 1048576 (bytes in 1 MB)
	}


	public static boolean HasPermission(Player player, String request)
	{
		return HasPermission(player, request, true);
	}
	public static boolean HasPermission(Player player, String request, boolean notify)
	{
		if (player == null)				// console, always permitted
			return true;

		if (player.hasPermission("worldborder." + request))	// built-in Bukkit superperms
			return true;

		if (notify)
			player.sendMessage("You do not have sufficient permissions.");

		return false;
	}


	private static final String logName = "WorldBorder";
	public static void Log(Level lvl, String text)
	{
		mcLog.log(lvl, String.format("[%s] %s", logName, text));
	}
	public static void Log(String text)
	{
		Log(Level.INFO, text);
	}
	public static void LogWarn(String text)
	{
		Log(Level.WARNING, text);
	}
	public static void LogConfig(String text)
	{
		Log(Level.INFO, "[CONFIG] " + text);
	}


	private static final int currentCfgVersion = 5;

	public static void load(WorldBorder master, boolean logIt)
	{	// load config from file
		plugin = master;

		plugin.reloadConfig();
		cfg = plugin.getConfig();

		int cfgVersion = cfg.getInt("cfg-version", currentCfgVersion);

		message = cfg.getString("message");
		shapeRound = cfg.getBoolean("round-border", true);
		DEBUG = cfg.getBoolean("debug-mode", false);
		whooshEffect = cfg.getBoolean("whoosh-effect", false);
		knockBack = cfg.getDouble("knock-back-dist", 3.0);
		timerTicks = cfg.getInt("timer-delay-ticks", 5);
		dynmapEnable = cfg.getBoolean("dynmap-border-enabled", true);
		dynmapMessage = cfg.getString("dynmap-border-message", "The border of the world.");
		LogConfig("Using " + (shapeRound ? "round" : "square") + " border, knockback of " + knockBack + " blocks, and timer delay of " + timerTicks + ".");

		StartBorderTimer();

		borders.clear();

		if (message == null || message.isEmpty())
		{	// store defaults
			LogConfig("Configuration not present, creating new file.");
			message = "You have reached the edge of this world.";
			save(false);
			return;
		}

		ConfigurationSection worlds = cfg.getConfigurationSection("worlds");
		if (worlds != null)
		{
			Set<String> worldNames = worlds.getKeys(false);

			for(String worldName : worldNames)
			{
				ConfigurationSection bord = worlds.getConfigurationSection(worldName);

				// we're swapping "<" to "." at load since periods denote configuration nodes without a working way to change that, so world names with periods wreak havoc and are thus modified for storage
				if (cfgVersion > 3)
					worldName = worldName.replace("<", ".");

				Boolean overrideShape = (Boolean) bord.get("shape-round");
				BorderData border = new BorderData(bord.getDouble("x", 0), bord.getDouble("z", 0), bord.getInt("radius", 0), overrideShape);
				borders.put(worldName, border);
				LogConfig(BorderDescription(worldName));
			}
		}

		// if we have an unfinished fill task stored from a previous run, load it up
		ConfigurationSection storedFillTask = cfg.getConfigurationSection("fillTask");
		if (storedFillTask != null)
		{
			String worldName = storedFillTask.getString("world");
			int fillDistance = storedFillTask.getInt("fillDistance", 176);
			int chunksPerRun = storedFillTask.getInt("chunksPerRun", 5);
			int tickFrequency = storedFillTask.getInt("tickFrequency", 20);
			int fillX = storedFillTask.getInt("x", 0);
			int fillZ = storedFillTask.getInt("z", 0);
			int fillLength = storedFillTask.getInt("length", 0);
			int fillTotal = storedFillTask.getInt("total", 0);
			RestoreFillTask(worldName, fillDistance, chunksPerRun, tickFrequency, fillX, fillZ, fillLength, fillTotal);
			save(false);
		}

		if (logIt)
			LogConfig("Configuration loaded.");

		if (cfgVersion < currentCfgVersion)
			save(false);
	}

	public static void save(boolean logIt)
	{
		save(logIt, false);
	}
	public static void save(boolean logIt, boolean storeFillTask)
	{	// save config to file
		if (cfg == null) return;

		cfg.set("cfg-version", currentCfgVersion);
		cfg.set("message", message);
		cfg.set("round-border", shapeRound);
		cfg.set("debug-mode", DEBUG);
		cfg.set("whoosh-effect", whooshEffect);
		cfg.set("knock-back-dist", knockBack);
		cfg.set("timer-delay-ticks", timerTicks);
		cfg.set("dynmap-border-enabled", dynmapEnable);
		cfg.set("dynmap-border-message", dynmapMessage);

		cfg.set("worlds", null);
		Iterator world = borders.entrySet().iterator();
		while(world.hasNext())
		{
			Entry wdata = (Entry)world.next();
			String name = ((String)wdata.getKey()).replace(".", "<");
			BorderData bord = (BorderData)wdata.getValue();

			cfg.set("worlds." + name + ".x", bord.getX());
			cfg.set("worlds." + name + ".z", bord.getZ());
			cfg.set("worlds." + name + ".radius", bord.getRadius());

			if (bord.getShape() != null)
				cfg.set("worlds." + name + ".shape-round", bord.getShape());
		}

		if (storeFillTask && fillTask != null && fillTask.valid())
		{
			cfg.set("fillTask.world", fillTask.refWorld());
			cfg.set("fillTask.fillDistance", fillTask.refFillDistance());
			cfg.set("fillTask.chunksPerRun", fillTask.refChunksPerRun());
			cfg.set("fillTask.tickFrequency", fillTask.refTickFrequency());
			cfg.set("fillTask.x", fillTask.refX());
			cfg.set("fillTask.z", fillTask.refZ());
			cfg.set("fillTask.length", fillTask.refLength());
			cfg.set("fillTask.total", fillTask.refTotal());
		}
		else
			cfg.set("fillTask", null);

		plugin.saveConfig();

		if (logIt)
			LogConfig("Configuration saved.");
	}
}
