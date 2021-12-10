package net.skds.bpo.network;

import java.util.BitSet;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.skds.bpo.client.particles.ExplodeParticle;

public class ExplosionPacket {

	public byte y;
	public byte[] data;
	public BitSet cords;
	public int x, z;

	public byte steps;
	ByteBuf buffer = Unpooled.buffer(512);

	public ExplosionPacket(int steps, int x, int y, int z) {
		this.steps = (byte) steps;
		this.x = x;
		this.y = (byte) y;
		this.z = z;
	}

	public void writeStep() {
		buffer.writeInt(data.length);
		buffer.writeBytes(data);
		byte[] arr = cords.toByteArray();
		buffer.writeInt(arr.length);
		buffer.writeBytes(arr);
	}

	public void readStep() {
		int l = buffer.readInt();
		this.data = new byte[l];
		buffer.readBytes(data);

		l = buffer.readInt();
		byte[] arr = new byte[l];
		buffer.readBytes(arr);
		this.cords = BitSet.valueOf(arr);
	}

	public ExplosionPacket(PacketBuffer buffer) {
		steps = buffer.readByte();
		this.x = buffer.readInt();
		this.y = buffer.readByte();
		this.z = buffer.readInt();

		this.buffer = Unpooled.wrappedBuffer(buffer.readByteArray());
	}

	void encoder(PacketBuffer buffer) {
		buffer.writeByte(steps);
		buffer.writeInt(x);
		buffer.writeByte(y);
		buffer.writeInt(z);

		buffer.writeByteArray(this.buffer.array());

		//System.out.println(buffer.arrayOffset());
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientWorld w = (ClientWorld) minecraft.player.world;
		ExplodeParticle.spawnParticles(w, this);
		
		context.get().setPacketHandled(true);
	}
}