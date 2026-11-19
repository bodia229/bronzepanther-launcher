package community.bronzepanther;

import dev.industrial.EuAcceptor;
import dev.industrial.BatteryBoxBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import community.bronzepanther.mixin.EnergyAccess;
import community.bronzepanther.api.CapacityView;

@Mod(BronzePanther.ID)
public final class BronzePanther {
    public static final String ID = "bronzepanther";
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final ClassValue<Integer> CAPACITY = new ClassValue<>() {
        @Override protected Integer computeValue(Class<?> type) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                Integer value = EnergyCapacities.VALUES.get(current.getName());
                if (value != null) return value;
            }
            return -1;
        }
    };
    public BronzePanther(IEventBus bus) {
        bus.addListener(BronzePanther::registerEnergy);
        if (Boolean.getBoolean("bronzepanther.selftest")) SelfTest.register();
        if (FMLEnvironment.getDist().isClient()) {
            ClientEvents.register();
            PerformanceClient.register(bus);
            if (Boolean.getBoolean("bronzepanther.uiSelfTest")) UiSelfTest.register();
        }
    }
    private static void registerEnergy(RegisterCapabilitiesEvent event) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("industrial")) continue;
            if (!event.isBlockRegistered(Capabilities.Energy.BLOCK, block)) {
                event.registerBlock(Capabilities.Energy.BLOCK,
                    (level, pos, state, be, side) -> be instanceof EnergyAccess energy
                        ? new IndustrialEnergy(be, energy, side) : null, block);
            }
        }
    }
    public static boolean isSource(BlockEntity be) {
        return be instanceof BatteryBoxBlockEntity ||
            (be.getClass().getSimpleName().contains("Generator") && !(be instanceof EuAcceptor));
    }
    public static void pushForgeEnergy(Level level, Object candidate) {
        if (!(candidate instanceof BlockEntity be) || !(candidate instanceof EnergyAccess) ||
            level.isClientSide() || be.isRemoved() || !isSource(be)) return;
        // Empty stores cannot push. Avoid six world and capability lookups per idle machine per tick.
        if (((EnergyAccess) candidate).bp$getEnergy() <= 0) return;
        Direction output = be instanceof BatteryBoxBlockEntity && be.getBlockState().hasProperty(BlockStateProperties.FACING)
            ? be.getBlockState().getValue(BlockStateProperties.FACING) : null;
        for (Direction direction : DIRECTIONS) {
            if (output != null && output != direction) continue;
            if (((EnergyAccess) candidate).bp$getEnergy() <= 0) break;
            if (!level.hasChunkAt(be.getBlockPos().relative(direction))) continue;
            BlockEntity targetBE = level.getBlockEntity(be.getBlockPos().relative(direction));
            if (targetBE instanceof EnergyAccess) continue; // Native EU still handles EU-to-EU connections.
            EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK,
                be.getBlockPos().relative(direction), direction.getOpposite());
            if (target == null) continue;
            IndustrialEnergy source = new IndustrialEnergy(be, (EnergyAccess) be, direction);
            int offered = Math.min(source.getAmountAsInt(), 32768);
            if (offered <= 0 || !source.outputAllowed()) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int accepted = target.insert(offered, transaction);
                if (accepted > 0 && accepted <= offered && source.extract(accepted, transaction) == accepted)
                    transaction.commit();
            }
        }
    }
    public static final class IndustrialEnergy extends SnapshotJournal<Integer> implements EnergyHandler {
        private final BlockEntity be;
        private final EnergyAccess energy;
        private final Direction side;
        public IndustrialEnergy(BlockEntity be, EnergyAccess energy, Direction side) {
            this.be = be; this.energy = energy; this.side = side;
        }
        @Override public long getAmountAsLong() { return be.isRemoved() ? 0 : Math.max(0, energy.bp$getEnergy()); }
        @Override public long getCapacityAsLong() {
            if (be instanceof CapacityView capacity) return Math.max(0, capacity.bp$getCapacity());
            int capacity = CAPACITY.get(be.getClass());
            if (capacity >= 0) return capacity;
            return getAmountAsLong();
        }
        public boolean outputAllowed() {
            if (!isSource(be)) return false;
            return !(be instanceof BatteryBoxBlockEntity) || side == null ||
                !be.getBlockState().hasProperty(BlockStateProperties.FACING) ||
                be.getBlockState().getValue(BlockStateProperties.FACING) == side;
        }
        @Override public int insert(int amount, TransactionContext transaction) {
            if (amount <= 0 || be.isRemoved() || !(be instanceof EuAcceptor acceptor) ||
                (side != null && !acceptor.acceptsFrom(side))) return 0;
            int accepted = (int) Math.min(amount, Math.max(0, getCapacityAsLong() - getAmountAsLong()));
            if (accepted > 0) { updateSnapshots(transaction); energy.bp$setEnergy(energy.bp$getEnergy() + accepted); }
            return accepted;
        }
        @Override public int extract(int amount, TransactionContext transaction) {
            if (amount <= 0 || be.isRemoved() || !outputAllowed()) return 0;
            int extracted = (int) Math.min(amount, getAmountAsLong());
            if (extracted > 0) { updateSnapshots(transaction); energy.bp$setEnergy(energy.bp$getEnergy() - extracted); }
            return extracted;
        }
        @Override protected Integer createSnapshot() { return energy.bp$getEnergy(); }
        @Override protected void revertToSnapshot(Integer value) { energy.bp$setEnergy(value); }
        @Override protected void onRootCommit(Integer previous) { if (!be.isRemoved()) be.setChanged(); }
    }
}
