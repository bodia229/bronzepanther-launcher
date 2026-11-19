package com.brandon3055.draconicevolution.client.render.item;

import java.util.Map;
import java.util.function.Supplier;

/** Authored renderer allow-list; resource packs cannot instantiate arbitrary classes. */
public final class NativeItemRenderers {
    private static final Map<String, Supplier<DEItemRenderer>> FACTORIES = Map.ofEntries(
            Map.entry(RenderItemChaosShard.CHAOS_SHARD.class.getName(), RenderItemChaosShard.CHAOS_SHARD::new),
            Map.entry(RenderItemChaosShard.CHAOS_FRAG_LARGE.class.getName(), RenderItemChaosShard.CHAOS_FRAG_LARGE::new),
            Map.entry(RenderItemChaosShard.CHAOS_FRAG_MEDIUM.class.getName(), RenderItemChaosShard.CHAOS_FRAG_MEDIUM::new),
            Map.entry(RenderItemChaosShard.CHAOS_FRAG_SMALL.class.getName(), RenderItemChaosShard.CHAOS_FRAG_SMALL::new),
            Map.entry(RenderItemMobSoul.class.getName(), RenderItemMobSoul::new),
            Map.entry(RenderItemEnergyCrystal.ITEM_BASIC_IO_CRYSTAL.class.getName(), RenderItemEnergyCrystal.ITEM_BASIC_IO_CRYSTAL::new),
            Map.entry(RenderItemEnergyCrystal.ITEM_WYVERN_IO_CRYSTAL.class.getName(), RenderItemEnergyCrystal.ITEM_WYVERN_IO_CRYSTAL::new),
            Map.entry(RenderItemEnergyCrystal.ITEM_DRACONIC_IO_CRYSTAL.class.getName(), RenderItemEnergyCrystal.ITEM_DRACONIC_IO_CRYSTAL::new),
            Map.entry(RenderItemEnergyCrystal.ITEM_BASIC_RELAY_CRYSTAL.class.getName(), RenderItemEnergyCrystal.ITEM_BASIC_RELAY_CRYSTAL::new),
            Map.entry(RenderItemEnergyCrystal.ITEM_WYVERN_RELAY_CRYSTAL.class.getName(), RenderItemEnergyCrystal.ITEM_WYVERN_RELAY_CRYSTAL::new),
            Map.entry(RenderItemEnergyCrystal.ITEM_DRACONIC_RELAY_CRYSTAL.class.getName(), RenderItemEnergyCrystal.ITEM_DRACONIC_RELAY_CRYSTAL::new),
            Map.entry(RenderItemEnergyCrystal.ITEM_BASIC_WIRELESS_CRYSTAL.class.getName(), RenderItemEnergyCrystal.ITEM_BASIC_WIRELESS_CRYSTAL::new),
            Map.entry(RenderItemEnergyCrystal.ITEM_WYVERN_WIRELESS_CRYSTAL.class.getName(), RenderItemEnergyCrystal.ITEM_WYVERN_WIRELESS_CRYSTAL::new),
            Map.entry(RenderItemEnergyCrystal.ITEM_DRACONIC_WIRELESS_CRYSTAL.class.getName(), RenderItemEnergyCrystal.ITEM_DRACONIC_WIRELESS_CRYSTAL::new),
            Map.entry(RenderItemDraconiumChest.class.getName(), RenderItemDraconiumChest::new),
            Map.entry(RenderItemReactorComponent.class.getName(), RenderItemReactorComponent::new),
            Map.entry(RenderModularPickaxe.PICKAXE_WYVERN.class.getName(), RenderModularPickaxe.PICKAXE_WYVERN::new),
            Map.entry(RenderModularPickaxe.PICKAXE_DRACONIC.class.getName(), RenderModularPickaxe.PICKAXE_DRACONIC::new),
            Map.entry(RenderModularPickaxe.PICKAXE_CHAOTIC.class.getName(), RenderModularPickaxe.PICKAXE_CHAOTIC::new),
            Map.entry(RenderModularAxe.AXE_WYVERN.class.getName(), RenderModularAxe.AXE_WYVERN::new),
            Map.entry(RenderModularAxe.AXE_DRACONIC.class.getName(), RenderModularAxe.AXE_DRACONIC::new),
            Map.entry(RenderModularAxe.AXE_CHAOTIC.class.getName(), RenderModularAxe.AXE_CHAOTIC::new),
            Map.entry(RenderModularShovel.SHOVEL_WYVERN.class.getName(), RenderModularShovel.SHOVEL_WYVERN::new),
            Map.entry(RenderModularShovel.SHOVEL_DRACONIC.class.getName(), RenderModularShovel.SHOVEL_DRACONIC::new),
            Map.entry(RenderModularShovel.SHOVEL_CHAOTIC.class.getName(), RenderModularShovel.SHOVEL_CHAOTIC::new),
            Map.entry(RenderModularSword.SWORD_WYVERN.class.getName(), RenderModularSword.SWORD_WYVERN::new),
            Map.entry(RenderModularSword.SWORD_DRACONIC.class.getName(), RenderModularSword.SWORD_DRACONIC::new),
            Map.entry(RenderModularSword.SWORD_CHAOTIC.class.getName(), RenderModularSword.SWORD_CHAOTIC::new),
            Map.entry(RenderModularBow.BOW_WYVERN.class.getName(), RenderModularBow.BOW_WYVERN::new),
            Map.entry(RenderModularBow.BOW_DRACONIC.class.getName(), RenderModularBow.BOW_DRACONIC::new),
            Map.entry(RenderModularBow.BOW_CHAOTIC.class.getName(), RenderModularBow.BOW_CHAOTIC::new),
            Map.entry(RenderModularStaff.STAFF_DRACONIC.class.getName(), RenderModularStaff.STAFF_DRACONIC::new),
            Map.entry(RenderModularStaff.STAFF_CHAOTIC.class.getName(), RenderModularStaff.STAFF_CHAOTIC::new),
            Map.entry(RenderModularHoe.HOE_WYVERN.class.getName(), RenderModularHoe.HOE_WYVERN::new),
            Map.entry(RenderModularHoe.HOE_DRACONIC.class.getName(), RenderModularHoe.HOE_DRACONIC::new),
            Map.entry(RenderModularHoe.HOE_CHAOTIC.class.getName(), RenderModularHoe.HOE_CHAOTIC::new),
            Map.entry(RenderModularChestpiece.CHESTPIECE_WYVERN.class.getName(), RenderModularChestpiece.CHESTPIECE_WYVERN::new),
            Map.entry(RenderModularChestpiece.CHESTPIECE_DRACONIC.class.getName(), RenderModularChestpiece.CHESTPIECE_DRACONIC::new),
            Map.entry(RenderModularChestpiece.CHESTPIECE_CHAOTIC.class.getName(), RenderModularChestpiece.CHESTPIECE_CHAOTIC::new));
    private NativeItemRenderers() {}
    public static void init() { DEItemModel.registerRenderers(FACTORIES); }
}
