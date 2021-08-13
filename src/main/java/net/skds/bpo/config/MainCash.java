package net.skds.bpo.config;

import java.util.List;

public class MainCash {

	public final List<String> dimensionBlacklist;
	public final int downCheckLimit, maxQueueLen, maxFallingBlocks;
	public final double damageMultiplier, explosionMultiplier;
	public final boolean dropDestroyedBlocks, triggerOnStep, explosionFire, debug;

	public MainCash(Main cfg) {
		this.downCheckLimit = cfg.downCheckLimit.get();
		this.maxQueueLen = cfg.maxQueueLen.get();
		this.maxFallingBlocks = cfg.maxFallingBlocks.get();
		this.damageMultiplier = cfg.damageMultiplier.get();
		this.explosionMultiplier = cfg.explosionMultiplier.get();
		this.explosionFire = cfg.explosionFire.get();
		this.triggerOnStep = cfg.triggerOnStep.get();
		this.dropDestroyedBlocks = cfg.dropDestroyedBlocks.get();
		this.debug = cfg.debug.get();

		this.dimensionBlacklist = cfg.dimensionBlacklist.get();
	}
}