package com.brandon3055.draconicevolution.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Assign each item's native registry key before construction, without mutating shared tech templates. */
public final class DEItemProperties {
    private DEItemProperties(){}
    @SuppressWarnings("unchecked")
    public static <P extends Item.Properties> P withId(Identifier id,P properties){
        P owned=properties instanceof TechProperties tech?(P)tech.copy():properties;
        owned.setId(ResourceKey.create(Registries.ITEM,id));return owned;
    }
}
