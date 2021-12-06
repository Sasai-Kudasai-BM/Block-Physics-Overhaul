package net.skds.bpo.util;

public class IndexedCord {
	
	public static int index(int x, int y, int z) {
		return (x & 255) | ((y & 255) << 8) | ((z & 255) << 16);
	}

	public static byte x(int index) {
		return (byte) (index & 255);
	}

	public static byte y(int index) {
		return (byte) ((index >> 8) & 255);
	}

	public static byte z(int index) {
		return (byte) ((index >> 16) & 255);
	}

	public static int packX(int x) {
		return (x & 255);
	}

	public static int packY(int y) {
		return ((y & 255) << 8);
	}

	public static int packZ(int z) {
		return ((z & 255) << 16);
	}
}
