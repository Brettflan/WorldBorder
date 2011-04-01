package com.wimbli.WorldBorder;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class WBPlayerListener extends PlayerListener
{
	@Override
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		if (player == null) return;
		Location loc = player.getLocation();
		if (loc == null) return;
		World world = loc.getWorld();
		if (world == null) return;
		BorderData border = Config.Border(world.getName());
		if (border == null) return;

		if (border.insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound()))
			return;

		if (Config.DEBUG)
		{
			Config.LogWarn("Border crossing. Border " + border.toString());
			Config.LogWarn("Player position X: " + Config.coord.format(loc.getX()) + " Y: " + Config.coord.format(loc.getY()) + " Z: " + Config.coord.format(loc.getZ()));
		}

		Location newLoc = border.correctedPosition(loc, Config.ShapeRound());

		// it's remotely possible (such as in the Nether) a suitable location isn't available, in which case...
		if (newLoc == null)
		{
			if (Config.DEBUG)
				Config.LogWarn("Target new location unviable, using spawn.");
			newLoc = player.getServer().getWorlds().get(0).getSpawnLocation();
		}

		if (Config.DEBUG)
			Config.LogWarn("New position X: " + Config.coord.format(newLoc.getX()) + " Y: " + Config.coord.format(newLoc.getY()) + " Z: " + Config.coord.format(newLoc.getZ()));

		player.sendMessage(ChatColor.RED + Config.Message());
		player.teleport(newLoc);
	}

	@Override
	public void onPlayerMove(PlayerMoveEvent event)
	{
		if (event.isCancelled()) return;

		Player player = event.getPlayer();
		if (player == null) return;
		Location loc = event.getTo();
		if (loc == null) return;
		World world = loc.getWorld();
		if (world == null) return;
		BorderData border = Config.Border(world.getName());
		if (border == null) return;

		if (border.insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound()))
			return;

		if (Config.DEBUG)
		{
			Config.LogWarn("Border crossing. Border " + border.toString());
			Config.LogWarn("Player position X: " + Config.coord.format(loc.getX()) + " Y: " + Config.coord.format(loc.getY()) + " Z: " + Config.coord.format(loc.getZ()));
		}

		Location newLoc = border.correctedPosition(loc, Config.ShapeRound());

		if (newLoc == null)
		{
			if (Config.DEBUG)
				Config.LogWarn("Target new location unviable, using spawn.");
			newLoc = player.getServer().getWorlds().get(0).getSpawnLocation();
		}

		if (Config.DEBUG)
			Config.LogWarn("New position X: " + Config.coord.format(newLoc.getX()) + " Y: " + Config.coord.format(newLoc.getY()) + " Z: " + Config.coord.format(newLoc.getZ()));

		player.sendMessage(ChatColor.RED + Config.Message());

		if (!player.isInsideVehicle())
			player.teleport(newLoc);
		else
		{
			newLoc.setY(newLoc.getY() + 1);
			player.getVehicle().setVelocity(new Vector(0, 0, 0));
			player.getVehicle().teleport(newLoc);
		}

		event.setTo(newLoc);
	}

	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		if (event.isCancelled()) return;

		Player player = event.getPlayer();
		if (player == null) return;
		Location loc = event.getTo();
		if (loc == null) return;
		World world = loc.getWorld();
		if (world == null) return;
		BorderData border = Config.Border(world.getName());
		if (border == null) return;

		if (border.insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound()))
			return;

		if (Config.DEBUG)
		{
			Config.LogWarn("Border crossing. Border " + border.toString());
			Config.LogWarn("Player position X: " + Config.coord.format(loc.getX()) + " Y: " + Config.coord.format(loc.getY()) + " Z: " + Config.coord.format(loc.getZ()));
		}

		Location newLoc = border.correctedPosition(loc, Config.ShapeRound());

		if (newLoc == null)
		{
			if (Config.DEBUG)
				Config.LogWarn("Target new location unviable, using spawn.");
			newLoc = player.getServer().getWorlds().get(0).getSpawnLocation();
		}

		if (Config.DEBUG)
			Config.LogWarn("New position X: " + Config.coord.format(newLoc.getX()) + " Y: " + Config.coord.format(newLoc.getY()) + " Z: " + Config.coord.format(newLoc.getZ()));

		player.sendMessage(ChatColor.RED + Config.Message());

		if (!player.isInsideVehicle())
			player.teleport(newLoc);
		else
		{
			newLoc.setY(newLoc.getY() + 1);
			player.getVehicle().setVelocity(new Vector(0, 0, 0));
			player.getVehicle().teleport(newLoc);
		}

		event.setTo(newLoc);
	}
}
