package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** All tile reads happen during extraction; native submission replays detached geometry. */
public interface DEBlockRenderer<T extends BlockEntity> extends BlockEntityRenderer<T,DEBlockRenderer.State> {
    ThreadLocal<java.util.List<NativeDraw>> ACTIVE_NATIVE = new ThreadLocal<>();
    record NativeDraw(org.joml.Matrix4fc transform,java.util.function.BiConsumer<PoseStack,SubmitNodeCollector> draw) {}
    static void addNative(PoseStack poses,java.util.function.BiConsumer<PoseStack,SubmitNodeCollector> draw) {
        var active=ACTIVE_NATIVE.get();
        if(active==null)throw new IllegalStateException("Native tile submissions must be captured during extraction");
        active.add(new NativeDraw(new org.joml.Matrix4f(poses.last().pose()),draw));
    }
    class State extends BlockEntityRenderState {
        public java.util.List<NativeDraw> nativeDraws=java.util.List.of();
        public ExtractedEquipmentGeometry geometry;
        public ExtractedEquipmentGeometry transparent;
    }
    void render(T tile,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay);
    @Override default State createRenderState() { return new State(); }
    @Override default void extractRenderState(T tile,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(tile,state,partial,camera,breaking);
        var draws=new java.util.ArrayList<NativeDraw>();var previous=ACTIVE_NATIVE.get();ACTIVE_NATIVE.set(draws);
        try { state.geometry=ExtractedEquipmentGeometry.capture(buffers->render(tile,partial,new PoseStack(),buffers,state.lightCoords,OverlayTexture.NO_OVERLAY)); }
        finally { if(previous==null)ACTIVE_NATIVE.remove();else ACTIVE_NATIVE.set(previous); }
        state.nativeDraws=java.util.List.copyOf(draws);
    }
    @Override default void submit(State state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera) {
        if(state.geometry!=null)state.geometry.submit(poses,collector);
        for(var nativeDraw:state.nativeDraws) {
            poses.pushPose();try { poses.mulPose(nativeDraw.transform());nativeDraw.draw().accept(poses,collector); }
            finally { poses.popPose(); }
        }
    }
}
