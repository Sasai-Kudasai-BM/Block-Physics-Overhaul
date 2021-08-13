package net.skds.bpo.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraftforge.common.ForgeConfigSpec;
import net.skds.bpo.BPO;

public class Main {

    public final ForgeConfigSpec.ConfigValue<List<String>> dimensionBlacklist;
    public final ForgeConfigSpec.IntValue downCheckLimit, maxQueueLen, maxFallingBlocks;
    public final ForgeConfigSpec.DoubleValue damageMultiplier, explosionMultiplier;
    public final ForgeConfigSpec.BooleanValue dropDestroyedBlocks, triggerOnStep, explosionFire, debug;

    private static final List<String> DL = new ArrayList<>();


    public Main(ForgeConfigSpec.Builder innerBuilder) {
        Function<String, ForgeConfigSpec.Builder> builder = name -> innerBuilder.translation(BPO.MOD_ID + ".config." + name);

        innerBuilder.push("General");

        maxFallingBlocks = builder.apply("maxFallingBlocks").comment("UwU").defineInRange("maxFallingBlocks", 1000, 100, 10_000);
        maxQueueLen = builder.apply("maxQueueLen").comment("UwU").defineInRange("maxQueueLen", 10_000, 100, 500_000);
        downCheckLimit = builder.apply("downCheckLimit").comment("UwU").defineInRange("downCheckLimit", 30, -1, 1024);

        damageMultiplier = builder.apply("damageMultiplier").comment("UwU").defineInRange("damageMultiplier", 1.0, 0.0, 999999.0);
        explosionMultiplier = builder.apply("explosionMultiplier").comment("UwU").defineInRange("explosionMultiplier", 1.0, 0.0, 999999.0);

        dropDestroyedBlocks = builder.apply("dropDestroyedBlocks").comment("UwU").define("dropDestroyedBlocks", true);
        triggerOnStep = builder.apply("triggerOnStep").comment("UwU").define("triggerOnStep", true);
        explosionFire = builder.apply("explosionFire").comment("UwU").define("explosionFire", true);

        dimensionBlacklist = builder.apply("dimensionBlacklist").comment("UwU").define("dimensionBlacklist", DL);

        debug = builder.apply("debug").comment("UwU").define("debug", false);

        innerBuilder.pop();
    }

    static {
        DL.add("minecraft:the_end");
    }
}