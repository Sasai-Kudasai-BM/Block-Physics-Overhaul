package net.skds.bpo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.skds.bpo.blockphysics.CustomExplosion;
import net.skds.bpo.blockphysics.WWS;
import net.skds.bpo.mixins.ExplosionMixin;
import net.skds.bpo.network.Packets;
import net.skds.bpo.util.pars.JCRUConv;
import net.skds.bpo.util.pars.JCRUPhys;
import net.skds.core.events.OnWWSAttachEvent;
import net.skds.core.events.PacketRegistryEvent;
import net.skds.core.events.SyncTasksHookEvent;
import net.skds.core.multithreading.ThreadProvider;
import net.skds.core.util.blockupdate.WWSGlobal;

public class Events {

	public static MinecraftServer serverInstance = null;

	// @SubscribeEvent
	public void onPacketReg(PacketRegistryEvent e) {
		Packets.reg(e);
	}

	// @SubscribeEvent
	public void onConfigL(ModConfig.Loading e) {
		BPOConfig.cash();
	}

	public void onConfigR(ModConfig.Reloading e) {
		BPOConfig.cash();
	}

	@SubscribeEvent
	public void onWWSAttach(OnWWSAttachEvent e) {
		WWSGlobal wwsg = e.getWWS();
		World w = e.getWorld();
		if (!w.isRemote) {
			WWS w1 = new WWS((ServerWorld) w, wwsg);
			wwsg.addWWS(w1);
		}
	}

	@SubscribeEvent
	public void onServerStart(FMLServerStartedEvent e) {
		if (e.getServer() != null) {
			serverInstance = e.getServer();
		}
	}

	@SubscribeEvent
	public void onTagsUpdated(TagsUpdatedEvent.CustomTagTypes e) {
		JCRUPhys.loadFromConfig();
		JCRUPhys.loadFromData();

		JCRUConv.loadFromConfig();
		JCRUConv.loadFromData();
	}

	/*
	 * @SubscribeEvent public void onPlayerConnected(PlayerEvent.PlayerLoggedInEvent
	 * e) { if (!e.getPlayer().world.isRemote) { //PacketHandler.send(e.getPlayer(),
	 * new JsonConfigPacket(ParsApplier.CASHED_JSON)); } }
	 */

	@SubscribeEvent
	public void onSyncMTHook(SyncTasksHookEvent e) {
		ThreadProvider.doSyncFork(WWS::nextTask);
	}

	@SubscribeEvent
	public void onExplode(ExplosionEvent.Detonate e) {
		// if (BFManager.onExplosion(e.getExplosion(), e.getWorld())) {
		// e.setCanceled(true);
		// }
	}

	@SubscribeEvent
	public void onExplode0(ExplosionEvent.Start e) {
		World world = e.getWorld();
		Explosion ex = e.getExplosion();
		//BPOConfig.cash();
		//System.out.println(BPOConfig.MAIN.explosionMultiplier);
		CustomExplosion ce = new CustomExplosion(world, ex.getPosition(),
				((ExplosionMixin) ex).getPower() * (float) BPOConfig.MAIN.explosionMultiplier, ex.getExploder(), false,
				ex);
		ce.doExplode();
		e.setCanceled(true);
	}
}