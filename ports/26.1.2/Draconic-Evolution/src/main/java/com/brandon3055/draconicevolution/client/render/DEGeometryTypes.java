package com.brandon3055.draconicevolution.client.render;

import com.brandon3055.brandonscore.client.shader.BCShader;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import java.util.*;

/** Explicit native material state for the original authored geometry. */
public final class DEGeometryTypes {
    private static final Map<String,RenderType> TYPES=new LinkedHashMap<>();
    private static final Set<RenderPipeline> BASIC_PIPELINES=Collections.newSetFromMap(new IdentityHashMap<>());
    private DEGeometryTypes() {}
    public static RenderType shader(String name,BCShader<?> shader,Identifier texture,VertexFormat.Mode mode,
            String blend,boolean cull,boolean depthWrite,boolean lightmap,boolean overlay,boolean sort,boolean blur,boolean outline) {
        return TYPES.computeIfAbsent(name,key->{
            var pipeline=shader.pipeline(mode,!blend.equals("none"),cull,depthWrite);
            if(blend.equals("lightning")) pipeline=shader.variant(pipeline.toBuilder().withLocation(id(name))
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING)).build());
            return setup(name,pipeline,texture,lightmap,overlay,sort,blur,outline);
        });
    }
    public static RenderType basic(String name,VertexFormat format,VertexFormat.Mode mode,String kind,Identifier texture,
            String blend,boolean cull,boolean depthWrite,boolean lightmap,boolean overlay,boolean sort,boolean blur,boolean outline) {
        return TYPES.computeIfAbsent(name,key->{
            RenderPipeline.Builder builder;
            if(format==DefaultVertexFormat.ENTITY) builder=RenderPipelines.ENTITY_SOLID.toBuilder();
            else if(format==DefaultVertexFormat.BLOCK) builder=RenderPipelines.SOLID_BLOCK.toBuilder();
            else if(kind.equals("getPositionColorTexLightmapShader")||kind.equals("getParticleShader")) builder=RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET);
            else {
                String shader=kind.equals("getPositionColorShader") ? "core/position_color" : kind.equals("getPositionTexShader") ? "core/position_tex" : "core/position_tex_color";
                builder=RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET).withVertexShader(shader).withFragmentShader(shader).withSampler("Sampler0");
            }
            if(kind.equals("getRendertypeCutoutShader"))builder.withShaderDefine("ALPHA_CUTOUT",.1F);
            builder.withLocation(id(name)).withVertexFormat(format,mode).withCull(cull)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL,depthWrite))
                    .withColorTargetState(blend.equals("none") ? ColorTargetState.DEFAULT : new ColorTargetState(blend.equals("lightning")?BlendFunction.LIGHTNING:BlendFunction.TRANSLUCENT));
            var pipeline=builder.build();BASIC_PIPELINES.add(pipeline);
            return setup(name,pipeline,texture,lightmap,overlay,sort,blur,outline);
        });
    }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("draconicevolution","pipeline/"+name); }
    private static RenderType setup(String name,RenderPipeline pipeline,Identifier texture,boolean lightmap,boolean overlay,boolean sort,boolean blur,boolean outline) {
        var builder=RenderSetup.builder(pipeline).bufferSize(256);
        if(texture!=null)builder.withTexture("Sampler0",texture,()->com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getSampler(
                com.mojang.blaze3d.textures.AddressMode.REPEAT,com.mojang.blaze3d.textures.AddressMode.REPEAT,
                blur?com.mojang.blaze3d.textures.FilterMode.LINEAR:com.mojang.blaze3d.textures.FilterMode.NEAREST,
                blur?com.mojang.blaze3d.textures.FilterMode.LINEAR:com.mojang.blaze3d.textures.FilterMode.NEAREST,false));
        // Native entity/block pipelines require their declared light/overlay samplers.
        if(lightmap || pipeline.getSamplers().contains("Sampler2"))builder.useLightmap();
        if(overlay || pipeline.getSamplers().contains("Sampler1"))builder.useOverlay();
        if(sort&&pipeline.getVertexFormatMode()==VertexFormat.Mode.QUADS)builder.sortOnUpload();
        if(outline)builder.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        return RenderType.create("draconicevolution:"+name,builder.createRenderSetup());
    }
    public static void init(IEventBus bus) {
        bootstrap();
        bus.addListener((RegisterRenderPipelinesEvent event)->BASIC_PIPELINES.forEach(event::registerPipeline));
    }
    private static void bootstrap() {
        for(String name:BOOTSTRAP) try { Class.forName(name,true,DEGeometryTypes.class.getClassLoader()); }
        catch(ClassNotFoundException error){throw new IllegalStateException(error);}
    }
    private static final List<String> BOOTSTRAP=List.of("com.brandon3055.draconicevolution.client.CustomBossInfoHandler","com.brandon3055.draconicevolution.client.render.effect.ReactorBeamFX","com.brandon3055.draconicevolution.client.render.effect.ExplosionFX","com.brandon3055.draconicevolution.client.render.entity.DraconicGuardianRenderer","com.brandon3055.draconicevolution.client.render.effect.StaffBeamEffect","com.brandon3055.draconicevolution.client.render.modelfx.StaffModelEffect","com.brandon3055.draconicevolution.client.render.tile.RenderEnergyCoreStabilizer","com.brandon3055.draconicevolution.client.render.tile.RenderTileChaosCrystal","com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyCore","com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyCrystal","com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyPylon","com.brandon3055.draconicevolution.client.render.tile.RenderTileFusionCraftingCore","com.brandon3055.draconicevolution.client.render.tile.RenderTileReactorComponent","com.brandon3055.draconicevolution.client.render.tile.RenderTileReactorCore");
}
