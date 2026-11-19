package com.brandon3055.brandonscore.api.serialization;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Keeps the existing managed-data schema while using registry-aware Value I/O. */
public final class LegacyValueIO {
    private LegacyValueIO(){}
    private static final MapCodec<Function<HolderLookup.Provider,CompoundTag>> CODEC=new MapCodec<>() {
        @Override public <T> Stream<T> keys(DynamicOps<T> ops){return Stream.empty();}
        @Override public <T> DataResult<Function<HolderLookup.Provider,CompoundTag>> decode(DynamicOps<T> ops,MapLike<T> input){
            return CompoundTag.CODEC.parse(ops,ops.createMap(input.entries())).map(tag->registries->tag);
        }
        @Override public <T> RecordBuilder<T> encode(Function<HolderLookup.Provider,CompoundTag> writer,DynamicOps<T> ops,RecordBuilder<T> prefix){
            if(!(ops instanceof RegistryOps<?> registryOps)||!(registryOps.lookupProvider instanceof RegistryOps.HolderLookupAdapter adapter))
                return prefix.withErrorsFrom(DataResult.error(()->"Managed block data requires a registry lookup provider"));
            var data=CompoundTag.CODEC.encodeStart(ops,writer.apply(adapter.lookupProvider)).flatMap(ops::getMap);
            data.result().ifPresent(map->map.entries().forEach(entry->prefix.add(entry.getFirst(),entry.getSecond())));
            return prefix.withErrorsFrom(data);
        }
    };
    public static void write(ValueOutput output,BiConsumer<HolderLookup.Provider,CompoundTag> writer){
        output.store(CODEC,registries->{CompoundTag tag=new CompoundTag();writer.accept(registries,tag);return tag;});
    }
    public static CompoundTag read(ValueInput input){
        return input.read(CODEC).map(reader->reader.apply(input.lookup())).orElseGet(CompoundTag::new);
    }
}
