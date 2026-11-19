package com.brandon3055.draconicevolution.client.render.item;

import codechicken.lib.math.MathHelper;
import com.brandon3055.draconicevolution.client.render.item.PerspectiveModelState;
import com.brandon3055.draconicevolution.client.render.item.DEItemRenderer;
import com.brandon3055.draconicevolution.client.render.item.TransformUtils;
import com.brandon3055.brandonscore.api.TimeKeeper;
import com.brandon3055.draconicevolution.init.DEContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by brandon3055 on 18/04/2017.
 */
public class RenderItemMobSoul implements DEItemRenderer {

    private static Set<Entity> brokenMobs = new HashSet<>();

    public RenderItemMobSoul() {
    }

    //region Unused

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    //endregion


//    //    Remember GuiInventory.drawEntityOnScreen
//    @Override
//    public void renderItem(ItemStack item, ItemCameraTransforms.TransformType transformType) {

//
//        try {
//            RenderSystem.pushMatrix();

//
//            if (transformType != ItemCameraTransforms.TransformType.GROUND && transformType != ItemCameraTransforms.TransformType.FIXED) {
//                RenderSystem.rotatef((float) Math.sin((ClientEventHandler.elapsedTicks + Minecraft.getInstance().getRenderPartialTicks()) / 50F) * 15F, 1, 0, -0.5F);
//                RenderSystem.rotated((ClientEventHandler.elapsedTicks + Minecraft.getInstance().getRenderPartialTicks()) * 3, 0, 1, 0);
//            }
//
//            EntityRendererManager rendermanager = Minecraft.getInstance().getRenderManager();
//            rendermanager.renderEntity(mob, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
//
//            if (transformType != ItemCameraTransforms.TransformType.GROUND && transformType != ItemCameraTransforms.TransformType.FIXED) {
////                RenderSystem.enableRescaleNormal();
////                RenderSystem.setActiveTexture(OpenGlHelper.lightmapTexUnit);
////                RenderSystem.disableTexture();
////                RenderSystem.setActiveTexture(OpenGlHelper.defaultTexUnit);
////                RenderSystem.disableLighting();
//
////                RenderHelper.disableStandardItemLighting();
////                RenderSystem.disableRescaleNormal();
////                RenderSystem.activeTexture(GLX.GL_TEXTURE1);
////                RenderSystem.disableTexture();
////                RenderSystem.activeTexture(GLX.GL_TEXTURE0);
//            }
//
//            //Some entities like the ender dragon modify the blend state which if not corrected like this breaks inventory rendering.
//            RenderSystem.enableBlend();
//            RenderSystem.blendFuncSeparate(RenderSystem.SourceFactor.SRC_ALPHA, RenderSystem.DestFactor.ONE_MINUS_SRC_ALPHA, RenderSystem.SourceFactor.ONE, RenderSystem.DestFactor.ZERO);
//            RenderSystem.popMatrix();
//        }
//        catch (Throwable e) {
//            if (MobSoul.randomDisplayList != null) {
//                MobSoul.randomDisplayList.remove(mob.getType().getRegistryName().toString());
//            } else {
//                brokenMobs.add(mob);
//                LogHelper.error("Error rendering mob soul! " + mob);
//                e.printStackTrace();
//            }
//        }
//    }

    private record SoulState(net.minecraft.client.renderer.entity.state.EntityRenderState entity,
            float scale, Quaternionf rotation, Quaternionf camera) {}

    @Override
    public Object extractState(ItemStack stack, ItemDisplayContext context, net.minecraft.client.multiplayer.ClientLevel level,
            net.minecraft.world.entity.ItemOwner owner) {
        Entity mob=DEContent.MOB_SOUL.get().getRenderEntity(stack);
        if(mob==null || brokenMobs.contains(mob)) return null;
        float size=Math.max(mob.getBbWidth(),mob.getBbHeight());
        if(!(size>0) || !Float.isFinite(size)) return null;
        var mc=Minecraft.getInstance();
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var state=mc.getEntityRenderDispatcher().extractEntity(mob,partial);
        state.nameTag=null; state.scoreText=null; state.shadowPieces.clear();
        Quaternionf rotation=new Quaternionf();
        if(context!=ItemDisplayContext.GROUND && context!=ItemDisplayContext.FIXED) {
            float time=TimeKeeper.getClientTick()+partial;
            float a=(float)Math.sin(time/50F)*15F*(float)MathHelper.torad;
            rotation.rotationXYZ(a,0,-.5F*a).rotateY(time*3F*(float)MathHelper.torad);
        }
        return new SoulState(state,1F/size,rotation,new Quaternionf(mc.gameRenderer.getMainCamera().rotation()));
    }
    @Override
    public void submitItem(ItemStack stack, ItemDisplayContext context, Object argument, PoseStack poses,
            net.minecraft.client.renderer.SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
        if(!(argument instanceof SoulState soul)) return;
        poses.pushPose();
        try {
            poses.translate(.5,0,.5); poses.scale(soul.scale,soul.scale,soul.scale); poses.mulPose(soul.rotation);
            soul.entity.lightCoords=light;
            var camera=new net.minecraft.client.renderer.state.level.CameraRenderState(); camera.orientation.set(soul.camera);
            Minecraft.getInstance().getEntityRenderDispatcher().submit(soul.entity,camera,0,0,0,poses,collector);
        } finally { poses.popPose(); }
    }

    @Override
    public @Nullable PerspectiveModelState getModelState() {
        return TransformUtils.DEFAULT_BLOCK;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }
}