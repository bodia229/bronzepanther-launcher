package com.brandon3055.draconicevolution.lib;

import java.util.function.Function;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Native FE access to a long-valued item component, with live module-dependent limits. */
public final class ItemAccessLongEnergyHandler implements EnergyHandler {
    public record Limits(long capacity, long insert, long extract) {}
    private final ItemAccess access;
    private final Item item;
    private final DataComponentType<Long> component;
    private final Function<ItemResource, Limits> limits;
    public ItemAccessLongEnergyHandler(ItemAccess access, DataComponentType<Long> component,
            Function<ItemResource, Limits> limits) {
        this.access = access;
        this.item = access.getResource().getItem();
        this.component = component;
        this.limits = limits;
    }
    private boolean valid(ItemResource resource) { return !resource.isEmpty() && resource.is(item); }
    private long stored(ItemResource resource) { return Math.max(0L, resource.getOrDefault(component, 0L)); }
    private static long total(long amount, int count) {
        if (amount <= 0 || count <= 0) return 0;
        return amount > Long.MAX_VALUE / count ? Long.MAX_VALUE : amount * count;
    }
    @Override public long getAmountAsLong() {
        var resource = access.getResource();
        return valid(resource) ? total(stored(resource), access.getAmount()) : 0;
    }
    @Override public long getCapacityAsLong() {
        var resource = access.getResource();
        return valid(resource) ? total(limits.apply(resource).capacity(), access.getAmount()) : 0;
    }
    @Override public int insert(int amount, TransactionContext transaction) { return transfer(amount, transaction, true); }
    @Override public int extract(int amount, TransactionContext transaction) { return transfer(amount, transaction, false); }
    private int transfer(int amount, TransactionContext transaction, boolean insert) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0) return 0;
        int count = access.getAmount();
        var resource = access.getResource();
        if (count <= 0 || !valid(resource)) return 0;
        long requested = amount / count;
        if (requested == 0) return 0;
        long energy = stored(resource);
        var bounds = limits.apply(resource);
        long capacity = Math.max(0L, bounds.capacity());
        long available = insert ? (capacity > energy ? capacity - energy : 0L) : energy;
        long rate = Math.max(0L, insert ? bounds.insert() : bounds.extract());
        int perItem = (int)Math.min(requested, Math.min(available, rate));
        if (perItem == 0) return 0;
        var changed = resource.with(component, insert ? energy + perItem : energy - perItem);
        // Exchange, rather than mutate a detached ItemStack: the inventory owns rollback and notifications.
        return perItem * access.exchange(changed, count, transaction);
    }
}
