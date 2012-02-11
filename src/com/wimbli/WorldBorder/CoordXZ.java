package com.wimbli.WorldBorder;


// simple storage class for chunk x/z values
public class CoordXZ
{
	public int x, z;
	public CoordXZ(int x, int z)
	{
		this.x = x;
		this.z = z;
	}

	// transform values between block, chunk, and region
	// bit-shifting is used because it's mucho rapido
	public static int blockToChunk(int blockVal)
	{	// 1 chunk is 16x16 blocks
		return blockVal >> 4;   // ">>4" == "/16"
	}
	public static int blockToRegion(int blockVal)
	{	// 1 region is 512x512 blocks
		return blockVal >> 9;   // ">>9" == "/512"
	}
	public static int chunkToRegion(int chunkVal)
	{	// 1 region is 32x32 chunks
		return chunkVal >> 5;   // ">>5" == "/32"
	}
	public static int chunkToBlock(int chunkVal)
	{
		return chunkVal << 4;   // "<<4" == "*16"
	}
	public static int regionToBlock(int regionVal)
	{
		return regionVal << 9;   // "<<9" == "*512"
	}
	public static int regionToChunk(int regionVal)
	{
		return regionVal << 5;   // "<<5" == "*32"
	}


	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		else if (obj == null || obj.getClass() != this.getClass())
			return false;

		CoordXZ test = (CoordXZ)obj;
		return test.x == this.x && test.z == this.z;
	}

	@Override
	public int hashCode()
	{
		return (this.x << 9) + this.z;
	}
}
