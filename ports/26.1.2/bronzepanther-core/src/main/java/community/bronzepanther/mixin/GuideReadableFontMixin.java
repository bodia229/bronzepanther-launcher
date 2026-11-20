package community.bronzepanther.mixin;

import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keep guide body text on the same readable font as normal Minecraft UI. */
@Pseudo
@Mixin(targets="guideme.document.DefaultStyles",remap=false)
public abstract class GuideReadableFontMixin {
    @ModifyArg(method="<clinit>",at=@At(value="INVOKE",target="Lguideme/style/ResolvedTextStyle;<init>(FZZZZZLnet/minecraft/network/chat/FontDescription;Lguideme/color/ColorValue;Lguideme/style/WhiteSpaceMode;Lguideme/style/TextAlignment;Z)V"),index=6)
    private static FontDescription bp$readableBaseFont(FontDescription original){return FontDescription.DEFAULT;}

    @ModifyArg(method="<clinit>",at=@At(value="INVOKE",target="Lguideme/style/TextStyle$Builder;font(Lnet/minecraft/network/chat/FontDescription;)Lguideme/style/TextStyle$Builder;"),index=0)
    private static FontDescription bp$readableGuideStyles(FontDescription original){return FontDescription.DEFAULT;}
}
