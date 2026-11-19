package com.brandon3055.draconicevolution.client.render.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.neoforged.api.distmarker.Dist;

/** Uses native wither render states, retaining the original vanilla texture, armor and scale. */

public class GuardianWitherRenderer extends WitherBossRenderer {
    public GuardianWitherRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
