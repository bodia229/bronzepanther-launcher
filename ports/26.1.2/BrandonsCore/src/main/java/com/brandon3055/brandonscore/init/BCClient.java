package com.brandon3055.brandonscore.init;

import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.client.*;
import com.brandon3055.brandonscore.client.hud.HudManager;
import com.brandon3055.brandonscore.client.model.EquippedItemModelLayer;
import com.brandon3055.brandonscore.client.shader.BCShaders;
import com.brandon3055.brandonscore.handlers.contributor.ContributorHandler;
import com.brandon3055.brandonscore.lib.DLRSCache;
import com.brandon3055.brandonscore.utils.BCProfiler;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Created by brandon3055 on 15/11/2022
 */
public class BCClient {
    private static final CrashLock LOCK = new CrashLock("Already Initialized.");

    public static void init(IEventBus modBus) {
        LOCK.lock();

        modBus.addListener(BCClient::clientSetupEvent);
        modBus.addListener(BCClient::onAddRenderLayers);
        modBus.addListener((net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event)->EquippedItemModelLayer.registerModifiers(event));
        modBus.addListener((net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) ->
                event.registerAvatarEntityModifier(new net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier() {
                    @Override
                    public <T extends net.minecraft.world.entity.Avatar & net.minecraft.client.entity.ClientAvatarEntity>
                    void accept(T avatar, net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
                        state.setRenderData(com.brandon3055.brandonscore.client.render.ContributorRenderData.HIDE_VANILLA_WINGS,
                                ContributorHandler.shouldCancelElytra(avatar));
                    }
                }));

        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> ContributorHandler.onClientLogin(event.getPlayer()));
        ProcessHandlerClient.init();
        HudManager.init(modBus);
        BCShaders.init(modBus);
        com.brandon3055.brandonscore.client.render.NativeEnergyBar.init(modBus);
        com.brandon3055.brandonscore.client.render.CachedModelMesh.init(modBus);
        com.brandon3055.brandonscore.client.render.BuildGuideRenderState.init(modBus);
        BCProfiler.init();
        DLRSCache.init();    }

    private static void clientSetupEvent(FMLClientSetupEvent event) {
        BCClientEventHandler.init();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void onAddRenderLayers(EntityRenderersEvent.AddLayers event) {
        EquippedItemModelLayer.clearLayers();
        for (PlayerModelType skin : event.getSkins()) {
            LivingEntityRenderer renderer = event.getPlayerRenderer(skin);
            if (renderer != null) renderer.addLayer(new EquippedItemModelLayer(renderer, skin == PlayerModelType.SLIM));
            LivingEntityRenderer mannequin = event.getMannequinRenderer(skin);
            if (mannequin != null) mannequin.addLayer(new EquippedItemModelLayer(mannequin, skin == PlayerModelType.SLIM));
        }

        for (var entityType : event.getEntityTypes()) {
            EntityRenderer r = event.getRenderer(entityType);
            if (r instanceof LivingEntityRenderer<?, ?, ?> renderer && renderer.getModel() instanceof HumanoidModel) {
                renderer.addLayer(new EquippedItemModelLayer(renderer, false));
            }
        }
    }
}
