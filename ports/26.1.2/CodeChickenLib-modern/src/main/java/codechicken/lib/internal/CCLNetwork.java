package codechicken.lib.internal;

import codechicken.lib.block.ModularBlockEntity;
import codechicken.lib.config.ConfigSyncManager;
import codechicken.lib.inventory.container.modular.ModularGuiContainerMenu;
import codechicken.lib.packet.StreamNetworkChannel;
import codechicken.lib.packet.StreamNetworkChannel.BidirectionalPacketHandle;
import codechicken.lib.packet.StreamNetworkChannel.ConfigurationPacketHandle;
import codechicken.lib.render.particle.CustomParticleHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static codechicken.lib.CodeChickenLib.MOD_ID;

/**
 * Created by covers1624 on 28/10/19.
 */
public class CCLNetwork {

    protected static final StreamNetworkChannel CHANNEL = new StreamNetworkChannel(MOD_ID)
            .optional();

    public static final BidirectionalPacketHandle GUI_SYNC = CHANNEL.playBidirectional("gui_sync", ModularGuiContainerMenu::handlePacketFromClient);
    public static final BidirectionalPacketHandle TILE_MESSAGE = CHANNEL.playBidirectional("tile_message", CCLNetwork::handleTileMessageServer);

    public static final ConfigurationPacketHandle CONFIG_SYNC = CHANNEL.configurationToClient("config_sync", ConfigSyncManager.ConfigSyncConfigurationTask::new);

    public static void init(IEventBus modBus, ModContainer container) {
        CHANNEL.init(modBus, container);
    }

    private static void handleTileMessageServer(RegistryFriendlyByteBuf packet, IPayloadContext ctx) {
        if (ctx.player().level().getBlockEntity(packet.readBlockPos()) instanceof ModularBlockEntity tile) {
            tile.onInternalServerPacket(packet, ctx);
        }
    }
}
