package com.brandon3055.brandonscore.client.shader;

import codechicken.lib.math.MathHelper;

import codechicken.lib.util.ClientUtils;
import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.client.BCClientEventHandler;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;


/**
 * Created by brandon3055 on 15/05/2022
 */
public class BCShaders {
    private static final CrashLock LOCK = new CrashLock("Already Initialized");

    public static final ChaosEntityShader CHAOS_ENTITY_SHADER = new ChaosEntityShader("chaos_entity", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> {
                Player player = Minecraft.getInstance().player;
                if (player == null) return;
                e.getTimeUniform().glUniform1f((float) ClientUtils.getRenderTime());
                e.getYawUniform().glUniform1f((float) (player.getYRot() * MathHelper.torad));
                e.getPitchUniform().glUniform1f((float) -(player.getXRot() * MathHelper.torad));
            });

    public static final ContribShader CONTRIB_BASE_SHADER = new ContribShader("contributor/contrib_base", DefaultVertexFormat.ENTITY);
    public static final ContribShader WINGS_WEB_SHADER = new ContribShader("contributor/wings_web", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));
    public static final ContribShader WINGS_BONE_SHADER = new ContribShader("contributor/wings_bone", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));

    public static final ContribShader VET_BADGE_SHADER = new ContribShader("contributor/vet_badge", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));

    public static final ContribShader BADGE_OUTLINE_SHADER = new ContribShader("contributor/badge_outline", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));
    public static final ContribShader BADGE_CORE_SHADER = new ContribShader("contributor/patreon_core", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 20)));
    public static final ContribShader BADGE_FOIL_SHADER = new ContribShader("contributor/badge_foil", DefaultVertexFormat.ENTITY)
            .onShaderApplied(e -> e.getTimeUniform().glUniform1f((float) (ClientUtils.getRenderTime() / 40)));

    public static final BCShader<?> posColourTexAlpha0 = new BCShader<>(Identifier.fromNamespaceAndPath("brandonscore", "position_color_tex_alpha0"), DefaultVertexFormat.POSITION_TEX_COLOR);

    public static void init(IEventBus modBus) {
        LOCK.lock();
        CONTRIB_BASE_SHADER.register(modBus);
        WINGS_WEB_SHADER.register(modBus);
        WINGS_BONE_SHADER.register(modBus);
        VET_BADGE_SHADER.register(modBus);
        CHAOS_ENTITY_SHADER.register(modBus);
        BADGE_OUTLINE_SHADER.register(modBus);
        BADGE_CORE_SHADER.register(modBus);
        BADGE_FOIL_SHADER.register(modBus);

        posColourTexAlpha0.register(modBus);
        NativeShaderBindings.init();
    }

}
