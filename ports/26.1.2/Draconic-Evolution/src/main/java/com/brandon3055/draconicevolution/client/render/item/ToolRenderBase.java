package com.brandon3055.draconicevolution.client.render.item;

import codechicken.lib.math.MathHelper;
import com.brandon3055.draconicevolution.client.render.item.PerspectiveModelState;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import com.brandon3055.brandonscore.client.render.CachedModelMesh;
import com.brandon3055.draconicevolution.client.render.item.DEItemRenderer;
import com.brandon3055.draconicevolution.client.render.item.TransformUtils;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.brandonscore.client.shader.BCShader;
import com.brandon3055.brandonscore.client.shader.BCShaders;
import com.brandon3055.brandonscore.client.shader.ChaosEntityShader;
import com.brandon3055.draconicevolution.client.DEShaders;
import com.brandon3055.draconicevolution.client.shader.ToolShader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.covers1624.quack.util.LazyValue;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Supplier;

import static com.brandon3055.draconicevolution.DraconicEvolution.MODID;

/**
 * Created by brandon3055 on 22/5/20.
 */
public abstract class ToolRenderBase implements DEGeometryItemRenderer {

    protected final TechLevel techLevel;
    protected final String tool;

    public ToolRenderBase(TechLevel techLevel, String tool) {
        this.techLevel = techLevel;
        this.tool = tool;
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
        Matrix4 mat = new Matrix4(mStack);
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = packedLight;
        ccrs.overlay = packedOverlay;

        DEShaders.TOOL_BASE_SHADER.getUv1OverrideUniform().glUniform2i(packedOverlay & 0xFFFF, (packedOverlay >> 16) & 0xFFFF);
        DEShaders.TOOL_BASE_SHADER.getUv2OverrideUniform().glUniform2i(packedLight & 0xFFFF, (packedLight >> 16) & 0xFFFF);

        renderTool(ccrs, stack, transformType, mat, getter, transformType == ItemDisplayContext.GUI);
    }

    public abstract void renderTool(CCRenderState ccrs, ItemStack stack, ItemDisplayContext transform, Matrix4 mat, MultiBufferSource buffers, boolean gui);

    public void transform(Matrix4 mat, double x, double y, double z, double scale) {
        mat.translate(x, y, z);
        mat.rotate(MathHelper.torad * 90, Vector3.Y_NEG);
        mat.rotate(MathHelper.torad * 45, Vector3.X_POS);
        mat.scale(scale);
    }

