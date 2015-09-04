package com.wimbli.WorldBorder.Events;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by timafh on 04.09.2015.
 */
public class WorldBorderTrimFinishedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private World world;
    private long totalChunks;

    public WorldBorderTrimFinishedEvent(World world, long totalChunks) {
        this.world = world;
        this.totalChunks = totalChunks;
    }


    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public World getWorld() {
        return world;
    }

    public long getTotalChunks() {
        return totalChunks;
    }
}