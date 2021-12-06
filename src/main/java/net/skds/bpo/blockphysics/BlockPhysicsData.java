package net.skds.bpo.blockphysics;

import static net.skds.bpo.BPO.LOGGER;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.skds.bpo.util.BFUtils;
import net.skds.bpo.util.JsonUtils;
import net.skds.bpo.util.BFUtils.ParsGroup;

public class BlockPhysicsData {

	public static final BlockPhysicsData DEFAULT_AIR = new BlockPhysicsData();

	public final float mass, strength, bounce, slideChance;
	public final int radial, linear, arc;
	public final boolean slide, hanging, attach, falling, ceiling, fragile, diagonal;
	public final Set<Block> attachIgnore, selfList;
	public final String name;
	private Map<String, BlockPhysicsData> dimOver = null;
	private BlockPhysicsData natural = null;

	private BlockPhysicsData() {
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
		this.natural = null;
		this.slideChance = 0;

	}

	public BlockPhysicsData(float mass, float strength, float bounce, int radial, int linear, int arc, boolean slide,
			boolean hanging, boolean attach, boolean falling, boolean ceiling, boolean fragile, Set<Block> attachIgnore,
			Set<Block> selfList, String name, boolean diagonal, float slideChance) {
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
		this.diagonal = diagonal;
		this.slideChance = slideChance;

	}

	public BlockPhysicsData(Block b, boolean empty, float resistance) {

		this.diagonal = false;
		this.slideChance = 0.5F;
		this.name = "Undefined";
		this.dimOver = null;
		this.natural = null;

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

	public boolean canBeDiagonal() {
		if (diagonal) {
			return true;
		}
		if (natural != null) {
			return natural.diagonal;
		}
		return false;
	}

	public static BlockPhysicsData createFromJson(JsonElement json, String name, int recursion,
			BlockPhysicsData parent) {
		if (json == null) {
			LOGGER.error("Invalid blockphysics properties: \"" + name + "\"");
			return null;
		}
		JsonObject jsonObject = json.getAsJsonObject();

		boolean noParent = parent == null;
		Set<Block> blocks;
		if (noParent) {
			blocks = new HashSet<>();
		} else {
			blocks = parent.selfList;
		}

		float mass = JsonUtils.getOrDefaultF(jsonObject, "mass", () -> parent.mass);
		float strength = JsonUtils.getOrDefaultF(jsonObject, "strength", () -> parent.strength);
		float bounce = JsonUtils.getOrDefaultF(jsonObject, "bounce", () -> parent.bounce);
		float slideChance = JsonUtils.getOrDefaultF(jsonObject, "slideChance", () -> parent.slideChance);
		int radial = JsonUtils.getOrDefaultI(jsonObject, "radial", () -> parent.radial);
		int linear = JsonUtils.getOrDefaultI(jsonObject, "linear", () -> parent.linear);
		int arc = JsonUtils.getOrDefaultI(jsonObject, "arc", () -> parent.arc);
		boolean falling = JsonUtils.getOrDefaultB(jsonObject, "falling", () -> parent.falling);
		boolean ceiling = JsonUtils.getOrDefaultB(jsonObject, "ceiling", () -> parent.ceiling);
		boolean fragile = JsonUtils.getOrDefaultB(jsonObject, "fragile", () -> parent.fragile);
		boolean slide = JsonUtils.getOrDefaultB(jsonObject, "slide", () -> parent.slide);
		boolean hanging = JsonUtils.getOrDefaultB(jsonObject, "hanging", () -> parent.hanging);
		boolean attach = JsonUtils.getOrDefaultB(jsonObject, "attach", () -> parent.attach);

		JsonElement Jdiagonal = jsonObject.get("diagonal");
		boolean diagonal = noParent ? false : parent.diagonal;
		if (Jdiagonal != null) {
			diagonal = Jdiagonal.getAsBoolean();
		}

		Set<Block> attachIgnore;
		if (noParent || jsonObject.has("attachIgnore")) {
			attachIgnore = new HashSet<>();
			attachIgnore.addAll(BFUtils.getBlocksFromJA(jsonObject.get("attachIgnore").getAsJsonArray()));
		} else {
			attachIgnore = parent.attachIgnore;
		}

		if (noParent) {
			blocks.addAll(BFUtils.getBlocksFromJA(jsonObject.get("blocks").getAsJsonArray()));
		}

		BlockPhysicsData pars = new BlockPhysicsData(mass, strength, bounce, radial, linear, arc, slide, hanging,
				attach, falling, ceiling, fragile, attachIgnore, blocks, name, diagonal, slideChance);

		JsonElement JdimOver = jsonObject.get("dimensionOverrides");
		Map<String, BlockPhysicsData> dimOver = null;
		if (JdimOver != null && recursion > 0) {
			dimOver = new HashMap<>();

			for (Entry<String, JsonElement> e : JdimOver.getAsJsonObject().entrySet()) {
				String key = e.getKey();
				JsonElement el = e.getValue();
				BlockPhysicsData par = createFromJson(el, name + " " + key, recursion - 1, pars);
				dimOver.put(key, par);
				// System.out.println(par);
			}
		}

		JsonElement Jnatural = jsonObject.get("natural");
		BlockPhysicsData natural = null;
		if (Jnatural != null && recursion > 0) {
			natural = createFromJson(Jnatural, name + " natural", recursion - 1, pars);
		}

		pars.natural = natural;
		pars.dimOver = dimOver;

		return pars;

	}

	public static ParsGroup<BlockPhysicsData> readFromJson(JsonElement json, String name) {
		if (json == null || !json.isJsonObject()) {
			LOGGER.error("Invalid blockphysics properties: \"" + name + "\"");
			return null;
		}
		BlockPhysicsData pars = createFromJson(json, name, 2, null);
		return new ParsGroup<BlockPhysicsData>(pars, pars.selfList);
	}

	public void setNatural(BlockPhysicsData dat) {
		natural = dat;
	}

	public void setDimOver(Map<String, BlockPhysicsData> dat) {
		dimOver = dat;
	}

	public BlockPhysicsData getNatural() {
		return natural;
	}

	public Map<String, BlockPhysicsData> getDimOver() {
		return dimOver;
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