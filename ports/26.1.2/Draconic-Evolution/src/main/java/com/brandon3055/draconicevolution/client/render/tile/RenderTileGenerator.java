package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.lighting.LightModel;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Scale;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.machines.Generator;
import com.brandon3055.draconicevolution.blocks.tileentity.TileGenerator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

public class RenderTileGenerator implements BlockEntityRenderer<TileGenerator, RenderTileGenerator.State> {
    private static final RenderType MODEL_TYPE = RenderTypes.solidMovingBlock();
    private static final SpriteId TEXTURE = new SpriteId(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,
            Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "block/generator/generator_2"));
    private final CCModel fan;
    public RenderTileGenerator(BlockEntityRendererProvider.Context context) {
        var models = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/generator/generator_fan.obj"))
                .quads().ignoreMtl().parse();
        fan = CCModel.combine(models.values()).backfacedCopy();
    }
    public static final class State extends BlockEntityRenderState {
        TextureAtlasSprite sprite;
        Direction facing = Direction.NORTH;
        double rotation;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileGenerator tile, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        state.sprite = Minecraft.getInstance().getAtlasManager().get(TEXTURE);
        state.facing = tile.getBlockState().getValue(Generator.FACING);
        state.rotation = tile.rotation + tile.rotationSpeed * partialTick;
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var sprite = state.sprite;
        if (sprite == null) return;
        var facing = state.facing;
        double rotation = state.rotation;
        int light = state.lightCoords;
        collector.submitCustomGeometry(pose, MODEL_TYPE, (entry, vertices) -> {
            var renderState = CCRenderState.instance();
            renderState.reset();
            renderState.brightness = light;
            renderState.overlay = OverlayTexture.NO_OVERLAY;
            renderState.bind(vertices, MODEL_TYPE);
            Matrix4 transform = new Matrix4(entry);
            transform.translate(Vector3.CENTER);
            transform.apply(new Rotation(-facing.getOpposite().toYRot() * MathHelper.torad, 0, 1, 0));
            transform.apply(new Scale(0.0625));
            transform.apply(new Rotation(rotation, 1, 0, 0).at(new Vector3(0, -1.5, -4.5)));
            fan.render(renderState, LightModel.standardLightModel, new IconTransformation(sprite), transform);
        });
    }
}
