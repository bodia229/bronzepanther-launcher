package com.brandon3055.brandonscore.handlers;

import com.brandon3055.brandonscore.BrandonsCore;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Created by brandon3055 on 24/01/2023
 */
public class SighEditHandler {

    private static final CrashLock LOCK = new CrashLock("Already Initialized");
    private static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.level.gamerules.GameRule<?>> RULES =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.GAME_RULE,BrandonsCore.MODID);
    private static final java.util.function.Supplier<net.minecraft.world.level.gamerules.GameRule<Boolean>> ALLOW_SIGN_EDIT = RULES.register("allow_sign_editing",()->
            new net.minecraft.world.level.gamerules.GameRule<>(net.minecraft.world.level.gamerules.GameRuleCategory.MISC,
                    net.minecraft.world.level.gamerules.GameRuleType.BOOL,com.mojang.brigadier.arguments.BoolArgumentType.bool(),
                    net.minecraft.world.level.gamerules.GameRuleTypeVisitor::visitBoolean,com.mojang.serialization.Codec.BOOL,
                    value->value?1:0,false,net.minecraft.world.flag.FeatureFlagSet.of()));

    public static void init(net.neoforged.bus.api.IEventBus modBus) {
        LOCK.lock();
        RULES.register(modBus);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, SighEditHandler::onBlockInteract);
    }

    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel) || event.isCanceled()) {
            return;
        }

        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !player.getAbilities().mayBuild || !serverLevel.getGameRules().get(ALLOW_SIGN_EDIT.get()) || !player.getItemInHand(event.getHand()).isEmpty()) {
            return;
        }

        BlockEntity entity = level.getBlockEntity(event.getHitVec().getBlockPos());
        if (entity instanceof SignBlockEntity signEntity) {
//            signEntity.setEditable(true);
            player.openTextEdit(signEntity, true); //TODO Side
            event.setCanceled(true);
        }
    }
}
