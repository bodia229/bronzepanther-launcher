package community.bronzepanther.mixin;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(targets="de.exitus.advancedpowah.compat.jade.AdvancedPowahJadePlugin",remap=false)
public abstract class PowahJadeMixin {
    @Unique private static final Map<Object,Object> bp$serverProviders=new IdentityHashMap<>();
    @Redirect(method="register",at=@At(value="INVOKE",target="Lsnownee/jade/api/IWailaCommonRegistration;registerBlockDataProvider(Lsnownee/jade/api/IServerDataProvider;Ljava/lang/Class;)V"))
    private void bp$separateServerProvider(@Coerce Object registration,@Coerce Object provider,Class<?> block){
        try{
            Class<?> api=Class.forName("snownee.jade.api.IServerDataProvider");
            Object serverOnly=bp$serverProviders.computeIfAbsent(provider,key->Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(proxy,method,args)->{
                if(method.getDeclaringClass()==Object.class)return switch(method.getName()){
                    case "equals"->proxy==args[0];case "hashCode"->System.identityHashCode(proxy);case "toString"->"BronzePanther server provider: "+key;default->null;};
                try{return method.invoke(key,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
            }));
            Class.forName("snownee.jade.api.IWailaCommonRegistration").getMethod("registerBlockDataProvider",api,Class.class).invoke(registration,serverOnly,block);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Advanced Powah Jade adapter failed",e);}
    }
}
