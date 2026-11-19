package com.brandon3055.draconicevolution.client;

import com.brandon3055.draconicevolution.DraconicEvolution;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by brandon3055 on 02/02/2024
 */
public class AtlasTextureHelper {
    private static final Map<Identifier, Function<Identifier, TextureAtlasSprite>> ATLAS_CACHE = new HashMap<>();

    public static TextureAtlasSprite[] ENERGY_PARTICLE = new TextureAtlasSprite[5];
    public static TextureAtlasSprite[] SPARK_PARTICLE = new TextureAtlasSprite[7];
    public static TextureAtlasSprite[] SPELL_PARTICLE = new TextureAtlasSprite[7];
    public static TextureAtlasSprite[] MIXED_PARTICLE;

    public static TextureAtlasSprite ORB_PARTICLE;
    public static TextureAtlasSprite PORTAL_PARTICLE;
    public static TextureAtlasSprite ENERGY_CORE_OVERLAY;

    public static final net.minecraft.client.particle.SingleQuadParticle.Layer PARTICLE_SHEET_TRANSLUCENT =
            DEParticleLayers.TRANSLUCENT_NO_DEPTH_WRITE;

    public static void init(IEventBus modBus) {
        modBus.addListener(AtlasTextureHelper::textureStitch);
    }

    private static void textureStitch(TextureAtlasStitchedEvent event) {
        TextureAtlas atlas = event.getAtlas();
        if (atlas.location().equals(TextureAtlas.LOCATION_PARTICLES)) {
            ATLAS_CACHE.clear();
            for (int i = 0; i < ENERGY_PARTICLE.length; i++) {
                ENERGY_PARTICLE[i] = atlas.getSprite(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "energy_" + i));
            }
            for (int i = 0; i < SPARK_PARTICLE.length; i++) {
                SPARK_PARTICLE[i] = atlas.getSprite(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "spark_" + i));
            }
            for (int i = 0; i < SPELL_PARTICLE.length; i++) {
                SPELL_PARTICLE[i] = atlas.getSprite(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "spell_" + i));
            }
            MIXED_PARTICLE = Stream.concat(Arrays.stream(SPARK_PARTICLE), Arrays.stream(SPELL_PARTICLE)).toArray(TextureAtlasSprite[]::new);

            ORB_PARTICLE = atlas.getSprite(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "white_orb"));
            PORTAL_PARTICLE = atlas.getSprite(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "portal"));
        }
        if (atlas.location().equals(TextureAtlas.LOCATION_BLOCKS)) {
            ENERGY_CORE_OVERLAY = atlas.getSprite(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "block/energy_core/energy_core_overlay"));
        }
    }
}
