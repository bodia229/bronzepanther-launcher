package com.brandon3055.draconicevolution.client.render.effect;

import codechicken.lib.colour.Colour;
import codechicken.lib.colour.ColourARGB;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCRenderState;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.brandon3055.draconicevolution.client.DEShaders;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.brandon3055.draconicevolution.DraconicEvolution.MODID;

/**
 * Created by brandon3055 on 12/02/2017.
 */
public class ReactorBeamFX extends DEVertexParticle {

    protected int ticksTillDeath = 0;
    protected float fxState;
    private float powerState;
    private TileReactorCore tile;
    private boolean isInjectorEffect;
    private Direction facing;
    private float dist;
    private static Colour fieldBeamColour = new ColourARGB(0x00B0FF);
    private static Colour extractBeamColour = new ColourARGB(0xff6600);
    private static Colour energyBeamColour = new ColourARGB(0xff0000);
    private int boltSeed = -1;
    private int ttl = 10;

    public static final RenderType REACTOR_BEAM_TYPE = com.brandon3055.draconicevolution.client.render.DEGeometryTypes.shader(
            "reactor_beam",DEShaders.reactorBeamShader,null,VertexFormat.Mode.TRIANGLE_STRIP,
            "translucent",false,true,false,false,false,false,false);

    public ReactorBeamFX(ClientLevel worldIn, Vec3D pos, Direction facing, TileReactorCore tile, boolean isInjectorEffect) {
        super(worldIn, pos.x, pos.y, pos.z);
        this.tile = tile;
        this.facing = facing;
        this.isInjectorEffect = isInjectorEffect;
        this.dist = (float) Utils.getDistance(pos, Vec3D.getCenter(tile.getBlockPos()));
        this.random.setSeed(worldIn.getRandom().nextLong());
        setBoundingBox(new AABB(x, y, z, tile.getBlockPos().getX(), tile.getBlockPos().getY(), tile.getBlockPos().getZ()));
    }

    public void updateFX(float fxState, float powerState) {
        this.fxState = fxState;
        if (powerState != this.powerState) {
            fieldBeamColour.r = (byte) (this.powerState * 0xFF);
            fieldBeamColour.g = (byte) ((1F - this.powerState) * 0xB0);
            fieldBeamColour.b = (byte) ((1F - this.powerState) * 0xFF);
        }
        this.powerState = powerState;
        ticksTillDeath = 4;
        if (ttl-- <= 0) {
            remove();
        }
    }

    @Override
    public void tick() {
        if (ticksTillDeath-- <= 0) {
            remove();
        }
        ttl = 10;
    }

    @Override
    public void render(VertexConsumer b, Camera renderInfo, float partialTicks) {
        if (tile.roller != null || true) {
            return;
        }

        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = 240;

        MultiBufferSource buffers = extractionBuffers();
        VertexConsumer buffer = bindBuffer(ccrs, buffers);

        Vec3 viewVec = renderInfo.position();
        Vec3D pos1 = new Vec3D(x - viewVec.x, y - viewVec.y, z - viewVec.z).offset(facing, -0.35D);

        float coreSize = (float) tile.getCoreDiameter() / 2.3F;


        if (isInjectorEffect) {
            DEShaders.reactorBeamType.glUniform1i(2);
            DEShaders.reactorBeamFade.glUniform1f(1F);
            DEShaders.reactorBeamPower.glUniform1f(fxState);
            DEShaders.reactorBeamStartup.glUniform1f(fxState);

            Vec3D pos2 = pos1.copy().offset(facing, 0.6D);
            renderShaderBeam(ccrs, buffer, pos1, 0.1F, 0.1F, 0.6F, true, false);
            ExtractedEquipmentGeometry.flush(buffers);
            buffer = bindBuffer(ccrs, buffers);

            DEShaders.reactorBeamFade.glUniform1f(0F);
            renderShaderBeam(ccrs, buffer, pos2, 0.1F, coreSize / 1.5, dist - (coreSize * 1.3F), false, false);
            ExtractedEquipmentGeometry.flush(buffers);

        } else {
            Vec3D pos2 = pos1.copy().offset(facing, 0.8D);

            //Inner Inner
            DEShaders.reactorBeamType.glUniform1i(1);
            DEShaders.reactorBeamFade.glUniform1f(1F);
            DEShaders.reactorBeamPower.glUniform1f((float) tile.animExtractState.get());
            DEShaders.reactorBeamStartup.glUniform1f((float) tile.animExtractState.get());

            renderShaderBeam(ccrs, buffer, pos1, 0.263F, 0.263F, 0.8F, true, false);
            ExtractedEquipmentGeometry.flush(buffers);
            buffer = bindBuffer(ccrs, buffers);

            DEShaders.reactorBeamFade.glUniform1f(0F);
            renderShaderBeam(ccrs, buffer, pos2, 0.263F, coreSize / 2, dist - (coreSize * 1.3F), false, false);
            ExtractedEquipmentGeometry.flush(buffers);
            buffer = bindBuffer(ccrs, buffers);


            //Draw Outer
            DEShaders.reactorBeamType.glUniform1i(0);
            DEShaders.reactorBeamFade.glUniform1f(1F);
            DEShaders.reactorBeamPower.glUniform1f(fxState);
            DEShaders.reactorBeamStartup.glUniform1f(fxState);

            renderShaderBeam(ccrs, buffer, pos1, 0.355F, 0.355F, 0.8F, true, false);
            ExtractedEquipmentGeometry.flush(buffers);
            buffer = bindBuffer(ccrs, buffers);

            DEShaders.reactorBeamFade.glUniform1f(0F);
            renderShaderBeam(ccrs, buffer, pos2, 0.355F, coreSize, dist - coreSize, false, true);
            ExtractedEquipmentGeometry.flush(buffers);
        }

        ccrs.reset();
    }

    private VertexConsumer bindBuffer(CCRenderState ccrs, MultiBufferSource buffers) {
        VertexConsumer buffer = buffers.getBuffer(REACTOR_BEAM_TYPE);
        ccrs.bind(buffer, DefaultVertexFormat.POSITION_TEX_COLOR);
        return buffer;
    }

    public void renderShaderBeam(CCRenderState ccrs, VertexConsumer buffer, Vec3D pos, double widthStart, double widthEnd, float length, boolean fadeReverse, boolean highRes) {
        float sides = highRes ? 599 : 99;
        for (int i = 0; i < sides + 1; i++) {
            double angle = (i / sides) * (Math.PI * 2);// + beamRotation;
            float sin = (float) MathHelper.sin(angle);
            float cos = (float) MathHelper.cos(angle);
            float texX = i / sides;
            Vec3D point = pos.copy().radialOffset(facing.getAxis(), sin, cos, widthStart);
            buffer.addVertex((float) point.x, (float) point.y, (float) point.z).setColor(1F, 1F, 1F, fadeReverse ? 0F : fxState).setUv(texX, (fadeReverse ? 0.1F : 1F));
            point.offset(facing, length);
            point.radialOffset(facing.getAxis(), sin, cos, widthEnd - widthStart);
            buffer.addVertex((float) point.x, (float) point.y, (float) point.z).setColor(1F, 1F, 1F, fadeReverse ? fxState : 0F).setUv(texX, 0);
        }
    }

    @Override
    public RenderType renderType() {
        return REACTOR_BEAM_TYPE;
    }


}
