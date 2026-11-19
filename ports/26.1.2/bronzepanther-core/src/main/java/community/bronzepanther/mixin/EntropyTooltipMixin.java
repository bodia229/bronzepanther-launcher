package community.bronzepanther.mixin;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(targets="appeng.client.integrations.jei.EntropyManipulatorCategory",remap=false)
public abstract class EntropyTooltipMixin {
    @Redirect(method="lambda$setFluidOrBlockSlot$0",at=@At(value="INVOKE",target="Ljava/util/List;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private static Object bp$replaceOrAppend(List<Object> lines,int index,Object value){
        if(index<lines.size())return lines.set(index,value);
        if(index==lines.size())lines.add(value);
        return null;
    }
}
