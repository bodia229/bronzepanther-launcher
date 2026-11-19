package com.brandon3055.draconicevolution.client;

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

public final class DEGuiTextures {
    private static final Map<String,SpriteId> SPRITES=new ConcurrentHashMap<>();
    private DEGuiTextures(){}
    public static TextureAtlasSprite get(String texture){
        SpriteId id=SPRITES.computeIfAbsent(texture,p->new SpriteId(Sheets.GUI_SHEET,Identifier.fromNamespaceAndPath("draconicevolution",p)));
        return Minecraft.getInstance().getAtlasManager().get(id);
    }
    public static SpriteSupplier getter(Supplier<String> texture){return ()->get(texture.get());}
    public static SpriteSupplier getter(String texture){return ()->get(texture);}
    public static TextureAtlasSprite getThemed(String texture){return get((BCConfig.darkMode?"dark/":"light/")+texture);}
    public static SpriteSupplier themedGetter(String texture){return ()->getThemed(texture);}
}
