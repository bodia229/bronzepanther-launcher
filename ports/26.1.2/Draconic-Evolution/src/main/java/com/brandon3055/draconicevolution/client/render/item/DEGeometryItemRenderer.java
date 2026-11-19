package com.brandon3055.draconicevolution.client.render.item;

import com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.*;

/** Converts authored vertex emission to native deferred geometry submissions. */
public interface DEGeometryItemRenderer extends DEItemRenderer {
    void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poses, MultiBufferSource buffers, int light, int overlay);
    default void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poses, MultiBufferSource buffers, int light, int overlay, Object state) {
        renderItem(stack,context,poses,buffers,light,overlay);
    }
    @Override default void submitItem(ItemStack stack, ItemDisplayContext context, Object state, PoseStack poses,
            SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
        ExtractedEquipmentGeometry.capture(buffers -> renderItem(stack,context,new PoseStack(),buffers,light,overlay,state)).submit(poses,collector);
    }
}
