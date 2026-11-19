package com.brandon3055.brandonscore.lib;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Native dimension transfer retains identity/data and honors cancellation without a clone fallback. */
public final class EntityTeleportBridge {
    private EntityTeleportBridge(){}
    public static Entity transfer(Entity entity,ServerLevel destination,Vec3 position,float yaw,float pitch){
        if(!entity.isAlive()||destination==null)return entity;
        Entity moved=entity.teleport(new TeleportTransition(destination,position,entity.getDeltaMovement(),yaw,pitch,TeleportTransition.PLACE_PORTAL_TICKET));
        return moved==null?entity:moved;
    }
}
