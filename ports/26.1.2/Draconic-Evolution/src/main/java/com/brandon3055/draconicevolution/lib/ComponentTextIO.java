package com.brandon3055.draconicevolution.lib;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;

/** Preserve the JSON text fields already stored in item profile files. */
public final class ComponentTextIO {
    private ComponentTextIO() {}
    public static String write(Component component, HolderLookup.Provider registries) {
        return ComponentSerialization.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), component).getOrThrow().toString();
    }
    public static Component read(String json, HolderLookup.Provider registries) {
        try {
            return ComponentSerialization.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), JsonParser.parseString(json))
                    .result().orElseGet(() -> Component.literal(json));
        } catch (JsonParseException exception) {
            return Component.literal(json);
        }
    }
}
