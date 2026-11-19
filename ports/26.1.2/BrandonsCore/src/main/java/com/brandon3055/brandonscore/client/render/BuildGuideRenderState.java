package com.brandon3055.brandonscore.client.render;

import codechicken.lib.render.buffer.DelegatingVertexConsumer;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import codechicken.lib.vec.Cuboid6;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import java.util.*;

/** A detached guide snapshot: submission never reads blocks, player position or animation clocks. */
public final class BuildGuideRenderState {
    private static final RenderType OUTLINE=RenderType.create("brandonscore:invalid_outline",
            RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT.toBuilder()
                    .withLocation(Identifier.fromNamespaceAndPath("brandonscore","pipeline/invalid_outline"))
                    .withDepthStencilState(Optional.empty()).withCull(false).build()).bufferSize(256).createRenderSetup());
    private record Preview(BlockPos position,BlockModelRenderState model){}
    private final List<Preview> previews;
    private final List<BlockPos> invalid;
    private final int overlay;
    private final double radius;
    private BuildGuideRenderState(List<Preview> previews,List<BlockPos> invalid,int overlay,double radius){
        this.previews=List.copyOf(previews);this.invalid=List.copyOf(invalid);this.overlay=overlay;this.radius=radius;
    }
    public static void init(IEventBus bus){bus.addListener((RegisterRenderPipelinesEvent event)->event.registerPipeline(OUTLINE.pipeline()));}
    public static BuildGuideRenderState capture(Map<BlockPos,BlockState> previews,Collection<BlockPos> invalid,int overlay,double radius){
        var models=new ArrayList<Preview>();
        previews.forEach((pos,block)->{
            var model=new BlockModelRenderState();
            Minecraft.getInstance().getBlockModelResolver().update(model,block,BlockDisplayContext.create());
            if(!model.isEmpty())models.add(new Preview(pos.immutable(),model));
        });
        return new BuildGuideRenderState(models,invalid.stream().map(BlockPos::immutable).toList(),overlay,radius);
    }
    public int previewCount(){return previews.size();}
    public int invalidCount(){return invalid.size();}
    public void submit(PoseStack poses,SubmitNodeCollector collector,int light){
        for(var preview:previews){
            poses.pushPose();
            try{
                var pos=preview.position();poses.translate(pos.getX()+.1,pos.getY()+.1,pos.getZ()+.1);poses.scale(.8F,.8F,.8F);
                preview.model().submit(poses,collector,light,overlay,0);
            }finally{poses.popPose();}
        }
        for(var pos:invalid){
            poses.pushPose();
            try{
                poses.translate(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
                collector.order(1).submitCustomGeometry(poses,OUTLINE,(pose,consumer)->{
                    VertexConsumer width=new DelegatingVertexConsumer(consumer){
                        @Override public VertexConsumer addVertex(float x,float y,float z){super.addVertex(x,y,z);super.setLineWidth(4F);return this;}
                    };
                    var transformed=new TransformingVertexConsumer(width,new codechicken.lib.vec.Matrix4(pose.pose()));
                    codechicken.lib.render.RenderUtils.bufferCuboidOutline(transformed,new Cuboid6().expand(radius),1F,0F,0F,1F);
                });
            }finally{poses.popPose();}
        }
    }
}
