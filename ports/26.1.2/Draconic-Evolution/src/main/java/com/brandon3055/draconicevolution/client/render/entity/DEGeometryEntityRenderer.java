package com.brandon3055.draconicevolution.client.render.entity;

import com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Captures authored entity geometry during extraction, without retaining live entities for submission. */
public abstract class DEGeometryEntityRenderer<T extends Entity> extends EntityRenderer<T,DEGeometryEntityRenderer.State> {
    protected DEGeometryEntityRenderer(EntityRendererProvider.Context context) { super(context); }
    public static class State extends EntityRenderState {
        public ExtractedEquipmentGeometry geometry;
    }
    @Override public State createRenderState() { return new State(); }
    protected abstract void extractGeometry(T entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light);
    @Override public void extractRenderState(T entity,State state,float partial) {
        super.extractRenderState(entity,state,partial);
        float yaw=Mth.rotLerp(partial,entity.yRotO,entity.getYRot());
        state.geometry=ExtractedEquipmentGeometry.capture(buffers->extractGeometry(entity,yaw,partial,new PoseStack(),buffers,state.lightCoords));
    }
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera) {
        if(state.geometry!=null)state.geometry.submit(poses,collector);
        super.submit(state,poses,collector,camera);
    }
}
