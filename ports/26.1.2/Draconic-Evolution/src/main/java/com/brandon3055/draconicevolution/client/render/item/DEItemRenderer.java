package com.brandon3055.draconicevolution.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Geometry emitter used by the native DE item model extraction bridge. */
public interface DEItemRenderer {
    void submitItem(ItemStack stack, ItemDisplayContext context, Object state, PoseStack poses,
            net.minecraft.client.renderer.SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline);
    default Object extractState(ItemStack stack, ItemDisplayContext context, net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.world.entity.ItemOwner owner) { return null; }
    default net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
    default PerspectiveModelState getModelState() { return TransformUtils.IDENTITY; }
    default boolean useAmbientOcclusion() { return false; }
    default boolean isGui3d() { return true; }
    default boolean usesBlockLight() { return true; }
}
