package com.brandon3055.brandonscore.client.render;

import com.brandon3055.brandonscore.client.shader.NativeShaderBindings;
import com.brandon3055.brandonscore.mixin.CachedRenderTypeAccess;
import com.brandon3055.brandonscore.mixin.CachedRenderSetupAccess;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.*;
import java.util.function.BiConsumer;

/** Immutable GPU geometry, with native material state resolved afresh for every draw. */
public final class CachedModelMesh implements AutoCloseable {
    private static final Set<CachedModelMesh> LIVE=Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean initialized;
    private final RenderType type;
    private final BiConsumer<VertexFormat,VertexConsumer> factory;
    private GpuBuffer vertices,ownedIndices;
    private VertexFormat.IndexType indexType;
    private int indexCount;
    private boolean closed;
    private List<ExtractedEquipmentGeometry.Vertex> capturedVertices;
    RenderType renderType(){return type;}
    List<ExtractedEquipmentGeometry.Vertex> captureVertices(){
        if(closed)throw new IllegalStateException("Cached model is closed");
        if(capturedVertices==null){
            var recorder=new ExtractedEquipmentGeometry.Recorder();factory.accept(type.format(),recorder);
            capturedVertices=recorder.freeze();LIVE.add(this);
        }
        return capturedVertices;
    }

    public CachedModelMesh(RenderType type,BiConsumer<VertexFormat,VertexConsumer> factory){
        this.type=Objects.requireNonNull(type);this.factory=Objects.requireNonNull(factory);
        // Translucent geometry needs camera-dependent quad sorting, which this immutable cache does not implement.
        if(type.hasBlending())throw new IllegalArgumentException("Cached models must be opaque: "+type);
    }
    public static void init(IEventBus bus){
        if(initialized)return;initialized=true;
        bus.addListener((AddClientReloadListenersEvent event)->event.addListener(
                Identifier.fromNamespaceAndPath("brandonscore","cached_model_meshes"),
                (ResourceManagerReloadListener)manager->invalidateAll()));

    }
    public static void closeAll(){
        for(var model:List.copyOf(LIVE))model.close();
    }
    public static void invalidateAll(){
        for(var model:List.copyOf(LIVE))model.invalidate();
    }
    public void invalidate(){
        capturedVertices=null;
        if(vertices!=null){vertices.close();vertices=null;}
        if(ownedIndices!=null){ownedIndices.close();ownedIndices=null;}
        LIVE.remove(this);
    }
    private void upload(){
        if(closed)throw new IllegalStateException("Cached model is closed");
        if(vertices!=null)return;
        try(var bytes=new ByteBufferBuilder(type.bufferSize())){
            var builder=new BufferBuilder(bytes,type.mode(),type.format());factory.accept(type.format(),builder);
            try(var mesh=builder.buildOrThrow()){
                GpuBuffer newVertices=null,newIndices=null;
                try{
                    newVertices=RenderSystem.getDevice().createBuffer(()->"BC cached model vertices",GpuBuffer.USAGE_VERTEX,mesh.vertexBuffer());
                    if(mesh.indexBuffer()!=null)newIndices=RenderSystem.getDevice().createBuffer(
                            ()->"BC cached model indices",GpuBuffer.USAGE_INDEX,mesh.indexBuffer());
                    indexCount=mesh.drawState().indexCount();indexType=mesh.drawState().indexType();
                    vertices=newVertices;ownedIndices=newIndices;LIVE.add(this);
                }catch(Throwable failure){
                    if(newVertices!=null)newVertices.close();if(newIndices!=null)newIndices.close();throw failure;
                }
            }
        }
    }
    public void draw(){
        if(ExtractedEquipmentGeometry.captureCached(this))return;
        upload();
        var setup=((CachedRenderTypeAccess)(Object)type).brandonscore$setup();
        var access=(CachedRenderSetupAccess)(Object)setup;
        var stack=RenderSystem.getModelViewStack();var layering=access.brandonscore$layering().getModifier();
        if(layering!=null)stack.pushMatrix();
        try{
            if(layering!=null)layering.accept(stack);
            var transforms=RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(),
                    new Vector4f(1),new Vector3f(),access.brandonscore$textureTransform().getMatrix());
            var settings=NativeShaderBindings.prepare(type.pipeline());
            var textures=setup.getTextures();
            GpuBuffer indices=ownedIndices;VertexFormat.IndexType actualIndexType=indexType;
            if(indices==null){
                // The shared sequence may resize when another model draws; never retain its old GPU buffer.
                var sequence=RenderSystem.getSequentialBuffer(type.mode());indices=sequence.getBuffer(indexCount);actualIndexType=sequence.type();
            }
            var target=type.outputTarget().getRenderTarget();
            var color=RenderSystem.outputColorTextureOverride!=null?RenderSystem.outputColorTextureOverride:target.getColorTextureView();
            var depth=target.useDepth?(RenderSystem.outputDepthTextureOverride!=null?RenderSystem.outputDepthTextureOverride:target.getDepthTextureView()):null;
            try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"BC cached model",color,OptionalInt.empty(),depth,OptionalDouble.empty())){
                pass.setPipeline(type.pipeline());RenderSystem.bindDefaultUniforms(pass);pass.setUniform("DynamicTransforms",transforms);
                if(settings!=null)pass.setUniform("BCSettings",settings);
                var scissor=RenderSystem.getScissorStateForRenderTypeDraws();
                if(scissor.enabled())pass.enableScissor(scissor.x(),scissor.y(),scissor.width(),scissor.height());
                pass.setVertexBuffer(0,vertices);pass.setIndexBuffer(indices,actualIndexType);
                textures.forEach((name,binding)->pass.bindTexture(name,binding.textureView(),binding.sampler()));
                pass.drawIndexed(0,0,indexCount,1);
            }
        }finally{if(layering!=null)stack.popMatrix();}
    }
    @Override public void close(){invalidate();closed=true;}
}
