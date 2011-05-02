package com.wimbli.WorldBorder;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
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
//		long startTime = Config.Now();  // for monitoring plugin efficiency
		Player[] players = server.getOnlinePlayers();

		if (server == null)
		{
//			Config.timeUsed += Config.Now() - startTime;  // for monitoring plugin efficiency
			return;
		}

		for (int i = 0; i < players.length; i++){
			if (players[i] == null || !players[i].isOnline()) continue;

			Location loc = players[i].getLocation();
			if (loc == null) continue;

			World world = loc.getWorld();
			if (world == null) continue;
			BorderData border = Config.Border(world.getName());
			if (border == null) continue;

			if (border.insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound()))
				continue;

			Location newLoc = newLocation(players[i], loc, border);

			if (!players[i].isInsideVehicle())
				players[i].teleport(newLoc);
			else
			{
				Vehicle ride = players[i].getVehicle();
				if (ride != null)
				{	// vehicles need to be offset vertically and have velocity stopped
					double vertOffset = ride.getLocation().getY() - loc.getY();
					newLoc.setY(newLoc.getY() + vertOffset);
					ride.setVelocity(new Vector(0, 0, 0));
					ride.teleport(newLoc);
				}
				else
				{	// when riding a pig, player.getVehicle() returns null; so, we unfortunately need to eject player in this rare case
					players[i].leaveVehicle();
					players[i].teleport(newLoc);
				}
			}
		}
//		Config.timeUsed += Config.Now() - startTime;  // for monitoring plugin efficiency
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
