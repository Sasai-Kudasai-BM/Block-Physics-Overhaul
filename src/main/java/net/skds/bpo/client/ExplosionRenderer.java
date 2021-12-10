package net.skds.bpo.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.skds.bpo.network.ExplosionPacket;

public class ExplosionRenderer {

	public final int x, y, z;

	public byte[] elements;
	public int steps;

	public ExplosionRenderer(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void render(MatrixStack matrixStack, float partialTicks) {

	}

	public void handlePacket(ExplosionPacket packet) {

	}



}
