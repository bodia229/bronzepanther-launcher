package com.brandon3055.brandonscore.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.brandon3055.brandonscore.client.render.CachedModelMesh;
import com.brandon3055.brandonscore.client.shader.NativeShaderBindings;
/** Release GPU resources after the final frame, while the device is still available. */
@Mixin(Minecraft.class)
public abstract class ClientRenderShutdownMixin {
    @Inject(method="close",at=@At("HEAD"))
    private void brandonscore$closeRenderResources(CallbackInfo ci){
        CachedModelMesh.closeAll();NativeShaderBindings.close();
    }
}
