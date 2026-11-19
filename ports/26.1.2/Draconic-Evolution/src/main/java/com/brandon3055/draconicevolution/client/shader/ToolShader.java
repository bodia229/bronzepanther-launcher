package com.brandon3055.draconicevolution.client.shader;

import com.brandon3055.brandonscore.client.shader.BCUniform;
import com.brandon3055.brandonscore.client.shader.BCShader;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Created by covers1624 on 1/10/22.
 */
public final class ToolShader extends BCShader<ToolShader> {

    private BCUniform uv1OverrideUniform;
    private BCUniform uv2OverrideUniform;

    private BCUniform baseColorUniform;

    public ToolShader(String path, VertexFormat format) {
        this(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, path), format);
    }

    public ToolShader(Identifier location, VertexFormat format) {
        super(location, format);
        uv1OverrideUniform = hasUniform("UV1Override") ? uniform("UV1Override") : null;
        uv2OverrideUniform = hasUniform("UV2Override") ? uniform("UV2Override") : null;
        baseColorUniform = hasUniform("BaseColor") ? uniform("BaseColor") : null;
    }

    // @formatter:off
    public final BCUniform getUv1OverrideUniform() { return Objects.requireNonNull(uv1OverrideUniform, missingUniformMessage("UV1Override")); }
    public final boolean hasUv1OverrideUniform() { return uv1OverrideUniform != null; }
    public final BCUniform getUv2OverrideUniform() { return Objects.requireNonNull(uv2OverrideUniform, missingUniformMessage("UV2Override")); }
    public final boolean hasUv2OverrideUniform() { return uv2OverrideUniform != null; }
    public final BCUniform getBaseColorUniform() { return Objects.requireNonNull(baseColorUniform, missingUniformMessage("BaseColor")); }
    public final boolean hasBaseColorUniform() { return baseColorUniform != null; }
    // @formatter:on

}
