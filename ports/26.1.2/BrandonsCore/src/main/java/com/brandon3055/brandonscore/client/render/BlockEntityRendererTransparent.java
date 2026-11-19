package com.brandon3055.brandonscore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Created by brandon3055 on 23/10/2022
 */
public interface BlockEntityRendererTransparent<T extends BlockEntity, S extends BlockEntityRenderState> extends BlockEntityRenderer<T,S> {

    /** The state must contain all tile data needed by this late pass, captured in extractRenderState. */
    void renderTransparent(S state, PoseStack poseStack, MultiBufferSource buffers, CameraRenderState camera);

}
