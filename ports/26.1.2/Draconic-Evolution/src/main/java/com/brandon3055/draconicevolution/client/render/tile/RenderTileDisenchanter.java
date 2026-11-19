package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.draconicevolution.blocks.tileentity.TileDisenchanter;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
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
import org.joml.Quaternionf;

/** Resolve item models during extraction; submission never reads the live machine inventory. */
public class RenderTileDisenchanter implements BlockEntityRenderer<TileDisenchanter, RenderTileDisenchanter.State> {
    private final ItemModelResolver models;
    public RenderTileDisenchanter(BlockEntityRendererProvider.Context context) { models = context.itemModelResolver(); }
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState input = new ItemStackRenderState();
        final ItemStackRenderState books = new ItemStackRenderState();
        double time;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileDisenchanter tile, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        state.time = ClientEventHandler.elapsedTicks + partialTick;
        models.updateForTopItem(state.input, tile.itemHandler.getStackInSlot(0), ItemDisplayContext.FIXED, tile.getLevel(), null, tile.posSeed());
        models.updateForTopItem(state.books, tile.itemHandler.getStackInSlot(1), ItemDisplayContext.FIXED, tile.getLevel(), null, tile.posSeed() + 1);
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.input.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 1.15 + Math.sin(state.time / 40) / 10, 0.5);
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees((float) (state.time * 0.8)));
            state.input.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        if (!state.books.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 0.76, 0.5);
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.mulPose(new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0, (float) Math.PI));
            state.books.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
