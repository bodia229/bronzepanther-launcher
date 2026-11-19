package com.brandon3055.draconicevolution.client;

import codechicken.lib.data.MCDataInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.brandon3055.brandonscore.client.render.GuiRenderBridge;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import com.brandon3055.brandonscore.api.TimeKeeper;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.render.entity.DraconicGuardianRenderer;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.UUID;

/**
 * Created by brandon3055 on 24/7/21
 */
public class CustomBossInfoHandler {
    //TODO Update this to sprites.
    private static final Identifier GUI_BARS_LOCATION = Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/gui/bars.png");
    private static final Map<UUID, BossShieldInfo> events = Maps.newLinkedHashMap();

    private static final Identifier ENDER_CRYSTAL_TEXTURES = Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/entity/guardian_crystal.png");
    private static final RenderType HUD_SHIELD_TYPE=createHudShieldType();
    private static RenderType createHudShieldType() {
        var shader=DEShaders.shieldShader;
        var pipeline=shader.variant(shader.pipeline(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,true,false,false).toBuilder()
                .withLocation(Identifier.fromNamespaceAndPath("draconicevolution","pipeline/boss_hud_shield"))
                .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.ALWAYS_PASS,false)).build());
        return RenderType.create("draconicevolution:boss_hud_shield",net.minecraft.client.renderer.rendertype.RenderSetup.builder(pipeline).useLightmap().createRenderSetup());
    }
    private static final RenderType RENDER_TYPE = net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(ENDER_CRYSTAL_TEXTURES);
    private static final float SIN_45 = (float) Math.sin((Math.PI / 4D));

    private static final ModelPart glass;
    private static final ModelPart cube;

    static {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("glass", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        partdefinition.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        ModelPart modelRoot = LayerDefinition.create(meshdefinition, 64, 32).bakeRoot();
        glass = modelRoot.getChild("glass");
        cube = modelRoot.getChild("cube");
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(CustomBossInfoHandler::onClientDisconnect);
        NeoForge.EVENT_BUS.addListener(CustomBossInfoHandler::preDrawBossInfo);
    }

    public static void preDrawBossInfo(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent info = event.getBossEvent();
        if (!events.containsKey(info.getId())) return;
        event.setCanceled(true);
        BossShieldInfo shieldInfo = events.get(info.getId());
        Minecraft mc = Minecraft.getInstance();
        GuiGraphicsExtractor render = event.getGuiGraphics();
        PoseStack poseStack = new PoseStack();
        poseStack.translate(-event.getWindow().getGuiScaledWidth()/2F,-event.getWindow().getGuiScaledHeight()/2F,0);
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        int width = event.getWindow().getGuiScaledWidth();
        int x = event.getX();
        int y = event.getY();

        drawBar(event.getGuiGraphics(), x, y, info);

        float shield = shieldInfo.isImmune() ? 1 : shieldInfo.getShield();
        var geometry=com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry.capture(getter->{

        if (DEConfig.guardianShaders) {
            if (shieldInfo.isImmune()) {
                DEShaders.shieldColour.glUniform4f(0F, 1F, 1F, 2F);
            } else {
                DEShaders.shieldColour.glUniform4f(1F, 0F, 0F, 2F);
            }
            DEShaders.shieldBarMode.glUniform1i(1);
            DEShaders.shieldActivation.glUniform1f(shield);
            VertexConsumer builder = new TransformingVertexConsumer(getter.getBuffer(HUD_SHIELD_TYPE), poseStack);
            drawShieldRect(builder, x, y, 182, 6);
            com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry.flush(getter);
        }

        if (shieldInfo.crystals > 0) {
            Component countText = Component.literal("x" + shieldInfo.crystals);
            int countWidth = mc.font.width(countText);

            float anim = (TimeKeeper.getClientTick() + partialTick) * 3.0F;
            VertexConsumer ivertexbuilder = getter.getBuffer(RENDER_TYPE);
            poseStack.pushPose();
            poseStack.translate(x + 182 - countWidth - 8, y - 6, 0.0D);
            poseStack.scale(14.0F, 14.0F, 14.0F);
            int i = OverlayTexture.NO_OVERLAY;
            poseStack.mulPose(Axis.YP.rotationDegrees(anim));
            poseStack.mulPose(new Quaternionf().setAngleAxis((float)Math.PI / 3F, SIN_45, 0.0F, SIN_45));
            glass.render(poseStack, ivertexbuilder, 240, i);
            poseStack.scale(0.875F, 0.875F, 0.875F);
            poseStack.mulPose(new Quaternionf().setAngleAxis((float)Math.PI / 3F, SIN_45, 0.0F, SIN_45));
            poseStack.mulPose(Axis.YP.rotationDegrees(anim));
            glass.render(poseStack, ivertexbuilder, 240, i);
            poseStack.scale(0.875F, 0.875F, 0.875F);
            poseStack.mulPose(new Quaternionf().setAngleAxis((float)Math.PI / 3F, SIN_45, 0.0F, SIN_45));
            poseStack.mulPose(Axis.YP.rotationDegrees(anim));
            cube.render(poseStack, ivertexbuilder, 240, i);
            poseStack.popPose();
            com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry.flush(getter);

            if (DEConfig.guardianShaders) {
                DEShaders.shieldColour.glUniform4f(1F, 0F, 0F, 1.5F);
                DEShaders.shieldActivation.glUniform1f(1F);
                VertexConsumer shaderBuilder = getter.getBuffer(HUD_SHIELD_TYPE);
                poseStack.pushPose();
                poseStack.translate(x + 182 - countWidth - 8, y - 6, 0.0D);
                poseStack.scale(14.0F, 14.0F, 14.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(anim));
                poseStack.mulPose(new Quaternionf().setAngleAxis((float)Math.PI / 3F, SIN_45, 0.0F, SIN_45));
                glass.render(poseStack, shaderBuilder, 240, i);
                poseStack.scale(0.875F, 0.875F, 0.875F);
                poseStack.mulPose(new Quaternionf().setAngleAxis((float)Math.PI / 3F, SIN_45, 0.0F, SIN_45));
                poseStack.mulPose(Axis.YP.rotationDegrees(anim));
                glass.render(poseStack, shaderBuilder, 240, i);
                poseStack.scale(0.875F, 0.875F, 0.875F);
                poseStack.mulPose(new Quaternionf().setAngleAxis((float)Math.PI / 3F, SIN_45, 0.0F, SIN_45));
                poseStack.mulPose(Axis.YP.rotationDegrees(anim));
                cube.render(poseStack, shaderBuilder, 240, i);
                poseStack.popPose();
                com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry.flush(getter);
            }

            render.cc$drawString(net.minecraft.client.Minecraft.getInstance().font,Component.literal("x" + shieldInfo.crystals), x + 182 - countWidth, (float) y - 9, 0xffFFFF);
        }

        });
        render.nextStratum();
        com.brandon3055.draconicevolution.client.render.DEGuiGeometry.extract(render,0,0,width,event.getWindow().getGuiScaledHeight(),geometry);
        render.nextStratum();
        Component itextcomponent = info.getName();
        int stringWidth = mc.font.width(itextcomponent);
        int stringX = shieldInfo.crystals > 0 ? x : width / 2 - stringWidth / 2;
        int stringY = y - 9;
        poseStack.translate(0, 0, 16);
        render.cc$drawString(net.minecraft.client.Minecraft.getInstance().font,itextcomponent, (float) stringX, (float) stringY, 0xff0000);
    }

    private static void drawBar(GuiGraphicsExtractor graphics, int x, int y, BossEvent info) {
        drawRect(graphics, x, y, 0, info.getColor().ordinal() * 5 * 2, 182, 5);
        if (info.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
            drawRect(graphics, x, y, 0, 80 + (info.getOverlay().ordinal() - 1) * 5 * 2, 182, 5);
        }

        int i = (int) (info.getProgress() * 183.0F);
        if (i > 0) {
            drawRect(graphics, x, y, 0, info.getColor().ordinal() * 5 * 2 + 5, i, 5);
            if (info.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
                drawRect(graphics, x, y, 0, 80 + (info.getOverlay().ordinal() - 1) * 5 * 2 + 5, i, 5);
            }
        }
    }

    private static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        events.clear();
    }

    public static void handlePacket(MCDataInput input) {
        UUID id = input.readUUID();
        int op = input.readByte();
//        LOGGER.info("Packet, OP: " + op + ". ID: " + id + ", Exists: " + events.containsKey(id));
        switch (op) {
            case 0: //Add
                events.put(id, new BossShieldInfo(input.readFloat(), input.readByte(), input.readBoolean()));
                break;
            case 1: //Remove
                events.remove(id);
                break;
            case 2: //Update Shield
                events.get(id).setShield(input.readFloat());
                break;
            case 3: //Update Crystals
                events.get(id).setCrystals(input.readByte());
                break;
            case 4: //Update Immune
                events.get(id).setImmune(input.readBoolean());
                break;
        }
    }

    public static void drawRect(GuiGraphicsExtractor render, int x, int y, int u, int v, int width, int height) {
        render.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, GUI_BARS_LOCATION, x, y, (float) u, (float) v, width, height, 256, 256);
    }

    public static void drawShieldRect(VertexConsumer builder, int x, int y, int width, int height) {
        drawQuad(builder, x, x + width, y, y + height, 0, 0F, 1F, 0F, 1F);
    }

    private static void drawQuad(VertexConsumer builder, int x, int xMax, int y, int yMax, int z, float u, float uMax, float v, float vMax) {
        builder.addVertex((float) x, (float) yMax, (float) z).setUv(u, vMax);
        builder.addVertex((float) xMax, (float) yMax, (float) z).setUv(uMax, vMax);
        builder.addVertex((float) xMax, (float) y, (float) z).setUv(uMax, v);
        builder.addVertex((float) x, (float) y, (float) z).setUv(u, v);
    }

    public static class BossShieldInfo {
        protected float lastPower;
        protected float targetPower;
        protected long setTime;
        protected int crystals;
        protected boolean immune;

        public BossShieldInfo(float power, int crystals, boolean immune) {
            this.lastPower = power;
            this.targetPower = power;
            this.crystals = crystals;
            this.immune = immune;
            this.setTime = Util.getMillis();
        }

        public BossShieldInfo setShield(float power) {
            this.lastPower = this.getShield();
            this.targetPower = power;
            this.setTime = Util.getMillis();
            return this;
        }

        public float getShield() {
            long i = Util.getMillis() - this.setTime;
            float f = Mth.clamp((float) i / 100.0F, 0.0F, 1.0F);
            return Mth.lerp(f, this.lastPower, this.targetPower);
        }

        public void setCrystals(int crystals) {
            this.crystals = crystals;
        }

        public int getCrystals() {
            return crystals;
        }

        public void setImmune(boolean immune) {
            this.immune = immune;
        }

        public boolean isImmune() {
            return immune;
        }
    }
}
