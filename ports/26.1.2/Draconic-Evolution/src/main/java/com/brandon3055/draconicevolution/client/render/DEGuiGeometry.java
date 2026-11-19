package com.brandon3055.draconicevolution.client.render;

import com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import org.joml.Matrix3x2f;

/** Detached 3D effect preview composited through Minecraft's native GUI offscreen pass. */
public final class DEGuiGeometry {
    private DEGuiGeometry() {}
    public static void init(IEventBus bus) { bus.addListener((RegisterPictureInPictureRenderersEvent event)->event.register(State.class,Renderer::new)); }
    public static void extract(GuiGraphicsExtractor graphics,int x,int y,int width,int height,ExtractedEquipmentGeometry geometry) {
        var pose=new Matrix3x2f(graphics.pose());var clip=graphics.peekScissorStack();
        var bounds=new ScreenRectangle(x,y,width,height).transformMaxBounds(pose);
        if(clip!=null)bounds=bounds.intersection(clip);
        if(bounds!=null)graphics.submitPictureInPictureRenderState(new State(x,y,x+width,y+height,1F,pose,clip,bounds,geometry));
    }
    public record State(int x0,int y0,int x1,int y1,float scale,Matrix3x2f pose,ScreenRectangle scissorArea,ScreenRectangle bounds,ExtractedEquipmentGeometry geometry) implements PictureInPictureRenderState {}
    private static final class Renderer extends PictureInPictureRenderer<State> {
        Renderer(MultiBufferSource.BufferSource buffers) { super(buffers); }
        @Override public Class<State> getRenderStateClass() { return State.class; }
        @Override protected String getTextureLabel() { return "Draconic Evolution effect preview"; }
        @Override protected float getTranslateY(int height,int guiScale) { return height/2F; }
        @Override protected void renderToTexture(State state,PoseStack poses) { state.geometry().emit(poses,bufferSource); }
    }
}
