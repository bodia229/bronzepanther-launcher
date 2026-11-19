package com.brandon3055.brandonscore.inventory;

import net.neoforged.neoforge.items.IItemHandler;

/** A slot-preserving automation view; rollback bypasses filters on its backing inventory. */
public interface ItemHandlerView {
    IItemHandler backingInventory();
}
