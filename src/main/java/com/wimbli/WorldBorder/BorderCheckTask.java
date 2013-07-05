package com.wimbli.WorldBorder;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
	@Override
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

	// track players who are being handled (moved back inside the border) already; needed since Bukkit is sometimes sending teleport events with the old (now incorrect) location still indicated, which can lead to a loop when we then teleport them thinking they're outside the border, triggering event again, etc.
	private static Set<String> handlingPlayers = Collections.synchronizedSet(new LinkedHashSet<String>());

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

		// if player is in bypass list (from bypass command), allow them beyond border; also ignore players currently being handled already
		if (Config.isPlayerBypassing(player.getName()) || handlingPlayers.contains(player.getName().toLowerCase()))
			return null;

		// tag this player as being handled so we can't get stuck in a loop due to Bukkit currently sometimes repeatedly providing incorrect location through teleport event
		handlingPlayers.add(player.getName().toLowerCase());

		Location newLoc = newLocation(player, loc, border, notify);
		boolean handlingVehicle = false;

		/*
		 * since we need to forcibly eject players who are inside vehicles, that fires a teleport event (go figure) and
		 * so would effectively double trigger for us, so we need to handle it here to prevent sending two messages and
		 * two log entries etc.
		 * after players are ejected we can wait a few ticks (long enough for their client to receive new entity location)
		 * and then set them as passenger of the vehicle again
		 */
		if (player.isInsideVehicle())
		{
			Entity ride = player.getVehicle();
			player.leaveVehicle();
			if (ride != null)
			{	// vehicles need to be offset vertically and have velocity stopped
				double vertOffset = (ride instanceof LivingEntity) ? 0 : ride.getLocation().getY() - loc.getY();
				Location rideLoc = newLoc.clone();
				rideLoc.setY(newLoc.getY() + vertOffset);
				if (Config.Debug())
					Config.LogWarn("Player was riding a \"" + ride.toString() + "\".");
				if (ride instanceof Boat)
				{	// boats currently glitch on client when teleported, so crappy workaround is to remove it and spawn a new one
					ride.remove();
					ride = world.spawnEntity(rideLoc, EntityType.BOAT);
				}
				else
				{
					ride.setVelocity(new Vector(0, 0, 0));
					ride.teleport(rideLoc);
				}
				setPassengerDelayed(ride, player, player.getName(), 10);
				handlingVehicle = true;
			}
		}

		// give some particle and sound effects where the player was beyond the border, if "whoosh effect" is enabled
		Config.showWhooshEffect(loc);

		if (!returnLocationOnly)
			player.teleport(newLoc);

		if (!handlingVehicle)
			handlingPlayers.remove(player.getName().toLowerCase());

		if (returnLocationOnly)
			return newLoc;

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

	private static void setPassengerDelayed(final Entity vehicle, final Player player, final String playerName, long delay)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(WorldBorder.plugin, new Runnable()
		{
			@Override
			public void run()
			{
				handlingPlayers.remove(playerName.toLowerCase());
				if (vehicle == null || player == null)
					return;

				vehicle.setPassenger(player);
			}
		}, delay);
	}
}
