package com.brandon3055.brandonscore.mixin;

import net.minecraft.client.renderer.rendertype.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSetup.class)
public interface CachedRenderSetupAccess {
    @Accessor("layeringTransform") LayeringTransform brandonscore$layering();
    @Accessor("textureTransform") TextureTransform brandonscore$textureTransform();
}
