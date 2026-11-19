package com.brandon3055.brandonscore.client;

import codechicken.lib.colour.EnumColour;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.api.IFOVModifierItem;
import com.brandon3055.brandonscore.blocks.BlockBCore;
import com.brandon3055.brandonscore.client.render.BlockEntityRendererTransparent;
import com.brandon3055.brandonscore.handlers.BCEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;

import codechicken.lib.render.CCRenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;

/**
 * Created by brandon3055 on 17/07/2016.
 */
public class BCClientEventHandler {
    private static final CrashLock LOCK = new CrashLock("Already Initialized");

    private static int remountTicksRemaining = 0;
    private static int remountEntityID = 0;
    private static int debugTimeout = 0;
    private static Map<ResourceKey<Level>, Integer[]> dimTickTimes = new HashMap<>();
    private static Integer[] overallTickTime = new Integer[200];
    private static int renderIndex = 0;
    private static LinkedList<ResourceKey<Level>> sortingOrder = new LinkedList<>();
    public static int elapsedTicks = 0;

    private static Comparator<ResourceKey<Level>> sorter = (value, compare) -> {
        long totalValue = 0;
        for (Integer time : dimTickTimes.get(value)) {
            totalValue += time;
        }
        totalValue /= 200;

        long totalCompare = 0;
        for (Integer time : dimTickTimes.get(compare)) {
            totalCompare += time;
        }
        totalCompare /= 200;

        return Long.compare(totalCompare, totalValue);
    };

    public static void init() {
        LOCK.lock();

        NeoForge.EVENT_BUS.register(new BCClientEventHandler());
    }

    @SubscribeEvent
    public void disconnectEvent(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            BCEventHandler.noClipPlayers.remove(mc.player.getUUID());
        }
    }

    @SubscribeEvent
    public void tickEnd(ClientTickEvent.Post event) {
        elapsedTicks++;
        if (Minecraft.getInstance().isPaused()) {
            return;
        }

        if (debugTimeout > 0) {
            debugTimeout--;
        }

        if (elapsedTicks % 100 == 0 && debugTimeout > 0) {
            sortingOrder.clear();
            sortingOrder.addAll(dimTickTimes.keySet());
            sortingOrder.sort(sorter);
        }
    }

    @SubscribeEvent
    public void drawSelectionEvent(ExtractBlockOutlineRenderStateEvent event) {
        Level level = Minecraft.getInstance().level;
        if (event.getHitResult().getType() == HitResult.Type.MISS || level == null) return;
        BlockState state = level.getBlockState(event.getHitResult().getBlockPos());
        if (state.getBlock() instanceof BlockBCore block) {
            if (!block.renderSelectionBox(event, level)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void fovUpdate(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        float originalFOV = event.getFovModifier();
        float newFOV = originalFOV;

        for (EquipmentSlot slot : List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD)) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof IFOVModifierItem) {
                newFOV = ((IFOVModifierItem) stack.getItem()).getNewFOV(player, stack, newFOV, originalFOV, slot);
            }
        }

        ItemStack stack = player.getOffhandItem();
        if (!stack.isEmpty() && stack.getItem() instanceof IFOVModifierItem) {
            newFOV = ((IFOVModifierItem) stack.getItem()).getNewFOV(player, stack, newFOV, originalFOV, EquipmentSlot.OFFHAND);
        }
        stack = player.getMainHandItem();
        if (!stack.isEmpty() && stack.getItem() instanceof IFOVModifierItem) {
            newFOV = ((IFOVModifierItem) stack.getItem()).getNewFOV(player, stack, newFOV, originalFOV, EquipmentSlot.MAINHAND);
        }

        if (newFOV != originalFOV) {
            event.setNewFovModifier(newFOV);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void renderLevelStage(RenderLevelStageEvent.AfterTranslucentParticles event) {
        var dispatcher=Minecraft.getInstance().getBlockEntityRenderDispatcher();
        com.brandon3055.brandonscore.client.render.TransparentRenderPass.render(event.getLevelRenderState(),
                event.getPoseStack(),Minecraft.getInstance().renderBuffers().bufferSource(),state->dispatcher.getRenderer(state));
    }

    private static final RenderType boxNoDepth = RenderType.create("brandonscore:box_no_depth",
            RenderSetup.builder(CCRenderPipelines.POSITION_COLOR_NO_DEPTH).bufferSize(256).sortOnUpload().createRenderSetup());

    @SubscribeEvent(priority = EventPriority.LOW)
    public void renderDebugStage(RenderLevelStageEvent.AfterOpaqueBlocks event) { doDebugRendering(event); }

    private static final Cuboid6 BOX = Cuboid6.full.copy().expand(0.02);
    private static final float[] RED = EnumColour.RED.getColour(128).packArray();

    public static List<Vector3> debugBlockList = null;
    int i = 0;
    public void doDebugRendering(RenderLevelStageEvent event) {
        if (debugBlockList == null) return;

        MultiBufferSource.BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        PoseStack pStack = event.getPoseStack();
        pStack.pushPose();

        if (i++ % 100 == 0) {
            debugBlockList.sort(Comparator.comparingDouble(value -> value.distanceSquared(Vector3.fromEntity(Minecraft.getInstance().player))));
        }

        pStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        TransformingVertexConsumer consumer = new TransformingVertexConsumer(source.getBuffer(boxNoDepth), pStack);

        int i = 0;
        for (Vector3 pos : debugBlockList) {
            i++;
            RenderUtils.bufferCuboidSolid(
                    consumer,
                    BOX.copy().add(pos),
                    RED[0], RED[1], RED[2], RED[3]
            );
            if (i > 1000) break;;
        }


        source.endBatch();
        pStack.popPose();

    }
}
