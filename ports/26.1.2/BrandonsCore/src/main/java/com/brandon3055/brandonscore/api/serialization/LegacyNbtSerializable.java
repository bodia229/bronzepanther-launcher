package com.brandon3055.brandonscore.api.serialization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

/** Registry-aware contract retained for Brandon's Core's existing managed data objects. */
public interface LegacyNbtSerializable<T extends Tag> {
    T serializeNBT(HolderLookup.Provider registries);
    void deserializeNBT(HolderLookup.Provider registries,T tag);
}
