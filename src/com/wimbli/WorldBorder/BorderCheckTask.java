package com.wimbli.WorldBorder;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class BorderCheckTask implements Runnable
{
	private transient Server server = null;

	public BorderCheckTask(Server theServer)
	{
		this.server = theServer;
	}

	public void run()
	{
		if (server == null)
			return;

		Player[] players = server.getOnlinePlayers();

		for (int i = 0; i < players.length; i++){
			checkPlayer(players[i], null, false);
		}
	}

	// set targetLoc only if not current player location; set returnLocationOnly to true to have new Location returned if they need to be moved to one, instead of directly handling it
	public static Location checkPlayer(Player player, Location targetLoc, boolean returnLocationOnly)
	{
		if (player == null || !player.isOnline()) return null;

		Location loc = (targetLoc == null) ? player.getLocation().clone() : targetLoc;
		if (loc == null) return null;

		World world = loc.getWorld();
		if (world == null) return null;
		BorderData border = Config.Border(world.getName());
		if (border == null) return null;

		if (border.insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound()))
			return null;

		Location newLoc = newLocation(player, loc, border);

		if (Config.whooshEffect())
		{	// give some particle and sound effects where the player was beyond the border
			world.playEffect(loc, Effect.ENDER_SIGNAL, 0);
			world.playEffect(loc, Effect.ENDER_SIGNAL, 0);
			world.playEffect(loc, Effect.SMOKE, 4);
			world.playEffect(loc, Effect.SMOKE, 4);
			world.playEffect(loc, Effect.SMOKE, 4);
			world.playEffect(loc, Effect.GHAST_SHOOT, 0);
		}

		if (returnLocationOnly)
			return newLoc;

		if (!player.isInsideVehicle())
			player.teleport(newLoc);
		else
		{
			Vehicle ride = (Vehicle)player.getVehicle();
			if (ride != null)
			{	// vehicles need to be offset vertically and have velocity stopped
				double vertOffset = ride.getLocation().getY() - loc.getY();
				newLoc.setY(newLoc.getY() + vertOffset);
				ride.setVelocity(new Vector(0, 0, 0));
				ride.teleport(newLoc);
			}
			else
			{	// if player.getVehicle() returns null (when riding a pig on older Bukkit releases, for instance), player has to be ejected
				player.leaveVehicle();
				player.teleport(newLoc);
			}
		}

		return null;
	}

	private static Location newLocation(Player player, Location loc, BorderData border)
	{
		if (Config.Debug())
		{
			Config.LogWarn("Border crossing in \"" + loc.getWorld().getName() + "\". Border " + border.toString());
			Config.LogWarn("Player position X: " + Config.coord.format(loc.getX()) + " Y: " + Config.coord.format(loc.getY()) + " Z: " + Config.coord.format(loc.getZ()));
		}

		Location newLoc = border.correctedPosition(loc, Config.ShapeRound());

		// it's remotely possible (such as in the Nether) a suitable location isn't available, in which case...
		if (newLoc == null)
		{
			if (Config.Debug())
				Config.LogWarn("Target new location unviable, using spawn.");
			newLoc = player.getWorld().getSpawnLocation();
		}

		if (Config.Debug())
			Config.LogWarn("New position in world \"" + newLoc.getWorld().getName() + "\" at X: " + Config.coord.format(newLoc.getX()) + " Y: " + Config.coord.format(newLoc.getY()) + " Z: " + Config.coord.format(newLoc.getZ()));

		player.sendMessage(ChatColor.RED + Config.Message());

		return newLoc;
	}
}
