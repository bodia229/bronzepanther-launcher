package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.draconicevolution.blocks.tileentity.TileStabilizedSpawner;
import com.brandon3055.draconicevolution.init.DEContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RenderTileStabilizedSpawner implements BlockEntityRenderer<TileStabilizedSpawner, RenderTileStabilizedSpawner.State> {
    private final EntityRenderDispatcher entities;
    private final ItemModelResolver items;
    public RenderTileStabilizedSpawner(BlockEntityRendererProvider.Context context) {
        entities = context.entityRenderer(); items = context.itemModelResolver();
    }
    public static final class State extends SpawnerRenderState {
        final ItemStackRenderState core = new ItemStackRenderState();
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileStabilizedSpawner tile, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        state.displayEntity = null;
        var spawner = tile.spawnerLogic;
        var display = spawner.getOrCreateDisplayEntity(tile.getLevel(), tile.getBlockPos());
        if (display != null) {
            state.displayEntity = entities.extractEntity(display, partialTick);
            state.displayEntity.lightCoords = state.lightCoords;
            state.spin = (float) net.minecraft.util.Mth.lerp((double) partialTick, spawner.getOSpin(), spawner.getSpin()) * 10;
            float size = Math.max(display.getBbWidth(), display.getBbHeight());
            state.scale = 0.53125F / Math.max(size, 1);
        }
        var coreItem=switch(tile.spawnerTier.get().ordinal()) {
            case 0 -> DEContent.CORE_DRACONIUM.get();case 1 -> DEContent.CORE_WYVERN.get();
            case 2 -> DEContent.CORE_AWAKENED.get();default -> DEContent.CORE_CHAOTIC.get();
        };
        items.updateForTopItem(state.core, new ItemStack(coreItem), ItemDisplayContext.FIXED,
                tile.getLevel(), null, tile.posSeed());
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.displayEntity != null) {
            SpawnerRenderer.submitEntityInSpawner(pose, collector, state.displayEntity, entities, state.spin, state.scale, camera);
        }
        pose.pushPose();
        pose.translate(0.5, 1 - 1 / 32F, 0.5);
        pose.scale(0.75F, 0.75F, 0.75F);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        state.core.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
    @Override public AABB getRenderBoundingBox(TileStabilizedSpawner tile) { return new AABB(tile.getBlockPos()).inflate(1); }
}
