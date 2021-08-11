package net.skds.bpo.util.pars;

import static net.skds.bpo.BPO.LOGGER;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.skds.bpo.util.BFUtils;

public class BlockPhysicsPars {

	public static final BlockPhysicsPars DEFAULT_AIR = new BlockPhysicsPars();

	public final float mass, strength, bounce, slideChance;
	public final int radial, linear, arc;
	public final boolean slide, hanging, attach, falling, ceiling, fragile, diagonal;
	public final Set<Block> attachIgnore, selfList;
	public final String name;
	public final Map<String, BlockPhysicsPars> dimOver;

	private BlockPhysicsPars() {
		this.mass = 0;
		this.strength = 0;
		this.bounce = 0;
		this.radial = 0;
		this.linear = 0;
		this.arc = 0;
		this.falling = false;
		this.ceiling = false;
		this.fragile = true;
		this.slide = false;
		this.hanging = false;
		this.attach = false;
		this.attachIgnore = new HashSet<>();
		this.selfList = new HashSet<>();
		this.name = "DEF AIR";
		this.dimOver = null;
		this.diagonal = false;
		this.slideChance = 0;

	}

	public BlockPhysicsPars(float mass, float strength, float bounce, int radial, int linear, int arc, boolean slide,
			boolean hanging, boolean attach, boolean falling, boolean ceiling, boolean fragile, Set<Block> attachIgnore,
			Set<Block> selfList, String name, Map<String, BlockPhysicsPars> dimOver, boolean diagonal,
			float slideChance) {
		this.mass = mass;
		this.strength = strength;
		this.bounce = bounce;
		this.radial = radial;
		this.linear = linear;
		this.arc = arc;
		this.falling = falling;
		this.ceiling = ceiling;
		this.fragile = fragile;
		this.slide = slide;
		this.hanging = hanging;
		this.attach = attach;
		this.attachIgnore = attachIgnore;
		this.selfList = selfList;
		this.name = name;
		this.dimOver = dimOver;
		this.diagonal = diagonal;
		this.slideChance = slideChance;

	}

	public BlockPhysicsPars(Block b, boolean empty, float resistance) {

		this.diagonal = false;
		this.slideChance = 0.5F;
		this.name = "Undefined";
		this.dimOver = null;


		float str = resistance;
		boolean bool = str > 3_000_000;
		boolean bool2 = str < 0.01F || empty;

		if (bool2 || bool) {
			str = 0.0F;
			if (bool) {
				str = 10.0F;
				this.fragile = false;
			} else {
				this.fragile = true;
			}
			this.falling = false;
		} else {
			this.falling = true;
			this.fragile = false;
		}
		this.mass = 1000;
		this.strength = (float) (str * 10);
		this.bounce = 0F;
		this.radial = 3;
		this.linear = 3;
		this.arc = (int) (str / 4);
		this.slide = str < 0.3;
		this.ceiling = false;
		this.hanging = false;
		this.attach = true;
		this.attachIgnore = new HashSet<>();
		this.selfList = new HashSet<>();

		// if (b == Blocks.BEDROCK) {
		// System.out.println(this);
		// }
	}

