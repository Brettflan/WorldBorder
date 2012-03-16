package com.wimbli.WorldBorder;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;


public class WorldBorder extends JavaPlugin
{
	@Override
	public void onEnable()
	{
		// Load (or create new) config file
		Config.load(this, false);

		// our one real command, though it does also have aliases "wb" and "worldborder"
		getCommand("wborder").setExecutor(new WBCommand(this));

		// keep an eye on teleports, to redirect them to a spot inside the border if necessary
		getServer().getPluginManager().registerEvents(new WBListener(), this);

		// integrate with DynMap if it's available
		DynMapFeatures.setup();

		// Well I for one find this info useful, so...
		Location spawn = getServer().getWorlds().get(0).getSpawnLocation();
		System.out.println("For reference, the main world's spawn location is at X: " + Config.coord.format(spawn.getX()) + " Y: " + Config.coord.format(spawn.getY()) + " Z: " + Config.coord.format(spawn.getZ()));
	}

	@Override
	public void onDisable()
	{
		DynMapFeatures.removeAllBorders();
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
