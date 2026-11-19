package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.draconicevolution.blocks.machines.CraftingInjector;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingInjector;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
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

public class RenderTileCraftingInjector implements BlockEntityRenderer<TileFusionCraftingInjector, RenderTileCraftingInjector.State> {
    private final ItemModelResolver models;
    public RenderTileCraftingInjector(BlockEntityRendererProvider.Context context) { models = context.itemModelResolver(); }
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        Direction facing = Direction.UP;
        float rotation;
        boolean visible;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileFusionCraftingInjector tile, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        var core = tile.getCore();
        state.visible = !(core != null && core.isCrafting() && core.getFusionState().ordinal() > 1)
                && tile.getBlockState().hasProperty(CraftingInjector.FACING);
        if (!state.visible) return;
        state.facing = tile.getBlockState().getValue(CraftingInjector.FACING);
        state.rotation = (ClientEventHandler.elapsedTicks + partialTick) * -0.8F;
        models.updateForTopItem(state.item, tile.itemHandler.getStackInSlot(0), ItemDisplayContext.FIXED,
                tile.getLevel(), null, tile.posSeed());
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible || state.item.isEmpty()) return;
        Direction facing = state.facing;
        pose.pushPose();
        pose.translate(0.5 + facing.getStepX() * 0.45, 0.5 + facing.getStepY() * 0.45, 0.5 + facing.getStepZ() * 0.45);
        pose.scale(0.5F, 0.5F, 0.5F);
        if (facing.getAxis() == Direction.Axis.Y) {
            if (facing == Direction.DOWN) pose.mulPose(Axis.XP.rotationDegrees(180));
        } else {
            pose.mulPose(new Quaternionf().rotationXYZ((float) (facing.getStepZ() * Math.PI / 2), 0,
                    (float) (-facing.getStepX() * Math.PI / 2)));
        }
        pose.mulPose(Axis.YP.rotationDegrees(state.rotation));
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
