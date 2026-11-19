package com.brandon3055.draconicevolution.datagen;

import com.mojang.math.Quadrant;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;

/** Normalizes legacy authored quarter-turns without changing their orientation. */
public final class NativeBlockStateModels {
    private NativeBlockStateModels() {}

    public static MultiVariant variant(Identifier model, int x, int y) {
        return BlockModelGenerators.variant(new Variant(model).withXRot(quadrant(x)).withYRot(quadrant(y)));
    }

    public static Quadrant quadrant(int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 0 -> Quadrant.R0;
            case 90 -> Quadrant.R90;
            case 180 -> Quadrant.R180;
            case 270 -> Quadrant.R270;
            default -> throw new IllegalArgumentException("Non-quadrant rotation " + degrees);
        };
    }
}
