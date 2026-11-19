package com.brandon3055.brandonscore.client.shader;

import com.google.gson.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.function.Consumer;

/** Native model pipelines with immutable per-draw std140 settings. */
public class BCShader<T extends BCShader<T>> {
    public static final int SETTINGS_BYTES = 144;
    private static final Map<String,Integer> OFFSETS = Map.ofEntries(
            Map.entry("ModelMat",0),Map.entry("BaseColor",64),Map.entry("UV1Override",80),Map.entry("UV2Override",88),
            Map.entry("Time",96),Map.entry("Decay",100),Map.entry("Transition",104),Map.entry("Hue",108),
            Map.entry("Yaw",112),Map.entry("Pitch",116),Map.entry("Alpha",120),Map.entry("SimpleLight",124),
            Map.entry("DisableLight",128),Map.entry("DisableOverlay",132),Map.entry("Activation",136));
    private final Identifier location;
    private final VertexFormat format;
    private final boolean entityLighting;
    private final List<String> effectSamplers = new ArrayList<>();
    private final ByteBuffer staging = ByteBuffer.allocate(SETTINGS_BYTES).order(ByteOrder.nativeOrder());
    private final Map<String,BCUniform> uniforms = new HashMap<>();
    private final Map<String,RenderPipeline> pipelines = new LinkedHashMap<>();
    private final List<Consumer<T>> callbacks = new ArrayList<>();
    private final ThreadLocal<Snapshot> scoped = new ThreadLocal<>();
    private boolean registered;

