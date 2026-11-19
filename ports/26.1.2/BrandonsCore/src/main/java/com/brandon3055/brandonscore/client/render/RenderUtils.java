package com.brandon3055.brandonscore.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.brandon3055.brandonscore.client.render.GuiRenderBridge;
import codechicken.lib.math.MathHelper;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;

/**
 * Created by brandon3055 on 16/10/18.
 */
public class RenderUtils {


//    /**
//     * * @return The buffer source used for GUI rendering. You must ALWAYS call endBatch on this when you are done with it.
//     */
//    @Deprecated
//    public static MultiBufferSource.BufferSource getGuiBuffers() {
//        return MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
//    }

//    @Deprecated
//    public static MultiBufferSource.BufferSource getBuffers() {
//        return Minecraft.getInstance().renderBuffers().bufferSource();
//    }

    @Deprecated
    public static void endBatch(MultiBufferSource getter) {
        if (getter instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) getter).endBatch();
        } else ExtractedEquipmentGeometry.flush(getter);
    }

    public static void drawPieProgress(GuiGraphicsExtractor render, double x, double y, double diameter, double progress, double offsetAngle, int colour) {
        drawPieProgress(render, x, y, diameter, progress, offsetAngle, colour, colour);
    }

    public static void drawPieProgress(GuiGraphicsExtractor render, double x, double y, double diameter, double progress, double offsetAngle, int innerColour, int outerColour) {
        if(diameter<=0||progress<=0)return;
        final double radius=diameter/2,centerX=x+radius,centerY=y+radius;
        GuiRenderBridge.submit(render,x,y,x+diameter,y+diameter,(builder,pose)->{
            for(int segment=0;segment<30;segment++){
                double first=((segment/30D*progress)+0.5-progress)*Math.PI*2+Math.toRadians(offsetAngle);
                double second=(((segment+1)/30D*progress)+0.5-progress)*Math.PI*2+Math.toRadians(offsetAngle);
                float ax=(float)(centerX+Math.sin(first)*radius),ay=(float)(centerY+Math.cos(first)*radius);
                float bx=(float)(centerX+Math.sin(second)*radius),by=(float)(centerY+Math.cos(second)*radius);
                // Each triangle occupies one GUI quad; the fourth vertex makes its second triangle degenerate.
                builder.addVertexWith2DPose(pose,(float)centerX,(float)centerY).setColor(innerColour);
                builder.addVertexWith2DPose(pose,ax,ay).setColor(outerColour);
                builder.addVertexWith2DPose(pose,bx,by).setColor(outerColour);
                builder.addVertexWith2DPose(pose,bx,by).setColor(outerColour);
            }
        });
    }

    private static final java.util.Map<Identifier,TextureAtlasSprite> RAW_SPRITES=new java.util.concurrent.ConcurrentHashMap<>();

    public static codechicken.lib.gui.modular.SpriteSupplier fromRawTexture(Identifier texture) {
        return ()->RAW_SPRITES.computeIfAbsent(texture,FullSprite::new);
    }

    public static float partialTick() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    private static class FullSprite extends TextureAtlasSprite {
        private FullSprite(Identifier location) {
            super(location, new SpriteContents(location,new FrameSize(1,1),new NativeImage(1,1,false)), 1, 1, 0, 0, 0);
        }

        @Override
        public float getU(float u)
        {
            return u;
        }

        @Override
        public float getV(float v)
        {
            return v;
        }
    }
}
