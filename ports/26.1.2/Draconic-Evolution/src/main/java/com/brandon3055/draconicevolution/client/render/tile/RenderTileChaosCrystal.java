package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.util.ClientUtils;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Vector3;
import com.brandon3055.draconicevolution.blocks.tileentity.TileChaosCrystal;
import com.brandon3055.draconicevolution.client.DEShaders;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Map;

import static com.brandon3055.draconicevolution.DraconicEvolution.MODID;

/**
 * Created by brandon3055 on 24/9/2015.
 */
public class RenderTileChaosCrystal implements DEBlockRenderer<TileChaosCrystal> {

    private static final RenderType CHAOS_CRYSTAL_INNER = com.brandon3055.draconicevolution.client.render.DEGeometryTypes.shader("rendertilechaoscrystal_chaos_crystal_inner", DEShaders.chaosBlockShader, Identifier.fromNamespaceAndPath(MODID, "textures/item/equipment/chaos_shader.png"), com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES, "translucent", false, true, true, false, false, true, false);

    private static final RenderType CHAOS_CRYSTAL = com.brandon3055.draconicevolution.client.render.DEGeometryTypes.basic("rendertilechaoscrystal_chaos_crystal", DefaultVertexFormat.BLOCK, com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES, "getRendertypeCutoutShader", Identifier.fromNamespaceAndPath(MODID, "textures/block/chaos_crystal.png"), "translucent", true, true, true, false, false, false, false);

    private static final RenderType CHAOS_CRYSTAL_SHIELD = com.brandon3055.draconicevolution.client.render.DEGeometryTypes.shader("rendertilechaoscrystal_chaos_crystal_shield", DEShaders.shieldShader, null, com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES, "translucent", false, true, true, false, false, false, false);

    private final CCModel model;
    private final com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry innerGeometry,surfaceGeometry,shieldGeometry;

    public RenderTileChaosCrystal(BlockEntityRendererProvider.Context context) {
        Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(MODID, "models/block/chaos_crystal.obj"))
                .ignoreMtl()
                .parse();
        model = CCModel.combine(map.values())
                .backfacedCopy()
                .computeNormals();
        innerGeometry=cacheGeometry(CHAOS_CRYSTAL_INNER,0xFFFFFFFF);
        surfaceGeometry=cacheGeometry(CHAOS_CRYSTAL,0xFFFFFFF0);
        shieldGeometry=cacheGeometry(CHAOS_CRYSTAL_SHIELD,0xFFFFFFF0);
    }

    private com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry cacheGeometry(RenderType type,int colour){
        return com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry.capture(buffers->{
            var state=CCRenderState.instance();state.reset();state.baseColour=colour;
            state.overlay=net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
            state.bind(type,buffers);model.render(state);
        });
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void render(TileChaosCrystal te, float partialTicks, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
        if (te.parentPos.notNull()) return;
        mStack.pushPose();
        try {
        mStack.translate(0.5,0.5,0.5);
        mStack.mulPose(com.mojang.math.Axis.YP.rotation((ClientEventHandler.elapsedTicks+partialTicks)/180F));
        mStack.scale(0.75F,0.75F,0.75F);
        Player player = Minecraft.getInstance().player;
        DEShaders.chaosBlockTime.glUniform1f((float) ClientUtils.getRenderTime());
        DEShaders.chaosBlockYaw.glUniform1f((float) (player.getYRot() * MathHelper.torad));
        DEShaders.chaosBlockPitch.glUniform1f((float) -(player.getXRot() * MathHelper.torad));
        var inner=innerGeometry.snapshotShaderState();
        DEBlockRenderer.addNative(mStack,(poses,collector)->inner.submit(poses,collector,packedLight));
        DEBlockRenderer.addNative(mStack,(poses,collector)->surfaceGeometry.submit(poses,collector,packedLight));

        if (!te.guardianDefeated.get()) {
            DEShaders.shieldBarMode.glUniform1i(0);
            DEShaders.shieldActivation.glUniform1f(1F);
            DEShaders.shieldColour.glUniform4f(1F, 0F, 0F, 1F);
            var shield=shieldGeometry.snapshotShaderState();
            DEBlockRenderer.addNative(mStack,(poses,collector)->shield.submit(poses,collector,packedLight));
        }
        } finally {mStack.popPose();}
    }

    @Override
    public AABB getRenderBoundingBox(TileChaosCrystal blockEntity) {
        return DEBlockRenderer.super.getRenderBoundingBox(blockEntity).inflate(3);
    }
}
