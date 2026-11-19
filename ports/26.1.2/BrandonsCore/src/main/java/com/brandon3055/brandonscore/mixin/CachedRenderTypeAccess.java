package com.brandon3055.brandonscore.mixin;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.class)
public interface CachedRenderTypeAccess {
    @Accessor("state") RenderSetup brandonscore$setup();
}
