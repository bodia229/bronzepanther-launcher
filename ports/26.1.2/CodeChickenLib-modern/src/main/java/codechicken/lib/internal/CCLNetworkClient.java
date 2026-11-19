package codechicken.lib.internal;

import codechicken.lib.block.ModularBlockEntity;
import codechicken.lib.config.ConfigSyncManager;
import codechicken.lib.inventory.container.modular.ModularGuiContainerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Created by covers1624 on 11/4/26.
 */
public class CCLNetworkClient extends CCLNetwork {

    public static void initClient() {
        CHANNEL.assignClientHandler(GUI_SYNC, ModularGuiContainerMenu::handlePacketFromServer);
        CHANNEL.assignClientHandler(TILE_MESSAGE, CCLNetworkClient::handleTileMessageClient);

        CHANNEL.assignClientHandler(CONFIG_SYNC, ConfigSyncManager::readSyncPacket);
    }

    private static void handleTileMessageClient(RegistryFriendlyByteBuf packet, IPayloadContext ctx) {
        if (ctx.player().level().getBlockEntity(packet.readBlockPos()) instanceof ModularBlockEntity tile) {
            tile.onInternalClientPacket(packet, ctx);
        }
    }
}
