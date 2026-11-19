package com.brandon3055.draconicevolution.lib;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** A passthrough OP/FE port whose per-tick allowance rolls back with the actual energy transfer. */
public class TransactionalEnergyGate implements IOPStorage {
    public static final class Budget {
        private final LongSupplier limit;
        private long used;
        private final SnapshotJournal<Long> journal=new SnapshotJournal<>(){
            @Override protected Long createSnapshot(){return used;}
            @Override protected void revertToSnapshot(Long value){used=value;}
        };
        public Budget(LongSupplier limit){this.limit=limit;}
        public long remaining(){return Math.max(0,Math.max(0,limit.getAsLong())-used);}
        public long used(){return used;}
        public void reset(){used=0;}
        private void consume(long amount,TransactionContext context){
            if(amount<0||amount>remaining())throw new IllegalStateException("Energy gate exceeded its allowance");
            if(amount!=0){journal.updateSnapshots(context);used+=amount;}
        }
    }
    private final boolean input;
    private final Supplier<IOPStorage> transferStorage,queryStorage;
    private final Budget budget;
    public TransactionalEnergyGate(boolean input,Supplier<IOPStorage> transferStorage,Supplier<IOPStorage> queryStorage,Budget budget){
        this.input=input;this.transferStorage=transferStorage;this.queryStorage=queryStorage;this.budget=budget;
    }
    private long transfer(long requested,boolean insert,TransactionContext parent){
        if(requested<=0||insert!=input)return 0;
        long limit=Math.min(requested,budget.remaining());if(limit==0)return 0;
        IOPStorage storage=transferStorage.get();if(storage==null||!(insert?storage.canReceive():storage.canExtract()))return 0;
        try(var transaction=Transaction.open(parent)){
            // Long OP calls join this same transaction; native FE adapters preserve it as well.
            long moved=insert?storage.receiveOP(limit,false):storage.extractOP(limit,false);
            if(moved<0||moved>limit)throw new IllegalStateException("Invalid energy transfer result");
            budget.consume(moved,transaction);transaction.commit();return moved;
        }
    }
    @Override public int insert(int amount,TransactionContext context){if(amount<0)throw new IllegalArgumentException("Negative energy insertion");return (int)transfer(amount,true,context);}
    @Override public int extract(int amount,TransactionContext context){if(amount<0)throw new IllegalArgumentException("Negative energy extraction");return (int)transfer(amount,false,context);}
    private long legacy(long amount,boolean simulate,boolean insert){
        if(amount<=0)return 0;
        try(var transaction=Transaction.open(Transaction.getCurrentOpenedTransaction())){
            long moved=transfer(amount,insert,transaction);if(!simulate)transaction.commit();return moved;
        }
    }
    @Override public long receiveOP(long amount,boolean simulate){return legacy(amount,simulate,true);}
    @Override public long extractOP(long amount,boolean simulate){return legacy(amount,simulate,false);}
    @Override public long getOPStored(){var storage=input?null:queryStorage.get();return storage==null?0:storage.getOPStored();}
    @Override public long getMaxOPStored(){var storage=input?null:queryStorage.get();return storage==null?0:storage.getMaxOPStored();}
    @Override public boolean canReceive(){return input;}
    @Override public boolean canExtract(){return !input;}
    @Override public long modifyEnergyStored(long amount){return 0; /* A gate owns no stored energy. */}
}
