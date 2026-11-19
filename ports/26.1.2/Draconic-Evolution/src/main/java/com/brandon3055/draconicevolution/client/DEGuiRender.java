package com.brandon3055.draconicevolution.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/** Native sprite submission and the original DE GUI color arithmetic. */
public final class DEGuiRender {
    private static final com.mojang.blaze3d.pipeline.RenderPipeline GLINT=RenderPipelines.GUI_TEXTURED.toBuilder()
            .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath("draconicevolution","pipeline/module_gui_glint"))
            .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.GLINT)).build();
    public static void init(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event)->event.registerPipeline(GLINT));
    }
    private DEGuiRender(){}
    public static java.util.List<net.minecraft.network.chat.Component> itemTooltip(net.minecraft.world.item.ItemStack stack) {
        var mc=net.minecraft.client.Minecraft.getInstance();
        return stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level),mc.player,
                mc.options.advancedItemTooltips?net.minecraft.world.item.TooltipFlag.Default.ADVANCED:net.minecraft.world.item.TooltipFlag.Default.NORMAL);
    }
    public static int mixColours(int first,int second){return mixColours(first,second,false);}
    public static int mixColours(int first,int second,boolean subtract){
        int result=0;for(int shift=0;shift<32;shift+=8){int a=(first>>>shift)&255,b=(second>>>shift)&255;result|=Mth.clamp(a+(subtract?-b:b),0,255)<<shift;}return result;
    }
    public static void sprite(GuiGraphicsExtractor graphics,TextureAtlasSprite sprite,double x,double y,double width,double height){sprite(graphics,sprite,x,y,width,height,0xffffffff);}
    public static void sprite(GuiGraphicsExtractor graphics,TextureAtlasSprite sprite,double x,double y,double width,double height,int color){graphics.cc$blitSprite(RenderPipelines.GUI_TEXTURED,sprite,x,y,width,height,color);}
    public static void sprite(GuiGraphicsExtractor graphics,TextureAtlasSprite sprite,double x,double y,double width,double height,float r,float g,float b,float a){
        int color=(Mth.clamp(Math.round(a*255),0,255)<<24)|(Mth.clamp(Math.round(r*255),0,255)<<16)|(Mth.clamp(Math.round(g*255),0,255)<<8)|Mth.clamp(Math.round(b*255),0,255);
        sprite(graphics,sprite,x,y,width,height,color);
    }
    public static void tooltip(GuiGraphicsExtractor graphics,java.util.List<net.minecraft.network.chat.Component> lines,double x,double y) {
        graphics.setComponentTooltipForNextFrame(net.minecraft.client.Minecraft.getInstance().font,lines,(int)x,(int)y);
    }
    public static void item(GuiGraphicsExtractor graphics,net.minecraft.world.item.ItemStack stack,double x,double y,double size) {
        graphics.pose().pushMatrix();
        try { graphics.pose().translate((float)x,(float)y);graphics.pose().scale((float)size/16F,(float)size/16F);graphics.item(stack,0,0); }
        finally { graphics.pose().popMatrix(); }
    }
    public static void partial(GuiGraphicsExtractor graphics,TextureAtlasSprite sprite,double x0,double y0,double x1,double y1,float u0,float v0,float u1,float v1,float r,float g,float b,float a) {
        graphics.cc$blitPartialSprite(RenderPipelines.GUI_TEXTURED,x0,y0,x1,y1,sprite,u0,v0,u1,v1,
                net.minecraft.util.ARGB.colorFromFloat(a,r,g,b));
    }
    public static void glint(GuiGraphicsExtractor graphics,double x,double y,double width,double height) {
        if(width<=0||height<=0)return;
        var texture=net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(
                net.minecraft.resources.Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png"));
        var setup=net.minecraft.client.gui.render.TextureSetup.singleTexture(texture.getTextureView(),
                com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getRepeat(com.mojang.blaze3d.textures.FilterMode.LINEAR));
        var pose=new org.joml.Matrix3x2f(graphics.pose());var clip=graphics.peekScissorStack();
        var bounds=new net.minecraft.client.gui.navigation.ScreenRectangle((int)x,(int)y,(int)Math.ceil(width),(int)Math.ceil(height)).transformMaxBounds(pose);
        if(clip!=null)bounds=bounds.intersection(clip);if(bounds==null)return;
        var uv=net.minecraft.client.renderer.rendertype.TextureTransform.GLINT_TEXTURING.getMatrix();
        var a=new org.joml.Vector4f(0,(float)(height/width)/64F,0,1).mul(uv);
        var b=new org.joml.Vector4f((float)(width/height)/64F,(float)(height/width)/64F,0,1).mul(uv);
        var c=new org.joml.Vector4f((float)(width/height)/64F,0,0,1).mul(uv);
        var d=new org.joml.Vector4f(0,0,0,1).mul(uv);
        graphics.nextStratum();
        graphics.submitGuiElementRenderState(new codechicken.lib.gui.render.CCCustomRenderState(GLINT,setup,pose,clip,bounds,(buffer,matrix)->{
            buffer.addVertexWith2DPose(matrix,(float)x,(float)(y+height)).setUv(a.x,a.y).setColor(-1);
            buffer.addVertexWith2DPose(matrix,(float)(x+width),(float)(y+height)).setUv(b.x,b.y).setColor(-1);
            buffer.addVertexWith2DPose(matrix,(float)(x+width),(float)y).setUv(c.x,c.y).setColor(-1);
            buffer.addVertexWith2DPose(matrix,(float)x,(float)y).setUv(d.x,d.y).setColor(-1);
        }));
        graphics.nextStratum();
    }
}
