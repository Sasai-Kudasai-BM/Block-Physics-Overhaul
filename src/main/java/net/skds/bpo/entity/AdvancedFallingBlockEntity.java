package net.skds.bpo.entity;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.ReuseableStream;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.skds.bpo.BPO;
import net.skds.bpo.BPOConfig;
import net.skds.bpo.blockphysics.BFExecutor;
import net.skds.bpo.blockphysics.WWS;
import net.skds.bpo.registry.Entities;
import net.skds.bpo.util.BFUtils;
import net.skds.bpo.util.pars.BlockPhysicsPars;
import net.skds.bpo.util.pars.ConversionPars;
import net.skds.core.api.IWorldExtended;
import net.skds.core.util.other.Pair;

public class AdvancedFallingBlockEntity extends Entity implements IEntityAdditionalSpawnData {

	public static final DamageSource DAMAGE_SOURCE = new DamageSource("bpo.afbe");

	public final WWS wws;

	private final float G = 9.81E0F;
	private final float TG = G / 400;
	private final float BDZ = 1 * TG + 0.005F;

	@OnlyIn(Dist.CLIENT)
	public TileEntity te = null;
	@OnlyIn(Dist.CLIENT)
	public byte slideDirectionV = 0;

	public BlockPhysicsPars pars;
	public ConversionPars conv;

	public byte slideProgress = -1;
	public byte slideDirection = -1;

	public boolean fall = false;
	public boolean slided = false;

	private boolean crm = false;
	private int crmTm = 0;

	private BlockState fallTile = Blocks.SAND.getDefaultState();
	public int fallTime;
	public CompoundNBT tileEntityData = null;
	public BlockPos slidePos = BlockPos.ZERO;

	protected static final DataParameter<Byte> SLIDE_PROGRESS = EntityDataManager
			.createKey(AdvancedFallingBlockEntity.class, DataSerializers.BYTE);
	protected static final DataParameter<Byte> SLIDE_DIRECTION = EntityDataManager
			.createKey(AdvancedFallingBlockEntity.class, DataSerializers.BYTE);
	protected static final DataParameter<BlockPos> SLIDE_POS = EntityDataManager
			.createKey(AdvancedFallingBlockEntity.class, DataSerializers.BLOCK_POS);

	public AdvancedFallingBlockEntity(EntityType<? extends Entity> t, World w) {
		super(t, w);
		this.wws = ((IWorldExtended) w).getWWS().getTyped(WWS.class);
		refteshPars();
		// System.out.println(pars);
	}

	public static EntityType<? extends Entity> getForReg(String id) {
		EntityType<? extends Entity> type = EntityType.Builder
				.create(AdvancedFallingBlockEntity::new, EntityClassification.MISC).size(1.0F, 1.0F).setTrackingRange(4)
				.setUpdateInterval(5).setShouldReceiveVelocityUpdates(true).build(id);
		return type;
	}

	public AdvancedFallingBlockEntity(World worldIn, double x, double y, double z, BlockState fallingBlockState) {
		super(Entities.ADVANCED_FALLING_BLOCK.get(), worldIn);
		this.wws = (WWS) ((IWorldExtended) worldIn).getWWS().getTyped(WWS.class);
		if (fallingBlockState != Blocks.AIR.getDefaultState()) {
			this.fallTile = fallingBlockState;

		}
		refteshPars();
		this.preventEntitySpawning = true;
		this.setPosition(x, y + (double) ((1.0F - this.getHeight()) / 2.0F), z);
		this.setMotion(Vector3d.ZERO);
		this.prevPosX = x;
		this.prevPosY = y;
		this.prevPosZ = z;
		this.onGround = false;
	}

	private void startSlide() {
		if (slideDirection != -1 && slideProgress == -1) {
			dataManager.set(SLIDE_DIRECTION, (byte) -1);
			//slidePos = dataManager.get(SLIDE_POS);
			//if (slidePos.equals(BlockPos.ZERO)) {
			slidePos = getPosition();
			//}
			slideDirectionV = slideDirection;
			slideProgress = 0;
		}
	}

