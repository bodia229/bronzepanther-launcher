package community.bronzepanther.mixin;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.registry.EmiRecipes;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Current registration batch, including author integrations before indexing. */
@Mixin(value=EmiRecipes.class,remap=false)
public interface EmiRecipeListAccess {
    @Accessor("recipes")
    static List<EmiRecipe> bp$registeredRecipes(){throw new AssertionError("EMI accessor mixin was not applied");}
}
