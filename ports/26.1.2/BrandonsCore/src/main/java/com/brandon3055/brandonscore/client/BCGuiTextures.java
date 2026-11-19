package com.brandon3055.brandonscore.client;

import codechicken.lib.gui.modular.SpriteSupplier;
import com.brandon3055.brandonscore.BCConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.function.Supplier;

import static com.brandon3055.brandonscore.BrandonsCore.MODID;

/**
 * Created by brandon3055 on 3/09/2016.
 */
public class BCGuiTextures {

    private static final Map<String,SpriteId> SPRITES=new ConcurrentHashMap<>();

    public static TextureAtlasSprite get(String texture) {
        SpriteId id=SPRITES.computeIfAbsent(texture,path->new SpriteId(Sheets.GUI_SHEET,Identifier.fromNamespaceAndPath(MODID,path)));
        // Resolve from the current atlas, so resource-pack reloads cannot leave stale sprites.
        return Minecraft.getInstance().getAtlasManager().get(id);
    }

    public static SpriteSupplier getter(Supplier<String> texture) {
        return ()->get(texture.get());
    }

    public static SpriteSupplier getter(String texture) {
        return () -> get(texture);
    }

    public static TextureAtlasSprite getThemed(String location) {
        return get((BCConfig.darkMode ? "dark/" : "light/") + location);
    }

    public static SpriteSupplier themedGetter(String location) {
        return () -> get((BCConfig.darkMode ? "dark/" : "light/") + location);
    }
}
