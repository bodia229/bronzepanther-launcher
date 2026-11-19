package com.brandon3055.brandonscore.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import java.util.List;
import java.util.Map;

/**
 * Created by brandon3055 on 19/11/2022.
 * Intentionally hides the vanilla armor mesh when an EquippedItemModel renders it.
 * Model.renderToBuffer is final in 26.1, so use an empty root instead of overriding
 * rendering and animation methods with no-ops as the previous implementation did.
 */
public final class DummyHumanoidModel<T> extends Model<T> {
    public static final DummyHumanoidModel<?> INSTANCE = new DummyHumanoidModel<>();
    public static final IClientItemExtensions DUMMY_ITEM_RENDER_PROPS = new IClientItemExtensions() {
        @Override
        public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layer, Model original) {
            return INSTANCE;
        }
    };
    public DummyHumanoidModel() {
        super(new ModelPart(List.of(), Map.of()), RenderTypes::entityCutout);
    }
}
