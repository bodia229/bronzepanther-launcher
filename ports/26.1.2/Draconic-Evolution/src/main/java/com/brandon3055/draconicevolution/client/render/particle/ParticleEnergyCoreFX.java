package com.brandon3055.draconicevolution.client.render.particle;

import com.brandon3055.brandonscore.client.particle.IntParticleData;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.brandonscore.utils.BCProfiler;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;

import java.util.List;

/**
 * Created by brandon3055 on 2/5/2016.
 * The particle used to render the beams on the Energy Core
 */
public class ParticleEnergyCoreFX extends SingleQuadParticle {

    public static final net.minecraft.client.particle.SingleQuadParticle.Layer PARTICLE_NO_DEPTH_NO_LIGHT =
            com.brandon3055.draconicevolution.client.DEParticleLayers.TRANSLUCENT_NO_DEPTH_WRITE;

    public Vec3D targetPos;
    public boolean toCore = false;
    public int startRotation = 0;
    private Direction.Axis direction;
    public boolean isLargeStabilizer = false;
    private final SpriteSet spriteSet;

    public ParticleEnergyCoreFX(ClientLevel world, double xPos, double yPos, double zPos, Vec3D targetPos, SpriteSet spriteSet) {
        super(world, xPos, yPos, zPos, spriteSet.first());
        this.targetPos = targetPos;
        this.spriteSet = spriteSet;
        setSprite(spriteSet.get(world.getRandom()));
        hasPhysics = false;
        Vec3D dir = Vec3D.getDirectionVec(new Vec3D(xPos, yPos, zPos), targetPos);
        this.direction = Direction.getApproximateNearest((float) dir.x, (float) dir.y, (float) dir.z).getAxis();
        lifetime = 40;
    }

    @Override
    public net.minecraft.client.particle.SingleQuadParticle.Layer getLayer() {
        return PARTICLE_NO_DEPTH_NO_LIGHT;
    }

    @Override
    protected int getLightCoords(float p_107249_) {
        return 255;
    }

    @Override
    public void tick() {
        BCProfiler.TICK.start("core_fx_update");
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        Vec3D tPos = this.targetPos.copy();
        setSprite(spriteSet.get(level.getRandom()));

        if (toCore) {
            double rotation = ClientEventHandler.elapsedTicks;
            double offsetX = Math.sin((rotation / 180D * Math.PI) + (startRotation / 100D));
            double offsetY = Math.cos((rotation / 180D * Math.PI) + (startRotation / 100D));

            double d = isLargeStabilizer ? 1.8 : 0.2;
            if (direction == Direction.Axis.Z) {
                tPos.add(offsetX * d, offsetY * d, 0);
            }
            else if (direction == Direction.Axis.Y) {
                tPos.add(offsetX * d, 0, offsetY * d);
            }
            else if (direction == Direction.Axis.X) {
                tPos.add(0, offsetY * d, offsetX * d);
            }
        }

        Vec3D dir = Vec3D.getDirectionVec(new Vec3D(x, y, z), tPos);
        double speed = (toCore ? 0.5D : 0.25D);
        xd = dir.x * speed;
        yd = dir.y * speed;
        zd = dir.z * speed;
        move(xd, yd, zd);

        if (age++ > lifetime || Utils.getDistanceSq(x, y, z, tPos.x, tPos.y, tPos.z) < 0.01) {
            remove();
        }
        BCProfiler.TICK.stop();
    }

    public static class Factory implements ParticleProvider<IntParticleData> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet p_i50823_1_) {
            this.spriteSet = p_i50823_1_;
        }

        @Override
        public net.minecraft.client.particle.SingleQuadParticle createParticle(IntParticleData data, ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, net.minecraft.util.RandomSource providerRandom) {
            ParticleEnergyCoreFX particle = new ParticleEnergyCoreFX(world, x, y, z, new Vec3D(xSpeed, ySpeed, zSpeed), spriteSet);
            List<Integer> list = data.get();
            particle.toCore = list.size() >= 1 && list.get(0) == 1;
            particle.startRotation = list.size() >= 2 ? list.get(1) : 0;
            particle.isLargeStabilizer = list.size() >= 3 && list.get(2) == 1;
            particle.scale(particle.isLargeStabilizer ? 2 : 1);
            return particle;
        }
    }
}
