package com.brandon3055.brandonscore.client.render;

import com.brandon3055.brandonscore.api.TimeKeeper;
import com.brandon3055.brandonscore.multiblock.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class MultiBlockRenderers {
    private MultiBlockRenderers(){}
    /** Call during tile-state extraction and retain the result in the tile's native render state. */
    public static BuildGuideRenderState extractBuildGuide(Level level,BlockPos origin,MultiBlockDefinition structure,float partialTicks){
        var states=new IdentityHashMap<MultiBlockPart,BlockState>();var previews=new LinkedHashMap<BlockPos,BlockState>();
        var invalid=new ArrayList<BlockPos>();var player=Minecraft.getInstance().player;int tick=TimeKeeper.getClientTick();
        int phase=tick%80,overlay=5;if(phase>=70)overlay+=(int)(Math.sin(((phase-70)/10F)*Math.PI)*5);
        for(var entry:structure.getBlocks().entrySet()){
            var pos=entry.getKey();var part=entry.getValue();if(part.validBlocks().isEmpty())continue;var worldPos=origin.offset(pos);
            if(level.isEmptyBlock(worldPos)){
                if(player!=null&&worldPos.distToCenterSqr(player.getEyePosition())<16)continue;
                var state=states.computeIfAbsent(part,p->List.copyOf(p.validBlocks()).get((tick/40)%p.validBlocks().size()).defaultBlockState());
                if(!state.isAir())previews.put(pos.immutable(),state);
            }else if(!part.isMatch(level,worldPos))invalid.add(pos.immutable());
        }
        double pulse=Math.max(.1,Math.abs(Math.sin((tick+partialTicks)/25D*3.141593)))/2;
        return BuildGuideRenderState.capture(previews,invalid,OverlayTexture.pack(overlay,10),pulse);
    }
}
