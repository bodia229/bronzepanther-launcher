package com.brandon3055.brandonscore.api.serialization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;

public final class NbtCompat {
    private NbtCompat(){}
    public static boolean contains(CompoundTag compound,String key,int type){
        var tag=compound.get(key);
        return type==99?tag instanceof NumericTag:tag!=null&&tag.getId()==type;
    }
}
