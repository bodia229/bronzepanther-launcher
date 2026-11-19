package com.brandon3055.brandonscore.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;
import java.util.function.Function;

/** Shared eligibility lookup for vanilla armor slots and optional equipment integrations. */
public final class CustomFlightHooks {
    private CustomFlightHooks(){}
    private static volatile Function<LivingEntity,ItemStack> accessoryLookup=entity->ItemStack.EMPTY;
    public static void setAccessoryLookup(Function<LivingEntity,ItemStack> lookup){accessoryLookup=Objects.requireNonNull(lookup);}
    public static boolean enabled(ItemStack stack,LivingEntity entity){return !stack.isEmpty()&&stack.getItem() instanceof ElytraEnabledItem item&&item.canElytraFlyBC(stack,entity);}
    public static ItemStack accessory(LivingEntity entity){ItemStack stack=accessoryLookup.apply(entity);return stack!=null&&enabled(stack,entity)?stack:ItemStack.EMPTY;}
    public static ItemStack active(LivingEntity entity){ItemStack chest=entity.getItemBySlot(EquipmentSlot.CHEST);return enabled(chest,entity)?chest:accessory(entity);}
}
