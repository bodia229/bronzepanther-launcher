package com.brandon3055.brandonscore.capability;

import com.brandon3055.brandonscore.api.power.IOPStorage;

/**
 * Created by brandon3055 on 20/3/20.
 */
public class OPIOControl implements IOPStorage {

    protected boolean allowExtract = true;
    protected boolean allowReceive = true;
    private IOPStorage wrapped;

    public OPIOControl(IOPStorage wrapped) {
        this.wrapped = wrapped;
    }
    @Override public int insert(int amount,net.neoforged.neoforge.transfer.transaction.TransactionContext transaction){
        if(amount<0)throw new IllegalArgumentException("Negative energy insertion");return canReceive()?wrapped.insert(amount,transaction):0;
    }
    @Override public int extract(int amount,net.neoforged.neoforge.transfer.transaction.TransactionContext transaction){
        if(amount<0)throw new IllegalArgumentException("Negative energy extraction");return canExtract()?wrapped.extract(amount,transaction):0;
    }

    public OPIOControl setIOMode(boolean allowExtract, boolean allowReceive) {
        this.allowExtract = allowExtract;
        this.allowReceive = allowReceive;
        return this;
    }

    public OPIOControl setExtractOnly() {
        return setIOMode(true, false);
    }

    public OPIOControl setReceiveOnly() {
        return setIOMode(false, true);
    }

    /**
     * @param inputOutput true = Input Only, false = Output Only.
     */
    public OPIOControl setIOMode(boolean inputOutput) {
        return setIOMode(!inputOutput, inputOutput);
    }

    @Override
    public boolean canExtract() {
        return allowExtract && wrapped.canExtract();
    }

    @Override
    public boolean canReceive() {
        return allowReceive && wrapped.canReceive();
    }

    @Override
    public long maxExtract() {
        return wrapped.maxExtract();
    }

    @Override
    public long maxReceive() {
        return wrapped.maxReceive();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return canReceive()?wrapped.receiveEnergy(maxReceive, simulate):0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return canExtract()?wrapped.extractEnergy(maxExtract, simulate):0;
    }

    @Override
    public int getEnergyStored() {
        return wrapped.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return wrapped.getMaxEnergyStored();
    }

    @Override
    public long modifyEnergyStored(long amount) {
        return wrapped.modifyEnergyStored(amount);
    }

    @Override
    public long receiveOP(long maxReceive, boolean simulate) {
        return canReceive()?wrapped.receiveOP(maxReceive, simulate):0;
    }

    @Override
    public long extractOP(long maxExtract, boolean simulate) {
        return canExtract()?wrapped.extractOP(maxExtract, simulate):0;
    }

    @Override
    public long getOPStored() {
        return wrapped.getOPStored();
    }

    @Override
    public long getMaxOPStored() {
        return wrapped.getMaxOPStored();
    }
}
