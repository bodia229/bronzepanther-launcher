package com.brandon3055.brandonscore.mixin;

import com.brandon3055.brandonscore.client.render.ContributorRenderData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WingsLayer.class)
public class ElytraLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void brandonscore$hideReplacedWings(PoseStack pose, SubmitNodeCollector collector,
            int light, HumanoidRenderState state, float yaw, float pitch, CallbackInfo ci) {
        if (Boolean.TRUE.equals(state.getRenderData(ContributorRenderData.HIDE_VANILLA_WINGS))) {
            ci.cancel();
        }
    }
}
