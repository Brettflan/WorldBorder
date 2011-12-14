package com.wimbli.WorldBorder;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Location;


public class WBPlayerListener extends PlayerListener
{
	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		if (event.isCancelled()) return;

		Location newLoc = BorderCheckTask.checkPlayer(event.getPlayer(), event.getTo(), true);
		if (newLoc != null)
			event.setTo(newLoc);
	}
}
