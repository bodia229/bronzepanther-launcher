package com.brandon3055.brandonscore.api.power;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import net.neoforged.neoforge.transfer.transaction.*;
import com.brandon3055.brandonscore.lib.IMCDataSerializable;
import com.brandon3055.brandonscore.lib.IValueHashable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.brandon3055.brandonscore.api.serialization.LegacyNbtSerializable;

import javax.annotation.Nullable;

/**
 * Created by brandon3055 on 14/8/19.
 * <p>
 * Operational Potential is the power system used by Draconic Evolution and related mods.
 * This system is an extension of Forge Energy that allows long based power transfer and storage.
 * <p>
 * When implementing this capability cap should be provided as both {@link net.minecraftforge.energy.CapabilityEnergy#ENERGY} and {@link CapabilityOP#OP}
 * So any mod that implements FE will find the FE cap and interact with it normally. However any mod that implements OP will fist check for the OP cap before falling back to RF.
 */
public class OPStorage implements LegacyNbtSerializable<CompoundTag>, IValueHashable<OPStorage.ComparableValue>, IMCDataSerializable, IOPStorage {
    protected Runnable changeListener;
    protected long energy;
    protected long capacity;
    protected long maxReceive;
    protected long maxExtract;
    protected IOTracker ioTracker;
    protected boolean allowExtract = true;
    protected boolean allowReceive = true;
    private long pendingInput,pendingOutput;
    private record EnergySnapshot(long energy,long input,long output){}
    private final SnapshotJournal<EnergySnapshot> journal=new SnapshotJournal<>(){
        @Override protected EnergySnapshot createSnapshot(){return new EnergySnapshot(energy,pendingInput,pendingOutput);}
        @Override protected void revertToSnapshot(EnergySnapshot snapshot){energy=snapshot.energy();pendingInput=snapshot.input();pendingOutput=snapshot.output();}
        @Override protected void onRootCommit(EnergySnapshot before){
            long input=pendingInput,output=pendingOutput;pendingInput=0;pendingOutput=0;
            if(ioTracker!=null){if(input>0)ioTracker.energyInserted(input);if(output>0)ioTracker.energyExtracted(output);}
            if(input!=0||output!=0)markDirty();
        }
    };
    private static long addSaturated(long a,long b){return b>Long.MAX_VALUE-a?Long.MAX_VALUE:a+b;}
    private long transfer(long limit,TransactionContext context,boolean insert){
        if(limit<=0||!(insert?canReceive():canExtract()))return 0;
        long available=insert?Math.max(0,getMaxOPStored()-energy):Math.max(0,energy);
        long amount=Math.min(limit,Math.min(available,Math.max(0,insert?maxReceive():maxExtract())));
        if(amount==0)return 0;journal.updateSnapshots(context);
        if(insert){energy+=amount;pendingInput=addSaturated(pendingInput,amount);}
        else{energy-=amount;pendingOutput=addSaturated(pendingOutput,amount);}
        return amount;
    }
    @Override public int insert(int amount,TransactionContext context){
        if(amount<0)throw new IllegalArgumentException("Negative energy insertion");return (int)transfer(amount,context,true);
    }
    @Override public int extract(int amount,TransactionContext context){
        if(amount<0)throw new IllegalArgumentException("Negative energy extraction");return (int)transfer(amount,context,false);
    }
    private long legacyTransfer(long amount,boolean simulate,boolean insert){
        if(amount<=0)return 0;
        try(var transaction=Transaction.open(Transaction.getCurrentOpenedTransaction())){
            long moved=transfer(amount,transaction,insert);if(!simulate)transaction.commit();return moved;
        }
    }

    public OPStorage(long capacity) {
        this(capacity, capacity);
    }

    public OPStorage(BlockEntity tile, long capacity) {
        this(capacity, capacity);
        setChangeListener(tile::setChanged);
    }

    public OPStorage(long capacity, long maxTransfer) {
        this(capacity, maxTransfer, maxTransfer);
    }

    public OPStorage(BlockEntity tile, long capacity, long maxTransfer) {
        this(capacity, maxTransfer, maxTransfer);
        setChangeListener(tile::setChanged);
    }

    public OPStorage(long capacity, long maxReceive, long maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public OPStorage(BlockEntity tile, long capacity, long maxReceive, long maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        setChangeListener(tile::setChanged);
    }

    public OPStorage setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    public OPStorage setIOMode(boolean allowExtract, boolean allowReceive) {
        this.allowExtract = allowExtract;
        this.allowReceive = allowReceive;
        return this;
    }

    public OPStorage setExtractOnly() {
        return setIOMode(true, false);
    }

    public OPStorage setReceiveOnly() {
        return setIOMode(false, true);
    }

    /**
     * @param inputOutput true = Input Only, false = Output Only.
     */
    public OPStorage setIOMode(boolean inputOutput) {
        return setIOMode(!inputOutput, inputOutput);
    }

    @Override public long receiveOP(long amount,boolean simulate){return legacyTransfer(amount,simulate,true);}
    @Override public long extractOP(long amount,boolean simulate){return legacyTransfer(amount,simulate,false);}

    @Override
    public long getOPStored() {
        return energy;
    }

    @Override
    public long getMaxOPStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return allowExtract && maxExtract() > 0;
    }

