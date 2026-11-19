package codechicken.lib.gui.modular;

import com.google.common.base.Suppliers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A supplier for sprites.
 * <p>
 * May return null to indicate the sprite is not available. This will usually disable
 * the thing being rendered.
 * <p>
 * Created by covers1624 on 4/1/26.
 */
@FunctionalInterface
public interface SpriteSupplier extends Supplier<TextureAtlasSprite> {

    SpriteSupplier EMPTY = () -> null;

    @Override
    @Nullable
    TextureAtlasSprite get();

    default void ifPresent(Consumer<TextureAtlasSprite> cons) {
        var tex = get();
        if (tex != null) {
            cons.accept(tex);
        }
    }

    static SpriteSupplier gui(Identifier spriteLoc) {
        return of(Sheets.GUI_SHEET, spriteLoc);
    }

    static SpriteSupplier of(Identifier atlasLoc, Identifier spriteLoc) {
        return () -> Minecraft.getInstance().getAtlasManager().get(new SpriteId(atlasLoc, spriteLoc));
    }

    static SpriteSupplier gui(Supplier<Identifier> sup) {
        return of(() -> new SpriteId(Sheets.GUI_SHEET, sup.get()));
    }

    static SpriteSupplier of(Supplier<SpriteId> sup) {
        return () -> Minecraft.getInstance().getAtlasManager().get(sup.get());
    }
}
