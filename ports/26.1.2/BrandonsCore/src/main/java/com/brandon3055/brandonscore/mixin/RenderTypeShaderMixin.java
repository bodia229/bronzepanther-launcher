package com.brandon3055.brandonscore.mixin;

import com.brandon3055.brandonscore.client.shader.NativeShaderBindings;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.buffers.GpuBufferSlice;

@Mixin(RenderType.class)
public abstract class RenderTypeShaderMixin {
    @Shadow public abstract RenderPipeline pipeline();
    @Unique private GpuBufferSlice brandonscore$settings;
    @Inject(method="draw",at=@At("HEAD"))
    private void brandonscore$prepareSettings(MeshData mesh,CallbackInfo ci){
        brandonscore$settings=NativeShaderBindings.prepare((RenderType)(Object)this);
    }
    @Redirect(method="draw",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
    private void brandonscore$bindSettings(RenderPass pass,RenderPipeline pipeline){
        pass.setPipeline(pipeline);
        if(brandonscore$settings!=null)pass.setUniform("BCSettings",brandonscore$settings);
    }
    @Inject(method="draw",at=@At("RETURN"))
    private void brandonscore$clearSettings(MeshData mesh,CallbackInfo ci){brandonscore$settings=null;}
    @Redirect(method="draw",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V"))
    private void brandonscore$drawWithMaterialContext(RenderPass pass,int a,int b,int c,int d){
        try {pass.drawIndexed(a,b,c,d);}
        catch(IllegalStateException failure){
            throw new IllegalStateException("Render material="+this+" pipeline="+pipeline().getLocation()+" samplers="+pipeline().getSamplers(),failure);
        }
    }
}
