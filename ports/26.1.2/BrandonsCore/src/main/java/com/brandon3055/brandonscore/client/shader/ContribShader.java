package com.brandon3055.brandonscore.client.shader;


import com.brandon3055.brandonscore.BrandonsCore;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Created by brandon3055 on 20/11/2022
 */
public class ContribShader extends BCShader<ContribShader> {



    public ContribShader(String path, VertexFormat format) {
        super(Identifier.fromNamespaceAndPath(BrandonsCore.MODID, path), format);
    }

    public ContribShader(Identifier location, VertexFormat format) {
        super(location, format);
    }

    // @formatter:off
    public final BCUniform getUv1OverrideUniform() { return uniform("UV1Override"); }
    public final boolean hasUv1OverrideUniform() { return hasUniform("UV1Override"); }
    public final BCUniform getUv2OverrideUniform() { return uniform("UV2Override"); }
    public final boolean hasUv2OverrideUniform() { return hasUniform("UV2Override"); }
    public final BCUniform getBaseColorUniform() { return uniform("BaseColor"); }
    public final boolean hasBaseColorUniform() { return hasUniform("BaseColor"); }
    public final BCUniform getTransitionUniform() { return uniform("Transition"); }
    public final boolean hasTransitionUniform() { return hasUniform("Transition"); }
    public final BCUniform getHueUniform() { return uniform("Hue"); }
    public final boolean hasHueUniform() { return hasUniform("Hue"); }
    // @formatter:on

}
