package net.skds.bpo.util;

public class IndexedCord4B {
	
	public static int index(int x, int y, int z) {
		return (x & 15) | ((y & 15) << 4) | ((z & 15) << 8);
	}

	public static byte x(int index) {
		return (byte) (index & 15);
	}

	public static byte y(int index) {
		return (byte) ((index >> 4) & 15);
	}

	public static byte z(int index) {
		return (byte) ((index >> 8) & 15);
	}

	public static int packX(int x) {
		return (x & 15);
	}

	public static int packY(int y) {
		return ((y & 15) << 4);
	}

	public static int packZ(int z) {
		return ((z & 15) << 8);
	}
}
