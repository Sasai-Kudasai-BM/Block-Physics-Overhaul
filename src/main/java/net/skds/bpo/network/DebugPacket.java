package net.skds.bpo.network;

import java.util.function.Supplier;

import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public class DebugPacket {

	private BlockPos pos;

	public DebugPacket(BlockPos pos) {
		this.pos = pos;
	}

	public DebugPacket(PacketBuffer buffer) {
		this.pos = buffer.readBlockPos();
	}

	void encoder(PacketBuffer buffer) {
		buffer.writeBlockPos(pos);
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientWorld w = (ClientWorld) minecraft.player.world;
		w.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0.06, 0);
		w.setBlockState(pos, Blocks.OAK_PLANKS.getDefaultState());
		context.get().setPacketHandled(true);
	}
}