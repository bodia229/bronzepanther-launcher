package com.brandon3055.draconicevolution.lib;

/** Visits the complete fight area without requesting that missing chunks be loaded. */
public final class GuardianArenaChunks {
    private GuardianArenaChunks() {}

    @FunctionalInterface
    public interface LoadedChunk {
        boolean test(int x, int z);
    }

    public static boolean allLoaded(int centerX, int centerZ, int radius, LoadedChunk loaded) {
        if (radius < 0) throw new IllegalArgumentException("Negative arena radius");
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (!loaded.test(centerX + x, centerZ + z)) return false;
            }
        }
        return true;
    }
}
