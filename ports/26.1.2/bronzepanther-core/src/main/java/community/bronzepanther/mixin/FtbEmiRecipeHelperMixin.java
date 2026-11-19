package community.bronzepanther.mixin;

import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supply the recipe helper FTB's item task buttons already call on left click. */
@Pseudo
@Mixin(targets="dev.ftb.mods.ftbquests.FTBQuests",remap=false)
public abstract class FtbEmiRecipeHelperMixin {
    @Unique private static final Map<Object,Object> bp$emiHelpers=new IdentityHashMap<>();
    @Inject(method="getRecipeModHelper",at=@At("RETURN"),cancellable=true)
    private static void bp$useEmiForQuestRecipes(CallbackInfoReturnable<Object> callback) {
        if(!net.neoforged.fml.ModList.get().isLoaded("emi"))return;
        Object original=callback.getReturnValue();
        Object helper=bp$emiHelpers.computeIfAbsent(original,delegate->{
            try {
                Class<?> api=Class.forName("dev.ftb.mods.ftbquests.integration.RecipeModHelper");
                return Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(proxy,method,args)->{
                    if(method.getDeclaringClass()==Object.class)return switch(method.getName()){
                        case "toString"->"BronzePanther FTB → EMI recipes";
                        case "hashCode"->System.identityHashCode(proxy);
                        case "equals"->proxy==args[0];
                        default->null;
                    };
                    if(method.getName().equals("getHelperName"))return "EMI";
                    if(method.getName().equals("isRecipeModAvailable"))return true;
                    if(method.getName().equals("showRecipes")&&args!=null&&args.length==1&&args[0] instanceof ItemStack stack){
                        Class<?> ingredient=Class.forName("dev.emi.emi.api.stack.EmiIngredient");
                        Object displayed=Class.forName("dev.emi.emi.api.stack.EmiStack").getMethod("of",ItemStack.class).invoke(null,stack);
                        Class.forName("dev.emi.emi.api.EmiApi").getMethod("displayRecipes",ingredient).invoke(null,displayed);
                        return null;
                    }
                    try{return method.invoke(delegate,args);}catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}
                });
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("FTB to EMI recipe helper could not initialize",failure);}
        });
        callback.setReturnValue(helper);
    }
}