    @Override
    @Nullable
    public PerspectiveModelState getModelState() {
        return TransformUtils.DEFAULT_TOOL;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    protected static float[][] baseColours = {
            { 0.0F, 0.5F, 0.8F, 1F },
            { 0.55F, 0.0F, 0.65F, 1F },
            { 0.8F, 0.5F, 0.1F, 1F },
            { 0.75F, 0.05F, 0.05F, 0.2F }
    };

    public static void glUniformBaseColor(BCShader<?> shader, TechLevel techLevel, float pulse) {
        if (!(shader instanceof ToolShader toolShader) || !toolShader.hasBaseColorUniform()) return;
        float[] baseColour = baseColours[techLevel.index];
        float r = baseColour[0];
        float g = baseColour[1];
        float b = baseColour[2];
        float a = baseColour[3];
        switch (techLevel) {
            case DRACONIUM, WYVERN, DRACONIC -> a *= 1F + pulse;
            case CHAOTIC -> {
                r += pulse * 0.2F;
                g += pulse * 0.2F;
                b += pulse * 0.2F;
            }
        }
        toolShader.getBaseColorUniform().glUniform4f(r, g, b, a);
    }

    //These parts will always be rendered solid using the model texture.
    protected ToolPart basePart(CCModel model) {
        String levelName = techLevel.name().toLowerCase(Locale.ROOT);
        Identifier texture = Identifier.fromNamespaceAndPath(MODID, "textures/item/equipment/" + levelName + "_" + tool + ".png");
        RenderType baseType = toolType("base", DEShaders.TOOL_BASE_SHADER, texture, true, true, false);
        RenderType guiType = toolType("base_gui", DEShaders.TOOL_BASE_SHADER, texture, true, false, false);

        return new BaseToolPart(model, baseType, guiType, DEShaders.TOOL_BASE_SHADER);
    }

    //These are parts like the head that are made out of the base material and will have the chaos shader applied if tech level is chaos.
    protected ToolPart materialPart(CCModel model) {
        if (techLevel != TechLevel.CHAOTIC) return basePart(model);

        RenderType chaoticType = toolType("tool_chaos", BCShaders.CHAOS_ENTITY_SHADER,
                Identifier.fromNamespaceAndPath(MODID, "textures/item/equipment/chaos_shader.png"), false, false, true);
        return new ChaoticToolPart(model, chaoticType, BCShaders.CHAOS_ENTITY_SHADER);
    }

    protected ToolPart gemPart(CCModel model) {
        String levelName = techLevel.name().toLowerCase(Locale.ROOT);
        RenderType gemType = toolType("tool_gem", DEShaders.TOOL_GEM_SHADER,
                Identifier.fromNamespaceAndPath(MODID, "textures/item/equipment/shader_fallback_" + levelName + ".png"), true, false, false);

        return new SimpleToolPart(model, gemType, DEShaders.TOOL_GEM_SHADER);
    }

    //These are the shaded model "inlays" on the handles of most tools
    protected ToolPart tracePart(CCModel model) {
        String levelName = techLevel.name().toLowerCase(Locale.ROOT);
        RenderType gemType = toolType("tool_trace", DEShaders.TOOL_TRACE_SHADER,
                Identifier.fromNamespaceAndPath(MODID, "textures/item/equipment/shader_fallback_" + levelName + ".png"), true, false, false);

        return new SimpleToolPart(model, gemType, DEShaders.TOOL_TRACE_SHADER);
    }

    protected ToolPart bladePart(CCModel model) {
        String levelName = techLevel.name().toLowerCase(Locale.ROOT);
        RenderType gemType = toolType("tool_blade", DEShaders.TOOL_BLADE_SHADER,
                Identifier.fromNamespaceAndPath(MODID, "textures/item/equipment/shader_fallback_" + levelName + ".png"), true, false, false);

        return new SimpleToolPart(model, gemType, DEShaders.TOOL_BLADE_SHADER);
    }

    public static RenderType toolType(String name, BCShader<?> shader, Identifier texture,
            boolean cull, boolean outline, boolean linear) {
        var setup = RenderSetup.builder(shader.pipeline(VertexFormat.Mode.TRIANGLES, false, cull))
                .withTexture("Sampler0", texture, () -> com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getSampler(
                        com.mojang.blaze3d.textures.AddressMode.REPEAT, com.mojang.blaze3d.textures.AddressMode.REPEAT, linear ? com.mojang.blaze3d.textures.FilterMode.LINEAR : com.mojang.blaze3d.textures.FilterMode.NEAREST, linear ? com.mojang.blaze3d.textures.FilterMode.LINEAR : com.mojang.blaze3d.textures.FilterMode.NEAREST, false))
                .useLightmap().useOverlay().bufferSize(256);
        if (outline) setup.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        return RenderType.create(MODID + ":" + name, setup.createRenderSetup());
    }

    protected abstract static class ToolPart {

        protected final BCShader<?> shader;

        protected ToolPart(BCShader<?> shader) {
            this.shader = shader;
        }

        public final void render(ItemDisplayContext transformType, MultiBufferSource buffers, Matrix4 mat) {
            render(transformType, buffers, mat, 1F);
        }

        public abstract void render(ItemDisplayContext transformType, MultiBufferSource buffers, Matrix4 mat, float pulse);
    }

    protected static class BaseToolPart extends ToolPart {

        private final Supplier<CachedModelMesh> vboType;
        private final Supplier<CachedModelMesh> guiVboType;

        public BaseToolPart(CCModel model, RenderType type, RenderType guiType, BCShader<?> shader) {
            super(shader);
            vboType = new LazyValue<>(() -> new CachedModelMesh(type, (format, builder) -> {
                CCRenderState ccrs = CCRenderState.instance();
                ccrs.reset();
                ccrs.bind(builder, format);
                model.render(ccrs);
            }));
            guiVboType = new LazyValue<>(() -> new CachedModelMesh(guiType, (format, builder) -> {
                CCRenderState ccrs = CCRenderState.instance();
                ccrs.reset();
                ccrs.bind(builder, format);
                model.render(ccrs);
            }));
        }

        @Override
        public void render(ItemDisplayContext transformType, MultiBufferSource buffers, Matrix4 mat, float pulse) {
            if (transformType == ItemDisplayContext.GUI) {
                shader.getModelMatUniform().glUniformMatrix4f(mat);
                guiVboType.get().draw();
            } else {
                shader.getModelMatUniform().glUniformMatrix4f(mat);
                vboType.get().draw();
            }
        }
    }

    protected class SimpleToolPart extends ToolPart {

        protected final Supplier<CachedModelMesh> vboType;

        public SimpleToolPart(CCModel model, RenderType baseType, BCShader<?> shader) {
            super(shader);
            vboType = new LazyValue<>(() -> new CachedModelMesh(baseType, (format, builder) -> {
                CCRenderState ccrs = CCRenderState.instance();
                ccrs.reset();
                ccrs.bind(builder, format);
                model.render(ccrs);
            }));
        }

        @Override
        public void render(ItemDisplayContext transformType, MultiBufferSource buffers, Matrix4 mat, float pulse) {
            {
                glUniformBaseColor(shader, techLevel, pulse);
                shader.getModelMatUniform().glUniformMatrix4f(mat);
            }
            vboType.get().draw();
        }

    }

    protected class ChaoticToolPart extends SimpleToolPart {

        private final ChaosEntityShader shader;

        public ChaoticToolPart(CCModel model, RenderType baseType, ChaosEntityShader shader) {
            super(model, baseType, shader);
            this.shader = shader;
        }

        @Override
        public void render(ItemDisplayContext transformType, MultiBufferSource buffers, Matrix4 mat, float pulse) {
            {
                shader.getDisableLightUniform().glUniform1b(true);
                shader.getDisableOverlayUniform().glUniform1b(true);
                shader.getAlphaUniform().glUniform1f(0.7F);
                shader.getModelMatUniform().glUniformMatrix4f(mat);
            }
            vboType.get().draw();
        }
    }
}
