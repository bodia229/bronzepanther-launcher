package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.DEOldConfig;
import com.brandon3055.draconicevolution.blocks.tileentity.TileDislocatorPedestal;
import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.items.tools.DislocatorAdvanced;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class RenderTileDislocatorPedestal implements BlockEntityRenderer<TileDislocatorPedestal, RenderTileDislocatorPedestal.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
    private final BlockModelResolver blocks;
    private final ItemModelResolver items;
    private final Font font;
    public RenderTileDislocatorPedestal(BlockEntityRendererProvider.Context context) {
        blocks = context.blockModelResolver(); items = context.itemModelResolver(); font = context.font();
    }
    public static final class State extends BlockEntityRenderState {
        final BlockModelRenderState pedestal = new BlockModelRenderState();
        final ItemStackRenderState item = new ItemStackRenderState();
        float rotation, yaw, pitch, labelX;
        FormattedCharSequence label;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileDislocatorPedestal tile, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        blocks.update(state.pedestal, DEContent.DISLOCATOR_PEDESTAL.get().defaultBlockState(), DISPLAY);
        var stack = tile.itemHandler.getStackInSlot(0);
        items.updateForTopItem(state.item, stack, ItemDisplayContext.FIXED, tile.getLevel(), null, tile.posSeed());
        state.rotation = -tile.rotation.get() * 22.5F;
        state.label = null;
        var player = Minecraft.getInstance().player;
        if (stack.isEmpty() || player == null) return;
        var hit = player.pick(10, partialTick, true);
        boolean hovered = hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(tile.getBlockPos());
        if (!hovered && player.isShiftKeyDown() != DEOldConfig.invertDPDSB) return;
        String name = stack.getOrDefault(DataComponents.CUSTOM_NAME, Component.empty()).getString();
        if (stack.getItem() instanceof DislocatorAdvanced dislocator) {
            var target = dislocator.getSelected(stack);
            if (target != null) name = target.getName();
        }
        if (name.isEmpty()) return;
        state.label = Component.literal(name).getVisualOrderText();
        state.labelX = -font.width(name) / 2F;
        double dx = player.getX() - (tile.getBlockPos().getX() + 0.5);
        double dy = player.getY() + player.getEyeHeight() - (tile.getBlockPos().getY() + 1.125);
        double dz = player.getZ() - (tile.getBlockPos().getZ() + 0.5);
        state.yaw = (float) Math.toDegrees(Math.atan2(dz, dx));
        state.pitch = (float) -Math.toDegrees(Math.atan2(dy, Utils.getDistance(player.getX(), player.getY(), player.getZ(),
                tile.getBlockPos().getX() + 0.5, tile.getBlockPos().getY() + 0.5, tile.getBlockPos().getZ() + 0.5)));
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.rotation));
        pose.translate(-0.5, -0.5, -0.5);
        state.pedestal.submitMultiLayer(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (!state.item.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 0.79, 0.52);
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.mulPose(Axis.XP.rotationDegrees(-67.5F));
            state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
        if (state.label != null) {
            pose.pushPose();
            pose.translate(0.5, 1.125, 0.5);
            pose.scale(0.02F, 0.02F, 0.02F);
            pose.mulPose(new Quaternionf().rotationXYZ(0, (float) (-Math.PI / 2), (float) Math.PI));
            pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(state.pitch));
            pose.scale(1, 1, -1);
            pose.translate(0, 0, -0.0125);
            collector.submitText(pose, state.labelX, 0, state.label, true, Font.DisplayMode.NORMAL, 15728880, 0xFFFFFFFF, 0, 0);
            pose.popPose();
        }
    }
}
