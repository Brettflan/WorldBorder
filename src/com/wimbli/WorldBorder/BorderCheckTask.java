package com.wimbli.WorldBorder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.World;


public class BorderCheckTask implements Runnable
{
	public void run()
	{
		// if knockback is set to 0, simply return
		if (Config.KnockBack() == 0.0)
			return;

		Player[] players = Bukkit.getServer().getOnlinePlayers();

		for (int i = 0; i < players.length; i++){
			checkPlayer(players[i], null, false, true);
		}
	}

	// set targetLoc only if not current player location; set returnLocationOnly to true to have new Location returned if they need to be moved to one, instead of directly handling it
	public static Location checkPlayer(Player player, Location targetLoc, boolean returnLocationOnly, boolean notify)
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

		// if player is in bypass list (from bypass command), allow them beyond border
		if (Config.isPlayerBypassing(player.getName()))
			return null;

		/*
		 * since we need to forcibly eject players who are inside vehicles, that fires a teleport event (go figure) and
		 * so would effectively double trigger for us, so we need to handle it here to prevent sending two messages and
		 * two log entries etc.
		 * after players are ejected we can wait a few ticks (long enough for their client to receive new entity location)
		 * and then set them as passenger of the vehicle again
		 */
		if (player.isInsideVehicle())
		{
			Location newLoc = newLocation(player, loc, border, false);
			Entity ride = player.getVehicle();
			player.leaveVehicle();
			if (ride != null)
			{	// vehicles need to be offset vertically and have velocity stopped
				double vertOffset = (ride instanceof LivingEntity) ? 0 : ride.getLocation().getY() - loc.getY();
				newLoc.setY(newLoc.getY() + vertOffset);
				if (ride instanceof Boat)
				{	// boats currently glitch on client when teleported, so crappy workaround is to remove it and spawn a new one
					ride.remove();
					ride = world.spawnEntity(newLoc, EntityType.BOAT);
				}
				else
				{
					ride.setVelocity(new Vector(0, 0, 0));
					ride.teleport(newLoc);
				}
				setPassengerDelayed(ride, player, 10);
			}
			return null;
		}

		Location newLoc = newLocation(player, loc, border, notify);

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

		player.teleport(newLoc);
		return null;
	}
	public static Location checkPlayer(Player player, Location targetLoc, boolean returnLocationOnly)
	{
		return checkPlayer(player, targetLoc, returnLocationOnly, true);
	}

	private static Location newLocation(Player player, Location loc, BorderData border, boolean notify)
	{
		if (Config.Debug())
		{
			Config.LogWarn((notify ? "Border crossing" : "Check was run") + " in \"" + loc.getWorld().getName() + "\". Border " + border.toString());
			Config.LogWarn("Player position X: " + Config.coord.format(loc.getX()) + " Y: " + Config.coord.format(loc.getY()) + " Z: " + Config.coord.format(loc.getZ()));
		}

		Location newLoc = border.correctedPosition(loc, Config.ShapeRound(), player.isFlying());

		// it's remotely possible (such as in the Nether) a suitable location isn't available, in which case...
		if (newLoc == null)
		{
			if (Config.Debug())
				Config.LogWarn("Target new location unviable, using spawn.");
			newLoc = player.getWorld().getSpawnLocation();
		}

		if (Config.Debug())
			Config.LogWarn("New position in world \"" + newLoc.getWorld().getName() + "\" at X: " + Config.coord.format(newLoc.getX()) + " Y: " + Config.coord.format(newLoc.getY()) + " Z: " + Config.coord.format(newLoc.getZ()));

		if (notify)
			player.sendMessage(ChatColor.RED + Config.Message());

		return newLoc;
	}

	private static void setPassengerDelayed(final Entity vehicle, final Player player, long delay)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(WorldBorder.plugin, new Runnable()
		{
			@Override
			public void run()
			{
				vehicle.setPassenger(player);
			}
		}, delay);
	}
}
