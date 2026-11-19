package com.brandon3055.draconicevolution.client.render.item;

import com.brandon3055.draconicevolution.client.render.item.PerspectiveModelState;
import com.brandon3055.draconicevolution.client.render.item.DEItemRenderer;
import codechicken.lib.texture.TextureUtils;
import com.brandon3055.draconicevolution.client.render.item.TransformUtils;
import com.brandon3055.brandonscore.blocks.BlockBCore;
import com.brandon3055.draconicevolution.client.render.tile.DraconiumChestTileRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Created by brandon3055 on 18/04/2017.
 */
public class RenderItemDraconiumChest implements DEGeometryItemRenderer {
    public DraconiumChestTileRenderer renderer = new DraconiumChestTileRenderer(null);

    public RenderItemDraconiumChest() {}

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return net.minecraft.client.Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS)
                .getSprite(net.minecraft.resources.Identifier.fromNamespaceAndPath("draconicevolution", "block/draconium_block"));
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
        int colour = 0x640096;
        if (stack.has(BlockBCore.BC_TILE_DATA_TAG)) {
            CompoundTag tag = stack.get(BlockBCore.BC_TILE_DATA_TAG).copyTag();
            if (tag.contains("bc_managed_data")) {
                colour = tag.getCompoundOrEmpty("bc_managed_data").getIntOr("colour",0);
            }
        }
        renderer.renderChest(mStack, getter, 180, 0, packedLight, packedOverlay, colour);
    }

    @Override
    public @Nullable PerspectiveModelState getModelState() {
        return TransformUtils.DEFAULT_BLOCK;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }
}
