package codechicken.lib.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Registry-aware inventory persistence, preserving the legacy Slot/Item/Quantity keys. */
public final class InventorySerialization {
    private InventorySerialization() {}

    private record Slot(int index, ItemStack stack, int quantity) {
        static final Codec<Slot> CODEC=RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("Slot").forGetter(Slot::index),
            ItemStack.OPTIONAL_CODEC.fieldOf("Item").forGetter(Slot::stack),
            Codec.INT.optionalFieldOf("Quantity",-1).forGetter(Slot::quantity)
        ).apply(instance,Slot::new));
    }

    private static List<Slot> snapshot(ItemStack[] items) {
        List<Slot> slots=new ArrayList<>(items.length);
        for(int i=0;i<items.length;i++) {
            ItemStack stack=items[i];
            // Large inventories can exceed the vanilla codec's stack-count bounds.
            // Preserve the real quantity separately and never modify the live stack.
            slots.add(new Slot(i,stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(1),stack.getCount()));
        }
        return slots;
    }

    private static void restore(ItemStack[] items,Slot slot) {
        if(slot.index<0||slot.index>=items.length)return;
        ItemStack stack=slot.stack.copy();
        if(slot.quantity>=0)stack.setCount(slot.quantity);
        items[slot.index]=stack;
    }

    public static ListTag write(HolderLookup.Provider registries,ItemStack[] items) {
        return (ListTag)Slot.CODEC.listOf().encodeStart(RegistryOps.create(NbtOps.INSTANCE,registries),snapshot(items)).getOrThrow();
    }

    public static void read(HolderLookup.Provider registries,ItemStack[] items,ListTag tags) {
        Arrays.fill(items,ItemStack.EMPTY);
        var ops=RegistryOps.create(NbtOps.INSTANCE,registries);
        for(var tag:tags)Slot.CODEC.parse(ops,tag).result().ifPresent(slot->restore(items,slot));
    }

    public static void write(ValueOutput output,String key,ItemStack[] items) {
        output.store(key,Slot.CODEC.listOf(),snapshot(items));
    }

    public static void read(ValueInput input,String key,ItemStack[] items) {
        Arrays.fill(items,ItemStack.EMPTY);
        for(Slot slot:input.listOrEmpty(key,Slot.CODEC))restore(items,slot);
    }
}
