package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.lighting.LightModel;
import codechicken.lib.vec.*;
import codechicken.lib.vec.uv.IconTransformation;
import codechicken.lib.vec.uv.UV;
import com.brandon3055.draconicevolution.blocks.Potentiometer;
import com.brandon3055.draconicevolution.blocks.tileentity.TilePotentiometer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class RenderTilePotentiometer implements BlockEntityRenderer<TilePotentiometer, RenderTilePotentiometer.State> {

    private static CCModel model;


    public RenderTilePotentiometer(BlockEntityRendererProvider.Context context) {

        double pxl = 1D / 16D;
        double ls = pxl * 2.5;      //Left Side
        double rs = pxl * 1.5;      //Right Side
        double fe = pxl * 7;        //Front Edge
        double be = pxl * 0;        //Back Edge
        double bh = pxl * 3;        //Back Height
        double fh = pxl * 0.5;      //Front Height
        int i = 24;

        model = CCModel.quadModel(40);
        model.generateBlock(0, new Cuboid6(0, 0, 0, pxl * 4, pxl, pxl * 4));

        //Top
        model.verts[i++] = new Vertex5(new Vector3(ls, bh, be), new UV(ls, be));
        model.verts[i++] = new Vertex5(new Vector3(ls, fh, fe), new UV(ls, fe));
        model.verts[i++] = new Vertex5(new Vector3(rs, fh, fe), new UV(rs, fe));
        model.verts[i++] = new Vertex5(new Vector3(rs, bh, be), new UV(rs, be));

        //Left
        model.verts[i++] = new Vertex5(new Vector3(ls, 0, fe), new UV(0, fe));
        model.verts[i++] = new Vertex5(new Vector3(ls, 0, be), new UV(0, be));
        model.verts[i++] = new Vertex5(new Vector3(ls, bh, be), new UV(bh, be));
        model.verts[i++] = new Vertex5(new Vector3(ls, fh, fe), new UV(fh, fe));

        //Right
        model.verts[i++] = new Vertex5(new Vector3(rs, 0, fe), new UV(0, fe));
        model.verts[i++] = new Vertex5(new Vector3(rs, 0, be), new UV(0, be));
        model.verts[i++] = new Vertex5(new Vector3(rs, bh, be), new UV(bh, be));
        model.verts[i++] = new Vertex5(new Vector3(rs, fh, fe), new UV(fh, fe));

        //Back
        model.verts[i++] = new Vertex5(new Vector3(ls, pxl, be), new UV(ls, 0));
        model.verts[i++] = new Vertex5(new Vector3(rs, pxl, be), new UV(rs, 0));
        model.verts[i++] = new Vertex5(new Vector3(rs, bh, be), new UV(rs, bh));
        model.verts[i++] = new Vertex5(new Vector3(ls, bh, be), new UV(ls, bh));

        model.computeNormals();
        model.computeLightCoords();
        model = model.twoFacedCopy();
    }

    public static final class State extends BlockEntityRenderState {
        TextureAtlasSprite sprite;
        Direction facing = Direction.NORTH;
        int power;
    }
    private static final RenderType MODEL_TYPE = RenderTypes.solidMovingBlock();
    private static final SpriteId TEXTURE = new SpriteId(TextureAtlas.LOCATION_BLOCKS,
            Identifier.withDefaultNamespace("block/oak_planks"));
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TilePotentiometer tile, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        state.sprite = Minecraft.getInstance().getAtlasManager().get(TEXTURE);
        state.facing = tile.getBlockState().getValue(Potentiometer.FACING);
        state.power = tile.power.get();
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var sprite = state.sprite;
        if (sprite == null) return;
        var facing = state.facing;
        int power = state.power, light = state.lightCoords;
        collector.submitCustomGeometry(pose, MODEL_TYPE, (entry, vertices) -> {
            CCRenderState render = CCRenderState.instance();
            render.reset();
            render.bind(vertices, MODEL_TYPE);
            render.brightness = light;
            render.overlay = OverlayTexture.NO_OVERLAY;
            double pxl = 1D / 16D;
            Matrix4 mat = new Matrix4(entry);
            mat.apply(Rotation.sideOrientation(facing.getOpposite().get3DDataValue(), 0).at(Vector3.CENTER));
            mat.apply(new Translation(6 * pxl, pxl, 6 * pxl));
            mat.apply(new Rotation(power * 22.5D * -MathHelper.torad, 0, 1, 0).at(new Vector3(pxl * 2, 0, pxl * 2)));
            model.render(render, LightModel.standardLightModel, new IconTransformation(sprite), mat);
        });
    }
}
