package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.vec.*;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.machines.Grinder;
import com.brandon3055.draconicevolution.blocks.tileentity.TileGrinder;
import com.brandon3055.draconicevolution.init.DEContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Vector3f;

/** Frame state contains positions and animation values, never live target entities or inventories. */
public class RenderTileGrinder implements BlockEntityRenderer<TileGrinder, RenderTileGrinder.State> {
    private static final double[] ROTATION_MAP = {0, 180, 90, -90};
    private static final RenderType SWORD = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/block/grinder.png"));
    private static final RenderType FAN = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/block/parts/machine_fan.png"));
    private final CCModel swordModel, fanModel;
    public RenderTileGrinder(BlockEntityRendererProvider.Context context) {
        var fan = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/grinder/grinder_fan.obj")).quads().ignoreMtl().parse();
        fanModel = CCModel.combine(fan.values());
        var sword = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/grinder/grinder_sword.obj")).quads().ignoreMtl().parse();
        swordModel = CCModel.combine(sword.values());
        swordModel.computeNormals();
        swordModel.apply(new Scale(-1 / 16F));
        swordModel.apply(new Rotation(Math.PI, 0, 0, 1));
        swordModel.apply(new Rotation(Math.PI / 2, 0, 1, 0));
    }
    public static final class State extends BlockEntityRenderState {
        boolean visible;
        Direction facing = Direction.NORTH;
        Vector3 center, targetA, targetB;
        double animationA, animationB, fanRotation;
        AABB area;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileGrinder tile, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        state.visible = tile.getBlockState().is(DEContent.GRINDER.get());
        state.area = null;
        if (!state.visible) return;
        state.facing = tile.getBlockState().getValue(Grinder.FACING);
        state.center = Vector3.fromTileCenter(tile);
        state.targetA = targetPosition(tile.targetA, partialTick);
        state.targetB = targetPosition(tile.targetB, partialTick);
        state.animationA = Math.min(tile.animA + partialTick * tile.getAnimSpeed(), 1);
        state.animationB = Math.min(tile.animB + partialTick * tile.getAnimSpeed(), 1);
        state.fanRotation = tile.fanRotation + tile.fanSpeed * partialTick;
        if (tile.aoeDisplay > 0.51) {
            tile.validateKillZone(true);
            state.area = tile.killZone.move(-tile.getBlockPos().getX(), -tile.getBlockPos().getY(), -tile.getBlockPos().getZ())
                    .deflate(0.01).deflate(tile.aoe.get() - tile.aoeDisplay);
        }
    }
    private static Vector3 targetPosition(Entity entity, float partialTick) {
        return entity == null ? null : new Vector3(entity.getPosition(partialTick)).add(0, entity.getBbHeight() / 2D, 0);
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) return;
        var facing = state.facing;
        var center = state.center.copy();
        var targetA = state.targetA == null ? null : state.targetA.copy();
        var targetB = state.targetB == null ? null : state.targetB.copy();
        double animationA = state.animationA, animationB = state.animationB, rotation = state.fanRotation;
        int light = state.lightCoords;
        collector.submitCustomGeometry(pose, SWORD, (entry, vertices) -> {
            var render = CCRenderState.instance();
            render.reset(); render.brightness = light; render.overlay = OverlayTexture.NO_OVERLAY;
            render.bind(new TransformingVertexConsumer(vertices, new Matrix4(entry)), SWORD);
            renderSword(render, facing, 0.34, center, targetA, animationA, 0);
            renderSword(render, facing, -0.34, center, targetB, animationB, 0);
        });
        collector.submitCustomGeometry(pose, FAN, (entry, vertices) -> {
            var render = CCRenderState.instance();
            render.reset(); render.brightness = light; render.overlay = OverlayTexture.NO_OVERLAY;
            render.bind(vertices, FAN);
            var transform = new Matrix4(entry).translate(Vector3.CENTER);
            transform.apply(new Rotation(-facing.toYRot() * MathHelper.torad, 0, 1, 0));
            transform.apply(new Scale(-0.0625));
            transform.apply(new Rotation(rotation, 0, 0, 1));
            fanModel.render(render, transform);
        });
        var area = state.area;
        if (area != null) {
            collector.submitCustomGeometry(pose, RenderTypes.linesTranslucent(), (entry, vertices) ->
                Shapes.create(area).forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                    var normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
                    vertices.addVertex(entry, (float) x1, (float) y1, (float) z1).setColor(0xFF000000).setNormal(entry, normal).setLineWidth(4);
                    vertices.addVertex(entry, (float) x2, (float) y2, (float) z2).setColor(0xFF000000).setNormal(entry, normal).setLineWidth(4);
                }));
            collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (entry, vertices) ->
                RenderUtils.bufferCuboidSolid(new TransformingVertexConsumer(vertices, new Matrix4(entry)), new Cuboid6(area), 0, 1, 1, 0.2F));
        }
    }
    @Override public AABB getRenderBoundingBox(TileGrinder tile) { return new AABB(tile.getBlockPos()).inflate(8); }

    private void renderSword(CCRenderState ccrs, Direction tileFacing, double sideOffset, Vector3 tilePos, Vector3 targetPos, double attackAnimTime, float partialTicks) {
        attackAnimTime *= 2;
        double yoyo = attackAnimTime > 1D ? 1D - (attackAnimTime - 1D) : attackAnimTime;
        sideOffset *= 1D - yoyo;
        double sideAngle = ROTATION_MAP[tileFacing.get3DDataValue() - 2] * MathHelper.torad;

        Transformation rotation = new Rotation(yoyo * Math.PI * -2.8, 1, 0, 0).at(new Vector3(0, -0.2, 0));
        Vector3 handPos = new Vector3(0.4975 + sideOffset, 0.5, 0.125 + (yoyo * (0.375)));
        handPos.apply(new Rotation(sideAngle, 0, 1, 0).at(new Vector3(0.5, 0, 0.5)));

        Transformation translation = new Translation(handPos);
        TransformationList transforms = rotation.with(translation);
        if (targetPos == null) {
            transforms = transforms.with(new Rotation(sideAngle, 0, 1, 0).at(handPos));
        } else {
            Vector3 targetVec = targetPos.copy().subtract(tilePos);
            double angle = Math.atan2(targetVec.x, targetVec.z) + Math.PI;
            transforms = transforms.with(new Rotation(angle, 0, 1, 0).at(handPos));
            transforms = transforms.with(new Translation(targetVec.multiply(yoyo)));
        }

        swordModel.render(ccrs, transforms);
    }

}
