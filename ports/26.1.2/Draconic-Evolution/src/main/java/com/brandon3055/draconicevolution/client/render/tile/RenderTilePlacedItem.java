package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.math.MathHelper;
import com.brandon3055.draconicevolution.blocks.PlacedItem;
import com.brandon3055.draconicevolution.blocks.tileentity.TilePlacedItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Created by brandon3055 on 25/07/2016.
 */
public class RenderTilePlacedItem implements BlockEntityRenderer<TilePlacedItem, RenderTilePlacedItem.State> {
    private final ItemModelResolver models;
    private record Entry(ItemStackRenderState model, boolean block, float rotation) {}
    public static final class State extends BlockEntityRenderState {
        List<Entry> items = List.of();
        Direction facing = Direction.UP;
        float scale;
    }
    @Override public State createRenderState() { return new State(); }

    public RenderTilePlacedItem(BlockEntityRendererProvider.Context context) { models = context.itemModelResolver(); }

    @Override public void extractRenderState(TilePlacedItem tile, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        List<ItemStack> stacks = tile.getStacksInOrder();
        state.scale = stacks.size() == 1 && tile.toolMode.get() ? 14 / 16F : 7 / 16F;
        state.facing = tile.getBlockState().getValue(PlacedItem.FACING);
        var items = new java.util.ArrayList<Entry>(stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            var model = new ItemStackRenderState();
            models.updateForTopItem(model, stacks.get(i), ItemDisplayContext.FIXED, tile.getLevel(), null,
                    (int) tile.getBlockPos().asLong() + i);
            items.add(new Entry(model, stacks.get(i).getItem() instanceof BlockItem, tile.rotation[i].get() * -22.5F));
        }
        state.items = List.copyOf(items);
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        rotateToSide(state.facing, pose);
        for (int i = 0; i < state.items.size(); i++) {
            Entry item = state.items.get(i);
            pose.pushPose();
            if (item.block()) {
                pose.translate(PlacedItem.getXOffset(i, state.items.size()), -0.5 + 3 / 16D, PlacedItem.getZOffset(i, state.items.size()));
                pose.mulPose(Axis.YP.rotationDegrees(item.rotation()));
                if (state.facing.getAxis() != Direction.Axis.Y) pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.scale(6 / 8F, 6 / 8F, 6 / 8F);
            } else {
                pose.translate(PlacedItem.getXOffset(i, state.items.size()), -0.5 + (0.55 / 16D) * state.scale, PlacedItem.getZOffset(i, state.items.size()));
                pose.mulPose(Axis.YP.rotationDegrees(item.rotation()));
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.scale(state.scale, state.scale, state.scale);
            }
            item.model().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
            pose.translate(0, 0.00005F, 0);
        }
        pose.popPose();
    }

    //TODO Test these
    private void rotateToSide(Direction direction, PoseStack mStack) {
        switch (direction) {
            case DOWN:
                mStack.mulPose(Axis.XP.rotationDegrees(180));
                break;
            case NORTH:
                mStack.mulPose(Axis.XN.rotationDegrees(90));
                break;
            case SOUTH:
                mStack.mulPose(new Quaternionf().rotationXYZ(-90 * (float) MathHelper.torad, 0, 180 * (float) MathHelper.torad));
                break;
            case WEST:
                mStack.mulPose(new Quaternionf().rotationXYZ(-90 * (float) MathHelper.torad, 0, 90 * (float) MathHelper.torad));
                break;
            case EAST:
                mStack.mulPose(new Quaternionf().rotationXYZ(-90 * (float) MathHelper.torad, 0, -90 * (float) MathHelper.torad));
                break;
        }
    }

    @Override
    public AABB getRenderBoundingBox(TilePlacedItem blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1);
    }
}
