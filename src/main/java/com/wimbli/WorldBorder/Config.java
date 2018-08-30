package com.wimbli.WorldBorder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;


public class Config
{
	// private stuff used within this class
	private static WorldBorder plugin;
	private static FileConfiguration cfg = null;
	private static Logger wbLog = null;
	public static volatile DecimalFormat coord = new DecimalFormat("0.0");
	private static int borderTask = -1;
	public static volatile WorldFillTask fillTask = null;
	public static volatile WorldTrimTask trimTask = null;
	private static Runtime rt = Runtime.getRuntime();

	// actual configuration values which can be changed
	private static boolean shapeRound = true;
	private static Map<String, BorderData> borders = Collections.synchronizedMap(new LinkedHashMap<String, BorderData>());
	private static Set<UUID> bypassPlayers = Collections.synchronizedSet(new LinkedHashSet<UUID>());
	private static String message;		// raw message without color code formatting
	private static String messageFmt;	// message with color code formatting ("&" changed to funky sort-of-double-dollar-sign for legitimate color/formatting codes)
	private static String messageClean;	// message cleaned of formatting codes
	private static boolean DEBUG = false;
	private static double knockBack = 3.0;
	private static int timerTicks = 4;
	private static boolean whooshEffect = true;
	private static boolean portalRedirection = true;
	private static boolean dynmapEnable = true;
	private static String dynmapMessage;
	private static int dynmapPriority = 0;
	private static boolean dynmapHideByDefault = false;
	private static int remountDelayTicks = 0;
	private static boolean killPlayer = false;
	private static boolean denyEnderpearl = false;
	private static int fillAutosaveFrequency = 30;
	private static int fillMemoryTolerance = 500;
	private static boolean preventBlockPlace = false;
	private static boolean preventMobSpawn = false;

	// for monitoring plugin efficiency
//	public static long timeUsed = 0;

	public static long Now()
	{
		return System.currentTimeMillis();
	}


	public static void setBorder(String world, BorderData border, boolean logIt)
	{
		borders.put(world, border);
		if (logIt)
			log("Border set. " + BorderDescription(world));
		save(true);
		DynMapFeatures.showBorder(world, border);
	}
	public static void setBorder(String world, BorderData border)
	{
		setBorder(world, border, true);
	}

	public static void setBorder(String world, int radiusX, int radiusZ, double x, double z, Boolean shapeRound)
	{
		BorderData old = Border(world);
		boolean oldWrap = (old != null) && old.getWrapping();
		setBorder(world, new BorderData(x, z, radiusX, radiusZ, shapeRound, oldWrap), true);
	}
	public static void setBorder(String world, int radiusX, int radiusZ, double x, double z)
	{
		BorderData old = Border(world);
		Boolean oldShape = (old == null) ? null : old.getShape();
		boolean oldWrap = (old != null) && old.getWrapping();
		setBorder(world, new BorderData(x, z, radiusX, radiusZ, oldShape, oldWrap), true);
	}


	// backwards-compatible methods from before elliptical/rectangular shapes were supported
	public static void setBorder(String world, int radius, double x, double z, Boolean shapeRound)
	{
		setBorder(world, new BorderData(x, z, radius, radius, shapeRound), true);
	}
	public static void setBorder(String world, int radius, double x, double z)
	{
		setBorder(world, radius, radius, x, z);
	}


