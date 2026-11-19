package com.brandon3055.brandonscore.lib;

import net.minecraft.core.BlockPos;

/**
 * Created by brandon3055 on 23/10/2016.
 */
public interface IChangeListener {

    /** A signal update may have no source position in the native 26.1 API. */
    void onNeighborChange(@org.jetbrains.annotations.Nullable BlockPos neighbor);
}
