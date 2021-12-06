package net.skds.bpo.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.world.World;
import net.skds.bpo.blockphysics.WWS;
import net.skds.bpo.blockphysics.explosion.CustomExplosion;
import net.skds.core.util.blockupdate.WWSGlobal;

public class ExplosionCommand {

	public static LiteralArgumentBuilder<CommandSource> reg() {
		return Commands.literal("explosion").requires(source -> source.hasPermissionLevel(2))
				.then((Commands.literal("create")).executes(context -> create(context)))
				.then((Commands.literal("tick")).executes(context -> tick(context)));
	}

	static int create(CommandContext<CommandSource> context) {
		try {

			CommandSource source = context.getSource();
			World w = source.getWorld();
			CustomExplosion explosion = new CustomExplosion(w, source.getPos(), 4, null);
			explosion.explode();
			WWSGlobal.get(w).getTyped(WWS.class).explosions.clear();
			WWSGlobal.get(w).getTyped(WWS.class).explosions.add(explosion);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1;
	}

	static int tick(CommandContext<CommandSource> context) {
		try {
			CommandSource source = context.getSource();
			World w = source.getWorld();
			WWSGlobal.get(w).getTyped(WWS.class).explosions.forEach(e -> e.iterate());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

}
