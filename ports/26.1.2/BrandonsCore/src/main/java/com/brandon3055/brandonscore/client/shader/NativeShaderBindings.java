package com.brandon3055.brandonscore.client.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.brandon3055.brandonscore.mixin.CachedRenderTypeAccess;

/** Binds only BC pipelines; vanilla render types pass through untouched. */
public final class NativeShaderBindings {
    private static final Map<RenderPipeline,BCShader<?>> SHADERS=new IdentityHashMap<>();
    private static final Map<RenderType,BCShader.Snapshot> FROZEN=new WeakHashMap<>();
    private static DynamicUniformStorage<BCShader.Snapshot> storage;
    private static boolean initialized;
    private NativeShaderBindings(){}
    static void register(RenderPipeline pipeline,BCShader<?> shader){SHADERS.put(pipeline,shader);}
    public static BCShader.Snapshot capture(RenderPipeline pipeline){var shader=SHADERS.get(pipeline);return shader==null?null:shader.drawSettings();}
    public static BCShader.Snapshot capture(RenderType type){var frozen=FROZEN.get(type);return frozen==null?capture(type.pipeline()):frozen;}
    public static RenderType frozenType(RenderType original,BCShader.Snapshot snapshot){
        if(snapshot==null)return original;
        var type=RenderType.create("brandonscore:frozen_equipment",((CachedRenderTypeAccess)(Object)original).brandonscore$setup());
        FROZEN.put(type,snapshot);return type;
    }
    public static GpuBufferSlice prepare(RenderType type){
        var snapshot=FROZEN.get(type);return snapshot==null?prepare(type.pipeline()):upload(snapshot);
    }
    private static GpuBufferSlice upload(BCShader.Snapshot snapshot){
        if(storage==null)storage=new DynamicUniformStorage<>("BrandonsCore model settings",BCShader.SETTINGS_BYTES,16);
        return storage.writeUniform(snapshot);
    }
    public static void init(){
        if(initialized)return;initialized=true;
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post event)->{if(storage!=null)storage.endFrame();});

    }
    public static void close(){if(storage!=null){storage.close();storage=null;}}
    public static GpuBufferSlice prepare(RenderPipeline pipeline){
        BCShader<?> shader=SHADERS.get(pipeline);if(shader==null)return null;
        // Mapping/uploading must finish before RenderType opens its render pass.
        return upload(shader.drawSettings());
    }
}
