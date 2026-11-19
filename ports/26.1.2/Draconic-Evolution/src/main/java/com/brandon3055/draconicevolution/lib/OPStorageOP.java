package com.brandon3055.draconicevolution.lib;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import com.brandon3055.brandonscore.api.power.IOInfo;
import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.brandonscore.api.power.IOTracker;
import com.brandon3055.brandonscore.api.power.IOTrackerSelfTimed;
import com.brandon3055.brandonscore.lib.IMCDataSerializable;
import com.brandon3055.brandonscore.lib.IValueHashable;
import net.neoforged.neoforge.transfer.transaction.*;
import com.google.common.math.BigIntegerMath;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.brandon3055.brandonscore.api.serialization.LegacyNbtSerializable;
import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Created by brandon3055 on 21/09/2022
 */
public class OPStorageOP implements LegacyNbtSerializable<CompoundTag>, IValueHashable<OPStorageOP.ComparableValue>, IMCDataSerializable, IOPStorage {

    /**
     * The base storage for the current value minus overflow.
     * This is used for efficiency as most of the time the energy stored will never exceed Long.MAX_VALUE
     */
    @VisibleForTesting
    protected long valueStorage = 0;
    /**
     * Stores the number of times valueStorage has rolled over.
     */
    @VisibleForTesting
    protected BigInteger overflowCount = BigInteger.ZERO;

    protected IOTracker ioTracker;
    private final BlockEntity tile;
    private Supplier<Long> capacity;
    private long pendingInput,pendingOutput;
    private record Snapshot(long value,BigInteger overflow,long input,long output){}
    private final SnapshotJournal<Snapshot> journal=new SnapshotJournal<>(){
        @Override protected Snapshot createSnapshot(){return new Snapshot(valueStorage,overflowCount,pendingInput,pendingOutput);}
        @Override protected void revertToSnapshot(Snapshot s){valueStorage=s.value();overflowCount=s.overflow();pendingInput=s.input();pendingOutput=s.output();}
        @Override protected void onRootCommit(Snapshot before){
            long in=pendingInput,out=pendingOutput;pendingInput=0;pendingOutput=0;
            if(ioTracker!=null){if(in>0)ioTracker.energyInserted(in);if(out>0)ioTracker.energyExtracted(out);}
            if((in!=0||out!=0)&&tile!=null)tile.setChanged();
        }
    };
    private static long saturatedAdd(long a,long b){return b>Long.MAX_VALUE-a?Long.MAX_VALUE:a+b;}
    private long transfer(long requested,TransactionContext transaction,boolean insert){
        if(requested<=0)return 0;
        long limit=capacity.get();
        long available=insert?(limit==-1?Long.MAX_VALUE:overflowCount.signum()>0?0:Math.max(0,limit-valueStorage)):
                overflowCount.signum()>0?Long.MAX_VALUE:valueStorage;
        long amount=Math.min(requested,available);if(amount<=0)return 0;
        journal.updateSnapshots(transaction);
        if(insert){
            long room=Long.MAX_VALUE-valueStorage;
            if(amount>room){valueStorage=amount-room;overflowCount=overflowCount.add(BigInteger.ONE);}
            else valueStorage+=amount;
            pendingInput=saturatedAdd(pendingInput,amount);
        }else{
            if(amount>valueStorage){valueStorage=Long.MAX_VALUE-(amount-valueStorage);overflowCount=overflowCount.subtract(BigInteger.ONE);}
            else valueStorage-=amount;
            pendingOutput=saturatedAdd(pendingOutput,amount);
        }
        return amount;
    }
    @Override public int insert(int amount,TransactionContext transaction){
        if(amount<0)throw new IllegalArgumentException("Negative energy insertion");return (int)transfer(amount,transaction,true);
    }
    @Override public int extract(int amount,TransactionContext transaction){
        if(amount<0)throw new IllegalArgumentException("Negative energy extraction");return (int)transfer(amount,transaction,false);
    }
    private long legacyTransfer(long amount,boolean simulate,boolean insert){
        if(amount<=0)return 0;
        try(var transaction=Transaction.open(Transaction.getCurrentOpenedTransaction())){
            long moved=transfer(amount,transaction,insert);if(!simulate)transaction.commit();return moved;
        }
    }

    /**
     * @param capacity supplier for the maximum capacity of this storage or -1 for unlimited.
     */
    public OPStorageOP(BlockEntity tile, Supplier<Long> capacity) {
        this.tile = tile;
        this.capacity = capacity;
    }

    @Override public long extractOP(long amount,boolean simulate){return legacyTransfer(amount,simulate,false);}
    @Override public long receiveOP(long amount,boolean simulate){return legacyTransfer(amount,simulate,true);}

    //This is capped at Long.MAX_VALUE / 2 to account for senders that preemptively check available space before sending energy.
    @Override
    public long getOPStored() {
        if (capacity.get() == -1) {
            if (!overflowCount.equals(BigInteger.ZERO)) {
                return Long.MAX_VALUE / 2;
            }
            return Math.min(valueStorage, Long.MAX_VALUE / 2);
        }
        return valueStorage;
    }

    //Version of getOPStored for internal use that does not cap out at Long.MAX_VALUE / 2
    public long getUncappedStored() {
        if (!overflowCount.equals(BigInteger.ZERO)) {
            return Long.MAX_VALUE;
        }
        return valueStorage;
    }

