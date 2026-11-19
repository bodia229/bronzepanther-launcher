package com.brandon3055.draconicevolution.lib;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

/** Imports pre-namespaced saved data once, leaving the original file intact. */
public final class LegacySavedDataMigration {
    private LegacySavedDataMigration() {}

    public static <T extends SavedData> T getOrMigrate(SavedDataStorage storage, SavedDataType<T> type,
            Path legacyFile, HolderLookup.Provider registries, ServerLevel level) {
        T existing = storage.get(type);
        if (existing != null) return existing;
        Path currentFile = type.id().withSuffix(".dat").resolveAgainst(legacyFile.getParent());
        if (Files.exists(currentFile)) throw new IllegalStateException("Cannot read existing saved data " + currentFile);
        if (!Files.exists(legacyFile)) return storage.computeIfAbsent(type);
        try {
            var tag = storage.readTagFromDisk(legacyFile, null, SharedConstants.getCurrentVersion().dataVersion().version());
            var payload = tag.get("data");
            if (payload == null) throw new IllegalArgumentException("Missing saved data payload");
            T imported = type.codecFactory().create(level).parse(registries.createSerializationContext(NbtOps.INSTANCE), payload).getOrThrow();
            storage.set(type, imported);
            return imported;
        } catch (Exception error) {
            // Do not replace unreadable legacy links with an empty, successfully saved map.
            throw new IllegalStateException("Cannot migrate saved data from " + legacyFile, error);
        }
    }
}
