package net.skds.bpo.blockphysics.features;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTUtil;
import net.skds.bpo.blockphysics.FeatureContainer;
import static net.skds.bpo.BPO.LOGGER;

public class TransformFeature extends IFeature.Simp {
	public static final TransformFeature EMPTY = new TransformFeature();

	public final boolean fall;
	public final double expP;
	public final double landVel;

	public final BlockState fallState;
	public final BlockState landState;
	public final BlockState expState;

	public TransformFeature(boolean fall, double landVel, double expP, BlockState fallState, BlockState landState, BlockState expState, FeatureContainer.Type type) {
		super(type);
		this.fall = fall;
		this.expP = expP;
		this.landVel = landVel;
		this.fallState = fallState;
		this.landState = landState;
		this.expState = expState;
	}

	private TransformFeature() {
		super(null);
		this.fall = false;
		this.expP = -1;
		this.landVel = -1;
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

	
	public static TransformFeature createFromJson(JsonObject json, FeatureContainer.Type type) {

		JsonElement triggersE = json.get("triggers");

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


			TransformFeature pars = new TransformFeature(fall, landVel, ExpP, fallState, landState, expState, type);

			return pars;

		} catch (Exception e) {
			LOGGER.error("Invalid conversion properties: \"" + type + "\"");
			return null;
		}
	}
}
