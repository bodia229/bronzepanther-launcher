package com.brandon3055.draconicevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Client field access with common return types, avoiding subtype checks in common-class verification. */
public final class DEClientAccess {
    private DEClientAccess() {}
    public static void openAuthorGuide() {
        var mc=Minecraft.getInstance();
        mc.setScreen(new com.brandon3055.draconicevolution.client.gui.PIDocumentationScreen(mc.screen));
    }
    public static void portalParticle(Level level,codechicken.lib.vec.Vector3 pos,codechicken.lib.vec.Vector3 target) {
        Minecraft.getInstance().particleEngine.add(new com.brandon3055.draconicevolution.client.render.particle.ParticlePortal((net.minecraft.client.multiplayer.ClientLevel)level,pos,target));
    }
    public static com.brandon3055.draconicevolution.client.render.effect.CrystalFXBase crystalIo(com.brandon3055.draconicevolution.blocks.energynet.tileentity.TileCrystalDirectIO tile) {
        return new com.brandon3055.draconicevolution.client.render.effect.CrystalFXIO((net.minecraft.client.multiplayer.ClientLevel)tile.getLevel(),tile);
    }
    public static com.brandon3055.draconicevolution.client.render.effect.CrystalFXBase crystalRing(com.brandon3055.draconicevolution.blocks.energynet.tileentity.TileCrystalBase tile) {
        return new com.brandon3055.draconicevolution.client.render.effect.CrystalFXRing((net.minecraft.client.multiplayer.ClientLevel)tile.getLevel(),tile);
    }
    public static Player player() { return Minecraft.getInstance().player; }
    public static Level level() { return Minecraft.getInstance().level; }
}
