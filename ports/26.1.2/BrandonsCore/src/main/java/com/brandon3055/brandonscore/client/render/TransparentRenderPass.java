package com.brandon3055.brandonscore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import java.util.*;
import java.util.function.Function;

/** Late transparency pass over the already extracted and culled native tile states. */
public final class TransparentRenderPass {
    private TransparentRenderPass(){}
    public static void render(LevelRenderState level,PoseStack poses,MultiBufferSource buffers,
            Function<BlockEntityRenderState,BlockEntityRenderer<?,?>> rendererLookup){
        var camera=level.cameraRenderState;
        if(camera==null||camera.pos==null||!camera.initialized)return;
        var states=new ArrayList<>(level.blockEntityRenderStates);
        states.sort(Comparator.comparingDouble((BlockEntityRenderState state)->state.blockPos.distToCenterSqr(camera.pos)).reversed());
        for(var state:states){
            var renderer=rendererLookup.apply(state);
            if(!(renderer instanceof BlockEntityRendererTransparent<?,?> transparent))continue;
            poses.pushPose();
            try{
                poses.translate(state.blockPos.getX()-camera.pos.x,state.blockPos.getY()-camera.pos.y,state.blockPos.getZ()-camera.pos.z);
                renderOne(transparent,state,poses,buffers,camera);
                // Keep each tile's shader settings paired with its geometry, and preserve far-to-near order.
                RenderUtils.endBatch(buffers);
            }finally{poses.popPose();}
        }
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void renderOne(BlockEntityRendererTransparent renderer,BlockEntityRenderState state,
            PoseStack poses,MultiBufferSource buffers,net.minecraft.client.renderer.state.level.CameraRenderState camera){
        renderer.renderTransparent(state,poses,buffers,camera);
    }
}
