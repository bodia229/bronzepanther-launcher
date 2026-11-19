package com.brandon3055.draconicevolution.blocks.tileentity.flowgate;

import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.inventory.FlowGateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import com.brandon3055.draconicevolution.lib.FlowGateResourceHandler;


/**
 * Created by brandon3055 on 15/11/2016.
 */
public class TileFluidGate extends TileFlowGate {

    private final ResourceHandler<FluidResource> inputHandler = new FlowGateResourceHandler<>(
            () -> neighbor(true), this::getFlow, FluidResource.EMPTY, true);
    private final ResourceHandler<FluidResource> outputHandler = new FlowGateResourceHandler<>(
            () -> neighbor(false), this::getFlow, FluidResource.EMPTY, false);

    private ResourceHandler<FluidResource> neighbor(boolean input) {
        if (level == null) return null;
        var direction = getDirection();
        return level.getCapability(Capabilities.Fluid.BLOCK,
                worldPosition.relative(input ? direction : direction.getOpposite()),
                input ? direction.getOpposite() : direction);
    }

    public TileFluidGate(BlockPos pos, BlockState state) {
        super(DEContent.TILE_FLUID_GATE.get(), pos, state);
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, DEContent.TILE_FLUID_GATE.get(), (tile, side) -> {
            if (side != null && side.getAxis() == tile.getDirection().getAxis()) {
                return side == tile.getDirection() ? tile.outputHandler : tile.inputHandler;
            }
            return null;
        });
    }

    @Override
    public String getUnits() {
        return "MB/t";
    }

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

}