    @Override
    public boolean canReceive() {
        return allowReceive && maxReceive() > 0;
    }

    @Override public long modifyEnergyStored(long amount){
        long moved=amount>=0?Math.min(amount,Math.max(0,getMaxOPStored()-energy)):
                Math.min(amount==Long.MIN_VALUE?Long.MAX_VALUE:-amount,Math.max(0,energy));
        if(moved==0)return 0;
        try(var transaction=Transaction.open(Transaction.getCurrentOpenedTransaction())){
            journal.updateSnapshots(transaction);
            if(amount>=0){energy+=moved;pendingInput=addSaturated(pendingInput,moved);}
            else{energy-=moved;pendingOutput=addSaturated(pendingOutput,moved);}
            transaction.commit();return moved;
        }
    }

    @Override
    public long maxExtract() {
        return maxExtract;
    }

    @Override
    public long maxReceive() {
        return maxReceive;
    }

    /**
     * This is a raw unchecked setter for the capacity value.
     * This is for internal use only. For things like saving and loading data.
     *
     * @param capacity the new capacity.
     */
    public OPStorage setCapacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * This is a raw unchecked setter for the maxExtract value.
     * This is for internal use only. For things like saving and loading data.
     *
     * @param maxExtract the new max extract.
     */
    public OPStorage setMaxExtract(long maxExtract) {
        this.maxExtract = maxExtract;
        return this;
    }

    /**
     * This is a raw unchecked setter for the maxInsert value.
     * This is for internal use only. For things like saving and loading data.
     *
     * @param maxReceive the new max insert.
     */
    public OPStorage setMaxReceive(long maxReceive) {
        this.maxReceive = maxReceive;
        return this;
    }

    public OPStorage setMaxTransfer(long maxTransfer) {
        this.maxReceive = this.maxExtract = maxTransfer;
        return this;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag compound = new CompoundTag();
        smartWrite("energy", energy, compound);
        return compound;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        energy = smartRead("energy", nbt);
    }

    private void smartWrite(String name, long value, CompoundTag compound) {
        if (value > Integer.MAX_VALUE) {
            compound.putLong(name, value);
        } else {
            compound.putInt(name, (int) value);
        }
    }

    private long smartRead(String name, CompoundTag compound) {
        Tag tag = compound.get(name);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).longValue();
        }
        return 0;
    }

    public void markDirty() {
        if (changeListener != null) changeListener.run();
    }

    @Override
    public void serializeMCD(MCDataOutput output) {
        output.writeVarLong(energy);
        output.writeVarLong(capacity);
        output.writeVarLong(maxReceive);
        output.writeVarLong(maxExtract);
        output.writeBoolean(ioTracker != null);
        if (ioTracker != null) {
            output.writeVarLong(ioTracker.currentInput());
            output.writeVarLong(ioTracker.currentOutput());
        }
    }

    @Override
    public void deSerializeMCD(MCDataInput input) {
        energy = input.readVarLong();
        capacity = input.readVarLong();
        maxReceive = input.readVarLong();
        maxExtract = input.readVarLong();
        if (input.readBoolean()) {
            if (ioTracker == null) {
                ioTracker = new IOTrackerSelfTimed();
            }
            ioTracker.syncClientValues(input.readVarLong(), input.readVarLong());
        } else if (ioTracker != null) {
            ioTracker = null;
        }
    }

    @Override
    public ComparableValue getValueHash() {
        return new ComparableValue(this);
    }

    @Override
    public boolean checkValueHash(Object vh) {
        if (vh instanceof ComparableValue) {
            ComparableValue v = (ComparableValue) vh;
            boolean mainCheck = v.energy == energy && v.capacity == capacity && v.maxExtract == maxExtract && v.maxReceive == maxReceive;
            if (ioTracker != null) {
                return mainCheck && v.currentInput == ioTracker.currentInput() && v.currentOutput == ioTracker.currentOutput();
            }
            return mainCheck;
        }

        return false;
    }

    public void setIOTracker(@Nullable IOTracker ioTracker) {
        this.ioTracker = ioTracker;
    }


    @Nullable
    @Override
    public IOInfo getIOInfo() {
        return ioTracker;
    }

    protected static class ComparableValue {
        private final long energy;
        private final long capacity;
        private final long maxReceive;
        private final long maxExtract;
        private long currentInput = 0;
        private long currentOutput = 0;

        public ComparableValue(OPStorage storage) {
            energy = storage.energy;
            capacity = storage.capacity;
            maxReceive = storage.maxReceive;
            maxExtract = storage.maxExtract;
            if (storage.ioTracker != null) {
                currentInput = storage.ioTracker.currentInput();
                currentOutput = storage.ioTracker.currentOutput();
            }
        }
    }
}
