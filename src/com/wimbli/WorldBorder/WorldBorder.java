package com.wimbli.WorldBorder;

import org.bukkit.event.Event;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;


public class WorldBorder extends JavaPlugin
{
	WBPlayerListener playerListener = new WBPlayerListener();

	public void onEnable()
	{
		PluginDescriptionFile desc = this.getDescription();
		System.out.println( desc.getName() + " version " + desc.getVersion() + " loading" );

		// Load (or create new) config file, and connect to Permissions if it's available
		Config.load(this, false);
		Config.loadPermissions(this);

		// Well I for one find this info useful, so...
		Location spawn = getServer().getWorlds().get(0).getSpawnLocation();
		System.out.println("For reference, the main world's spawn location is at X: " + Config.coord.format(spawn.getX()) + " Y: " + Config.coord.format(spawn.getY()) + " Z: " + Config.coord.format(spawn.getZ()));

		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_MOVE, this.playerListener, Event.Priority.High, this);
		pm.registerEvent(Event.Type.PLAYER_TELEPORT, this.playerListener, Event.Priority.High, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, this.playerListener, Event.Priority.High, this);

		// our one real command, though it does also have aliases "wb" and "worldborder"
		getCommand("wborder").setExecutor(new WBCommand(this));
	}

	public void onDisable()
	{
		PluginDescriptionFile desc = this.getDescription();
		System.out.println( desc.getName() + " version " + desc.getVersion() + " shutting down" );
		Config.StopBorderTimer();
	}
}