	public static BlockPhysicsPars createFromJson(JsonElement json, String name, boolean tryDimension,
			@Nullable Set<Block> existingList) {
		if (json == null) {
			LOGGER.error("Invalid blockphysics properties: \"" + name + "\"");
			return null;
		}
		JsonObject jsonObject = json.getAsJsonObject();

		Set<Block> blocks;
		if (existingList == null) {
			blocks = new HashSet<>();
		} else {
			blocks = existingList;
		}

		JsonElement listsE = jsonObject.get("blocks");

		JsonElement Jdiagonal = jsonObject.get("diagonal");
		JsonElement JslideChance = jsonObject.get("slideChance");

		JsonElement Jmass = jsonObject.get("mass");
		JsonElement Jstrength = jsonObject.get("strength");
		JsonElement Jbounce = jsonObject.get("bounce");
		JsonElement Jradial = jsonObject.get("radial");
		JsonElement Jlinear = jsonObject.get("linear");
		JsonElement Jceiling = jsonObject.get("ceiling");
		JsonElement Jarc = jsonObject.get("arc");
		JsonElement Jslide = jsonObject.get("slide");
		JsonElement Jfalling = jsonObject.get("falling");
		JsonElement Jfragile = jsonObject.get("fragile");
		JsonElement Jhanging = jsonObject.get("hanging");
		JsonElement Jattach = jsonObject.get("attach");
		JsonElement JattachIgnore = jsonObject.get("attachIgnore");

		JsonElement JdimOver = jsonObject.get("dimensionOverrides");

		Map<String, BlockPhysicsPars> dimOver = null;

		try {

			float mass = Jmass.getAsFloat();
			float strength = Jstrength.getAsFloat();
			float bounce = Jbounce.getAsFloat();
			float slideChance = JslideChance.getAsFloat();
			int radial = Jradial.getAsInt();
			int linear = Jlinear.getAsInt();
			int arc = Jarc.getAsInt();
			boolean falling = Jfalling.getAsBoolean();
			boolean ceiling = Jceiling.getAsBoolean();
			boolean fragile = Jfragile.getAsBoolean();
			boolean slide = Jslide.getAsBoolean();
			boolean hanging = Jhanging.getAsBoolean();
			boolean attach = Jattach.getAsBoolean();

			boolean diagonal = false;
			if (Jdiagonal != null) {
				diagonal = Jdiagonal.getAsBoolean();
			}

			Set<Block> attachIgnore = new HashSet<>();


			attachIgnore.addAll(BFUtils.getBlocksFromJA(JattachIgnore.getAsJsonArray()));
			if (existingList == null) {
				blocks.addAll(BFUtils.getBlocksFromJA(listsE.getAsJsonArray()));
			}

			if (JdimOver != null && tryDimension) {
				dimOver = new HashMap<>();

				for (Entry<String, JsonElement> e : JdimOver.getAsJsonObject().entrySet()) {
					String key = e.getKey();
					JsonElement el = e.getValue();
					BlockPhysicsPars par = createFromJson(el, name + " " + key, false, blocks);
					dimOver.put(key, par);
					// System.out.println(par);
				}

				
				dimOver = ImmutableMap.copyOf(dimOver);
			}

			attachIgnore = ImmutableSet.copyOf(attachIgnore);
			blocks = ImmutableSet.copyOf(blocks);

			BlockPhysicsPars pars = new BlockPhysicsPars(mass, strength, bounce, radial, linear, arc, slide, hanging,
					attach, falling, ceiling, fragile, attachIgnore, blocks, name, dimOver, diagonal, slideChance);

			return pars;

		} catch (Exception e) {
			LOGGER.error("Invalid blockphysics properties: \"" + name + "\"");
			return null;
		}
	}

	public static JCRUPhys.ParsGroup<BlockPhysicsPars> readFromJson(JsonElement json, String name) {
		if (json == null || !json.isJsonObject()) {
			LOGGER.error("Invalid blockphysics properties: \"" + name + "\"");
			return null;
		}
		BlockPhysicsPars pars = createFromJson(json, name, true, null);
		return new JCRUPhys.ParsGroup<BlockPhysicsPars>(pars, pars.selfList);
	}

	@Override
	public String toString() {
		String ss = "\n========================\n";
		return String.format(ss
				+ "%s\nmass: %s\nstrength: %s\nradial: %s\nlinear: %s\narc: %s\nceiling: %s\nfalling: %s\nslide: %s\nhanging: %s\nattach: %s\nbounce: %s\nblocks: %s\ndimOver: %s"
				+ ss, name, mass, strength, radial, linear, arc, ceiling, falling, slide, hanging, attach, bounce,
				selfList.size(), dimOver);
	}
}