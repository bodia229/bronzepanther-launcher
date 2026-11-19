package com.brandon3055.brandonscore.mixin;

import com.brandon3055.brandonscore.api.CustomFlightHooks;
import com.brandon3055.brandonscore.api.ElytraEnabledItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native eligibility is also used by Player and LocalPlayer in 26.1. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow protected abstract boolean canGlide();
    private LivingEntity brandonscore$self(){return (LivingEntity)(Object)this;}

    @Redirect(method="canGlide()Z",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;canGlideUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;)Z"))
    private boolean brandonscore$customChestEligibility(ItemStack stack,EquipmentSlot slot){
        if(slot==EquipmentSlot.CHEST&&stack.getItem() instanceof ElytraEnabledItem)return CustomFlightHooks.enabled(stack,brandonscore$self());
        return LivingEntity.canGlideUsing(stack,slot);
    }

    @Inject(method="canGlide()Z",at=@At("RETURN"),cancellable=true)
    private void brandonscore$accessoryEligibility(CallbackInfoReturnable<Boolean> result){
        var entity=brandonscore$self();
        if(!result.getReturnValue()&&!entity.onGround()&&!entity.isPassenger()&&!entity.hasEffect(MobEffects.LEVITATION)
                &&!CustomFlightHooks.accessory(entity).isEmpty())result.setReturnValue(true);
    }

    @Inject(method="updateFallFlying()V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;canGlide()Z"),cancellable=true)
    private void brandonscore$customFlightTick(CallbackInfo callback){
        var entity=brandonscore$self();if(!entity.isFallFlying())return;
        var stack=CustomFlightHooks.active(entity);
        if(!stack.isEmpty()&&canGlide()){
            boolean keepFlying=((ElytraEnabledItem)stack.getItem()).elytraFlightTickBC(stack,entity,entity.getFallFlyingTicks());
            if(!keepFlying)((EntityAccessor)(Object)entity).invokeSetSharedFlag(7,false);
            // Custom energy/durability handling replaces vanilla wear for this item.
            callback.cancel();
        }
    }
}
