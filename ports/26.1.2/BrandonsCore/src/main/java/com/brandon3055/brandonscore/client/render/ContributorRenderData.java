package com.brandon3055.brandonscore.client.render;

import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/** Immutable extraction-time contributor settings consumed by deferred layers. */
public final class ContributorRenderData {
    public static final ContextKey<Boolean> HIDE_VANILLA_WINGS =
            new ContextKey<>(Identifier.fromNamespaceAndPath("brandonscore", "hide_vanilla_wings"));

    private ContributorRenderData() {}
}