    public BCShader(Identifier location, VertexFormat format) {
        this(location, format, OFFSETS, true);
    }
    protected BCShader(Identifier location, VertexFormat format, Map<String,Integer> offsets, boolean entityLighting) {
        this.location=location; this.format=format; this.entityLighting=entityLighting;
        String resource="/assets/"+location.getNamespace()+"/shaders/core/"+location.getPath()+".json";
        try(InputStream stream=getClass().getResourceAsStream(resource)) {
            if(stream==null)throw new IOException("Missing shader metadata "+resource);
            JsonObject metadata=JsonParser.parseReader(new InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            if (metadata.has("samplers")) for (JsonElement sampler : metadata.getAsJsonArray("samplers")) {
                effectSamplers.add(sampler.getAsJsonObject().get("name").getAsString());
            }
            for(JsonElement entry:metadata.getAsJsonArray("uniforms")) {
                JsonObject u=entry.getAsJsonObject();String name=u.get("name").getAsString();Integer offset=offsets.get(name);
                if(offset==null)continue;String type=u.get("type").getAsString().toLowerCase(Locale.ROOT);
                BCUniform uniform=new BCUniform(staging,offset,type);uniforms.put(name,uniform);
                JsonArray values=u.getAsJsonArray("values");int count=u.get("count").getAsInt();
                if (offset < 0 || offset + count * 4 > SETTINGS_BYTES) throw new IllegalArgumentException("Uniform outside settings block: " + name);
                for(int i=0;i<count;i++) {
                    Number value=values.get(Math.min(i,values.size()-1)).getAsNumber();
                    if(type.equals("int")||type.equals("i_vec2"))staging.putInt(offset+i*4,value.intValue());
                    else staging.putFloat(offset+i*4,value.floatValue());
                }
            }
        } catch(IOException|RuntimeException error) { throw new IllegalStateException("Cannot initialize "+location,error); }
        for(VertexFormat.Mode mode:List.of(VertexFormat.Mode.QUADS,VertexFormat.Mode.TRIANGLES,VertexFormat.Mode.TRIANGLE_STRIP))
            for(boolean translucent:List.of(false,true))for(boolean cull:List.of(false,true))for(boolean depthWrite:List.of(false,true))createPipeline(mode,translucent,cull,depthWrite);
    }
    private RenderPipeline createPipeline(VertexFormat.Mode mode,boolean translucent,boolean cull,boolean depthWrite) {
        String key=mode.name().toLowerCase(Locale.ROOT)+"_"+translucent+"_"+cull+"_"+depthWrite;
        boolean gui=location.getPath().equals("position_color_tex_alpha0");
        var builder=RenderPipeline.builder(gui || !entityLighting?RenderPipelines.MATRICES_PROJECTION_SNIPPET:RenderPipelines.ENTITY_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(location.getNamespace(),"pipeline/"+location.getPath()+"/"+key))
                .withVertexShader(Identifier.fromNamespaceAndPath(location.getNamespace(),"core/"+location.getPath()+"_native"))
                .withFragmentShader(Identifier.fromNamespaceAndPath(location.getNamespace(),"core/"+location.getPath()+"_native"))
                .withUniform("BCSettings",UniformType.UNIFORM_BUFFER)
                .withVertexFormat(format,mode).withCull(cull)
                .withDepthStencilState(new DepthStencilState(com.mojang.blaze3d.platform.CompareOp.LESS_THAN_OR_EQUAL,depthWrite));
        // ENTITY_SNIPPET already declares Sampler0 and the lightmap Sampler2.
        if (!entityLighting) effectSamplers.stream().distinct().forEach(builder::withSampler);
        if(translucent)builder.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
        RenderPipeline pipeline=builder.build();pipelines.put(key,pipeline);NativeShaderBindings.register(pipeline,this);return pipeline;
    }
    public final RenderPipeline pipeline(VertexFormat.Mode mode,boolean translucent,boolean cull) {
        return pipeline(mode,translucent,cull,true);
    }
    public final RenderPipeline pipeline(VertexFormat.Mode mode,boolean translucent,boolean cull,boolean depthWrite) {
        return Objects.requireNonNull(pipelines.get(mode.name().toLowerCase(Locale.ROOT)+"_"+translucent+"_"+cull+"_"+depthWrite));
    }
    /** Register an authored blend/depth variant while retaining this shader's settings binding. */
    public final RenderPipeline variant(RenderPipeline pipeline) {
        if (registered) throw new IllegalStateException("Shader variants must be declared before pipeline registration");
        String key="variant:"+pipeline.getLocation();
        if(pipelines.putIfAbsent(key,pipeline)!=null) throw new IllegalArgumentException("Duplicate shader variant "+key);
        NativeShaderBindings.register(pipeline,this);
        return pipeline;
    }
    public final void register(IEventBus bus) {
        bus.addListener((RegisterRenderPipelinesEvent event)->{pipelines.values().forEach(event::registerPipeline);registered=true;});
    }
    @SuppressWarnings("unchecked")
    public final T onShaderApplied(Consumer<T> callback) { callbacks.add(callback);return (T)this; }
    public final boolean isLoaded() { return registered; }
    protected final BCUniform uniform(String name) { return Objects.requireNonNull(uniforms.get(name),missingUniformMessage(name)); }
    protected final boolean hasUniform(String name) { return uniforms.containsKey(name); }
    public final BCUniform getModelMatUniform(){return uniform("ModelMat");}
    public final boolean hasModelMatUniform(){return hasUniform("ModelMat");}
    public final BCUniform getTimeUniform(){return uniform("Time");}
    public final boolean hasTimeUniform(){return hasUniform("Time");}
    public final BCUniform getDecayUniform(){return uniform("Decay");}
    public final boolean hasDecayUniform(){return hasUniform("Decay");}
    public final BCUniform getSimpleLightUniform(){return uniform("SimpleLight");}
    public final BCUniform getDisableLightUniform(){return uniform("DisableLight");}
    public final BCUniform getDisableOverlayUniform(){return uniform("DisableOverlay");}
    public final BCUniform getAlphaUniform(){return uniform("Alpha");}
    protected final String missingUniformMessage(String name){return location+" does not have '"+name+"' uniform";}
    @SuppressWarnings("unchecked")
    public final Snapshot capture() {
        callbacks.forEach(callback->callback.accept((T)this));
        byte[] bytes=new byte[SETTINGS_BYTES];staging.get(0,bytes);return new Snapshot(bytes,hasModelMatUniform());
    }
    public final void withSnapshot(Snapshot snapshot,Runnable draw) {
        Snapshot previous=scoped.get();scoped.set(Objects.requireNonNull(snapshot));
        try {draw.run();} finally {if(previous==null)scoped.remove();else scoped.set(previous);}
    }
    final Snapshot drawSettings(){Snapshot snapshot=scoped.get();return snapshot==null?capture():snapshot;}
    public static final class Snapshot implements DynamicUniformStorage.DynamicUniform {
        private final byte[] bytes;
        private final boolean modelMatrix;
        private Snapshot(byte[] bytes,boolean modelMatrix){this.bytes=bytes.clone();this.modelMatrix=modelMatrix;}
        public boolean hasModelMatrix(){return modelMatrix;}
        public Snapshot transformed(org.joml.Matrix4fc outer){
            if(!modelMatrix)return this;
            var buffer=ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());float[] matrix=new float[16];
            for(int i=0;i<16;i++)matrix[i]=buffer.getFloat(i*4);
            var combined=new org.joml.Matrix4f(outer).mul(new org.joml.Matrix4f().set(matrix));
            combined.get(matrix);byte[] copy=bytes.clone();buffer=ByteBuffer.wrap(copy).order(ByteOrder.nativeOrder());
            for(int i=0;i<16;i++)buffer.putFloat(i*4,matrix[i]);
            return new Snapshot(copy,true);
        }
        @Override public void write(ByteBuffer destination){destination.put(bytes);}
    }
}
