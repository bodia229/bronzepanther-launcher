//package com.brandon3055.brandonscore.client.render;
//
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.texture.TextureAtlas;
//import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//import net.minecraft.client.resources.TextureAtlasHolder;
//import net.minecraft.resources.Identifier;
//import net.minecraft.server.packs.resources.ReloadableResourceManager;
//import net.minecraft.server.packs.resources.ResourceManager;
//import net.minecraft.util.profiling.ProfilerFiller;
//
//import java.util.Map;
//import java.util.Objects;
//import java.util.function.Consumer;
//import java.util.stream.Stream;
//
///**
// * Created by brandon3055 on 6/5/20.
// */
//public class CustomSpriteUploader extends TextureAtlasHolder {
//    private final String prefix;
//    private final Map<Identifier, Consumer<TextureAtlasSprite>> registeredSprites;
//    private Runnable reloadListener = null;
//
//    public CustomSpriteUploader(Map<Identifier, Consumer<TextureAtlasSprite>> registeredSprites, Identifier atlasLocation, String prefix) {
//        super(Minecraft.getInstance().textureManager, atlasLocation, prefix);
//        this.prefix = prefix;
//        this.registeredSprites = registeredSprites;
//        ReloadableResourceManager resourceManager = (ReloadableResourceManager) Minecraft.getInstance().getResourceManager();
//        resourceManager.registerReloadListener(this);
//    }
//
//    public CustomSpriteUploader(Map<Identifier, Consumer<TextureAtlasSprite>> registeredSprites, Identifier atlasLocation) {
//        this(registeredSprites, atlasLocation, null);
//    }
//
//    @Override
//    protected Stream<Identifier> getResourcesToLoad() {
//        return registeredSprites.keySet().stream();
//    }
//
//    @Override
//    public Identifier resolveLocation(Identifier location) {
//        if (prefix != null) {
//            return new Identifier(location.getNamespace(), this.prefix + "/" + location.getPath());
//        }
//
//        return location;
//    }
//
//    @Override
//    public TextureAtlasSprite getSprite(Identifier locationIn) {
//        return super.getSprite(locationIn);
//    }
//
//    @Override
//    protected void apply(TextureAtlas.Preparations preparations, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
//        super.apply(preparations, resourceManager, profilerFiller);
//        registeredSprites.entrySet()
//                .stream()
//                .filter(Objects::nonNull)
//                .filter(e -> e.getValue() != null)
//                .forEach(e -> e.getValue().accept(getSprite(e.getKey())));
//
//        if (reloadListener != null) {
//            reloadListener.run();
//        }
//    }
//
//    public void addReloadListener(Runnable reloadListener) {
//        this.reloadListener = this.reloadListener == null ? reloadListener : () -> {
//            this.reloadListener.run();
//            reloadListener.run();
//        };
//    }
//}
