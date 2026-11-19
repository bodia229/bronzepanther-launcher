package com.brandon3055.draconicevolution.client;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import codechicken.lib.gui.modular.SpriteSupplier;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.client.atlas.MultiModAtlasHolder;
import com.brandon3055.draconicevolution.init.DEModules;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.brandon3055.draconicevolution.DraconicEvolution.MODID;

public class ModuleTextures {
   private static final MultiModAtlasHolder ATLAS_HOLDER = new MultiModAtlasHolder(MODID, "textures/atlas/module.png", "module", DEModules.MODULE_PROVIDING_MODS);
   private static final Map<String, TextureAtlasSprite> MATERIAL_CACHE = new HashMap<>();

   public static MultiModAtlasHolder getAtlasHolder() {
      return ATLAS_HOLDER;
   }

   protected static TextureAtlasSprite get(Identifier texture) {
      return getUncached(texture);
   }

   protected static TextureAtlasSprite getUncached(Identifier texture) {
      return ATLAS_HOLDER.getSprite(Identifier.fromNamespaceAndPath(texture.getNamespace(), "module/" + texture.getPath()));
   }

   public static TextureAtlasSprite get(Module<?> module) {
      return get(Objects.requireNonNull(DEModules.REGISTRY.getKey(module)));
   }
}