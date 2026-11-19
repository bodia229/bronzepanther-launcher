package com.brandon3055.draconicevolution.client.render.entity;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.entity.guardian.GuardianProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Billboard submission uses only extracted state, never a live projectile. */
public class GuardianProjectileRenderer extends EntityRenderer<GuardianProjectileEntity, EntityRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/entity/guardian_fireball.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

    public GuardianProjectileRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override protected int getBlockLightLevel(GuardianProjectileEntity entity, BlockPos pos) { return 15; }
    @Override public EntityRenderState createRenderState() { return new EntityRenderState(); }

    @Override
    public void submit(EntityRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        final int light = state.lightCoords;
        poses.pushPose();
        try {
            poses.scale(2F, 2F, 2F);
            poses.mulPose(camera.orientation);
            // Retain DE's original billboard orientation, including its mirrored UV direction.
            poses.mulPose(Axis.YP.rotationDegrees(180F));
            collector.submitCustomGeometry(poses, RENDER_TYPE, (pose, buffer) -> {
                vertex(buffer, pose, light, 0F, 0, 0, 1);
                vertex(buffer, pose, light, 1F, 0, 1, 1);
                vertex(buffer, pose, light, 1F, 1, 1, 0);
                vertex(buffer, pose, light, 0F, 1, 0, 0);
            });
        } finally { poses.popPose(); }
        super.submit(state, poses, collector, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, float x, int y, int u, int v) {
        buffer.addVertex(pose, x - .5F, y - .25F, 0F).setColor(-1).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0F, 1F, 0F);
    }
}
