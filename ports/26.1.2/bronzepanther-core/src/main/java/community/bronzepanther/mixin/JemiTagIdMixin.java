package community.bronzepanther.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep item and block tag pages: JEI's tag location alone is not a unique ID. */
@Pseudo
@Mixin(targets="dev.emi.emi.jemi.JemiRecipe",remap=false)
public abstract class JemiTagIdMixin {
    @Shadow public Identifier id;
    @Shadow public Object recipe;
    @Inject(method="<init>",at=@At("RETURN"))
    private void bp$includeTagRegistry(CallbackInfo ci) {
        if(recipe==null||!recipe.getClass().getName().equals("mezz.jei.library.plugins.jei.tags.TagInfoRecipe"))return;
        try {
            var tag=(TagKey<?>)recipe.getClass().getMethod("getTag").invoke(recipe);
            var registry=tag.registry().identifier();
            var name=tag.location();
            id=Identifier.fromNamespaceAndPath("jei","/tag/"+registry.getNamespace()+"/"+registry.getPath()+"/"+name.getNamespace()+"/"+name.getPath());
        } catch(ReflectiveOperationException failure) {
            throw new IllegalStateException("JEI tag ID adapter failed",failure);
        }
    }
}
