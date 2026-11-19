package com.brandon3055.draconicevolution.client;

import codechicken.lib.util.ClientUtils;
import com.brandon3055.brandonscore.api.TimeKeeper;
import com.brandon3055.brandonscore.client.render.RenderUtils;
import com.brandon3055.brandonscore.client.shader.BCUniform;
import com.brandon3055.draconicevolution.client.shader.EffectShader;
import com.brandon3055.draconicevolution.client.shader.ShieldShader;
import com.brandon3055.draconicevolution.client.shader.ToolShader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.covers1624.quack.util.CrashLock;
import net.neoforged.bus.api.IEventBus;

/** Native pipelines and immutable per-draw uniform snapshots for DE effects. */
public class DEShaders {
    private static final CrashLock LOCK = new CrashLock("Already Initialized");
    public static final ToolShader TOOL_BASE_SHADER = new ToolShader("tools/tool_base", DefaultVertexFormat.ENTITY);
    public static final ToolShader TOOL_GEM_SHADER = new ToolShader("tools/tool_gem", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));
    public static final ToolShader TOOL_TRACE_SHADER = new ToolShader("tools/tool_trace", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));
    public static final ToolShader TOOL_BLADE_SHADER = new ToolShader("tools/tool_blade", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));
    public static final ToolShader BOW_STRING_SHADER = new ToolShader("tools/bow_string", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));
    public static final ToolShader CHESTPIECE_GEM_SHADER = new ToolShader("tools/chestpiece_gem", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));
    public static final ShieldShader CHESTPIECE_SHIELD_SHADER = new ShieldShader("tools/chestpiece_shield", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));

    public static final EffectShader reactorShader = new EffectShader("reactor", DefaultVertexFormat.POSITION_TEX);
    public static final BCUniform reactorTime = reactorShader.getUniform("time");
    public static final BCUniform reactorIntensity = reactorShader.getUniform("intensity");

    public static final EffectShader reactorShieldShader = new EffectShader("reactor_shield", DefaultVertexFormat.POSITION_TEX);
    public static final BCUniform reactorShieldTime = reactorShieldShader.getUniform("time");
    public static final BCUniform reactorShieldIntensity = reactorShieldShader.getUniform("intensity");

    public static final EffectShader chaosBlockShader = new EffectShader("chaos_block", DefaultVertexFormat.BLOCK);
    public static final BCUniform chaosBlockTime = chaosBlockShader.getUniform("Time");
    public static final BCUniform chaosBlockYaw = chaosBlockShader.getUniform("Yaw");
    public static final BCUniform chaosBlockPitch = chaosBlockShader.getUniform("Pitch");
    public static final BCUniform chaosBlockAlpha = chaosBlockShader.getUniform("Alpha");

    public static final EffectShader shieldShader = new EffectShader("shield", DefaultVertexFormat.POSITION_TEX)
            .onShaderApplied(e -> e.getUniform("Time").glUniform1f((TimeKeeper.getClientTick() + RenderUtils.partialTick()) / 20F));
    public static final BCUniform shieldTime = shieldShader.getUniform("Time");
    public static final BCUniform shieldActivation = shieldShader.getUniform("Activation");
    public static final BCUniform shieldColour = shieldShader.getUniform("BaseColour");
    public static final BCUniform shieldBarMode = shieldShader.getUniform("BarMode");

    public static final EffectShader energyCrystalShader = new EffectShader("energy_crystal", DefaultVertexFormat.POSITION_TEX)
            .onShaderApplied(e -> e.getUniform("Time").glUniform1f((TimeKeeper.getClientTick() + RenderUtils.partialTick()) / 50F));
    public static final BCUniform energyCrystalTime = energyCrystalShader.getUniform("Time");
    public static final BCUniform energyCrystalColour = energyCrystalShader.getUniform("Colour");
    public static final BCUniform energyCrystalMipmap = energyCrystalShader.getUniform("Mipmap");

    public static final EffectShader energyCoreShader = new EffectShader("energy_core", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
            .onShaderApplied(e -> e.getUniform("Time").glUniform1f((TimeKeeper.getClientTick() + RenderUtils.partialTick()) / 20F));
    public static final BCUniform energyCoreTime = energyCoreShader.getUniform("Time");
    public static final BCUniform energyCoreActivation = energyCoreShader.getUniform("Activation");
    public static final BCUniform energyCoreEffectColour = energyCoreShader.getUniform("EffectColour");
    public static final BCUniform energyCoreFrameColour = energyCoreShader.getUniform("FrameColour");
    public static final BCUniform energyCoreRotTriColour = energyCoreShader.getUniform("InnerTriColour");

    public static final EffectShader reactorBeamShader = new EffectShader("reactor_beam", DefaultVertexFormat.POSITION_TEX_COLOR)
            .onShaderApplied(e -> e.getUniform("Time").glUniform1f((TimeKeeper.getClientTick() + RenderUtils.partialTick()) * 0.02F));
    public static final BCUniform reactorBeamTime = reactorBeamShader.getUniform("Time");
    public static final BCUniform reactorBeamFade = reactorBeamShader.getUniform("Fade");
    public static final BCUniform reactorBeamPower = reactorBeamShader.getUniform("Power");
    public static final BCUniform reactorBeamStartup = reactorBeamShader.getUniform("Startup");
    public static final BCUniform reactorBeamType = reactorBeamShader.getUniform("Type");

    public static final EffectShader explosionFlashShader = new EffectShader("explosion_flash", DefaultVertexFormat.POSITION_COLOR);
    public static final BCUniform explosionFlashScreenPos = explosionFlashShader.getUniform("ScreenPos");
    public static final BCUniform explosionFlashScreenSize = explosionFlashShader.getUniform("ScreenSize");
    public static final BCUniform explosionFlashIntensity = explosionFlashShader.getUniform("Intensity");

    public static final EffectShader explosionShader = new EffectShader("explosion", DefaultVertexFormat.POSITION_TEX);
    public static final BCUniform explosionTime = explosionShader.getUniform("Time");
    public static final BCUniform explosionScale = explosionShader.getUniform("Scale");
    public static final BCUniform explosionAlpha = explosionShader.getUniform("Alpha");
    public static final BCUniform explosionType = explosionShader.getUniform("Type");

    // The legacy Angle uniform was absent from the shader and has no active callers.
    public static BCUniform energyCrystalAngle;

    public static final com.mojang.blaze3d.pipeline.RenderPipeline BOW_STRING_ADDITIVE = BOW_STRING_SHADER.variant(
            BOW_STRING_SHADER.pipeline(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, true, false).toBuilder()
                    .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath("draconicevolution", "pipeline/bow_string_additive"))
                    .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.LIGHTNING))
                    .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.LESS_THAN_OR_EQUAL, false)).build());

    public static void init(IEventBus modBus) {
        LOCK.lock();
        com.brandon3055.draconicevolution.client.DEGuiRender.init(modBus);
        com.brandon3055.draconicevolution.client.render.DEGuiGeometry.init(modBus);
        com.brandon3055.draconicevolution.client.render.DEGeometryTypes.init(modBus);
        com.brandon3055.draconicevolution.client.render.item.NativeItemRenderers.init();
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) ->
                com.brandon3055.draconicevolution.client.render.item.RenderItemChaosShard.registerPipelines(event));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) ->
                event.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("draconicevolution", "custom_item"),
                        com.brandon3055.draconicevolution.client.render.item.DEItemModel.Unbaked.CODEC));
        com.brandon3055.draconicevolution.api.render.DERenderTypes.init(modBus);
        TOOL_BASE_SHADER.register(modBus);
        TOOL_GEM_SHADER.register(modBus);
        TOOL_TRACE_SHADER.register(modBus);
        TOOL_BLADE_SHADER.register(modBus);
        BOW_STRING_SHADER.register(modBus);
        CHESTPIECE_GEM_SHADER.register(modBus);
        CHESTPIECE_SHIELD_SHADER.register(modBus);
        reactorShader.register(modBus);
        reactorShieldShader.register(modBus);
        chaosBlockShader.register(modBus);
        shieldShader.register(modBus);
        energyCrystalShader.register(modBus);
        energyCoreShader.register(modBus);
        reactorBeamShader.register(modBus);
        explosionFlashShader.register(modBus);
        explosionShader.register(modBus);
    }
}
