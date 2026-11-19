package com.brandon3055.draconicevolution.mixin;

import com.brandon3055.brandonscore.network.BCoreNetwork;
import com.brandon3055.draconicevolution.entity.projectile.DraconicArrowEntity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

/** Preserve high-precision arrow motion without relying on invocation ordinals. */
@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {
    @Shadow @Final private Entity entity;
    @ModifyArg(method="sendChanges()V", at=@At(value="INVOKE", target="Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V"), index=0)
    private Packet<? super ClientGamePacketListener> draconic$trackingMotion(Packet<? super ClientGamePacketListener> packet) {
        return draconic$motionPacket(packet);
    }
    @ModifyArg(method="sendChanges()V", at=@At(value="INVOKE", target="Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersAndSelf(Lnet/minecraft/network/protocol/Packet;)V"), index=0)
    private Packet<? super ClientGamePacketListener> draconic$selfMotion(Packet<? super ClientGamePacketListener> packet) {
        return draconic$motionPacket(packet);
    }
    @Unique @SuppressWarnings("unchecked")
    private Packet<? super ClientGamePacketListener> draconic$motionPacket(Packet<? super ClientGamePacketListener> packet) {
        if(!(entity instanceof DraconicArrowEntity))return packet;
        if(packet instanceof ClientboundSetEntityMotionPacket)
            return (Packet<? super ClientGamePacketListener>)BCoreNetwork.sendEntityVelocity(entity,false);
        if(packet instanceof ClientboundMoveEntityPacket.PosRot)
            return (Packet<? super ClientGamePacketListener>)BCoreNetwork.sendEntityVelocity(entity,true);
        return packet;
    }
    @Inject(method="sendPairingData", at=@At("RETURN"))
    @SuppressWarnings("unchecked")
    private void draconic$pairingMotion(ServerPlayer player,Consumer<Packet<? super ClientGamePacketListener>> broadcast,CallbackInfo ci) {
        if(entity instanceof DraconicArrowEntity)
            broadcast.accept((Packet<? super ClientGamePacketListener>)BCoreNetwork.sendEntityVelocity(entity,false));
    }
}
