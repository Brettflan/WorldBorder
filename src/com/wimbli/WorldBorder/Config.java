package com.wimbli.WorldBorder;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import org.anjocaido.groupmanager.GroupManager;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;


public class Config
{
	// private stuff used within this class
	private static WorldBorder plugin;
	private static Configuration cfg = null;
	private static PermissionHandler Permissions = null;
	private static GroupManager GroupPlugin = null;
	private static final Logger mcLog = Logger.getLogger("Minecraft");
	public static DecimalFormat coord = new DecimalFormat("0.0");

	// actual configuration values which can be changed
	private static boolean shapeRound = false;
	private static Map<String, BorderData> borders = Collections.synchronizedMap(new HashMap<String, BorderData>());
	private static String message;
	private static boolean DEBUG = false;

	public static void setBorder(String world, BorderData border)
	{
		borders.put(world, border);
		Log("Border set. " + BorderDescription(world));
		save(true);
	}
	public static void setBorder(String world, int radius, double x, double z)
	{
		setBorder(world, new BorderData(x, z, radius));
	}

	public static void removeBorder(String world)
	{
		borders.remove(world);
		Log("Removed border for world \"" + world + "\".");
		save(true);
	}

	public static void removeAllBorders()
	{
		borders.clear();
		Log("Removed all borders for all worlds.");
		save(true);
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
		Log("Set border shape to " + (round ? "round" : "square") + ".");
		save(true);
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

	public static void loadPermissions(WorldBorder plugin)
	{
		if (GroupPlugin != null || Permissions != null || plugin == null)
			return;

		// try GroupManager first
		Plugin test = plugin.getServer().getPluginManager().getPlugin("GroupManager");

		if (test != null)
		{
            if (!test.isEnabled()) {
                plugin.getServer().getPluginManager().enablePlugin(test);
            }
            GroupPlugin = (GroupManager) test;
			LogConfig("Will use plugin for permissions: "+((GroupManager)test).getDescription().getFullName());
			return;
		}

		// if GroupManager isn't available, try Permissions
		test = plugin.getServer().getPluginManager().getPlugin("Permissions");

		if (test != null)
		{
			Permissions = ((Permissions)test).getHandler();
			LogConfig("Will use plugin for permissions: "+((Permissions)test).getDescription().getFullName());
		} else {
			LogConfig("Permissions plugin not found. Only Ops will have access to this plugin's commands.");
		}
	}

	public static boolean HasPermission(Player player, String request)
	{
		if (player == null)				// console, always permitted
			return true;
		else if (player.isOp())			// Op, always permitted
			return true;
		else if (GroupPlugin != null)	// GroupManager plugin available
		{
			if (GroupPlugin.getWorldsHolder().getWorldPermissions(player).has(player, "worldborder." + request))
				return true;
			player.sendMessage("You do not have sufficient permissions to do that.");
			return false;
		}
		else if (Permissions != null)	// Permissions plugin available
		{
			if (Permissions.permission(player, "worldborder." + request))
				return true;
			player.sendMessage("You do not have sufficient permissions to do that.");
			return false;
		}
		else
			return true;
	}

	public static void Log(Level lvl, String text)
	{
		String name = (plugin == null) ? "WorldBorder" : plugin.getDescription().getName();
		mcLog.log(lvl, String.format("[%s] %s", name, text));
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

	public static void load(WorldBorder master, boolean logIt)
	{	// load config from file
		plugin = master;
		cfg = plugin.getConfiguration();

		message = cfg.getString("message");
		shapeRound = cfg.getBoolean("round-border", false);
		LogConfig("Using " + (shapeRound ? "round" : "square") + " border shape.");
		DEBUG = cfg.getBoolean("debug-mode", false);

		borders.clear();

		Map<String, ConfigurationNode> worlds = cfg.getNodes("worlds");
		if (worlds != null)
		{
			Iterator world = worlds.entrySet().iterator();
			while(world.hasNext())
			{
				Entry wdata = (Entry)world.next();
				String name = (String)wdata.getKey();
				ConfigurationNode bord = (ConfigurationNode)wdata.getValue();
				BorderData border = new BorderData(bord.getDouble("x", 0), bord.getDouble("z", 0), bord.getInt("radius", 0));
				borders.put(name, border);
				LogConfig(BorderDescription(name));
			}
		}

		if (message == null || message.isEmpty())
		{	// store defaults
			LogConfig("Configuration not present, creating new file.");
			message = "You have reached the edge of this world.";
			shapeRound = false;
			save(false);
		}
		else if (logIt)
			LogConfig("Configuration loaded.");
	}

	public static void save(boolean logIt)
	{	// save config to file
		if (cfg == null) return;

		cfg.setProperty("message", message);
		cfg.setProperty("round-border", shapeRound);
		cfg.setProperty("debug-mode", DEBUG);

		cfg.removeProperty("worlds");
		Iterator world = borders.entrySet().iterator();
		while(world.hasNext())
		{
			Entry wdata = (Entry)world.next();
			String name = (String)wdata.getKey();
			BorderData bord = (BorderData)wdata.getValue();
			cfg.setProperty("worlds." + name + ".x", bord.getX());
			cfg.setProperty("worlds." + name + ".z", bord.getZ());
			cfg.setProperty("worlds." + name + ".radius", bord.getRadius());
		}

		cfg.save();

		if (logIt)
			LogConfig("Configuration saved.");
	}
}
