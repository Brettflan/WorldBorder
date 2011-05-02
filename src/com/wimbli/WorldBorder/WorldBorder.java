package com.wimbli.WorldBorder;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;


public class WorldBorder extends JavaPlugin
{
	public void onEnable()
	{
		PluginDescriptionFile desc = this.getDescription();
		System.out.println( desc.getName() + " version " + desc.getVersion() + " loading" );

		// Load (or create new) config file, and connect to Permissions if it's available
		Config.load(this, false);
		Config.loadPermissions(this);

		// Well I for one find this info useful, so...
		Location spawn = getServer().getWorlds().get(0).getSpawnLocation();
		System.out.println("For reference, the main world's spawn location is at X: " + (int)spawn.getX() + " Y: " + (int)spawn.getY() + " Z: " + (int)spawn.getZ());

		// our one real command, though it does also have aliases "wb" and "worldborder"
		getCommand("wborder").setExecutor(new WBCommand(this));

/*		// for monitoring plugin efficiency
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			public long startTime = Config.Now();
			public void run()
			{
				Config.Log("Running for " + (int)((Config.Now() - startTime) / (60000)) + " minutes, has used total border checking time of " + Config.timeUsed + "ms.");
			}
		}, 1200, 1200);
*/
	}

	public void onDisable()
	{
		PluginDescriptionFile desc = this.getDescription();
		System.out.println( desc.getName() + " version " + desc.getVersion() + " shutting down" );
		Config.StopBorderTimer();
	}
}
