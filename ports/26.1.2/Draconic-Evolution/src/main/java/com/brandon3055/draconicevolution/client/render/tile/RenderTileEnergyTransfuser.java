package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyTransfuser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class RenderTileEnergyTransfuser implements BlockEntityRenderer<TileEnergyTransfuser, RenderTileEnergyTransfuser.State> {
    private static final Style GALACTIC_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace("alt")));
    public static final Component[] TEXT = {
        Component.literal("N").withStyle(GALACTIC_STYLE), Component.literal("E").withStyle(GALACTIC_STYLE),
        Component.literal("S").withStyle(GALACTIC_STYLE), Component.literal("W").withStyle(GALACTIC_STYLE)
    };
    private static final Direction[] DIRECTIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private final ItemModelResolver models;
    public RenderTileEnergyTransfuser(BlockEntityRendererProvider.Context context) { models = context.itemModelResolver(); }
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState()};
        final int[] colors = new int[4];
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileEnergyTransfuser tile, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        for (int i = 0; i < 4; i++) {
            state.colors[i] = 0xFF000000 | tile.ioModes[i].get().getColour();
            models.updateForTopItem(state.items[i], tile.itemsCombined.getStackInSlot(i), ItemDisplayContext.FIXED,
                    tile.getLevel(), null, tile.posSeed() + i);
        }
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        for (int i = 0; i < 4; i++) {
            Direction direction = DIRECTIONS[i];
            pose.pushPose();
            pose.translate(0.5 * direction.getStepX(), 0, 0.5 * direction.getStepZ());
            pose.mulPose(direction.getRotation());
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90));
            double offset = direction == Direction.NORTH ? 2 : direction == Direction.SOUTH ? 1 : 2.5;
            pose.translate(-offset / 16, -3.5 / 16, 1.375 / 16);
            pose.scale(0.0625F, 0.0625F, 0.0625F);
            collector.submitText(pose, 0, 0, TEXT[i].getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                    15728880, state.colors[i], 0, 0);
            pose.popPose();
            if (!state.items[i].isEmpty()) {
                pose.mulPose(new Quaternionf().rotationYXZ((float) (Math.PI / 2), (float) (Math.PI / 2), (float) Math.PI));
                pose.translate(0, 0, 0.75 / 16);
                pose.scale(0.5F, 0.5F, 0.5F);
                state.items[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
            pose.popPose();
        }
        pose.popPose();
    }
}
