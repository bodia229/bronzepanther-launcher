package com.brandon3055.brandonscore.client.render;

import codechicken.lib.gui.render.CCCustomRenderState;
import codechicken.lib.gui.modular.lib.geometry.Rectangle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import codechicken.lib.gui.render.GuiGraphicsExtension;
import org.joml.Matrix3x2f;

/** Submits immutable 2D geometry to Minecraft's deferred GUI renderer. */
public final class GuiRenderBridge {
    private GuiRenderBridge(){}
    public static void submit(GuiGraphicsExtractor graphics,double x0,double y0,double x1,double y1,CCCustomRenderState.VertBuilder vertices){
        Matrix3x2f pose=new Matrix3x2f(graphics.pose());
        var clip=graphics.peekScissorStack();
        int left=(int)Math.floor(x0),top=(int)Math.floor(y0);
        var bounds=new ScreenRectangle(left,top,(int)Math.ceil(x1)-left,(int)Math.ceil(y1)-top).transformMaxBounds(pose);
        if(clip!=null)bounds=bounds.intersection(clip);
        // Custom HUD meshes must retain painter order across preceding GUI widgets.
        graphics.nextStratum();
        graphics.submitGuiElementRenderState(new CCCustomRenderState(RenderPipelines.GUI,TextureSetup.noTexture(),pose,clip,bounds,vertices));
    }
    public static void rect(GuiGraphicsExtractor graphics,Rectangle rectangle,int color){rect(graphics,rectangle.x(),rectangle.y(),rectangle.width(),rectangle.height(),color);}
    public static void rect(GuiGraphicsExtractor graphics,double x,double y,double width,double height,int color){
        if(width==0||height==0)return;
        if(width<0){x+=width;width=-width;}
        if(height<0){y+=height;height=-height;}
        // Use the native rectangle state, retaining fractional coordinates through its pose.
        graphics.pose().pushMatrix();
        try{
            graphics.pose().translate((float)x,(float)y);
            graphics.pose().scale((float)width,(float)height);
            graphics.fill(0,0,1,1,color);
        }finally{graphics.pose().popMatrix();}
    }
    public static void tooltip(GuiGraphicsExtractor graphics,double x,double y,double width,double height,int topBackground,int bottomBackground,int topBorder,int bottomBorder,boolean empty){
        ((GuiGraphicsExtension)(Object)graphics).cc$tooltipBackground(x,y,width,height,topBackground,bottomBackground,topBorder,bottomBorder,empty);
    }
    public static void tooltip(GuiGraphicsExtractor graphics,double x,double y,double width,double height){tooltip(graphics,x,y,width,height,0xf0100010,0x505000ff,0x5028007f);}
    public static void tooltip(GuiGraphicsExtractor graphics,double x,double y,double width,double height,int background,int top,int bottom){((GuiGraphicsExtension)(Object)graphics).cc$tooltipBackground(x,y,width,height,background,background,top,bottom,false);}
}
