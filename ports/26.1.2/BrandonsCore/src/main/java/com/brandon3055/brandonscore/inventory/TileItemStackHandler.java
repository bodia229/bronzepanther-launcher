package com.brandon3055.brandonscore.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Created by brandon3055 on 13/9/19.
 */
public class TileItemStackHandler extends ItemStackHandler implements com.brandon3055.brandonscore.api.serialization.LegacyNbtSerializable<CompoundTag> {

    private final BlockEntity tile;
    // Delay machine callbacks until the root transfer commits. Rollback must not cancel an active craft.
    private final net.neoforged.neoforge.transfer.transaction.SnapshotJournal<java.util.List<ItemStack>> transferJournal =
            new net.neoforged.neoforge.transfer.transaction.SnapshotJournal<>() {
        @Override protected java.util.List<ItemStack> createSnapshot() {
            return stacks.stream().map(ItemStack::copy).toList();
        }
        @Override protected void revertToSnapshot(java.util.List<ItemStack> before) {
            if (before.size() != stacks.size()) throw new IllegalStateException("Inventory resized during transfer");
            for (int i = 0; i < before.size(); i++) stacks.set(i, before.get(i));
        }
        @Override protected void onRootCommit(java.util.List<ItemStack> before) {
            for (int i = 0; i < before.size(); i++) {
                if (!ItemStack.matches(before.get(i), stacks.get(i))) {
                    prevStack = before.get(i);
                    onContentsChanged(i);
                }
            }
        }
    };
    private void prepareTransferMutation() {
        if (net.neoforged.neoforge.transfer.transaction.Transaction.getLifecycle()
                == net.neoforged.neoforge.transfer.transaction.Transaction.Lifecycle.OPEN) {
            transferJournal.updateSnapshots(net.neoforged.neoforge.transfer.transaction.Transaction.getCurrentOpenedTransaction());
        }
    }
    @Override public void setStackInSlot(int slot, ItemStack stack) {
        prepareTransferMutation();
        super.setStackInSlot(slot, stack);
    }

    private BiPredicate<Integer, ItemStack> stackValidator = null;
    private Runnable loadListener = null;
    private Consumer<Integer> contentsChangeListener = null;
    private Map<Integer, Predicate<ItemStack>> slotValidators = new HashMap<>();
    private ItemStack prevStack = ItemStack.EMPTY;
    private Supplier<Integer> perSlotLimit = null;
    private int slotLimit = 64;

    public TileItemStackHandler(BlockEntity tile) {
        this.tile = tile;
    }

    public TileItemStackHandler(BlockEntity tile, int size) {
        super(size);
        this.tile = tile;
    }

    public TileItemStackHandler(BlockEntity tile, NonNullList<ItemStack> stacks) {
        super(stacks);
        this.tile = tile;
    }

    public TileItemStackHandler setStackValidator(BiPredicate<Integer, ItemStack> stackValidator) {
        this.stackValidator = stackValidator;
        return this;
    }

    public TileItemStackHandler setStackValidator(Predicate<ItemStack> stackValidator) {
        this.stackValidator = (integer, stack) -> stackValidator.test(stack);
        return this;
    }

    public TileItemStackHandler setSlotValidator(int slot, Predicate<ItemStack> validator) {
        slotValidators.put(slot, validator);
        return this;
    }

    public TileItemStackHandler setLoadListener(Runnable loadListener) {
        this.loadListener = loadListener;
        return this;
    }

    public TileItemStackHandler setContentsChangeListener(Consumer<Integer> contentsChangeListener) {
        this.contentsChangeListener = contentsChangeListener;
        return this;
    }

    public TileItemStackHandler setPerSlotLimit(Supplier<Integer> perSlotLimit) {
        this.perSlotLimit = perSlotLimit;
        return this;
    }

    public TileItemStackHandler setSlotLimit(int slotLimit) {
        this.slotLimit = slotLimit;
        return this;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        prevStack = getStackInSlot(slot);
        if (!isItemValid(slot, stack)) {
            return stack;
        }
        if (!simulate) prepareTransferMutation();
        return super.insertItem(slot, stack, simulate);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        prevStack = getStackInSlot(slot);
        if (!simulate) prepareTransferMutation();
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        if (slotValidators.containsKey(slot)) {
            return slotValidators.get(slot).test(stack);
        }
        return stackValidator == null || stackValidator.test(slot, stack);
    }

    //I dont want this handler's size to be controlled by NBT
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                itemTag.merge((CompoundTag)ItemStack.CODEC.encodeStart(net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, provider), stacks.get(i)).getOrThrow());
                nbtTagList.add(itemTag);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        stacks.clear();
        ListTag tagList = nbt.getListOrEmpty("Items");
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTags = tagList.getCompoundOrEmpty(i);
            int slot = itemTags.getIntOr("Slot",0);

            if (slot >= 0 && slot < stacks.size()) {
                stacks.set(slot, ItemStack.OPTIONAL_CODEC.parse(net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, provider), itemTags).getOrThrow());
            }
        }
        onLoad();
    }

    @Override
    public void serialize(net.minecraft.world.level.storage.ValueOutput output) {
        com.brandon3055.brandonscore.api.serialization.LegacyValueIO.write(output, (provider, tag) -> tag.merge(serializeNBT(provider)));
    }

    @Override
    public void deserialize(net.minecraft.world.level.storage.ValueInput input) {
        deserializeNBT(input.lookup(), com.brandon3055.brandonscore.api.serialization.LegacyValueIO.read(input));
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (transferJournal.isInTransaction() || net.neoforged.neoforge.transfer.transaction.Transaction.getLifecycle()
                == net.neoforged.neoforge.transfer.transaction.Transaction.Lifecycle.CLOSING) return;
        super.onContentsChanged(slot);
        if (contentsChangeListener != null) {
            contentsChangeListener.accept(slot);
        }
        tile.setChanged();
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        if (loadListener != null) {
            loadListener.run();
        }
    }

    public ItemStack getListenerPrevStack() {
        return prevStack;
    }

    @Override
    public int getSlotLimit(int slot) {
        return perSlotLimit == null ? slotLimit : perSlotLimit.get();
    }
}