    @Override
    public long getMaxOPStored() {
        long cap = capacity.get();
        return cap == -1 ? Long.MAX_VALUE : cap;
    }

    public void validateStorage() {
        long cap = capacity.get();
        if (cap != -1) {
            valueStorage = overflowCount.signum()>0?cap:Math.min(valueStorage, cap);
            overflowCount = BigInteger.ZERO;
        }
    }

    @Override
    public void serializeMCD(MCDataOutput output) {
        output.writeVarLong(valueStorage);
        output.writeBytes(overflowCount.toByteArray());
        output.writeBoolean(ioTracker != null);
        if (ioTracker != null) {
            output.writeVarLong(ioTracker.currentInput());
            output.writeVarLong(ioTracker.currentOutput());
        }
    }

    @Override
    public void deSerializeMCD(MCDataInput input) {
        valueStorage = input.readVarLong();
        overflowCount = new BigInteger(input.readBytes());
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
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("storage", valueStorage);
        if (!overflowCount.equals(BigInteger.ZERO)) {
            tag.putByteArray("overflow", overflowCount.toByteArray());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        valueStorage = nbt.getLongOr("storage",0L);
        byte[] bytes=nbt.getByteArray("overflow").orElse(new byte[0]);
        overflowCount=bytes.length==0?BigInteger.ZERO:new BigInteger(bytes);
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    public void setIOTracker(@javax.annotation.Nullable IOTracker ioTracker) {
        this.ioTracker = ioTracker;
    }

    @Nullable
    @Override
    public IOInfo getIOInfo() {
        return ioTracker;
    }

    @Override
    public long modifyEnergyStored(long amount) {
        if (amount > 0) {
            return receiveOP(amount, false);
        } else {
            return extractOP(amount==Long.MIN_VALUE?Long.MAX_VALUE:-amount, false);
        }
    }

    @Override
    public ComparableValue getValueHash() {
        return new ComparableValue(this);
    }

    @Override
    public boolean checkValueHash(Object vh) {
        if (vh instanceof ComparableValue v) {
            boolean mainCheck = v.valueStorage == valueStorage && v.overflowCount.equals(overflowCount);
            if (ioTracker != null) {
                return mainCheck && v.currentInput == ioTracker.currentInput() && v.currentOutput == ioTracker.currentOutput();
            }
            return mainCheck;
        }

        return false;
    }

    public boolean isUnlimited() {
        return capacity.get() == -1;
    }

    protected static class ComparableValue {
        private long valueStorage;
        private BigInteger overflowCount;
        private long currentInput = 0;
        private long currentOutput = 0;

        public ComparableValue(OPStorageOP storage) {
            valueStorage = storage.valueStorage;
            overflowCount = storage.overflowCount;
            if (storage.ioTracker != null) {
                currentInput = storage.ioTracker.currentInput();
                currentOutput = storage.ioTracker.currentOutput();
            }
        }
    }

    private static BigInteger prefixEnd = new BigInteger("975781955236953990712502012356953416011859675");
    private static NumberFormat decimalFormat = new DecimalFormat("0.######E0", DecimalFormatSymbols.getInstance(Locale.ROOT));


    public Component getReadable() {
        if (overflowCount.compareTo(prefixEnd) > 0) {
            return Component.literal(decimalFormat.format(overflowCount) + " x (2^64)");
        }

        BigInteger value = BigInteger.valueOf(valueStorage).add(overflowCount.multiply(BigInteger.valueOf(Long.MAX_VALUE)));
        if (value.equals(BigInteger.ZERO)) {
            return Component.literal("0");
        }
        int digits = BigIntegerMath.log10(value, RoundingMode.DOWN);
        int prefixStep = (digits / 3) * 3;

        if (digits < 6) {
            return Component.literal(NumberFormat.getIntegerInstance(Locale.ROOT).format(value.longValue()));
        }

        BigDecimal decimal = new BigDecimal(value).divide(BigDecimal.valueOf(10).pow(prefixStep), 3, RoundingMode.DOWN);
        return Component.literal(decimal.doubleValue() + "").append(Component.translatable("numprefix.draconicevolution.10-" + prefixStep));
    }

    public Component getReadableCapacity() {
        if (capacity.get() == -1) {
            return Component.literal("~1x10^1300000000");
        } else {
            long cap = capacity.get();
            int digits = (int) Math.log10(cap);
            int prefixStep = (digits / 3) * 3;

            if (digits < 6) {
                return Component.literal(NumberFormat.getIntegerInstance(Locale.ROOT).format(cap) + " OP");
            }

            double decimal = cap / Math.pow(10, prefixStep);//new BigDecimal(value).divide(BigDecimal.valueOf(10).pow(prefixStep), 3, RoundingMode.DOWN);
            return Component.literal(String.valueOf(Math.round(decimal * 1000) / 1000D)).append(Component.translatable("numprefix.draconicevolution.10-" + prefixStep));
        }
    }

    public String getScientific() {
        if (overflowCount.compareTo(prefixEnd) > 0) {
            return decimalFormat.format(overflowCount) + " x (2^64) OP";
        }

        return decimalFormat.format(getStoredBig());
    }

    public BigInteger getStoredBig() {
        return BigInteger.valueOf(valueStorage).add(overflowCount.multiply(BigInteger.valueOf(Long.MAX_VALUE)));
    }
}
