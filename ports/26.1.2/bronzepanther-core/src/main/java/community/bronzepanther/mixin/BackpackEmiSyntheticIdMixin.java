package community.bronzepanther.mixin;

import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** JEI generates dye examples; they are not server recipe-manager entries. */
@Pseudo
@Mixin(targets="dev.emi.emi.api.recipe.EmiCraftingRecipe",remap=false)
public abstract class BackpackEmiSyntheticIdMixin {
    @Inject(method="getId",at=@At("RETURN"),cancellable=true)
    private void bp$markGeneratedDyeExamples(CallbackInfoReturnable<Identifier> result) {
        var id=result.getReturnValue();
        if(id!=null&&id.getNamespace().equals("sophisticatedbackpacks")&&
                (id.getPath().startsWith("single_color_")||id.getPath().startsWith("multiple_colors_")))
            result.setReturnValue(Identifier.fromNamespaceAndPath(id.getNamespace(),"/"+id.getPath()));
    }
}
