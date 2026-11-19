package codechicken.lib.packet;

import codechicken.lib.internal.CCLNetwork;
import io.netty.buffer.Unpooled;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.handling.MainThreadPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * A simple Stream based network channel, very similar to the NeoForge api, however
 * provides raw, stream-based packets.
 * <p>
 * You would construct one of these and store it statically, see {@link CCLNetwork} for inspiration.
 * <p>
 * Created by covers1624 on 10/26/25.
 */
public final class StreamNetworkChannel {

    private final CrashLock LOCK = new CrashLock("Already initialized.");

    private final String namespace;
    private boolean optional;

    private final Map<Identifier, PacketType<?>> packets = new HashMap<>();

    /**
     * Create a new network channel.
     *
     * @param namespace The namespace, usually your mod id.
     */
    public StreamNetworkChannel(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Called with your mod event bus and mod container.
     *
     * @param bus       Your mod event bus.
     * @param container Your mod container.
     */
    public void init(IEventBus bus, ModContainer container) {
        LOCK.lock();
        bus.addListener(RegisterPayloadHandlersEvent.class, e -> onRegisterPayloadHandlerEvent(e, container));
        bus.addListener(this::onGameConfigurationEvent);
    }

    /**
     * Mark this network channel as optional, not required on the client/server.
     *
     * @return The same channel.
     */
    public StreamNetworkChannel optional() {
        this.optional = true;
        return this;
    }

    /**
     * Register a packet to be set to the client during gameplay.
     * <p>
     * The packet handle must be assigned via {@link #assignClientHandler} on the client-side only.
     *
     * @param name The name of the packet.
     * @return A handle to construct packets.
     * @see #assignClientHandler(ClientPacketHandle, PlayHandler)
     */
    public ClientPacketHandle playToClient(String name) {
        return createType("play", name, PacketKind.CLIENT_HANDLED, PlayPacketType::new);
    }

    /**
     * Register a packet to be set to the server during gameplay.
     *
     * @param name    The name of the packet.
     * @param handler The handler for the packet.
     * @return A handle to construct packets.
     */
    public ServerPacketHandle playToServer(String name, PlayHandler handler) {
        var type = createType("play", name, PacketKind.SERVER_HANDLED, PlayPacketType::new);
        type.serverHandler = handler;
        return type;
    }

    /**
     * Register a packet to be set to either the client or the server during gameplay.
     * <p>
     * The packet's client handle must be assigned via {@link #assignClientHandler} on the client-side only.
     *
     * @param name          The name of the packet.
     * @param serverHandler The handler for the packet server-side.
     * @return A handle to construct packets.
     * @see #assignClientHandler(ClientPacketHandle, PlayHandler)
     */
    public BidirectionalPacketHandle playBidirectional(String name, PlayHandler serverHandler) {
        var type = createType("play", name, PacketKind.BI_DIRECTIONAL, PlayPacketType::new);
        type.serverHandler = serverHandler;
        return type;
    }

    /**
     * Assign the client-side handler for a packet.
     *
     * @param handle  The packet handle to attach to.
     * @param handler The handler.
     */
    public void assignClientHandler(ClientPacketHandle handle, PlayHandler handler) {
        // ClientPacketHandle is sealed.
        var packet = ((PlayPacketType) handle);
        // Technically possible with whacky casts.
        if (packet.kind != PacketKind.CLIENT_HANDLED && packet.kind != PacketKind.BI_DIRECTIONAL) {
            throw new IllegalStateException("Server packets can't have client handlers.");
        }
        if (packet.clientHandler != null) {
            throw new IllegalArgumentException("Client handler already assigned for packet. " + packet.type.id());
        }

        packet.clientHandler = requireNonNull(handler);
    }

    /**
     * Register a packet to configure a connecting client.
     *
     * @param name    The name of the packet.
     * @param factory The factory to construct a {@link ICustomConfigurationTask}.
     * @return The packet handle.
     */
    public ConfigurationPacketHandle configurationToClient(String name, ConfigurationTaskFactory factory) {
        return createType("configuration", name, PacketKind.CLIENT_HANDLED, (id, kind) -> new ConfigurationPacketType(id, kind, factory));
    }

    /**
     * Assign the client-side handler for a configuration task packet.
     *
     * @param handle  The packet handle to attach to.
     * @param handler The handler.
     */
    public void assignClientHandler(ConfigurationPacketHandle handle, ConfigurationHandler handler) {
        var packet = ((ConfigurationPacketType) handle);
        if (packet.kind != PacketKind.CLIENT_HANDLED && packet.kind != PacketKind.BI_DIRECTIONAL) {
            throw new IllegalStateException("Server packets can't have client handlers.");
        }

        if (packet.handler != null) {
            throw new IllegalArgumentException("Handler already assigned for packet. " + packet.type.id());
        }
        packet.handler = requireNonNull(handler);
    }

    // region Events
    private void onRegisterPayloadHandlerEvent(RegisterPayloadHandlersEvent event, ModContainer container) {
        var registrar = event.registrar(container.getModInfo().getVersion().toString());
        if (optional) registrar = registrar.optional();

        for (PacketType<?> type : packets.values()) {
            type.register(registrar);
        }
    }

    private void onGameConfigurationEvent(RegisterConfigurationTasksEvent event) {
        for (PacketType<?> type : packets.values()) {
            if (type instanceof ConfigurationPacketType cType) {
                event.register(cType.getTask(event.getListener()));
            }
        }
    }
    // endregion

    private <T extends PacketType<?>> T createType(String phase, String name, PacketKind kind, BiFunction<Identifier, PacketKind, ? extends T> func) {
        var id = makeId(phase, name);
        if (packets.containsKey(id)) throw new IllegalArgumentException("Packet with name " + name + " for the " + phase + " phase is already registered.");

        var type = func.apply(id, kind);
        packets.put(id, type);
        return type;
    }

    private Identifier makeId(String prefix, String name) {
        return Identifier.fromNamespaceAndPath(namespace, prefix + "/" + name);
    }

    // region Registration internal
    private static abstract sealed class PacketType<T extends CustomPacketPayload> permits PlayPacketType, ConfigurationPacketType {

        protected final CustomPacketPayload.Type<T> type;

        private PacketType(Identifier id) {
            type = new CustomPacketPayload.Type<>(id);
        }

        public abstract void register(PayloadRegistrar registrar);
    }

    private static final class PlayPacketType extends PacketType<StreamPacket> implements BidirectionalPacketHandle {

        private final PacketKind kind;
        private @Nullable PlayHandler clientHandler;
        private @Nullable PlayHandler serverHandler;

        private PlayPacketType(Identifier id, PacketKind kind) {
            super(id);
            this.kind = kind;
        }

        private IPayloadHandler<StreamPacket> clientHandler() {
            if (clientHandler == null) {
                throw new IllegalStateException("Packet " + type.id() + " does not have a client handler assigned.");
            }

            return clientHandler::accept;
        }

        private IPayloadHandler<StreamPacket> serverHandler() {
            // Should not be possible..
            if (serverHandler == null) {
                throw new IllegalStateException("Packet " + type.id() + " does not have a server handler assigned.");
            }

            return serverHandler::accept;
        }

        @Override
        public void register(PayloadRegistrar registrar) {
            var codec = StreamPacket.createCodec(type);

            switch (kind) {
                case CLIENT_HANDLED -> {
                    if (FMLEnvironment.getDist().isClient()) {
                        registrar.playToClient(type, codec, clientHandler());
                    } else {
                        registrar.playToClient(type, codec);
                    }
                }
                case SERVER_HANDLED -> registrar.playToServer(type, codec, serverHandler());
                case BI_DIRECTIONAL -> {
                    if (FMLEnvironment.getDist().isClient()) {
                        if (FMLEnvironment.getDist().isClient()) {
                            registrar.playBidirectional(type, codec, serverHandler(), clientHandler());
                        } else {
                            registrar.playBidirectional(type, codec, serverHandler());
                        }
                    }
                }
            }
        }

        @Override
        public StreamPacket.ToClient toClient(RegistryAccess registryAccess) {
            return new StreamPacket.ToClient(
                    type,
                    Unpooled.buffer(),
                    registryAccess,
                    ConnectionType.NEOFORGE
            );
        }

        @Override
        public StreamPacket.ToServer toServer() {
            return new StreamPacket.ToServer(
                    type,
                    Unpooled.buffer(),
                    Minecraft.getInstance().getConnection().registryAccess(),
                    ConnectionType.NEOFORGE
            );
        }
    }

    private static final class ConfigurationPacketType extends PacketType<ConfigurationStreamPacket> implements ConfigurationPacketHandle {

        private final PacketKind kind;
        public final ConfigurationTask.Type configurationType;
        private final ConfigurationTaskFactory factory;
        private @Nullable ConfigurationHandler handler;

        private ConfigurationPacketType(Identifier id, PacketKind kind, ConfigurationTaskFactory factory) {
            super(id);
            this.kind = kind;
            this.factory = factory;
            configurationType = new ConfigurationTask.Type(type.id());
        }

        private IPayloadHandler<ConfigurationStreamPacket> handler() {
            if (handler == null) {
                throw new IllegalStateException("Packet " + type.id() + " does not have a handler assigned.");
            }

            return handler::accept;
        }

        @Override
        public void register(PayloadRegistrar registrar) {
            var codec = ConfigurationStreamPacket.createCodec(type);

            if (FMLEnvironment.getDist().isClient()) {
                registrar.configurationToClient(type, codec, handler());
            } else {
                registrar.configurationToClient(type, codec);
            }
        }

        public ConfigurationTask getTask(ServerConfigurationPacketListener listener) {
            var context = new ConfigurationContext() {

                @Override
                public ConfigurationStreamPacket newPacket() {
                    return new ConfigurationStreamPacket(type, Unpooled.buffer());
                }

                @Override
                public ServerConfigurationPacketListener packetListener() {
                    return listener;
                }
            };
            return factory.apply(configurationType, context);
        }
    }
    // endregion

    /**
     * A helpful interface for the gameplay packet handler type.
     */
    public interface PlayHandler extends BiConsumer<RegistryFriendlyByteBuf, IPayloadContext> {

        @Override
        void accept(RegistryFriendlyByteBuf packet, IPayloadContext ctx);
    }

    /**
     * A helpful interface for the configuration phase packet handler type.
     */
    public interface ConfigurationHandler extends BiConsumer<FriendlyByteBuf, IPayloadContext> {

        @Override
        void accept(FriendlyByteBuf packet, IPayloadContext ctx);
    }

    /**
     * The context available when executing a configuration task.
     */
    public interface ConfigurationContext {

        /**
         * Create a new packet to be sent on the configuration channel.
         *
         * @return The packet.
         */
        ConfigurationStreamPacket newPacket();

        /**
         * @return The active configuration packet listener.
         */
        ServerConfigurationPacketListener packetListener();
    }

    /**
     * A factory for constructing {@link ICustomConfigurationTask} instances. Usually a record, storing both the type and context.
     */
    public interface ConfigurationTaskFactory extends BiFunction<ConfigurationTask.Type, ConfigurationContext, ICustomConfigurationTask> { }

    public sealed interface ConfigurationPacketHandle { }

    /**
     * A handle for constructing client-bound packets.
     */
    public sealed interface ClientPacketHandle {

        /**
         * Construct a packet bound for a client.
         *
         * @param registryAccess The {@link RegistryAccess} instance.
         * @return The packet.
         */
        StreamPacket.ToClient toClient(RegistryAccess registryAccess);

        /**
         * Construct a packet bound for a client.
         *
         * @param level A level to grab the {@link RegistryAccess} from.
         * @return The packet.
         */
        default StreamPacket.ToClient toClient(Level level) {
            return toClient(level.registryAccess());
        }

        /**
         * Construct a packet bound for a client.
         *
         * @param player A player to grab the {@link RegistryAccess} from.
         * @return The packet.
         */
        default StreamPacket.ToClient toClient(Player player) {
            return toClient(player.registryAccess());
        }

        /**
         * Construct a packet bound for a client.
         *
         * @param tile A BlockEntity to grab the {@link RegistryAccess} from.
         * @return The packet.
         */
        default StreamPacket.ToClient toClient(BlockEntity tile) {
            return toClient(tile.getLevel());
        }

        /**
         * Construct a packet bound for a client.
         *
         * @param entity An entity to grab the {@link RegistryAccess} from.
         * @return The packet.
         */
        default StreamPacket.ToClient toClient(Entity entity) {
            return toClient(entity.level());
        }
    }

    /**
     * A handle for constructing server-bound packets.
     */
    public sealed interface ServerPacketHandle {

        /**
         * Construct a packet bound for the server.
         *
         * @return The packet.
         */
        StreamPacket.ToServer toServer();
    }

    /**
     * A handle for constructing a packet bound for a client or the server.
     */
    public sealed interface BidirectionalPacketHandle extends ClientPacketHandle, ServerPacketHandle { }

    private enum PacketKind {
        CLIENT_HANDLED,
        SERVER_HANDLED,
        BI_DIRECTIONAL
    }
}
