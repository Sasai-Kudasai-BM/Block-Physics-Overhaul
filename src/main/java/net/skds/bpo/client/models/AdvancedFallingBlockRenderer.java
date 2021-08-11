package net.skds.bpo.client.models;

import java.util.Random;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.skds.bpo.BPOConfig;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;

@OnlyIn(Dist.CLIENT)
public class AdvancedFallingBlockRenderer extends EntityRenderer<AdvancedFallingBlockEntity> {
	public AdvancedFallingBlockRenderer(EntityRendererManager renderManagerIn) {
		super(renderManagerIn);
		this.shadowSize = 0.5F;
	}

	@Override
	public boolean shouldRender(AdvancedFallingBlockEntity livingEntityIn, ClippingHelper camera, double camX,
			double camY, double camZ) {
		double d0 = this.renderManager.squareDistanceTo(livingEntityIn);
		final int d = 64;
		if (d0 > d * d) {
			return false;
		}
		if (!livingEntityIn.isInRangeToRender3d(camX, camY, camZ)) {
			return false;
		} else if (livingEntityIn.ignoreFrustumCheck) {
			return true;
		} else {
			AxisAlignedBB axisalignedbb = livingEntityIn.getRenderBoundingBox().grow(0.5D);
			if (axisalignedbb.hasNaN() || axisalignedbb.getAverageEdgeLength() == 0.0D) {
				axisalignedbb = new AxisAlignedBB(livingEntityIn.getPosX() - 2.0D, livingEntityIn.getPosY() - 2.0D,
						livingEntityIn.getPosZ() - 2.0D, livingEntityIn.getPosX() + 2.0D,
						livingEntityIn.getPosY() + 2.0D, livingEntityIn.getPosZ() + 2.0D);
			}

			return camera.isBoundingBoxInFrustum(axisalignedbb);
		}
	}

	@Override
	public Vector3d getRenderOffset(AdvancedFallingBlockEntity entityIn, float partialTicks) {
		return super.getRenderOffset(entityIn, partialTicks);		
	}

	@SuppressWarnings("deprecation")
	public void render(AdvancedFallingBlockEntity entityIn, float entityYaw, float partialTicks,
			MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {

		if (entityIn.slideProgress >= 0) {
			BlockPos pos = entityIn.slidePos;

			Direction dir = Direction.byHorizontalIndex(entityIn.slideDirectionV);

			Vector3d cam = renderManager.info.getProjectedView();
			double x = pos.getX() - cam.x + 0.5;
			double y = pos.getY() - cam.y + 0.0;
			double z = pos.getZ() - cam.z + 0.5;

			matrixStackIn.pop();
			matrixStackIn.push();

			double progress = (entityIn.slideProgress + partialTicks) / (BPOConfig.SLIDESTEPS + 1);
			double phase = progress * Math.PI / 2;

			double cos = Math.cos(phase);
			double sin = Math.sin(phase);

			x += dir.getXOffset() * sin;
			y += cos - 1;
			z += dir.getZOffset() * sin;

			matrixStackIn.translate(x, y, z);

			if (pos.distanceSq(entityIn.getPosition()) > 2.5) {
				entityIn.setPosition(pos.getX() + dir.getXOffset() + 0.5, pos.getY() - 1, pos.getZ() + dir.getZOffset() + 0.5);

				//System.out.println(entityIn.slidePos + " " + entityIn.getPosition() + " " + entityIn.getPositionVec());
			}
		} else {
			Vector3d cam = renderManager.info.getProjectedView();
			Vector3d motion = entityIn.getMotion();
			float f = 1F - partialTicks;
			double x = entityIn.getPosX() - cam.x - (motion.x * f);
			double y = entityIn.getPosY() - cam.y - (motion.y * f);
			double z = entityIn.getPosZ() - cam.z - (motion.z * f);

			matrixStackIn.pop();
			matrixStackIn.push();

			matrixStackIn.translate(x, y, z);
		}

		//matrixStackIn.push();

		BlockState blockstate = entityIn.getBlockState();
		if (blockstate.getRenderType() == BlockRenderType.MODEL) {
			World world = entityIn.getWorldObj();
			if (blockstate != world.getBlockState(entityIn.getPosition())
					&& blockstate.getRenderType() != BlockRenderType.INVISIBLE) {
						
				BlockPos blockpos = new BlockPos(entityIn.getPosX(), entityIn.getBoundingBox().maxY,
				entityIn.getPosZ());
				matrixStackIn.push();
				matrixStackIn.translate(-0.5D, 0.0D, -0.5D);
				BlockRendererDispatcher blockrendererdispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
				for (RenderType type : RenderType.getBlockRenderTypes()) {
					if (RenderTypeLookup.canRenderInLayer(blockstate, type)) {
						ForgeHooksClient.setRenderLayer(type);
						blockrendererdispatcher.getBlockModelRenderer().renderModel(world,
								blockrendererdispatcher.getModelForState(blockstate), blockstate, blockpos,
								matrixStackIn, bufferIn.getBuffer(type), false, new Random(),
								blockstate.getPositionRandom(entityIn.slidePos), OverlayTexture.NO_OVERLAY);
					}
				}
				ForgeHooksClient.setRenderLayer(null);
				matrixStackIn.pop();
				super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
			}
		}
		if (entityIn.tileEntityData != null) {
			matrixStackIn.push();
			matrixStackIn.translate(-0.5D, 0.0D, -0.5D);
			World world = entityIn.getWorldObj();
			TileEntity te = entityIn.te;
			te.setWorldAndPos(world, entityIn.getPosition());
			TileEntityRendererDispatcher TERD = TileEntityRendererDispatcher.instance;
			TERD.renderTileEntity(te, partialTicks, matrixStackIn, bufferIn);
			//TERD.getRenderer(te).render(te, partialTicks, matrixStackIn, bufferIn, packedLightIn, OverlayTexture.NO_OVERLAY);

			matrixStackIn.pop();
			super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
		}
		//matrixStackIn.pop();
	}

	@SuppressWarnings("deprecation")
	public ResourceLocation getEntityTexture(AdvancedFallingBlockEntity entity) {
		return AtlasTexture.LOCATION_BLOCKS_TEXTURE;
	}
}
