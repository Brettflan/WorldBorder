package com.wimbli.WorldBorder;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;


public class WBPlayerListener extends PlayerListener
{
	@Override
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Config.movedPlayers.add(event.getPlayer().getName());
	}

	@Override
	public void onPlayerMove(PlayerMoveEvent event)
	{
		if (event.isCancelled()) return;

		Config.movedPlayers.add(event.getPlayer().getName());
	}

	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		if (event.isCancelled()) return;

		Config.movedPlayers.add(event.getPlayer().getName());
	}
}
