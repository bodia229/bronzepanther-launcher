package com.brandon3055.draconicevolution.lib;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Keep mob soul NBT compatible while adapting to registry-aware native entity value I/O. */
public final class EntityDataIO {
    private EntityDataIO() {}
    public static CompoundTag save(Entity entity) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }
    public static void load(Entity entity, CompoundTag data) {
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), data));
    }
}
