package com.brandon3055.draconicevolution.api.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import java.util.Optional;

/** Native pipelines for crystal binder highlights, visible through terrain. */
public final class DERenderTypes {
    private static final RenderPipeline BOX_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("draconicevolution", "pipeline/binder_box"))
            .withDepthStencilState(Optional.empty()).build();
    private static final RenderPipeline OUTLINE_PIPELINE = RenderPipelines.LINES_TRANSLUCENT.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath("draconicevolution", "pipeline/binder_outline"))
            .withDepthStencilState(Optional.empty()).withCull(false).build();
    public static final RenderType BOX_NO_DEPTH = RenderType.create("de:box_no_depth",
            RenderSetup.builder(BOX_PIPELINE).bufferSize(256).sortOnUpload().createRenderSetup());
    public static final RenderType OUTLINE_TYPE = RenderType.create("de:outline",
            RenderSetup.builder(OUTLINE_PIPELINE).bufferSize(256).createRenderSetup());

    public static void init(IEventBus bus) {
        bus.addListener((RegisterRenderPipelinesEvent event) -> {
            event.registerPipeline(BOX_PIPELINE);
            event.registerPipeline(OUTLINE_PIPELINE);
        });
    }
}
