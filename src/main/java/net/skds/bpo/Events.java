package net.skds.bpo;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.skds.bpo.blockphysics.WWS;
import net.skds.bpo.blockphysics.explosion.ExplosionPlug;
import net.skds.bpo.commands.ExplosionCommand;
import net.skds.bpo.network.Packets;
import net.skds.bpo.util.Interface.IExplosionMix;
import net.skds.bpo.util.pars.JCRUFeature;
import net.skds.bpo.util.pars.JCRUPhys;
import net.skds.core.events.OnWWSAttachEvent;
import net.skds.core.events.PacketRegistryEvent;
import net.skds.core.events.SyncTasksHookEvent;
import net.skds.core.multithreading.ThreadProvider;
import net.skds.core.util.blockupdate.WWSGlobal;

public class Events {

	public static MinecraftServer serverInstance = null;

	public void onPacketReg(PacketRegistryEvent e) {
		Packets.reg(e);
	}

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
		if (w.isRemote) {

		} else {
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
		//JCRUConv.loadFromConfig();
		//JCRUConv.loadFromData();
		JCRUFeature.loadFromConfig();
		JCRUFeature.loadFromData();
	}

	@SubscribeEvent
	public void onSyncMTHook(SyncTasksHookEvent e) {
		ThreadProvider.doSyncFork(WWS::nextTask);
	}

	@SubscribeEvent
	public void onServerStartTick(ServerTickEvent e) {
		if (e.phase == Phase.START) {
			WWS.tickServerIn();
		}
	}

	@SubscribeEvent
	public void onExplode(ExplosionEvent.Start e) {
		//e.setCanceled(true);
		//Explosion ex = e.getExplosion();
		//CustomExplosion ce = new CustomExplosion(e.getWorld(), ex.getPosition(), ((ExplosionMixin) ex).getPower() * (float) BPOConfig.MAIN.explosionMultiplier, ex);
		//try {			
		//	ce.explode();
		//} catch (Exception exception) {
		//	BPO.LOGGER.error(exception);
		//}
	}

	@SubscribeEvent
	public void onExplode0(ExplosionEvent.Detonate e) {
		World world = e.getWorld();
		Explosion ex = e.getExplosion();
		ExplosionPlug ep = new ExplosionPlug(world, ex.getPosition(), ((IExplosionMix) ex).getPower(), ex);
		ep.explode();
	}

	@SubscribeEvent
	public void commandsReg(RegisterCommandsEvent e) {
		CommandDispatcher<CommandSource> dispatcher = e.getDispatcher();
		dispatcher.register(ExplosionCommand.reg());
	}

}