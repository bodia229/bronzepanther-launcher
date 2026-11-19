package community.bronzepanther.mixin;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** ExtendedAE 26.1-1.0.3 registers block items in both item and block loops. */
@Pseudo
@Mixin(targets="com.glodblock.github.extendedae.common.EAERegistryHandler",remap=false)
public abstract class ExtendedAECreativeTabMixin {
    @ModifyVariable(method="lambda$registerTab$1",at=@At("HEAD"),argsOnly=true)
    private CreativeModeTab.Output bp$uniqueTabEntries(CreativeModeTab.Output output) {
        var seen=ItemStackLinkedSet.createTypeAndComponentsSet();
        return (stack,visibility)->{
            if(seen.add(stack.copy()))output.accept(stack,visibility);
        };
    }
}