	// set border based on corner coordinates
	public static void setBorderCorners(String world, double x1, double z1, double x2, double z2, Boolean shapeRound, boolean wrap)
	{
		double radiusX = Math.abs(x1 - x2) / 2;
		double radiusZ = Math.abs(z1 - z2) / 2;
		double x = ((x1 < x2) ? x1 : x2) + radiusX;
		double z = ((z1 < z2) ? z1 : z2) + radiusZ;
		setBorder(world, new BorderData(x, z, (int)Math.round(radiusX), (int)Math.round(radiusZ), shapeRound, wrap), true);
	}
	public static void setBorderCorners(String world, double x1, double z1, double x2, double z2, Boolean shapeRound)
	{
		setBorderCorners(world, x1, z1, x2, z2, shapeRound, false);
	}
	public static void setBorderCorners(String world, double x1, double z1, double x2, double z2)
	{
		BorderData old = Border(world);
		Boolean oldShape = (old == null) ? null : old.getShape();
		boolean oldWrap = (old != null) && old.getWrapping();
		setBorderCorners(world, x1, z1, x2, z2, oldShape, oldWrap);
	}


	public static void removeBorder(String world)
	{
		borders.remove(world);
		log("Removed border for world \"" + world + "\".");
		save(true);
		DynMapFeatures.removeBorder(world);
	}

