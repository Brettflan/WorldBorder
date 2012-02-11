package com.wimbli.WorldBorder;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;


public class WorldBorder extends JavaPlugin
{
	@Override
	public void onEnable()
	{
		// Load (or create new) config file
		Config.load(this, false);

		// Well I for one find this info useful, so...
		Location spawn = getServer().getWorlds().get(0).getSpawnLocation();
		System.out.println("For reference, the main world's spawn location is at X: " + (int)spawn.getX() + " Y: " + (int)spawn.getY() + " Z: " + (int)spawn.getZ());

		// our one real command, though it does also have aliases "wb" and "worldborder"
		getCommand("wborder").setExecutor(new WBCommand(this));

		// keep an eye on teleports, to redirect them to a spot inside the border if necessary
		getServer().getPluginManager().registerEvents(new WBListener(), this);
	}

	@Override
	public void onDisable()
	{
		Config.StopBorderTimer();
		Config.StoreFillTask();
		Config.StopFillTask();
	}

	// for other plugins to hook into
	public BorderData GetWorldBorder(String worldName)
	{
		return Config.Border(worldName);
	}
}
