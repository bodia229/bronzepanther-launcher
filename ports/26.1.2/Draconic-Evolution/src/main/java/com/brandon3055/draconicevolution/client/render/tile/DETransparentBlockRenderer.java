package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.brandonscore.client.render.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Captures the reactor's late pass alongside its normal block render snapshot. */
public interface DETransparentBlockRenderer<T extends BlockEntity> extends DEBlockRenderer<T>,BlockEntityRendererTransparent<T,DEBlockRenderer.State> {
    void renderTransparent(T tile,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay);
    @Override default void extractRenderState(T tile,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking) {
        DEBlockRenderer.super.extractRenderState(tile,state,partial,camera,breaking);
        state.transparent=ExtractedEquipmentGeometry.capture(buffers->renderTransparent(tile,partial,new PoseStack(),buffers,state.lightCoords,OverlayTexture.NO_OVERLAY));
    }
    @Override default void renderTransparent(State state,PoseStack poses,MultiBufferSource buffers,CameraRenderState camera) {
        if(state.transparent!=null)state.transparent.emit(poses,buffers);
    }
}
