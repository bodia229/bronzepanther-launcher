package community.bronzepanther.mixin;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Apply the pack's shared selection before Minecraft builds its resource stack. */
@Mixin(Options.class)
public abstract class ResourcePackPolicyMixin {
    @Shadow public List<String> resourcePacks;
    @Shadow public List<String> incompatibleResourcePacks;

    @Inject(method="load",at=@At("TAIL"))
    private void bp$sharedTexturePacks(CallbackInfo ci) {
        try(var stream=community.bronzepanther.BronzePanther.class.getResourceAsStream("/bronzepanther-resourcepacks.json")) {
            if(stream==null)throw new IllegalStateException("Missing shared resource pack policy");
            String[] selected=new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),String[].class);
            if(selected==null||selected.length==0)throw new IllegalStateException("Empty resource pack policy");
            resourcePacks=new ArrayList<>(List.of(selected));
            incompatibleResourcePacks=new ArrayList<>();
            com.mojang.logging.LogUtils.getLogger().info("BronzePanther shared resource packs selected: {}",resourcePacks);
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot read shared resource pack policy",e);}
    }
}
