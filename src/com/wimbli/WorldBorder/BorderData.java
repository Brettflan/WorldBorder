package com.wimbli.WorldBorder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;

public class BorderData {
	// the main data interacted with
	private double x = 0;
	private double z = 0;
	private int radius = 0;

	// some extra data kept handy for faster border checks
	private double maxX;
	private double minX;
	private double maxZ;
	private double minZ;
	private int radiusSquared;
	private double DefiniteSquare;

	public BorderData(double x, double z, int radius)
	{
		this.x = x;
		this.z = z;
		this.radius = radius;
		this.maxX = x + radius;
		this.minX = x - radius;
		this.maxZ = z + radius;
		this.minZ = z - radius;
		this.radiusSquared = radius * radius;
		this.DefiniteSquare = Math.sqrt(.5 * this.radiusSquared);
	}

	public double getX()
	{
		return x;
	}
	public void setX(double x)
	{
		this.x = x;
		this.maxX = x + radius;
		this.minX = x - radius;
	}
	public double getZ()
	{
		return z;
	}
	public void setZ(double z)
	{
		this.z = z;
		this.maxZ = z + radius;
		this.minZ = z - radius;
	}
	public int getRadius()
	{
		return radius;
	}
	public void setRadius(int radius)
	{
		this.radius = radius;
		this.maxX = x + radius;
		this.minX = x - radius;
		this.maxZ = z + radius;
		this.minZ = z - radius;
		this.radiusSquared = radius * radius;
		this.DefiniteSquare = Math.sqrt(.5 * this.radiusSquared);
	}

	@Override
	public String toString()
	{
		return "radius " + radius + " at X: " + Config.coord.format(x) + " Z: " + Config.coord.format(z);
	}

	// This algorithm of course needs to be fast, since it will be run very frequently
	public boolean insideBorder(double xLoc, double zLoc, boolean round)
	{
		if (!round)	// square border
			return (xLoc > minX && xLoc < maxX && zLoc > minZ && zLoc < maxZ);
		else		// round border
		{
			// round border checking algorithm is from rBorder by Reil with almost no changes, thanks
			double X = Math.abs(x - xLoc);
			double Z = Math.abs(z - zLoc);

			if (X < DefiniteSquare && Z < DefiniteSquare)
				return true;	// Definitely inside
			else if (X >= radius || Z >= radius)
				return false;	// Definitely outside
			else if (X * X + Z * Z < radiusSquared)
				return true;	// After much calculation, inside
			else
				return false;	// Apparently outside, then
		}
	}

	public Location correctedPosition(Location loc, boolean round)
	{
		double xLoc = loc.getX();
		double zLoc = loc.getZ();
		double yLoc = loc.getY();

		if (!round)	// square border
		{
			if (xLoc <= minX)
				xLoc = minX + 3;
			else if (xLoc >= maxX)
				xLoc = maxX - 3;
			if (zLoc <= minZ)
				zLoc = minZ + 3;
			else if (zLoc >= maxZ)
				zLoc = maxZ - 3;
		}
		else		// round border
		{
			// algorithm from: http://stackoverflow.com/questions/300871/best-way-to-find-a-point-on-a-circle-closest-to-a-given-point
			double vX = xLoc - x;
			double vZ = zLoc - z;
			double magV = Math.sqrt(vX*vX + vZ*vZ);
			xLoc = x + vX / magV * (radius - 3);
			zLoc = z + vZ / magV * (radius - 3);
		}

		yLoc = getSafeY(loc.getWorld(), Location.locToBlock(xLoc), Location.locToBlock(yLoc), Location.locToBlock(zLoc));
		if (yLoc == -1)
			return null;

		return new Location(loc.getWorld(), Math.floor(xLoc) + 0.5, yLoc, Math.floor(zLoc) + 0.5, loc.getYaw(), loc.getPitch());
	}

	//these material IDs are acceptable for places to teleport player; breathable blocks and water
	private static Set<Integer> acceptableBlocks = new HashSet<Integer>(Arrays.asList(
		 new Integer[] {0, 6, 8, 9, 37, 38, 39, 40, 50, 55, 59, 63, 64, 65, 66, 68, 69, 70, 71, 72, 75, 76, 77, 83, 93, 94}
	));

	//these material IDs are ones we don't want to drop the player onto
	private static Set<Integer> painfulBlocks = new HashSet<Integer>(Arrays.asList(
		 new Integer[] {10, 11, 81}
	));

	// check if a particular spot consists of 2 breathable blocks over something relatively solid
	private boolean isSafeSpot(World world, int X, int Y, int Z)
	{
		Integer below = (Integer)world.getBlockAt(X, Y - 1, Z).getTypeId();
		return (acceptableBlocks.contains((Integer)world.getBlockAt(X, Y, Z).getTypeId())		// target block breatheable
			 && acceptableBlocks.contains((Integer)world.getBlockAt(X, Y + 1, Z).getTypeId())	// above target block breathable
			 && !acceptableBlocks.contains(below)												// below target block not breathable (probably solid)
			 && !painfulBlocks.contains(below)													// below target block not something painful
			);
	}

	// find closest safe Y position from the starting position
	private double getSafeY(World world, int X, int Y, int Z)
	{
		// Expanding Y search method adapted from Acru's code in the Nether plugin
		int limTop = 120, limBot = 1;

		for(int y1 = Y, y2 = Y; (y1 > limBot) || (y2 < limTop); y1--, y2++){
			// Look below.
			if(y1 > limBot){
				if (isSafeSpot(world, X, y1, Z))
					return (double)y1;
			}

			// Look above.
			if(y2 < limTop && y2 != y1){
				if (isSafeSpot(world, X, y2, Z))
					return (double)y2;
			}
		}

		return -1.0;	// no safe Y location?!?!? Must be a rare spot in a Nether world or something
	}
}