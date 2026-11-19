package com.brandon3055.draconicevolution.client.render.particle;

import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.utils.MathUtils;
import com.brandon3055.draconicevolution.client.AtlasTextureHelper;
import com.brandon3055.draconicevolution.entity.guardian.control.ChargeUpPhase;
import com.brandon3055.draconicevolution.entity.guardian.control.PhaseManager;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import org.joml.Quaternionf;
import org.joml.Vector3f;


public class GuardianChargeParticle extends SingleQuadParticle {

    private Vector3 startPos;
    private Vector3 endPos;
    private double angularPos;
    private PhaseManager phaseManager;
    public TextureAtlasSprite sprite = AtlasTextureHelper.ORB_PARTICLE;

    public GuardianChargeParticle(ClientLevel world, Vector3 startPos, Vector3 endPos, double angularPos, int life, PhaseManager phaseManager) {
        super(world, startPos.x, startPos.y, startPos.z, AtlasTextureHelper.ORB_PARTICLE);
        this.startPos = startPos;
        this.endPos = endPos;
        this.angularPos = angularPos;
        this.phaseManager = phaseManager;
        lifetime = life * 4;
        setColor(0.75F, 0F, 0F);
        scale(5);
    }
//
//    @Override
//    public boolean shouldCull() {
//        return false;
//    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        Vector3 position = orbitPosition(0);
        setPos(position.x, position.y, position.z);
        if (!(phaseManager.getCurrentPhase() instanceof ChargeUpPhase)) {
            alpha -= 0.1;
        }
        if (this.age++ >= this.lifetime || alpha <= 0) {
            this.remove();
        }
    }

    @Override
    public void extract(net.minecraft.client.renderer.state.level.QuadParticleRenderState state, Camera camera, float partialTicks) {
        if (age + partialTicks > lifetime) return;
        Vec3 cameraPosition = camera.position();
        Vector3 position = orbitPosition(partialTicks);
        Quaternionf rotation = new Quaternionf(camera.rotation());
        if (roll != 0) rotation.rotateZ(Mth.lerp(partialTicks, oRoll, roll));
        state.add(getLayer(), (float) (position.x - cameraPosition.x()), (float) (position.y - cameraPosition.y()),
                (float) (position.z - cameraPosition.z()), rotation.x, rotation.y, rotation.z, rotation.w,
                getQuadSize(partialTicks), getU0(), getU1(), getV0(), getV1(),
                net.minecraft.util.ARGB.colorFromFloat(alpha, rCol, gCol, bCol), 240);
    }

    private Vector3 orbitPosition(float partialTicks) {
        float animation = (age + partialTicks) / lifetime;
        Vector3 position = MathUtils.interpolateVec3(startPos, endPos, animation);
        float radius = animation * 2 + Mth.sin(animation * (float) Math.PI) * 5;
        position.x += Mth.sin((float) (angularPos * Math.PI * 2) + animation) * radius;
        position.z += Mth.cos((float) (angularPos * Math.PI * 2) + animation) * radius;
        return position;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() { return AtlasTextureHelper.PARTICLE_SHEET_TRANSLUCENT; }

    @Override
    protected float getU0() {
        return sprite.getU0();
    }

    @Override
    protected float getU1() {
        return sprite.getU1();
    }

    @Override
    protected float getV0() {
        return sprite.getV0();
    }

    @Override
    protected float getV1() {
        return sprite.getV1();
    }
}