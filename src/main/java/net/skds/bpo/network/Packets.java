package net.skds.bpo.network;

import net.skds.core.events.PacketRegistryEvent;

public class Packets {
	public static void reg(PacketRegistryEvent e) {		
		e.registerPacket(DebugPacket.class, DebugPacket::encoder, DebugPacket::new, DebugPacket::handle);
		e.registerPacket(ExplosionPacket.class, ExplosionPacket::encoder, ExplosionPacket::new, ExplosionPacket::handle);
		//e.registerPacket(JsonConfigPacket.class, JsonConfigPacket::encoder, JsonConfigPacket::new, JsonConfigPacket::handle);
	}
}