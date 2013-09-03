package com.wimbli.WorldBorder;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.Location;


public class WBListener implements Listener
{
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		// if knockback is set to 0, simply return
		if (Config.KnockBack() == 0.0)
			return;

		if (Config.Debug())
			Config.Log("Teleport cause: "+event.getCause().toString());

		Location newLoc = BorderCheckTask.checkPlayer(event.getPlayer(), event.getTo(), true, true);
		if (newLoc != null)
			event.setTo(newLoc);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent event)
	{
		// if knockback is set to 0, or portal redirection is disabled, simply return
		if (Config.KnockBack() == 0.0 || !Config.portalRedirection())
			return;

		Location newLoc = BorderCheckTask.checkPlayer(event.getPlayer(), event.getTo(), true, false);
		if (newLoc != null)
			event.setTo(newLoc);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent event)
	{
		// make sure our border monitoring task is still running like it should
		if (Config.isBorderTimerRunning()) return;

		Config.LogWarn("Border-checking task was not running! Something on your server apparently killed it. It will now be restarted.");
		Config.StartBorderTimer();
	}
}
