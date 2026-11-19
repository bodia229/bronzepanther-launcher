package com.brandon3055.brandonscore.inventory;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Native transactional automation around existing machine inventories and their side filters. */
public final class LegacyItemTransfer implements ResourceHandler<ItemResource> {
    private final IItemHandler view;
    private final Journal journal;
    private LegacyItemTransfer(IItemHandler view, Journal journal) {
        this.view = view;
        this.journal = journal;
    }
    /** Keep one group per owner so aliases share journals without a global inventory cache. */
    public static final class Group {
        private final IdentityHashMap<IItemHandler, LegacyItemTransfer> views = new IdentityHashMap<>();
        private final IdentityHashMap<IItemHandlerModifiable, Journal> journals = new IdentityHashMap<>();
        public LegacyItemTransfer wrap(IItemHandler view) {
            return views.computeIfAbsent(view, key -> {
                IItemHandler backing = key;
                var visited = new IdentityHashMap<IItemHandler, Boolean>();
                while (backing instanceof ItemHandlerView alias) {
                    if (visited.put(backing, Boolean.TRUE) != null) throw new IllegalArgumentException("Cyclic item handler view");
                    backing = alias.backingInventory();
                }
                if (!(backing instanceof IItemHandlerModifiable target) || target.getSlots() != key.getSlots())
                    throw new IllegalArgumentException("Transactional item handler needs a slot-preserving, modifiable backing inventory");
                return new LegacyItemTransfer(key, journals.computeIfAbsent(target, Journal::new));
            });
        }
    }
    private static final class Journal extends SnapshotJournal<List<ItemStack>> {
        private final IItemHandlerModifiable target;
        private Journal(IItemHandlerModifiable target) { this.target = target; }
        @Override protected List<ItemStack> createSnapshot() {
            var result = new ArrayList<ItemStack>(target.getSlots());
            for (int i = 0; i < target.getSlots(); i++) result.add(target.getStackInSlot(i).copy());
            return result;
        }
        @Override protected void revertToSnapshot(List<ItemStack> snapshot) {
            if (snapshot.size() != target.getSlots()) throw new IllegalStateException("Machine inventory resized during transfer");
            for (int i = 0; i < snapshot.size(); i++) target.setStackInSlot(i, snapshot.get(i));
        }
    }
    @Override public int size() { return view.getSlots(); }
    @Override public ItemResource getResource(int index) { return ItemResource.of(view.getStackInSlot(index)); }
    @Override public long getAmountAsLong(int index) { return view.getStackInSlot(index).getCount(); }
    @Override public long getCapacityAsLong(int index, ItemResource resource) {
        if (resource.isEmpty()) return view.getSlotLimit(index);
        return isValid(index, resource) ? Math.min(view.getSlotLimit(index), resource.getMaxStackSize()) : 0;
    }
    @Override public boolean isValid(int index, ItemResource resource) {
        return !resource.isEmpty() && view.isItemValid(index, resource.toStack());
    }
    @Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;
        int requested = Math.min(amount, getCapacityAsInt(index, resource));
        if (requested == 0) return 0;
        var offered = resource.toStack(requested);
        if (view.insertItem(index, offered, true).getCount() == requested) return 0;
        journal.updateSnapshots(transaction);
        return requested - view.insertItem(index, offered, false).getCount();
    }
    @Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || !resource.matches(view.getStackInSlot(index))) return 0;
        if (view.extractItem(index, amount, true).isEmpty()) return 0;
        journal.updateSnapshots(transaction);
        return view.extractItem(index, amount, false).getCount();
    }
}
