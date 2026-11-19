package com.brandon3055.draconicevolution.client.shader;

import com.brandon3055.brandonscore.client.shader.BCUniform;
import com.brandon3055.brandonscore.client.shader.BCShader;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Created by brandon3055 on 13/11/2022
 */
public class ShieldShader extends BCShader<ShieldShader> {

    private BCUniform activationUniform;
    private BCUniform baseColourUniform;

    public ShieldShader(String path, VertexFormat format) {
        this(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, path), format);
    }

    public ShieldShader(Identifier location, VertexFormat format) {
        super(location, format);
        activationUniform = hasUniform("Activation") ? uniform("Activation") : null;
        baseColourUniform = hasUniform("BaseColor") ? uniform("BaseColor") : null;
    }

    public BCUniform getActivationUniform() {
        return Objects.requireNonNull(activationUniform, missingUniformMessage("Activation"));
    }

    public BCUniform getBaseColourUniform() {
        return Objects.requireNonNull(baseColourUniform, missingUniformMessage("BaseColor"));
    }

}
