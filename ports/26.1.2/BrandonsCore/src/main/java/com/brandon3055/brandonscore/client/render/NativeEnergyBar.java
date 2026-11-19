package com.brandon3055.brandonscore.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import codechicken.lib.gui.render.GuiGraphicsExtension;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

/** Original energy effect rendered through native GUI extraction and uniform buffers. */
public final class NativeEnergyBar {
    private NativeEnergyBar(){}
    private static final RenderPipeline PIPELINE=RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("brandonscore","pipeline/energy_bar"))
            .withVertexShader(Identifier.fromNamespaceAndPath("brandonscore","core/energy_bar_native"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("brandonscore","core/energy_bar_native"))
            .withUniform("EnergyBarSettings",UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS)
            .withCull(false).withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build();
    public static void init(IEventBus modBus){
        modBus.addListener((RegisterRenderPipelinesEvent event)->event.registerPipeline(PIPELINE));
        modBus.addListener((RegisterPictureInPictureRenderersEvent event)->event.register(State.class,Renderer::new));
    }
    public static void extract(GuiGraphicsExtractor graphics,double x,double y,double width,double height,double charge,float time){
        if(width<=0||height<=0)return;
        int x0=(int)Math.floor(x),y0=(int)Math.floor(y),x1=(int)Math.ceil(x+width),y1=(int)Math.ceil(y+height);
        var pose=new Matrix3x2f(graphics.pose());var clip=graphics.peekScissorStack();
        var bounds=new ScreenRectangle(x0,y0,x1-x0,y1-y0).transformMaxBounds(pose);
        if(clip!=null)bounds=bounds.intersection(clip);
        if(bounds==null)return;
        graphics.submitPictureInPictureRenderState(new State(x0,y0,x1,y1,1F,pose,clip,bounds,(float)Math.clamp(charge,0,1),time));
    }
    public record State(int x0,int y0,int x1,int y1,float scale,Matrix3x2f pose,ScreenRectangle scissorArea,ScreenRectangle bounds,float charge,float time) implements PictureInPictureRenderState {}
    public static void extractTextured(GuiGraphicsExtractor graphics,double x,double y,double width,double height,double charge,TextureAtlasSprite empty,TextureAtlasSprite full){
        if(width<=0||height<=0)return;
        charge=Math.clamp(charge,0,1);boolean horizontal=width>height;
        double length=horizontal?width:height,thickness=horizontal?height:width;
        graphics.pose().pushMatrix();
        try{
            graphics.pose().translate((float)x,(float)y);
            if(horizontal){graphics.pose().translate((float)width,0);graphics.pose().rotate((float)(Math.PI/2));}
            tileSprite(graphics,0,0,thickness,length,empty);
            tileSprite(graphics,0,length*(1-charge),thickness,length*charge,full);
        }finally{graphics.pose().popMatrix();}
    }
    private static void tileSprite(GuiGraphicsExtractor graphics,double x,double y,double width,double height,TextureAtlasSprite sprite){
        if(width<=0||height<=0)return;
        var extension=(GuiGraphicsExtension)(Object)graphics;
        int textureHeight=sprite.contents().height(),textureWidth=sprite.contents().width();
        for(double offset=0;offset<height;offset+=Math.min(Math.max(1,textureHeight-2),height-offset)){
            double part=Math.min(textureHeight,height-offset);
            extension.cc$blitPartialSprite(RenderPipelines.GUI_TEXTURED,x,y+height-offset-part,x+width,y+height-offset,
                    sprite,0,(float)(part/textureHeight),(float)(width/textureWidth),0);
        }
    }
    private record Settings(float time,float charge,int x,int y,int width,int height,int screenWidth,int screenHeight) implements DynamicUniformStorage.DynamicUniform {
        @Override public void write(ByteBuffer buffer){buffer.putFloat(time).putFloat(charge).putInt(x).putInt(y).putInt(width).putInt(height).putInt(screenWidth).putInt(screenHeight);}
    }
    private static final class Renderer extends PictureInPictureRenderer<State> {
        private final DynamicUniformStorage<Settings> uniforms=new DynamicUniformStorage<>("BrandonsCore energy bar",32,4);
        private final Consumer<RenderFrameEvent.Post> endFrame=event->uniforms.endFrame();
        Renderer(MultiBufferSource.BufferSource buffers){super(buffers);NeoForge.EVENT_BUS.addListener(endFrame);}
        @Override public Class<State> getRenderStateClass(){return State.class;}
        @Override protected String getTextureLabel(){return "BrandonsCore animated energy bar";}
        @Override protected void renderToTexture(State state,PoseStack ignored){
            var target=RenderSystem.outputColorTextureOverride;
            if(target==null)throw new IllegalStateException("Energy bar render target is unavailable");
            var window=Minecraft.getInstance().getWindow();float guiScale=(float)window.getGuiScale();
            var bottomLeft=new Vector2f(state.x0(),state.y1()).mulPosition(state.pose());
            var settings=uniforms.writeUniform(new Settings(state.time(),state.charge()*1.01F,
                    Math.round(bottomLeft.x*guiScale),window.getHeight()-Math.round(bottomLeft.y*guiScale),
                    target.getWidth(0),target.getHeight(0),window.getWidth(),window.getHeight()));
            var builder=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);
            builder.addVertex(-1,-1,0);builder.addVertex(1,-1,0);builder.addVertex(1,1,0);builder.addVertex(-1,1,0);
            try(var mesh=builder.buildOrThrow()){
                var vertices=DefaultVertexFormat.POSITION.uploadImmediateVertexBuffer(mesh.vertexBuffer());
                var sequence=RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                var indices=sequence.getBuffer(mesh.drawState().indexCount());
                try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"BrandonsCore energy bar",target,OptionalInt.empty())){
                    pass.setPipeline(PIPELINE);pass.setUniform("EnergyBarSettings",settings);
                    pass.setVertexBuffer(0,vertices);pass.setIndexBuffer(indices,sequence.type());
                    pass.drawIndexed(0,0,mesh.drawState().indexCount(),1);
                }
            }
        }
        @Override public void close(){NeoForge.EVENT_BUS.unregister(endFrame);uniforms.close();super.close();}
    }
}
