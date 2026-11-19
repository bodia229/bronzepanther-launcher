package com.brandon3055.draconicevolution.client.render.particle;

import com.brandon3055.brandonscore.client.particle.BCParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.phys.Vec3;

/**
 * Compatibility for unregistered legacy effects whose rendering was already commented out upstream.
 * Active DE particle providers and vertex effects use their separate native render groups.
 */
@Deprecated
abstract class InactiveLegacyParticle extends BCParticle {
    protected InactiveLegacyParticle(ClientLevel level,Vec3 pos) { super(level,pos); }
    protected InactiveLegacyParticle(ClientLevel level,Vec3 pos,Vec3 speed) { super(level,pos,speed); }
    @Override public final ParticleRenderType getGroup() { return ParticleRenderType.NO_RENDER; }
    public void setAlpha(float alpha) { this.alpha=alpha; }
}
