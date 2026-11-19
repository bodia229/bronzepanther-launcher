package com.brandon3055.draconicevolution.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/** Keep legacy particle blending and depth-write behavior in explicit native pipelines. */
public final class DEParticleLayers {
    private DEParticleLayers() {}
    private static RenderPipeline pipeline(String name, BlendFunction blend) {
        return RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("draconicevolution", "pipeline/" + name))
                .withColorTargetState(new ColorTargetState(blend))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)).withCull(false).build();
    }
    public static final SingleQuadParticle.Layer TRANSLUCENT_NO_DEPTH_WRITE = new SingleQuadParticle.Layer(true,
            TextureAtlas.LOCATION_PARTICLES, pipeline("translucent_particle_no_depth_write", BlendFunction.TRANSLUCENT));
    public static final SingleQuadParticle.Layer ADDITIVE_NO_DEPTH_WRITE = new SingleQuadParticle.Layer(true,
            TextureAtlas.LOCATION_PARTICLES, pipeline("additive_particle_no_depth_write", BlendFunction.LIGHTNING));
    public static net.minecraft.client.renderer.rendertype.RenderType vertexEffect(Identifier texture) {
        return net.minecraft.client.renderer.rendertype.RenderType.create("draconicevolution:vertex/"+texture.getPath(),
                net.minecraft.client.renderer.rendertype.RenderSetup.builder(ADDITIVE_NO_DEPTH_WRITE.pipeline())
                        .withTexture("Sampler0",texture).useLightmap().bufferSize(256).sortOnUpload().createRenderSetup());
    }
    public static void init(IEventBus bus) {
        com.brandon3055.draconicevolution.client.render.effect.DEVertexParticle.init(bus);
        bus.addListener((RegisterRenderPipelinesEvent event) -> {
            event.registerPipeline(TRANSLUCENT_NO_DEPTH_WRITE.pipeline());
            event.registerPipeline(ADDITIVE_NO_DEPTH_WRITE.pipeline());
        });
    }
}
