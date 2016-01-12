package com.wimbli.WorldBorder.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wimbli.WorldBorder.WorldFillTask;


/**
 * Created by Maximvdw on 12.01.2016.
 */
public class WorldBorderFillStartEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();
	private WorldFillTask fillTask;

	public WorldBorderFillStartEvent(WorldFillTask worldFillTask)
	{
		this.fillTask = worldFillTask;
	}

	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}

	public static HandlerList getHandlerList()
	{
		return handlers;
	}

	public WorldFillTask getFillTask(){
		return this.fillTask;
	}
}
