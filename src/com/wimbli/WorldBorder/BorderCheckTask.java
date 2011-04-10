package com.wimbli.WorldBorder;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class BorderCheckTask implements Runnable
{
	Server server = null;

	public BorderCheckTask(Server theServer)
	{
		this.server = theServer;
	}

	public void run()
	{
		if (Config.movedPlayers.isEmpty() || server == null)
			return;

		for (Iterator<String> p = Config.movedPlayers.iterator(); p.hasNext();)
		{
			Player player = null;
			try
			{
				String playerName = p.next();
				player = server.getPlayer(playerName);
				p.remove();
			}
			catch (ConcurrentModificationException ex)
			{
				continue;
			}

			if (player == null || !player.isOnline()) continue;

			Location loc = player.getLocation();
			if (loc == null) continue;
			World world = loc.getWorld();
			if (world == null) continue;
			BorderData border = Config.Border(world.getName());
			if (border == null) continue;

			if (border.insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound()))
				continue;

			Location newLoc = newLocation(player, loc, border);

			if (!player.isInsideVehicle())
				player.teleport(newLoc);
			else
			{	// vehicles need to be offset vertically and have velocity stopped
				double vertOffset = player.getVehicle().getLocation().getY() - loc.getY();
				newLoc.setY(newLoc.getY() + vertOffset);
				player.getVehicle().setVelocity(new Vector(0, 0, 0));
				player.getVehicle().teleport(newLoc);
			}
		}
	}

	private static Location newLocation(Player player, Location loc, BorderData border)
	{
		if (Config.Debug())
		{
			Config.LogWarn("Border crossing. Border " + border.toString());
			Config.LogWarn("Player position X: " + Config.coord.format(loc.getX()) + " Y: " + Config.coord.format(loc.getY()) + " Z: " + Config.coord.format(loc.getZ()));
		}

		Location newLoc = border.correctedPosition(loc, Config.ShapeRound());

		// it's remotely possible (such as in the Nether) a suitable location isn't available, in which case...
		if (newLoc == null)
		{
			if (Config.Debug())
				Config.LogWarn("Target new location unviable, using spawn.");
			newLoc = player.getServer().getWorlds().get(0).getSpawnLocation();
		}

		if (Config.Debug())
			Config.LogWarn("New position X: " + Config.coord.format(newLoc.getX()) + " Y: " + Config.coord.format(newLoc.getY()) + " Z: " + Config.coord.format(newLoc.getZ()));

		player.sendMessage(ChatColor.RED + Config.Message());

		return newLoc;
	}
}
