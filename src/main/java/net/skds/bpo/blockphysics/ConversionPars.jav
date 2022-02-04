package net.skds.bpo.blockphysics;

import static net.skds.bpo.BPO.LOGGER;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTUtil;
import net.skds.bpo.util.BFUtils;
import net.skds.bpo.util.BFUtils.ParsGroup;

public class ConversionPars {
	public static final ConversionPars EMPTY = new ConversionPars();

	public final boolean fall;
	public final double expP;
	public final double landVel;
	public final Set<Block> selfList;

	public final BlockState fallState;
	public final BlockState landState;
	public final BlockState expState;

	public ConversionPars(boolean fall, double landVel, double expP, Set<Block> selfList, BlockState fallState, BlockState landState, BlockState expState) {
		this.fall = fall;
		this.expP = expP;
		this.landVel = landVel;
		this.selfList = selfList;
		this.fallState = fallState;
		this.landState = landState;
		this.expState = expState;
	}

	private ConversionPars() {
		this.fall = false;
		this.expP = -1;
		this.landVel = -1;
		this.selfList = new HashSet<>();
		this.fallState = null;
		this.landState = null;
		this.expState = null;
	}

	public boolean onFall() {
		return fall;
	}

	public boolean onExp() {
		return expP > -1E-5;
	}

	public boolean onLand() {
		return landVel > -1E-5;
	}

	public static ConversionPars createFromJson(JsonElement json, String name) {
		if (json == null) {
			LOGGER.error("Invalid conversion properties: \"" + name + "\"");
			return null;
		}
		JsonObject jsonObject = json.getAsJsonObject();
		Set<Block> blocks = new HashSet<>();
		JsonElement blocksE = jsonObject.get("blocks");
		JsonElement triggersE = jsonObject.get("triggers");

		try {
			JsonObject triggersO = triggersE.getAsJsonObject();

			JsonObject fallO = triggersO.get("fall").getAsJsonObject();
			JsonObject landVelO = triggersO.get("land").getAsJsonObject();
			JsonObject ExpPO = triggersO.get("explode").getAsJsonObject();

			BlockState fallState = null;
			BlockState landState = null;
			BlockState expState = null;

			boolean fall = fallO.get("val").getAsBoolean();
			double landVel = landVelO.get("val").getAsDouble();
			double ExpP = ExpPO.get("val").getAsDouble();

			if (fall) {
				String string = fallO.get("to").toString();
				CompoundNBT nbt = JsonToNBT.getTagFromJson(string);
				fallState = NBTUtil.readBlockState(nbt);
			}
			if (landVel > -1E-5) {
				String string = landVelO.get("to").toString();
				CompoundNBT nbt = JsonToNBT.getTagFromJson(string);
				landState = NBTUtil.readBlockState(nbt);
			}
			if (ExpP > -1E-5) {
				String string = ExpPO.get("to").toString();
				CompoundNBT nbt = JsonToNBT.getTagFromJson(string);
				expState = NBTUtil.readBlockState(nbt);
			}

			blocks.addAll(BFUtils.getBlocksFromJA(blocksE.getAsJsonArray()));

			ConversionPars pars = new ConversionPars(fall, landVel, ExpP, blocks, fallState, landState, expState);

			return pars;

		} catch (Exception e) {
			LOGGER.error("Invalid conversion properties: \"" + name + "\"");
			return null;
		}
	}

	public static ParsGroup<ConversionPars> readFromJson(JsonElement json, String name) {
		if (json == null || !json.isJsonObject()) {
			LOGGER.error("Invalid conversion properties: \"" + name + "\"");
			return null;
		}
		ConversionPars pars = createFromJson(json, name);
		return new ParsGroup<ConversionPars>(pars, pars.selfList);
	}
}
