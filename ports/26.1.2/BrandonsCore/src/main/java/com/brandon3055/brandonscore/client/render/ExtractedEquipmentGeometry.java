package com.brandon3055.brandonscore.client.render;

import com.brandon3055.brandonscore.client.shader.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.*;
import java.util.function.Consumer;

/** Converts legacy model emission at extraction time into detached native geometry submissions. */
public final class ExtractedEquipmentGeometry {
    private static final ThreadLocal<Builder> ACTIVE=new ThreadLocal<>();
    private final List<Geometry> geometry;
    private ExtractedEquipmentGeometry(List<Geometry> geometry){this.geometry=List.copyOf(geometry);}
    public static ExtractedEquipmentGeometry capture(Consumer<MultiBufferSource> render){
        var builder=new Builder();var previous=ACTIVE.get();ACTIVE.set(builder);
        try{render.accept(builder);builder.flush();return new ExtractedEquipmentGeometry(builder.result);}
        finally{if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}
    }
    public int drawCount(){return geometry.size();}
    /** Reuse immutable vertices while capturing this frame's animated shader uniforms. */
    public ExtractedEquipmentGeometry snapshotShaderState(){
        return new ExtractedEquipmentGeometry(geometry.stream().map(draw->new Geometry(draw.type(),draw.vertices(),NativeShaderBindings.capture(draw.type()),draw.local(),draw.light(),draw.overlay())).toList());
    }
    /** Append immutable model vertices with this extraction's pose, light and uniforms. */
    public static boolean captureInstance(ExtractedEquipmentGeometry mesh,org.joml.Matrix4fc local,int light,int overlay){
        var builder=ACTIVE.get();if(builder==null)return false;builder.flush();
        for(var draw:mesh.geometry){
            var transform=new org.joml.Matrix4f(local);
            if(draw.local()!=null)transform.mul(draw.local());
            builder.result.add(new Geometry(draw.type(),draw.vertices(),NativeShaderBindings.capture(draw.type()),transform,light,overlay));
        }
        return true;
    }
    public static void flush(MultiBufferSource buffers){if(buffers instanceof Builder builder)builder.flush();}
    static boolean captureCached(CachedModelMesh mesh){
        var builder=ACTIVE.get();if(builder==null)return false;builder.flush();
        builder.result.add(new Geometry(mesh.renderType(),mesh.captureVertices(),NativeShaderBindings.capture(mesh.renderType().pipeline())));return true;
    }
    public void submit(PoseStack poses,SubmitNodeCollector collector){
        submit(poses,collector,-1);
    }
    /** Instance lighting is applied during emission, without rebuilding immutable model vertices. */
    public void submit(PoseStack poses,SubmitNodeCollector collector,int packedLight){
        var outer=new org.joml.Matrix4f(poses.last().pose());
        for(var draw:geometry){
            poses.pushPose();
            if(draw.local()!=null)poses.mulPose(draw.local());
            var shader=draw.shader()==null?null:draw.shader().transformed(poses.last().pose());
            var type=NativeShaderBindings.frozenType(draw.type(),shader);
            boolean transform=shader==null||!shader.hasModelMatrix();
            int light=packedLight<0?draw.light():packedLight;
            try{collector.submitCustomGeometry(poses,type,(pose,consumer)->draw.vertices().forEach(vertex->vertex.emit(pose,consumer,transform,light,draw.overlay())));}
            finally{poses.popPose();}
        }
    }
    /** Replays an extracted snapshot in the existing native late transparent pass. */
    public void emit(PoseStack poses,MultiBufferSource buffers) {
        var outer=new org.joml.Matrix4f(poses.last().pose());
        for(var draw:geometry){
            poses.pushPose();
            if(draw.local()!=null)poses.mulPose(draw.local());
            var shader=draw.shader()==null?null:draw.shader().transformed(poses.last().pose());
            var type=NativeShaderBindings.frozenType(draw.type(),shader);
            boolean transform=shader==null||!shader.hasModelMatrix();
            var consumer=buffers.getBuffer(type);
            try{draw.vertices().forEach(vertex->vertex.emit(poses.last(),consumer,transform,draw.light(),draw.overlay()));}
            finally{poses.popPose();}
        }
    }
    private record Geometry(RenderType type,List<Vertex> vertices,BCShader.Snapshot shader,org.joml.Matrix4f local,int light,int overlay){
        Geometry(RenderType type,List<Vertex> vertices,BCShader.Snapshot shader){this(type,vertices,shader,null,-1,-1);}
    }
    static record Vertex(float x,float y,float z,int color,float u,float v,int u1,int v1,int u2,int v2,float nx,float ny,float nz,float width){
        void emit(PoseStack.Pose pose,VertexConsumer consumer,boolean transform){
            emit(pose,consumer,transform,-1);
        }
        void emit(PoseStack.Pose pose,VertexConsumer consumer,boolean transform,int packedLight){
            emit(pose,consumer,transform,packedLight,-1);
        }
        void emit(PoseStack.Pose pose,VertexConsumer consumer,boolean transform,int packedLight,int packedOverlay){
            if(transform)consumer.addVertex(pose,x,y,z);else consumer.addVertex(x,y,z);
            consumer.setColor(color).setUv(u,v);
            if(packedOverlay<0)consumer.setUv1(u1,v1);else consumer.setOverlay(packedOverlay);
            if(packedLight<0)consumer.setUv2(u2,v2);else consumer.setLight(packedLight);
            consumer.setLineWidth(width);
            if(transform)consumer.setNormal(pose,nx,ny,nz);else consumer.setNormal(nx,ny,nz);
        }
    }
    static final class Recorder implements VertexConsumer {
        private final List<Vertex> vertices=new ArrayList<>();
        private boolean current;private float x,y,z,u,v,nx,ny,nz,width;private int color,u1,v1,u2,v2;
        private void finish(){if(current)vertices.add(new Vertex(x,y,z,color,u,v,u1,v1,u2,v2,nx,ny,nz,width));current=false;}
        List<Vertex> freeze(){finish();return List.copyOf(vertices);}
        @Override public VertexConsumer addVertex(float x,float y,float z){finish();this.x=x;this.y=y;this.z=z;color=-1;u=v=nx=ny=nz=0;u1=u2=v2=0;v1=10;width=1;current=true;return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){return setColor((a&255)<<24|(r&255)<<16|(g&255)<<8|(b&255));}
        @Override public VertexConsumer setColor(int color){this.color=color;return this;}
        @Override public VertexConsumer setUv(float u,float v){this.u=u;this.v=v;return this;}
        @Override public VertexConsumer setUv1(int u,int v){u1=u;v1=v;return this;}
        @Override public VertexConsumer setUv2(int u,int v){u2=u;v2=v;return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){nx=x;ny=y;nz=z;return this;}
        @Override public VertexConsumer setLineWidth(float width){this.width=width;return this;}
    }
    private static final class Builder implements MultiBufferSource {
        final List<Geometry> result=new ArrayList<>();private RenderType type;private Recorder recorder;
        @Override public VertexConsumer getBuffer(RenderType next){
            if(type!=next){flush();type=next;recorder=new Recorder();}return recorder;
        }
        void flush(){
            if(recorder!=null){var vertices=recorder.freeze();if(!vertices.isEmpty())result.add(new Geometry(type,vertices,NativeShaderBindings.capture(type.pipeline())));}
            recorder=null;type=null;
        }
    }
}
