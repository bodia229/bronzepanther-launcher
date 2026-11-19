package com.brandon3055.draconicevolution.client.render.particle;

import com.brandon3055.brandonscore.client.particle.IntParticleData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by brandon3055 on 2/5/2016.
 * The particle used to render the beams on the Energy Core
 */
public class ParticleLineIndicator extends SingleQuadParticle {
    public static final net.minecraft.client.particle.SingleQuadParticle.Layer PARTICLE_NO_DEPTH_NO_LIGHT =
            com.brandon3055.draconicevolution.client.DEParticleLayers.ADDITIVE_NO_DEPTH_WRITE;

    public ParticleLineIndicator(Level worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, SpriteSet initialSprites) {
        super((ClientLevel)worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, initialSprites.first());
        this.xd = xSpeedIn;
        this.yd = ySpeedIn;
        this.zd = zSpeedIn;
    }

    @Override
    public net.minecraft.client.particle.SingleQuadParticle.Layer getLayer() {
        return PARTICLE_NO_DEPTH_NO_LIGHT;
    }

    @Override
    protected int getLightCoords(float p_107249_) {
        return 255;
    }


    public static class Factory implements ParticleProvider<IntParticleData> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet p_i50823_1_) {
            this.spriteSet = p_i50823_1_;
        }

        @Override
        public net.minecraft.client.particle.SingleQuadParticle createParticle(IntParticleData data, ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, net.minecraft.util.RandomSource providerRandom) {
            ParticleLineIndicator particle = new ParticleLineIndicator(world, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
            particle.setSprite(spriteSet.get(providerRandom));

            List<Integer> list = data.get();
            if (list.size() >= 3) {
                particle.setColor(list.get(0) / 255F, list.get(1) / 255F, list.get(2) / 255F);
            }
            if (list.size() >= 4) {
                particle.setLifetime(list.get(3));
            }

            return particle;
        }
    }
}
