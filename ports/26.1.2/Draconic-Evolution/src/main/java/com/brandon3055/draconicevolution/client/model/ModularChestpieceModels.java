package com.brandon3055.draconicevolution.client.model;

import com.brandon3055.brandonscore.client.render.EquippedItemModel;
import com.brandon3055.draconicevolution.items.equipment.ModularChestpiece;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import java.util.IdentityHashMap;
import java.util.Map;

/** Owns client model instances without exposing their class hierarchy to item registration. */
public final class ModularChestpieceModels {
    private record Models(ModularChestpieceModel<?> normal,ModularChestpieceModel<?> overArmor) {}
    private static final Map<ModularChestpiece,Models> CACHE=new IdentityHashMap<>();
    private ModularChestpieceModels() {}
    public static EquippedItemModel get(ModularChestpiece item,LivingEntity entity,EquipmentSlot slot,HumanoidModel<?> parent) {
        ItemStack chest=entity.getItemBySlot(EquipmentSlot.CHEST);
        boolean overArmor=slot==null&&!chest.isEmpty()&&chest.is(net.minecraft.tags.ItemTags.CHEST_ARMOR);
        Models models=CACHE.computeIfAbsent(item,key->new Models(new ModularChestpieceModel<>(key.getTechLevel(),false),new ModularChestpieceModel<>(key.getTechLevel(),true)));
        var active=overArmor?models.overArmor():models.normal();
        ClientHooks.copyModelProperties(parent,active);
        return active;
    }
}
