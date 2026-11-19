package com.brandon3055.brandonscore.capability;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.util.Objects;

/** Adapts native transactional FE to legacy OP callers, without changing energy units. */
public final class TransferEnergyOP implements IOPStorage {
    private final EnergyHandler handler;
    public TransferEnergyOP(EnergyHandler handler){this.handler=Objects.requireNonNull(handler);}
    private int transfer(long amount,boolean simulate,boolean insert){
        if(amount<=0)return 0;
        int limit=(int)Math.min(amount,Integer.MAX_VALUE);
        // Join an existing transaction when called by another transactional handler.
        try(var transaction=Transaction.open(Transaction.getCurrentOpenedTransaction())){
            int transferred=insert?handler.insert(limit,transaction):handler.extract(limit,transaction);
            if(!simulate)transaction.commit();
            return transferred;
        }
    }
    @Override public int insert(int amount,net.neoforged.neoforge.transfer.transaction.TransactionContext transaction){return handler.insert(amount,transaction);}
    @Override public int extract(int amount,net.neoforged.neoforge.transfer.transaction.TransactionContext transaction){return handler.extract(amount,transaction);}
    @Override public long receiveOP(long amount,boolean simulate){return transfer(amount,simulate,true);}
    @Override public long extractOP(long amount,boolean simulate){return transfer(amount,simulate,false);}
    @Override public long getOPStored(){return handler.getAmountAsLong();}
    @Override public long getMaxOPStored(){return handler.getCapacityAsLong();}
    // The native API exposes current acceptance, rather than static direction flags.
    @Override public boolean canReceive(){return receiveOP(1,true)>0;}
    @Override public boolean canExtract(){return extractOP(1,true)>0;}
    @Override public long modifyEnergyStored(long amount){
        return amount>=0?receiveOP(amount,false):extractOP(amount==Long.MIN_VALUE?Long.MAX_VALUE:-amount,false);
    }
}
