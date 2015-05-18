package com.wimbli.WorldBorder;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;


public class MobSpawnListener implements Listener
{
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event)
	{
		Location loc = event.getEntity().getLocation();
		if (loc == null) return;
	
		World world = loc.getWorld();
		if (world == null) return;
		BorderData border = Config.Border(world.getName());
		if (border == null) return;
		
		if (!border.insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound())) 
		{
			event.setCancelled(true);
		}
	}

	public void unregister() 
	{
		HandlerList.unregisterAll(this);
	}
}
