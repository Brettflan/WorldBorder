package com.wimbli.WorldBorder.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wimbli.WorldBorder.WorldTrimTask;


/**
 * Created by Maximvdw on 12.01.2016.
 */
public class WorldBorderTrimStartEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();
	private WorldTrimTask trimTask;

	public WorldBorderTrimStartEvent(WorldTrimTask trimTask)
	{
		this.trimTask = trimTask;
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

	public WorldTrimTask getTrimTask(){
		return this.trimTask;
	}
}