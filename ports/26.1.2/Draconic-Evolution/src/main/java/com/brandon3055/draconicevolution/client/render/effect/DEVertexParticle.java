package com.brandon3055.draconicevolution.client.render.effect;

import com.brandon3055.brandonscore.client.particle.BCParticle;
import com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;

/** Extracts legacy vertex effects once; deferred draws retain no particle/tile references. */
public abstract class DEVertexParticle extends BCParticle {
    public static final ParticleRenderType GROUP=new ParticleRenderType("DRACONIC_VERTEX_EFFECTS");
    protected DEVertexParticle(ClientLevel level,double x,double y,double z) { super(level,new Vec3(x,y,z)); }
    protected DEVertexParticle(ClientLevel level,Vec3 pos) { super(level,pos); }
    protected DEVertexParticle(ClientLevel level,Vec3 pos,Vec3 speed) { super(level,pos,speed); }
    @Override public final ParticleRenderType getGroup() { return GROUP; }
    public abstract RenderType renderType();
    private static final ThreadLocal<net.minecraft.client.renderer.MultiBufferSource> ACTIVE_BUFFERS=new ThreadLocal<>();
    protected final net.minecraft.client.renderer.MultiBufferSource extractionBuffers() {
        var buffers=ACTIVE_BUFFERS.get();
        if(buffers==null)throw new IllegalStateException("Particle geometry must be extracted before submission");
        return buffers;
    }
    protected boolean useFrustumCulling() { return true; }
    @Override public abstract void render(VertexConsumer vertices,Camera camera,float partial);
    public static void init(IEventBus bus) {
        bus.addListener((RegisterParticleGroupsEvent event)->event.register(GROUP,Group::new));
    }
    private static final class Group extends ParticleGroup<DEVertexParticle> {
        Group(ParticleEngine engine) { super(engine); }
        @Override public ParticleGroupRenderState extractRenderState(Frustum frustum,Camera camera,float partial) {
            var geometry=ExtractedEquipmentGeometry.capture(buffers->{
                for(var particle:particles) {
                    if(particle.useFrustumCulling()&&frustum!=null&&!frustum.isVisible(particle.getBoundingBox().inflate(.5)))continue;
                    var previous=ACTIVE_BUFFERS.get();ACTIVE_BUFFERS.set(buffers);
                    try { particle.render(buffers.getBuffer(particle.renderType()),camera,partial); }
                    finally { if(previous==null)ACTIVE_BUFFERS.remove();else ACTIVE_BUFFERS.set(previous); }
                    ExtractedEquipmentGeometry.flush(buffers);
                }
            });
            return (collector,cameraState)->geometry.submit(new PoseStack(),collector);
        }
    }
}
