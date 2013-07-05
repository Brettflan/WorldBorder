package com.wimbli.WorldBorder;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;


public class BorderData
{
	// the main data interacted with
	private double x = 0;
	private double z = 0;
	private int radiusX = 0;
	private int radiusZ = 0;
	private Boolean shapeRound = null;
	private boolean wrapping = false;

	// some extra data kept handy for faster border checks
	private double maxX;
	private double minX;
	private double maxZ;
	private double minZ;
	private double radiusXSquared;
	private double radiusZSquared;
	private double DefiniteRectangleX;
	private double DefiniteRectangleZ;
	private double radiusSquaredQuotient;

	public BorderData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound, boolean wrap)
	{
		setData(x, z, radiusX, radiusZ, shapeRound, wrap);
	}
	public BorderData(double x, double z, int radiusX, int radiusZ)
	{
		setData(x, z, radiusX, radiusZ, null);
	}
	public BorderData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound)
	{
		setData(x, z, radiusX, radiusZ, shapeRound);
	}
	public BorderData(double x, double z, int radius)
	{
		setData(x, z, radius, null);
	}
	public BorderData(double x, double z, int radius, Boolean shapeRound)
	{
		setData(x, z, radius, shapeRound);
	}

	public final void setData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound, boolean wrap)
	{
		this.x = x;
		this.z = z;
		this.shapeRound = shapeRound;
		this.wrapping = wrap;
		this.setRadiusX(radiusX);
		this.setRadiusZ(radiusZ);
	}
	public final void setData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound)
	{
		setData(x, z, radiusX, radiusZ, shapeRound, false);
	}
	public final void setData(double x, double z, int radius, Boolean shapeRound)
	{
		setData(x, z, radius, radius, shapeRound, false);
	}

	public BorderData copy()
	{
		return new BorderData(x, z, radiusX, radiusZ, shapeRound, wrapping);
	}

	public double getX()
	{
		return x;
	}
	public void setX(double x)
	{
		this.x = x;
		this.maxX = x + radiusX;
		this.minX = x - radiusX;
	}
	public double getZ()
	{
		return z;
	}
	public void setZ(double z)
	{
		this.z = z;
		this.maxZ = z + radiusZ;
		this.minZ = z - radiusZ;
	}
	public int getRadiusX()
	{
		return radiusX;
	}
	public int getRadiusZ()
	{
		return radiusZ;
	}
	public void setRadiusX(int radiusX)
	{
		this.radiusX = radiusX;
		this.maxX = x + radiusX;
		this.minX = x - radiusX;
		this.radiusXSquared = (double)radiusX * (double)radiusX;
		this.radiusSquaredQuotient = this.radiusXSquared / this.radiusZSquared;
		this.DefiniteRectangleX = Math.sqrt(.5 * this.radiusXSquared);
	}
	public void setRadiusZ(int radiusZ)
	{
		this.radiusZ = radiusZ;
		this.maxZ = z + radiusZ;
		this.minZ = z - radiusZ;
		this.radiusZSquared = (double)radiusZ * (double)radiusZ;
		this.radiusSquaredQuotient = this.radiusXSquared / this.radiusZSquared;
		this.DefiniteRectangleZ = Math.sqrt(.5 * this.radiusZSquared);
	}


	// backwards-compatible methods from before elliptical/rectangular shapes were supported
	/**
	 * @deprecated  Replaced by {@link #getRadiusX()} and {@link #getRadiusZ()};
	 * this method now returns an average of those two values and is thus imprecise
	 */
	public int getRadius()
	{
		return (radiusX + radiusZ) / 2;  // average radius; not great, but probably best for backwards compatibility
	}
	public void setRadius(int radius)
	{
		setRadiusX(radius);
		setRadiusZ(radius);
	}


	public Boolean getShape()
	{
		return shapeRound;
	}
	public void setShape(Boolean shapeRound)
	{
		this.shapeRound = shapeRound;
	}


	public boolean getWrapping()
	{
		return wrapping;
	}
	public void setWrapping(boolean wrap)
	{
		this.wrapping = wrap;
	}


	@Override
	public String toString()
	{
		return "radius " + ((radiusX == radiusZ) ? radiusX : radiusX + "x" + radiusZ) + " at X: " + Config.coord.format(x) + " Z: " + Config.coord.format(z) + (shapeRound != null ? (" (shape override: " + Config.ShapeName(shapeRound.booleanValue()) + ")") : "") + (wrapping ? (" (wrapping)") : "");
	}

	// This algorithm of course needs to be fast, since it will be run very frequently
	public boolean insideBorder(double xLoc, double zLoc, boolean round)
	{
		// if this border has a shape override set, use it
		if (shapeRound != null)
			round = shapeRound.booleanValue();

		// square border
		if (!round)
			return !(xLoc < minX || xLoc > maxX || zLoc < minZ || zLoc > maxZ);

		// round border
		else
		{
			// elegant round border checking algorithm is from rBorder by Reil with almost no changes, all credit to him for it
			double X = Math.abs(x - xLoc);
			double Z = Math.abs(z - zLoc);

			if (X < DefiniteRectangleX && Z < DefiniteRectangleZ)
				return true;	// Definitely inside
			else if (X >= radiusX || Z >= radiusZ)
				return false;	// Definitely outside
			else if (X * X + Z * Z * radiusSquaredQuotient < radiusXSquared)
				return true;	// After further calculation, inside
			else
				return false;	// Apparently outside, then
		}
	}
	public boolean insideBorder(double xLoc, double zLoc)
	{
		return insideBorder(xLoc, zLoc, Config.ShapeRound());
	}
	public boolean insideBorder(Location loc)
	{
		return insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound());
	}
	public boolean insideBorder(CoordXZ coord, boolean round)
	{
		return insideBorder(coord.x, coord.z, round);
	}
	public boolean insideBorder(CoordXZ coord)
	{
		return insideBorder(coord.x, coord.z, Config.ShapeRound());
	}

	public Location correctedPosition(Location loc, boolean round, boolean flying)
	{
		// if this border has a shape override set, use it
		if (shapeRound != null)
			round = shapeRound.booleanValue();

		double xLoc = loc.getX();
		double zLoc = loc.getZ();
		double yLoc = loc.getY();

		// square border
		if (!round)
		{
			if (wrapping)
			{
				if (xLoc <= minX)
					xLoc = maxX - Config.KnockBack();
				else if (xLoc >= maxX)
					xLoc = minX + Config.KnockBack();
				if (zLoc <= minZ)
					zLoc = maxZ - Config.KnockBack();
				else if (zLoc >= maxZ)
					zLoc = minZ + Config.KnockBack();
			}
			else
			{
				if (xLoc <= minX)
					xLoc = minX + Config.KnockBack();
				else if (xLoc >= maxX)
					xLoc = maxX - Config.KnockBack();
				if (zLoc <= minZ)
					zLoc = minZ + Config.KnockBack();
				else if (zLoc >= maxZ)
					zLoc = maxZ - Config.KnockBack();
			}
		}

		// round border
		else
		{
			// algorithm originally from: http://stackoverflow.com/questions/300871/best-way-to-find-a-point-on-a-circle-closest-to-a-given-point
			// modified by Lang Lukas to support elliptical border shape

			//Transform the ellipse to a circle with radius 1 (we need to transform the point the same way)
			double dX = xLoc - x;
			double dZ = zLoc - z;
			double dU = Math.sqrt(dX *dX + dZ * dZ); //distance of the untransformed point from the center
			double dT = Math.sqrt(dX *dX / radiusXSquared + dZ * dZ / radiusZSquared); //distance of the transformed point from the center
			double f = (1 / dT - Config.KnockBack() / dU); //"correction" factor for the distances
			if (wrapping)
			{
				xLoc = x - dX * f;
				zLoc = z - dZ * f;
			} else {
				xLoc = x + dX * f;
				zLoc = z + dZ * f;
			}
		}

		int ixLoc = Location.locToBlock(xLoc);
		int izLoc = Location.locToBlock(zLoc);

		// Make sure the chunk we're checking in is actually loaded
		Chunk tChunk = loc.getWorld().getChunkAt(CoordXZ.blockToChunk(ixLoc), CoordXZ.blockToChunk(izLoc));
		if (!tChunk.isLoaded())
			tChunk.load();

		yLoc = getSafeY(loc.getWorld(), ixLoc, Location.locToBlock(yLoc), izLoc, flying);
		if (yLoc == -1)
			return null;

		return new Location(loc.getWorld(), Math.floor(xLoc) + 0.5, yLoc, Math.floor(zLoc) + 0.5, loc.getYaw(), loc.getPitch());
	}
	public Location correctedPosition(Location loc, boolean round)
	{
		return correctedPosition(loc, round, false);
	}
	public Location correctedPosition(Location loc)
	{
		return correctedPosition(loc, Config.ShapeRound(), false);
	}

	//these material IDs are acceptable for places to teleport player; breathable blocks and water
	public static final LinkedHashSet<Integer> safeOpenBlocks = new LinkedHashSet<Integer>(Arrays.asList(
		 new Integer[] {0, 6, 8, 9, 27, 28, 30, 31, 32, 37, 38, 39, 40, 50, 55, 59, 63, 64, 65, 66, 68, 69, 70, 71, 72, 75, 76, 77, 78, 83, 90, 93, 94, 96, 104, 105, 106, 115, 131, 132, 141, 142, 149, 150, 157, 171}
	));

	//these material IDs are ones we don't want to drop the player onto, like cactus or lava or fire or activated Ender portal
	public static final LinkedHashSet<Integer> painfulBlocks = new LinkedHashSet<Integer>(Arrays.asList(
		 new Integer[] {10, 11, 51, 81, 119}
	));

	// check if a particular spot consists of 2 breathable blocks over something relatively solid
	private boolean isSafeSpot(World world, int X, int Y, int Z, boolean flying)
	{
		boolean safe = safeOpenBlocks.contains((Integer)world.getBlockTypeIdAt(X, Y, Z))		// target block open and safe
					&& safeOpenBlocks.contains((Integer)world.getBlockTypeIdAt(X, Y + 1, Z));	// above target block open and safe
		if (!safe || flying)
			return safe;

		Integer below = (Integer)world.getBlockTypeIdAt(X, Y - 1, Z);
		return (safe
			 && (!safeOpenBlocks.contains(below) || below == 8 || below == 9)	// below target block not open/breathable (so presumably solid), or is water
			 && !painfulBlocks.contains(below)									// below target block not painful
			);
	}

	private static final int limBot = 1;

	// find closest safe Y position from the starting position
	private double getSafeY(World world, int X, int Y, int Z, boolean flying)
	{
		// artificial height limit of 127 added for Nether worlds since CraftBukkit still incorrectly returns 255 for their max height, leading to players sent to the "roof" of the Nether
		final int limTop = (world.getEnvironment() == World.Environment.NETHER) ? 125 : world.getMaxHeight() - 2;
		// Expanding Y search method adapted from Acru's code in the Nether plugin

		for(int y1 = Y, y2 = Y; (y1 > limBot) || (y2 < limTop); y1--, y2++){
			// Look below.
			if(y1 > limBot)
			{
				if (isSafeSpot(world, X, y1, Z, flying))
					return (double)y1;
			}

			// Look above.
			if(y2 < limTop && y2 != y1)
			{
				if (isSafeSpot(world, X, y2, Z, flying))
					return (double)y2;
			}
		}

		return -1.0;	// no safe Y location?!?!? Must be a rare spot in a Nether world or something
	}


	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		else if (obj == null || obj.getClass() != this.getClass())
			return false;

		BorderData test = (BorderData)obj;
		return test.x == this.x && test.z == this.z && test.radiusX == this.radiusX && test.radiusZ == this.radiusZ;
	}

	@Override
	public int hashCode()
	{
		return (((int)(this.x * 10) << 4) + (int)this.z + (this.radiusX << 2) + (this.radiusZ << 3));
	}
}
