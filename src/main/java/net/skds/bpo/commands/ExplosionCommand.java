package net.skds.bpo.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.skds.bpo.blockphysics.WWS;
import net.skds.bpo.blockphysics.explosion.CustomExplosion;
import net.skds.core.util.blockupdate.WWSGlobal;

public class ExplosionCommand {

	public static LiteralArgumentBuilder<CommandSource> reg() {
		return Commands.literal("explosion").requires(source -> source.hasPermissionLevel(2))
				.then((Commands.literal("create")).executes(context -> create(context, 8))
				.then((Commands.argument("power", FloatArgumentType.floatArg())).executes(context -> create(context, FloatArgumentType.getFloat(context, "power")))))
				.then((Commands.literal("tick")).executes(context -> tick(context)));
	}

	static int create(CommandContext<CommandSource> context, float power) {
		CommandSource source = context.getSource();
		World w = source.getWorld();
		CustomExplosion explosion = new CustomExplosion(w, source.getPos(), power, null);
		explosion.explode();
        source.sendFeedback(new StringTextComponent("Created explosion with power " + power), true);
		//WWSGlobal.get(w).getTyped(WWS.class).explosions.clear();
		//WWSGlobal.get(w).getTyped(WWS.class).explosions.add(explosion);

		return 1;
	}

	static int tick(CommandContext<CommandSource> context) {
		CommandSource source = context.getSource();
		World w = source.getWorld();
		boolean s = WWSGlobal.get(w).getTyped(WWS.class).iterateExplosions();
        source.sendFeedback(new StringTextComponent("tick " + s), true);
		return 0;
	}

}
