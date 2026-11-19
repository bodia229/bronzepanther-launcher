package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.draconicevolution.blocks.tileentity.TileEntityDetector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class RenderTileEntityDetector implements BlockEntityRenderer<TileEntityDetector, RenderTileEntityDetector.State> {
    private final ItemModelResolver models;
    public RenderTileEntityDetector(BlockEntityRendererProvider.Context context) { models = context.itemModelResolver(); }
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float yaw, pitch, scale;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileEntityDetector tile, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        boolean advanced = tile.isAdvanced();
        state.scale = advanced ? 0.5F : 0.35F;
        state.yaw = tile.lastLookYaw + (tile.lookYaw - tile.lastLookYaw) * partialTick;
        state.pitch = tile.lastLookPitch + (tile.lookPitch - tile.lastLookPitch) * partialTick;
        models.updateForTopItem(state.item, new ItemStack(advanced ? Items.WITHER_SKELETON_SKULL : Items.ENDER_EYE), ItemDisplayContext.FIXED, tile.getLevel(), null, tile.posSeed());
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.73, 0.5);
        pose.scale(state.scale, state.scale, state.scale);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(state.pitch));
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
    @Override public AABB getRenderBoundingBox(TileEntityDetector tile) { return new AABB(tile.getBlockPos()).expandTowards(0, 1, 0); }
}
