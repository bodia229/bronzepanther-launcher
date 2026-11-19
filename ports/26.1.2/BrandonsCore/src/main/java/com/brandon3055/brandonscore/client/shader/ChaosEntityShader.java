package com.brandon3055.brandonscore.client.shader;


import com.brandon3055.brandonscore.BrandonsCore;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 1/10/22.
 */
public final class ChaosEntityShader extends BCShader<ChaosEntityShader> {


    public ChaosEntityShader(String path, VertexFormat format) {
        super(Identifier.fromNamespaceAndPath(BrandonsCore.MODID, path), format);
    }

    public ChaosEntityShader(Identifier location, VertexFormat format) {
        super(location, format);
    }

    // @formatter:off
    public final BCUniform getYawUniform() { return uniform("Yaw"); }
    public final boolean hasYawUniform() { return hasUniform("Yaw"); }
    public final BCUniform getPitchUniform() { return uniform("Pitch"); }
    public final boolean hasPitchUniform() { return hasUniform("Pitch"); }
    public final boolean hasAlphaUniform() { return hasUniform("Alpha"); }
    public final boolean hasSimpleLightUniform() { return hasUniform("SimpleLight"); }
    public final boolean hasDisableLightUniform() { return hasUniform("DisableLight"); }
    public final boolean hasDisableOverlayUniform() { return hasUniform("DisableOverlay"); }
    // @formatter:on

}
