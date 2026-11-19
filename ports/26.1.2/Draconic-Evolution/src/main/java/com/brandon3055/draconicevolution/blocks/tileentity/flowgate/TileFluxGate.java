package com.brandon3055.draconicevolution.blocks.tileentity.flowgate;

import codechicken.lib.data.MCDataInput;
import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.brandonscore.capability.CapabilityOP;
import com.brandon3055.brandonscore.utils.EnergyUtils;
import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.inventory.FlowGateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Created by brandon3055 on 15/11/2016.
 */
public class TileFluxGate extends TileFlowGate {

    private final com.brandon3055.draconicevolution.lib.TransactionalEnergyGate.Budget budget = new com.brandon3055.draconicevolution.lib.TransactionalEnergyGate.Budget(this::getFlow);

    @Override public void tick(){super.tick();budget.reset();}

    private OPRegulator inputReg = new OPRegulator(this, true);
    private OPRegulator outputReg = new OPRegulator(this, false);

    public TileFluxGate(BlockPos pos, BlockState state) {
        super(DEContent.TILE_FLUX_GATE.get(), pos, state);
    }

    //THis is an annoying hack required due to the fact you cant get the block state in onLoad()
    private boolean capsLoaded = false;

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(CapabilityOP.BLOCK, DEContent.TILE_FLUX_GATE.get(), (tile, side) -> {
            if (!tile.capsLoaded) {
                tile.updateCapabilities();
                tile.capsLoaded = true;
            }
            return tile.getCapManager().getCapability(CapabilityOP.BLOCK, side);
        });

        event.registerBlockEntity(Capabilities.Energy.BLOCK, DEContent.TILE_FLUX_GATE.get(), (tile, side) -> {
            if (!tile.capsLoaded) {
                tile.updateCapabilities();
                tile.capsLoaded = true;
            }
            return tile.getCapManager().getCapability(CapabilityOP.BLOCK, side);
        });
    }

    @Override
    public String getUnits() {
        return "RF/t";
    }

    private void updateCapabilities() {
        capManager.remove(CapabilityOP.BLOCK);
        capManager.set(CapabilityOP.BLOCK, inputReg, getDirection().getOpposite());
        capManager.set(CapabilityOP.BLOCK, outputReg, getDirection());
    }

    @Override
    public void setBlockState(BlockState p_155251_) {
        super.setBlockState(p_155251_);
        updateCapabilities();
    }

    //endregion

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FlowGateMenu(id, player.getInventory(), this);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer) {
            player.openMenu(this, worldPosition);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void receivePacketFromClient(MCDataInput data, ServerPlayer client, int id) {
        if (flowOverridden.get()) {
            return;
        }

        try {
            String value = data.readString();
            long l = Long.parseLong(value);
            if (l < 0) {
                l = 0;
            }

            if (id == 0) {
                minFlow.set(l);
            } else if (id == 1) {
                maxFlow.set(l);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static class OPRegulator extends com.brandon3055.draconicevolution.lib.TransactionalEnergyGate {
        public OPRegulator(TileFluxGate tile,boolean input){
            super(input,()->{
                BlockEntity peer=input?tile.getTarget():tile.getSource();
                return peer==null?null:EnergyUtils.getStorage(peer,input?tile.getDirection().getOpposite():tile.getDirection());
            },()->{
                BlockEntity peer=tile.getTarget();
                return peer==null?null:EnergyUtils.getStorage(peer,tile.getDirection().getOpposite());
            },tile.budget);
        }
    }
}
