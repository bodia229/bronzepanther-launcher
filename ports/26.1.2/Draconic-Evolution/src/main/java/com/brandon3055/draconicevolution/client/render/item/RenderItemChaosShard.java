package com.brandon3055.draconicevolution.client.render.item;

import com.brandon3055.draconicevolution.client.render.item.PerspectiveModelState;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import com.brandon3055.draconicevolution.client.render.item.DEItemRenderer;
import codechicken.lib.render.model.OBJParser;
import com.brandon3055.draconicevolution.client.render.item.TransformUtils;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Scale;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.client.shader.BCShaders;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.init.DEContent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static com.brandon3055.draconicevolution.DraconicEvolution.MODID;

/**
 * Created by brandon3055 on 27/2/20.
 */
public class RenderItemChaosShard implements DEGeometryItemRenderer {

    private static final RenderType CHAOS_CRYSTAL_INNER = RenderType.create(MODID+":chaos_crystal_inner",
            net.minecraft.client.renderer.rendertype.RenderSetup.builder(BCShaders.CHAOS_ENTITY_SHADER.pipeline(VertexFormat.Mode.TRIANGLES,true,false))
                    .withTexture("Sampler0",Identifier.fromNamespaceAndPath(MODID,"textures/item/equipment/chaos_shader.png"),
                            () -> com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getSampler(
                                    com.mojang.blaze3d.textures.AddressMode.REPEAT,com.mojang.blaze3d.textures.AddressMode.REPEAT,
                                    com.mojang.blaze3d.textures.FilterMode.LINEAR,com.mojang.blaze3d.textures.FilterMode.LINEAR,false))
                    .useLightmap().useOverlay().bufferSize(256).sortOnUpload().createRenderSetup());
    private static final RenderType CHAOS_CRYSTAL = RenderType.create(MODID+":chaos_crystal",
            net.minecraft.client.renderer.rendertype.RenderSetup.builder(net.minecraft.client.renderer.RenderPipelines.ENTITY_TRANSLUCENT_CULL.toBuilder()
                    .withLocation(Identifier.fromNamespaceAndPath(MODID,"pipeline/chaos_shard_shell"))
                    .withVertexFormat(DefaultVertexFormat.ENTITY,VertexFormat.Mode.TRIANGLES).build())
                    .withTexture("Sampler0",Identifier.fromNamespaceAndPath(MODID,"textures/block/chaos_crystal.png"))
                    .useLightmap().useOverlay().bufferSize(256).sortOnUpload().createRenderSetup());
    public static void registerPipelines(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CHAOS_CRYSTAL.pipeline());
    }

    private final CCModel shard;
    private final Item item;

    public RenderItemChaosShard(Item item) {
        this.item = item;
        Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/item/chaos_shard.obj"))
                .ignoreMtl()
                .parse();
        shard = CCModel.combine(map.values())
                .backfacedCopy()
                .computeNormals();
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
        Matrix4 mat = new Matrix4(mStack);
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = packedLight;
        ccrs.overlay = packedOverlay;
        mat.apply(new Scale(item == DEContent.CHAOS_SHARD.get() ? 1 : item == DEContent.CHAOS_FRAG_LARGE.get() ? 0.75 : item == DEContent.CHAOS_FRAG_MEDIUM.get() ? 0.5 : 0.25).at(new Vector3(0.5, 0.5, 0.5)));

        BCShaders.CHAOS_ENTITY_SHADER.getModelMatUniform().glUniformMatrix4f(new Matrix4());
        BCShaders.CHAOS_ENTITY_SHADER.getSimpleLightUniform().glUniform1b(true);
        BCShaders.CHAOS_ENTITY_SHADER.getDisableLightUniform().glUniform1b(false);
        BCShaders.CHAOS_ENTITY_SHADER.getDisableOverlayUniform().glUniform1b(false);

        ccrs.bind(CHAOS_CRYSTAL_INNER, getter);
        shard.render(ccrs, mat);

        ccrs.baseColour = 0xFFFFFFF0;
        ccrs.bind(CHAOS_CRYSTAL, getter);
        mat.apply(new Scale(1.005).at(new Vector3(0.5, 0.5, 0.5)));
        shard.render(ccrs, mat);
    }

    // @formatter:off
    @Override
    public @Nullable PerspectiveModelState getModelState() { return TransformUtils.DEFAULT_ITEM; }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return false; }
    @Override public boolean usesBlockLight() { return false; }

    //This is not cursed at all! idk what your talking about!
    public static class CHAOS_SHARD extends RenderItemChaosShard { public CHAOS_SHARD() {super(DEContent.CHAOS_SHARD.get());}}
    public static class CHAOS_FRAG_LARGE extends RenderItemChaosShard { public CHAOS_FRAG_LARGE() {super(DEContent.CHAOS_FRAG_LARGE.get());}}
    public static class CHAOS_FRAG_MEDIUM extends RenderItemChaosShard { public CHAOS_FRAG_MEDIUM() {super(DEContent.CHAOS_FRAG_MEDIUM.get());}}
    public static class CHAOS_FRAG_SMALL extends RenderItemChaosShard { public CHAOS_FRAG_SMALL() {super(DEContent.CHAOS_FRAG_SMALL.get());}}
    // @formatter:on
}