	public static void removeAllBorders()
	{
		borders.clear();
		log("Removed all borders for all worlds.");
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

		for(String worldName : borders.keySet())
		{
			output.add(BorderDescription(worldName));
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
		updateMessage(msg);
		save(true);
	}

	public static void updateMessage(String msg)
	{
		message = msg;
		messageFmt = replaceAmpColors(msg);
		messageClean = stripAmpColors(msg);
	}

	public static String Message()
	{
		return messageFmt;
	}
	public static String MessageRaw()
	{
		return message;
	}
	public static String MessageClean()
	{
		return messageClean;
	}

	public static void setShape(boolean round)
	{
		shapeRound = round;
		log("Set default border shape to " + (ShapeName()) + ".");
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static boolean ShapeRound()
	{
		return shapeRound;
	}

	public static String ShapeName()
	{
		return ShapeName(shapeRound);
	}
	public static String ShapeName(Boolean round)
	{
		if (round == null)
			return "default";
		return round ? "elliptic/round" : "rectangular/square";
	}

	public static void setDebug(boolean debugMode)
	{
		DEBUG = debugMode;
		log("Debug mode " + (DEBUG ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean Debug()
	{
		return DEBUG;
	}

	public static void setWhooshEffect(boolean enable)
	{
		whooshEffect = enable;
		log("\"Whoosh\" knockback effect " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean whooshEffect()
	{
		return whooshEffect;
	}

	public static void showWhooshEffect(Location loc)
	{
		if (!whooshEffect())
			return;

		World world = loc.getWorld();
		world.playEffect(loc, Effect.ENDER_SIGNAL, 0);
		world.playEffect(loc, Effect.ENDER_SIGNAL, 0);
		world.playEffect(loc, Effect.SMOKE, 4);
		world.playEffect(loc, Effect.SMOKE, 4);
		world.playEffect(loc, Effect.SMOKE, 4);
		world.playEffect(loc, Effect.GHAST_SHOOT, 0);
	}
	
	public static void setPreventBlockPlace(boolean enable)
	{
		if (preventBlockPlace != enable)
			WorldBorder.plugin.enableBlockPlaceListener(enable);

		preventBlockPlace = enable;
		log("prevent block place " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}
	
	public static void setPreventMobSpawn(boolean enable)
	{
		if (preventMobSpawn != enable)
			WorldBorder.plugin.enableMobSpawnListener(enable);

		preventMobSpawn = enable;
		log("prevent mob spawn " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean preventBlockPlace()
	{
		return preventBlockPlace;
	}

	public static boolean preventMobSpawn()
	{
		return preventMobSpawn;
	}

	public static boolean getIfPlayerKill()
	{
		return killPlayer;
	}

	public static boolean getDenyEnderpearl()
	{
		return denyEnderpearl;
	}

	public static void setDenyEnderpearl(boolean enable)
	{
		denyEnderpearl = enable;
		log("Direct cancellation of ender pearls thrown past the border " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static void setPortalRedirection(boolean enable)
	{
		portalRedirection = enable;
		log("Portal redirection " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean portalRedirection()
	{
		return portalRedirection;
	}

	public static void setKnockBack(double numBlocks)
	{
		knockBack = numBlocks;
		log("Knockback set to " + knockBack + " blocks inside the border.");
		save(true);
	}

	public static double KnockBack()
	{
		return knockBack;
	}

	public static void setTimerTicks(int ticks)
	{
		timerTicks = ticks;
		log("Timer delay set to " + timerTicks + " tick(s). That is roughly " + (timerTicks * 50) + "ms / " + (((double)timerTicks * 50.0) / 1000.0) + " seconds.");
		StartBorderTimer();
		save(true);
	}

	public static int TimerTicks()
	{
		return timerTicks;
	}

	public static void setRemountTicks(int ticks)
	{
		remountDelayTicks = ticks;
		if (remountDelayTicks == 0)
			log("Remount delay set to 0. Players will be left dismounted when knocked back from the border while on a vehicle.");
		else
		{
			log("Remount delay set to " + remountDelayTicks + " tick(s). That is roughly " + (remountDelayTicks * 50) + "ms / " + (((double)remountDelayTicks * 50.0) / 1000.0) + " seconds.");
			if (ticks < 10)
				logWarn("setting the remount delay to less than 10 (and greater than 0) is not recommended. This can lead to nasty client glitches.");
		}
		save(true);
	}

	public static int RemountTicks()
	{
		return remountDelayTicks;
	}

	public static void setFillAutosaveFrequency(int seconds)
	{
		fillAutosaveFrequency = seconds;
		if (fillAutosaveFrequency == 0)
			log("World autosave frequency during Fill process set to 0, disabling it. Note that much progress can be lost this way if there is a bug or crash in the world generation process from Bukkit or any world generation plugin you use.");
		else
			log("World autosave frequency during Fill process set to " + fillAutosaveFrequency + " seconds (rounded to a multiple of 5). New chunks generated by the Fill process will be forcibly saved to disk this often to prevent loss of progress due to bugs or crashes in the world generation process.");
		save(true);
	}

	public static int FillAutosaveFrequency()
	{
		return fillAutosaveFrequency;
	}


	public static void setDynmapBorderEnabled(boolean enable)
	{
		dynmapEnable = enable;
		log("DynMap border display is now " + (enable ? "enabled" : "disabled") + ".");
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
		log("DynMap border label is now set to: " + msg);
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static String DynmapMessage()
	{
		return dynmapMessage;
	}

	public static boolean DynmapHideByDefault()
	{
		return dynmapHideByDefault;
	}

	public static int DynmapPriority()
	{
		return dynmapPriority;
	}

	public static void setPlayerBypass(UUID player, boolean bypass)
	{
		if (bypass)
			bypassPlayers.add(player);
		else
			bypassPlayers.remove(player);
		save(true);
	}

	public static boolean isPlayerBypassing(UUID player)
	{
		return bypassPlayers.contains(player);
	}

	public static ArrayList<UUID> getPlayerBypassList()
	{
		return new ArrayList<>(bypassPlayers);
	}

	// for converting bypass UUID list to/from String list, for storage in config
	private static void importBypassStringList(List<String> strings)
	{
		for (String string: strings)
		{
			bypassPlayers.add(UUID.fromString(string));
		}
	}
	private static ArrayList<String> exportBypassStringList()
	{
		ArrayList<String> strings = new ArrayList<String>();
		for (UUID uuid: bypassPlayers)
		{
			strings.add(uuid.toString());
		}
		return strings;
	}


	public static boolean isBorderTimerRunning()
	{
		if (borderTask == -1) return false;
		return (plugin.getServer().getScheduler().isQueued(borderTask) || plugin.getServer().getScheduler().isCurrentlyRunning(borderTask));
	}

	public static void StartBorderTimer()
	{
		StopBorderTimer(false);

		borderTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new BorderCheckTask(), timerTicks, timerTicks);

		if (borderTask == -1)
			logWarn("Failed to start timed border-checking task! This will prevent the plugin from working. Try restarting Bukkit.");

		logConfig("Border-checking timed task started.");
	}

	public static void StopBorderTimer()
	{
		StopBorderTimer(true);
	}
	public static void StopBorderTimer(boolean logIt)
	{
		if (borderTask == -1) return;

		plugin.getServer().getScheduler().cancelTask(borderTask);
		borderTask = -1;
		if (logIt)
			logConfig("Border-checking timed task stopped.");
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

	public static void RestoreFillTask(String world, int fillDistance, int chunksPerRun, int tickFrequency, int x, int z, int length, int total, boolean forceLoad)
	{
		fillTask = new WorldFillTask(plugin.getServer(), null, world, fillDistance, chunksPerRun, tickFrequency, forceLoad);
		if (fillTask.valid())
		{
			fillTask.continueProgress(x, z, length, total);
			int task = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, fillTask, 20, tickFrequency);
			fillTask.setTaskID(task);
		}
	}
	// for backwards compatibility
	public static void RestoreFillTask(String world, int fillDistance, int chunksPerRun, int tickFrequency, int x, int z, int length, int total)
	{
		RestoreFillTask(world, fillDistance, chunksPerRun, tickFrequency, x, z, length, total, false);
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

	public static boolean AvailableMemoryTooLow()
	{
		return AvailableMemory() < fillMemoryTolerance;
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


	public static String replaceAmpColors (String message)
	{
		return ChatColor.translateAlternateColorCodes('&', message);
	}
	// adapted from code posted by Sleaker
	public static String stripAmpColors (String message)
	{
		return message.replaceAll("(?i)&([a-fk-or0-9])", "");
	}


	public static void log(Level lvl, String text)
	{
		wbLog.log(lvl, text);
	}
	public static void log(String text)
	{
		log(Level.INFO, text);
	}
	public static void logWarn(String text)
	{
		log(Level.WARNING, text);
	}
	public static void logConfig(String text)
	{
		log(Level.INFO, "[CONFIG] " + text);
	}


	private static final int currentCfgVersion = 11;

	public static void load(WorldBorder master, boolean logIt)
	{	// load config from file
		plugin = master;
		wbLog = plugin.getLogger();

		plugin.reloadConfig();
		cfg = plugin.getConfig();

		int cfgVersion = cfg.getInt("cfg-version", currentCfgVersion);

		String msg = cfg.getString("message");
		shapeRound = cfg.getBoolean("round-border", true);
		DEBUG = cfg.getBoolean("debug-mode", false);
		whooshEffect = cfg.getBoolean("whoosh-effect", true);
		portalRedirection = cfg.getBoolean("portal-redirection", true);
		knockBack = cfg.getDouble("knock-back-dist", 3.0);
		timerTicks = cfg.getInt("timer-delay-ticks", 5);
		remountDelayTicks = cfg.getInt("remount-delay-ticks", 0);
		dynmapEnable = cfg.getBoolean("dynmap-border-enabled", true);
		dynmapMessage = cfg.getString("dynmap-border-message", "The border of the world.");
		dynmapHideByDefault = cfg.getBoolean("dynmap-border-hideByDefault", false);
		dynmapPriority = cfg.getInt("dynmap-border-priority", 0);
		logConfig("Using " + (ShapeName()) + " border, knockback of " + knockBack + " blocks, and timer delay of " + timerTicks + ".");
		killPlayer = cfg.getBoolean("player-killed-bad-spawn", false);
		denyEnderpearl = cfg.getBoolean("deny-enderpearl", true);
		fillAutosaveFrequency = cfg.getInt("fill-autosave-frequency", 30);
		importBypassStringList(cfg.getStringList("bypass-list-uuids"));
		fillMemoryTolerance = cfg.getInt("fill-memory-tolerance", 500);
		preventBlockPlace = cfg.getBoolean("prevent-block-place");
		preventMobSpawn = cfg.getBoolean("prevent-mob-spawn");

		StartBorderTimer();

		borders.clear();

		// if empty border message, assume no config
		if (msg == null || msg.isEmpty())
		{	// store defaults
			logConfig("Configuration not present, creating new file.");
			msg = "&cYou have reached the edge of this world.";
			updateMessage(msg);
			save(false);
			return;
		}
		// if loading older config which didn't support color codes in border message, make sure default red color code is added at start of it
		else if (cfgVersion < 8 && !(msg.substring(0, 1).equals("&")))
			updateMessage("&c" + msg);
		// otherwise just set border message
		else
			updateMessage(msg);

		// this option defaulted to false previously, but what it actually does has changed to something that almost everyone should now want by default
		if (cfgVersion < 10)
			denyEnderpearl = true;

		// the border bypass list used to be stored as list of names rather than UUIDs; wipe that old list so the data won't be automatically saved back to the config file again
		if (cfgVersion < 11)
			cfg.set("bypass-list", null);

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

				// backwards compatibility for config from before elliptical/rectangular borders were supported
				if (bord.isSet("radius") && !bord.isSet("radiusX"))
				{
					int radius = bord.getInt("radius");
					bord.set("radiusX", radius);
					bord.set("radiusZ", radius);
				}

				Boolean overrideShape = (Boolean) bord.get("shape-round");
				boolean wrap = bord.getBoolean("wrapping", false);
				BorderData border = new BorderData(bord.getDouble("x", 0), bord.getDouble("z", 0), bord.getInt("radiusX", 0), bord.getInt("radiusZ", 0), overrideShape, wrap);
				borders.put(worldName, border);
				logConfig(BorderDescription(worldName));
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
			boolean forceLoad = storedFillTask.getBoolean("forceLoad", false);
			RestoreFillTask(worldName, fillDistance, chunksPerRun, tickFrequency, fillX, fillZ, fillLength, fillTotal, forceLoad);
			save(false);
		}

		if (logIt)
			logConfig("Configuration loaded.");

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
		cfg.set("portal-redirection", portalRedirection);
		cfg.set("knock-back-dist", knockBack);
		cfg.set("timer-delay-ticks", timerTicks);
		cfg.set("remount-delay-ticks", remountDelayTicks);
		cfg.set("dynmap-border-enabled", dynmapEnable);
		cfg.set("dynmap-border-message", dynmapMessage);
		cfg.set("dynmap-border-hideByDefault", dynmapHideByDefault);
		cfg.set("dynmap-border-priority", dynmapPriority);
		cfg.set("player-killed-bad-spawn", killPlayer);
		cfg.set("deny-enderpearl", denyEnderpearl);
		cfg.set("fill-autosave-frequency", fillAutosaveFrequency);
		cfg.set("bypass-list-uuids", exportBypassStringList());
		cfg.set("fill-memory-tolerance", fillMemoryTolerance);
		cfg.set("prevent-block-place", preventBlockPlace);
		cfg.set("prevent-mob-spawn", preventMobSpawn);

		cfg.set("worlds", null);
		for(Entry<String, BorderData> stringBorderDataEntry : borders.entrySet())
		{
			String name = stringBorderDataEntry.getKey().replace(".", "<");
			BorderData bord = stringBorderDataEntry.getValue();

			cfg.set("worlds." + name + ".x", bord.getX());
			cfg.set("worlds." + name + ".z", bord.getZ());
			cfg.set("worlds." + name + ".radiusX", bord.getRadiusX());
			cfg.set("worlds." + name + ".radiusZ", bord.getRadiusZ());
			cfg.set("worlds." + name + ".wrapping", bord.getWrapping());

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
			cfg.set("fillTask.forceLoad", fillTask.refForceLoad());
		}
		else
			cfg.set("fillTask", null);

		plugin.saveConfig();

		if (logIt)
			logConfig("Configuration saved.");
	}
}