	public void syncData() {
		if (world.isRemote) {
			slideDirection = dataManager.get(SLIDE_DIRECTION);
			startSlide();

		} else {
			dataManager.set(SLIDE_DIRECTION, slideDirection);
			//dataManager.set(SLIDE_PROGRESS, slideProgress);
			if (slideDirection != -1)
				dataManager.set(SLIDE_POS, slidePos);
		}
	}

	/**
	 * Returns true if it's possible to attack this entity with an item.
	 */
	@Override
	public boolean canBeAttackedWithItem() {
		return false;
	}

	@Override
	public boolean func_241845_aY() {
		return true;
	}

	@Override
	protected boolean canTriggerWalking() {
		return false;
	}

	@Override
	protected void registerData() {
		this.dataManager.register(SLIDE_POS, BlockPos.ZERO);
		this.dataManager.register(SLIDE_PROGRESS, (byte) -1);
		this.dataManager.register(SLIDE_DIRECTION, (byte) -1);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean canBeCollidedWith() {
		return !this.removed;
	}

	@Override
	public void onAddedToWorld() {
		super.onAddedToWorld();
	}

	public void tick2() {
		tickMain();
	}

	@Override
	public void tick() {
		if (world.isRemote) {
			tickMain();
		} else {
			wws.collisionMap.addBox(this, getBoundingBox().expand(getMotion()));
			WWS.pushAFBT(this);
		}
	}

	public void tickMain() {
		syncData();
		if (crm) {
			crmTm++;
			if (crmTm > 100) {
				killBreak();
				return;
			}
		} else {
			crmTm = 0;
		}
		++fallTime;

		Vector3d velocity = getMotion();
		if (!this.world.isRemote) {
			moveCustom();
			BlockPos blockpos1 = this.getPosition();

			for (Pair<Entity, AxisAlignedBB> pair : wws.collisionMap.getEntities(getBoundingBox())) {
				damageEntity(velocity, pair.a);
			}

			if (fall) {

				// for (Pair<Entity, AxisAlignedBB> pair :
				// wws.collisionMap.getEntities(getBoundingBox())) {
				// damageEntity(velocity, pair.a);
				// }

				if (!slided) {
					double vel = velocity.y;
					boolean willSliide = pars.slide
							|| (vel < -4 * TG && getMotion().y < 2 * TG && rand.nextFloat() < pars.slideChance);
					if (willSliide && slide()) {
						// velocity.add(0.5 * (rand.nextFloat() - 0.5), 0, 0.5 * (rand.nextFloat() -
						// 0.5));
						// setMotion(velocity);
						onGround = false;
						fall = false;
						return;
					}
				}
				fall = false;
			}

			if (!this.onGround) {
				if (!this.world.isRemote && (this.fallTime > 100 && (blockpos1.getY() < 1 || blockpos1.getY() > 256)
						|| this.fallTime > 600)) {

					this.remove();
				}
			} else {
				if (getMotion().length() < 0.01D) {
					onLand();
				}
			}
		} else {
			if (slideProgress != -1) {
				slideProgress++;
				if (slideProgress >= BPOConfig.SLIDESTEPS) {
					slideProgress = -1;
				}
			}
			moveCustom();
		}
	}

	private void damageEntity(Vector3d vel, Entity e) {
		double damage = vel.lengthSquared() * pars.mass * 0.02 * BPOConfig.MAIN.damageMultiplier;
		if (damage > 1) {
			synchronized (e) {
				e.attackEntityFrom(DAMAGE_SOURCE, (float) damage);
				// System.out.println(damage);
			}
		}
	}

	private boolean isValidForReplace(BlockState state) {
		if (state.isIn(Blocks.MOVING_PISTON)) {
			return false;
		}
		Material mat = state.getMaterial();
		boolean b = mat.isReplaceable();
		BlockPhysicsPars bp = getParam(state);
		b = b || (bp.fragile && bp.strength < 0.001);

		return b;
	}

	public void onLand() {
		BlockPos blockpos1 = this.getPosition();
		BlockState blockstate = this.world.getBlockState(blockpos1);
		if (!blockstate.isIn(Blocks.MOVING_PISTON)) {
			if (isValidForReplace(blockstate)) {
				set();
			} else {
				if (crm) {
					// return;
				}
				pushOut();
			}
		}
	}

	private boolean slide() {
		for (Direction d : BFExecutor.getRandomizedDirections(rand, false)) {
			BlockPos pos0 = getPosition();
			BlockPos pos2 = pos0.offset(d);
			BlockState state2 = world.getBlockState(pos2);
			if (isValidForReplace(state2)) {
				BlockPos pos3 = pos2.down();
				BlockState state3 = world.getBlockState(pos3);
				if (isValidForReplace(state3) && occupyPos(pos3)) {
					slideDirection = (byte) d.getHorizontalIndex();
					if (!world.isRemote)
						slidePos = getPosition();
					syncData();
					return true;
				}
			}
		}
		return false;
	}

	private boolean occupyPos(BlockPos pos) {
		return BFUtils.occupyPos(wws, pos);
	}

	@Override
	public void remove() {
		WWS.E_COUNT--;
		super.remove();
	}

	private void killBreak() {
		synchronized (world) {
			Block.spawnDrops(fallTile, world, getPosition());
			world.playEvent(2001, getPosition(), Block.getStateId(fallTile));
			if (isBurning()) {
				BlockPos pos = getPosition();
				BlockState oldState = world.getBlockState(pos);
				if (isValidForReplace(oldState)) {
					BlockState firestate = Blocks.FIRE.getDefaultState().updatePostPlacement(Direction.UP, oldState,
							world, pos, pos);
					world.setBlockState(getPosition(), firestate, 3);
				}
			}
			remove();
		}
	}

	private void set() {

		synchronized (world) {

			BlockPos blockpos1 = this.getPosition();
			if (BPO.hasWPO()) {
				displaceFluid(blockpos1);
			}
			// this.world.setBlockState(blockpos1,
			// this.fallTile.updatePostPlacement(Direction.UP, fallTile, world, blockpos1,
			// blockpos1), 3);
			this.world.setBlockState(blockpos1, this.fallTile, 3);

			if (isBurning()) {
				for (Direction dir : Direction.values()) {
					BlockPos pos2 = blockpos1.offset(dir);
					BlockState state2 = world.getBlockState(pos2);
					if (isValidForReplace(state2)) {
						BlockState firestate = Blocks.FIRE.getDefaultState().updatePostPlacement(Direction.UP, state2,
								world, pos2, pos2);
						world.setBlockState(pos2, firestate, 3);
					}
				}
			}

			if (this.tileEntityData != null && this.fallTile.hasTileEntity()) {
				TileEntity tileentity = this.world.getTileEntity(blockpos1);
				// System.out.println(world.getBlockState(blockpos1));

				if (tileentity == null) {
					tileentity = fallTile.createTileEntity(world);
					world.setTileEntity(blockpos1, tileentity);
					// System.out.println(tileentity);
				}
				if (tileentity != null) {
					CompoundNBT compoundnbt = tileentity.write(new CompoundNBT());

					for (String s : this.tileEntityData.keySet()) {
						INBT inbt = this.tileEntityData.get(s);
						if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s)) {
							compoundnbt.put(s, inbt.copy());
						}
					}

					tileentity.read(this.fallTile, compoundnbt);
					tileentity.markDirty();
				}
			}
			this.remove();
		}
	}

	private void moveCustom() {
		Vector3d motion;
		slided = false;

		if (slideDirection == -1) {
			motion = getMotion();
		} else {
			slided = true;
			Vector3i dir = Direction.byHorizontalIndex(slideDirection).getDirectionVec();
			motion = new Vector3d(dir.getX(), 0, dir.getZ());
			slideDirection = -1;
		}

		Vector3d offset = new Vector3d(0.0D, 0.0D, 0.0D);

		AxisAlignedBB myBox = this.getBoundingBox();

		if (!world.isRemote && slideDirection == -1) {
			crm = false;

			// Set<AxisAlignedBB> set = new HashSet<>();
			Set<AxisAlignedBB> set = wws.collisionMap.getBoxesExeptE(this, myBox);

			world.getCollisionShapes(this, myBox).map(BFUtils::VoxelShapeFilter).forEach((s) -> {
				set.addAll(s.toBoundingBoxList());
			});

			Iterator<AxisAlignedBB> iter = set.iterator();
			while (iter.hasNext()) {
				AxisAlignedBB box = iter.next();
				if (box.intersects(myBox)) {
					crm = true;
				}
			}
		}

		if (!this.hasNoGravity() && !slided) {
			motion = motion.add(0.0D, -TG, 0.0D);
		}

		Vector3d maxMove = getAllowedMovementV2(motion);

		if (!maxMove.equals(motion)) {
			Vector3d motion2 = onCollision(motion, maxMove);
			if (pars.bounce > 0 || fallTime < 0 || motion2.lengthSquared() > 1E-8) {
				motion = getAllowedMovementV2(motion2);
				if (!motion.equals(motion2)) {
					if (motion.x != motion2.x) {
						motion = motion2.mul(0, 1, 1);
					}
					if (motion.y != motion2.y) {
						motion = motion2.mul(1, 0, 1);
					}
					if (motion.z != motion2.z) {
						motion = motion2.mul(1, 1, 0);
					}
				}
			} else {
				motion = motion2;
			}
		} else {
		}
		if (!slided) {
			setMotion(motion);
		} else {
			setMotion(0, -0.2, 0);
		}

		BlockPos blockpos = this.getOnPosition();
		BlockState blockstate = this.world.getBlockState(blockpos);
		this.updateFallState(motion.y, this.onGround, blockstate, blockpos);

		// if (!world.isRemote)
		// wws.collisionMap.addBox(this, getBoundingBox().expand(motion));

		this.setPosition(getPosX() + motion.x + offset.x, getPosY() + motion.y + offset.y,
				getPosZ() + motion.z + offset.z);
	}

	private void pushOut() {
		setPosition(Math.floor(getPosX()) + 0.5, Math.floor(getPosY()), Math.floor(getPosZ()) + 0.5);
		BlockPos pos = getPosition();
		if (isValidForReplace(world.getBlockState(pos))) {
			return;
		}
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			BlockPos pos2 = pos.offset(dir);
			BlockState state2 = world.getBlockState(pos2);
			if (isValidForReplace(state2)) {
				setPosition(getPosX() + dir.getXOffset(), getPosY(), getPosZ() + dir.getZOffset());
				return;
			}
		}
		setPosition(getPosX(), getPosY() + 1, getPosZ());
	}

	private void displaceFluid(BlockPos pos) {
		FluidState fs = world.getFluidState(pos);
		if (!fs.isEmpty()) {
			for (int i = 0; i < 20; i++) {

				BlockState state = world.getBlockState(pos);
				if (state.getMaterial() == Material.AIR) {
					world.setBlockState(pos, fs.getBlockState(), 3);
					return;
				}

				for (Direction dir : Direction.Plane.HORIZONTAL) {
					BlockPos pos2 = pos.offset(dir);
					BlockState state2 = world.getBlockState(pos2);
					if (state2.getMaterial() == Material.AIR) {
						world.setBlockState(pos2, fs.getBlockState(), 3);
						return;
					}
				}

				pos = pos.up();
			}
		}
	}

	private Vector3d onCollision(Vector3d motion, Vector3d maxMove) {

		Vector3d shift = motion.normalize().scale(0.5);
		AxisAlignedBB box = getBoundingBox().offset(shift);
		Set<BlockPos> con = BFUtils.getBlockPoses(box);
		double mv2c = motion.lengthSquared() * pars.mass * 4.0E-2;
		synchronized (world) {
			boolean willBreak = true;
			boolean soft = true;
			double mv2c2 = mv2c;
			for (BlockPos pos : con) {
				BlockState state = world.getBlockState(pos);
				// if (!state.getCollisionShape(world, pos).isEmpty()) {
				BlockPhysicsPars par = getParam(state);
				if (par.strength >= pars.strength) {
					soft = false;
				}
				if (par.strength >= mv2c || !par.fragile) {
					//System.out.println(par);
					willBreak = false;
					break;
				}
				mv2c2 = Math.min(mv2c2, mv2c - par.strength);
				// }
			}
			if (willBreak) {
				for (BlockPos pos : con) {

					if (!world.isRemote) {
						destroyBlock(pos, Blocks.AIR.getDefaultState());
					}
				}
				if (pars.strength < mv2c && pars.fragile && !soft) {
					killBreak();
					return Vector3d.ZERO;
				}
				return getAllowedMovementV2(motion.scale(mv2c2 / mv2c));
			}
		}
		if (pars.strength < mv2c && pars.fragile) {
			killBreak();
			return Vector3d.ZERO;
		}

		landConvert(motion.length() * 20);

		double friction = (fallTime < 0) ? 0.0 : 0.5D;

		boolean collideX = motion.x != maxMove.x;
		boolean collideY = motion.y != maxMove.y;
		boolean collideZ = motion.z != maxMove.z;
		this.collidedVertically = collideY;

		float bonk = (fallTime < 0) ? (float) Math.max(pars.bounce, 1.) : pars.bounce;

		double vx = collideX ? (-inThreadhold(motion.x) * bonk) : motion.x;
		double vy = collideY ? (-inThreadhold(motion.y) * bonk) : motion.y;
		double vz = collideZ ? (-inThreadhold(motion.z) * bonk) : motion.z;

		if (collideX) {
			double deltaLen = Math.abs(maxMove.x);
			vy = applyFriction(vy, deltaLen * friction);
			vz = applyFriction(vz, deltaLen * friction);
		}

		if (collideY) {
			double deltaLen = Math.abs(maxMove.y);
			vx = applyFriction(vx, deltaLen * friction);
			vz = applyFriction(vz, deltaLen * friction);
		}

		if (collideZ) {
			double deltaLen = Math.abs(maxMove.z);
			vy = applyFriction(vy, deltaLen * friction);
			vx = applyFriction(vx, deltaLen * friction);
		}
		this.fall = this.collidedVertically && motion.y < 0/* && Math.abs((maxMove.y + getPosY()) % 1) < 7.5E-2 */;
		if (world.isRemote) {
			if (!onGround && fall && Math.abs(vy) < BDZ) {
				if (world.getClosestPlayer(getPosX(), getPosY(), getPosZ(), 16, (e) -> true) != null)
					world.playSound(getPosX(), getPosY(), getPosZ(), fallTile.getSoundType().getPlaceSound(),
							SoundCategory.BLOCKS, 0.55F, 0.8F, true);

			}
		}
		this.onGround = fall && Math.abs(vy) < BDZ;
		return new Vector3d(vx, vy, vz);
	}

	private double applyFriction(double v, double f) {
		f += 2E-1D;
		double f2 = v >= 0 ? f : -f;
		return Math.abs(v) > f ? v - f2 : 0;
	}

	private double inThreadhold(double d) {
		double d2 = Math.abs(d) > BDZ ? d : 0;
		if (d2 >= TG) {
			d2 -= TG;
		} else if (d2 <= -TG) {
			d2 += TG;
		}
		return d2;
	}

	private Vector3d getAllowedMovementV2(Vector3d vec) {
		AxisAlignedBB axisalignedbb;
		if (slided) {
			setPosition(Math.round(getPosX() - 0.5) + 0.5, Math.round(getPosY()), Math.round(getPosZ() - 0.5) + 0.5);
			axisalignedbb = this.getBoundingBox();
		} else {
			axisalignedbb = this.getBoundingBox();
		}
		ISelectionContext iselectioncontext = ISelectionContext.forEntity(this);
		VoxelShape voxelshape = this.world.getWorldBorder().getShape();
		Stream<VoxelShape> stream = VoxelShapes.compare(voxelshape, VoxelShapes.create(axisalignedbb),
				IBooleanFunction.AND) ? Stream.empty() : Stream.of(voxelshape);

		ReuseableStream<VoxelShape> reuseablestream;
		//if (world.isRemote || crm) {
		if (world.isRemote || crmTm > 10 || fallTime < 0) {
			reuseablestream = new ReuseableStream<>(stream);
		} else {
			Set<AxisAlignedBB> set = wws.collisionMap.getBoxesExeptE(this, axisalignedbb.expand(vec));
			if (!slided)
				for (AxisAlignedBB bb : set) {
					if (bb.intersects(axisalignedbb)) {
						stream.close();
						return Vector3d.ZERO;
					}
				}

			Stream<VoxelShape> stream2 = set.stream().map((b) -> {
				return VoxelShapes.create(b);
			});
			reuseablestream = new ReuseableStream<>(Stream.concat(stream, stream2));
		}

		Vector3d vector3d = vec.lengthSquared() == 0.0D ? vec
				: collideCustom(vec, axisalignedbb, this.world, iselectioncontext, reuseablestream);

		return vector3d;
	}

	public Vector3d collideCustom(Vector3d vec, AxisAlignedBB collisionBox, World world, ISelectionContext context,
			ReuseableStream<VoxelShape> potentialHits) {
		Stream<VoxelShape> stream = world.getCollisionShapes(this, collisionBox.expand(vec).expand(vec.normalize()))
				.map(BFUtils::VoxelShapeFilter);

		if (crm) {
			stream = stream.filter((vs) -> {
				return vs.getStart(Axis.Y) <= collisionBox.maxY;
			});
		}
		ReuseableStream<VoxelShape> reuseablestream = new ReuseableStream<>(
				Stream.concat(potentialHits.createStream(), stream));
		return collideBoundingBox(vec, collisionBox, reuseablestream);
		// return getAllowedMovement(vec, collisionBox, world, context, potentialHits);
	}

	protected void writeAdditional(CompoundNBT compound) {
		compound.put("BlockState", NBTUtil.writeBlockState(this.fallTile));
		compound.putInt("Time", this.fallTime);
		if (this.tileEntityData != null) {
			compound.put("TileEntityData", this.tileEntityData);
		}
	}

	@SuppressWarnings("deprecation")
	protected void readAdditional(CompoundNBT compound) {

		BlockState ns = NBTUtil.readBlockState(compound.getCompound("BlockState"));
		if (ns != this.fallTile) {
			this.fallTile = ns;
			refteshPars();
		}

		this.fallTime = compound.getInt("Time");

		if (compound.contains("TileEntityData", 10)) {
			this.tileEntityData = compound.getCompound("TileEntityData");
		}

		if (this.fallTile.isAir()) {
			this.fallTile = Blocks.SAND.getDefaultState();
		}

	}

	@OnlyIn(Dist.CLIENT)
	public World getWorldObj() {
		return this.world;
	}

	@OnlyIn(Dist.CLIENT)
	public boolean canRenderOnFire() {
		// return true;
		return isBurning();
	}

	public void fillCrashReport(CrashReportCategory category) {
		super.fillCrashReport(category);
		category.addDetail("Immitating BlockState", this.fallTile.toString());
	}

	public BlockState getBlockState() {
		return this.fallTile;
	}

	public boolean ignoreItemEntityData() {
		return true;
	}

	public IPacket<?> createSpawnPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public void writeSpawnData(PacketBuffer buffer) {
		buffer.writeBoolean(isBurning());
		buffer.writeByte(slideDirection);
		buffer.writeCompoundTag(NBTUtil.writeBlockState(fallTile));
		buffer.writeCompoundTag(tileEntityData);
		syncData();
	}

	@Override
	public void readSpawnData(PacketBuffer additionalData) {
		boolean burn = additionalData.readBoolean();
		if (burn) {
			setFire(100);
		}
		slideDirection = additionalData.readByte();
		fallTile = NBTUtil.readBlockState(additionalData.readCompoundTag());
		refteshPars();
		tileEntityData = additionalData.readCompoundTag();
		if (tileEntityData != null && tileEntityData.contains("id")) {
			te = TileEntity.readTileEntity(fallTile, tileEntityData);
			if (te != null) {
				te.setWorldAndPos(world, getPosition());
				te.getBlockState();
			}
		}
		slidePos = getPosition();
		startSlide();
	}

	private BlockPhysicsPars getParam(Block b) {
		return BFUtils.getParam(b, getPosition(), world);
	}

	private BlockPhysicsPars getParam(BlockState s) {
		return getParam(s.getBlock());
	}

	private void destroyBlock(BlockPos pos, BlockState forReplace) {
		// System.out.println("x");
		synchronized (world) {
			world.destroyBlock(pos, BPOConfig.MAIN.dropDestroyedBlocks);
			world.setBlockState(pos, forReplace, 1);
		}
	}

	private void refteshPars() {
		this.pars = getParam(this.fallTile);
		this.conv = BFUtils.getConvParam(this.fallTile.getBlock());
	}

	private void landConvert(double velocity) {
		if (conv.onLand() && velocity > conv.landVel) {
			//System.out.println(conv.landState);
			this.fallTile = conv.landState;
			refteshPars();
		}
	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return super.getBoundingBox();
	}
}