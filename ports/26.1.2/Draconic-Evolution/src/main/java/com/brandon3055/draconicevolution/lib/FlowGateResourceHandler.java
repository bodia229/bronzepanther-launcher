package com.brandon3055.draconicevolution.lib;

import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** A directional, per-operation flow limit over a live native resource handler.
 * The delegate owns all transaction journals, including nested rollback.
 */
public final class FlowGateResourceHandler<R extends Resource> implements ResourceHandler<R> {
    private final Supplier<ResourceHandler<R>> neighbor;
    private final LongSupplier limit;
    private final R empty;
    private final boolean input;
    public FlowGateResourceHandler(Supplier<ResourceHandler<R>> neighbor, LongSupplier limit, R empty, boolean input) {
        this.neighbor = neighbor;
        this.limit = limit;
        this.empty = empty;
        this.input = input;
    }
    private int limited(int amount) { return (int)Math.min(amount, Math.max(0L, limit.getAsLong())); }
    private boolean valid(ResourceHandler<R> handler, int index) { return handler != null && index >= 0 && index < handler.size(); }
    @Override public int size() { var handler = neighbor.get(); return handler == null ? 0 : handler.size(); }
    @Override public R getResource(int index) { var handler = neighbor.get(); return valid(handler,index) ? handler.getResource(index) : empty; }
    @Override public long getAmountAsLong(int index) { var handler = neighbor.get(); return valid(handler,index) ? handler.getAmountAsLong(index) : 0; }
    @Override public long getCapacityAsLong(int index, R resource) { var handler = neighbor.get(); return valid(handler,index) ? handler.getCapacityAsLong(index,resource) : 0; }
    @Override public boolean isValid(int index, R resource) {
        TransferPreconditions.checkNonEmptyNonNegative(resource,0);
        var handler = neighbor.get(); return valid(handler,index) && handler.isValid(index,resource);
    }
    @Override public int insert(int index, R resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource,amount);
        if (!input || amount == 0) return 0;
        var handler = neighbor.get(); return valid(handler,index) ? handler.insert(index,resource,limited(amount),transaction) : 0;
    }
    @Override public int extract(int index, R resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource,amount);
        if (input || amount == 0) return 0;
        var handler = neighbor.get(); return valid(handler,index) ? handler.extract(index,resource,limited(amount),transaction) : 0;
    }
    // Limit the entire operation, so multiple tanks cannot multiply the configured rate.
    @Override public int insert(R resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource,amount);
        if (!input || amount == 0) return 0;
        var handler = neighbor.get(); return handler == null ? 0 : handler.insert(resource,limited(amount),transaction);
    }
    @Override public int extract(R resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource,amount);
        if (input || amount == 0) return 0;
        var handler = neighbor.get(); return handler == null ? 0 : handler.extract(resource,limited(amount),transaction);
    }
}
