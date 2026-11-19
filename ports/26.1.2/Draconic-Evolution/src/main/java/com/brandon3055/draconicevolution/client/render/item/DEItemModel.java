package com.brandon3055.draconicevolution.client.render.item;

import com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.*;
import org.joml.*;
import java.util.function.Consumer;

/** Native item model with a detached stack and the authored DE hand transforms. */
public final class DEItemModel implements ItemModel {
    private static final java.util.Map<String, java.util.function.Supplier<DEItemRenderer>> FACTORIES = new java.util.HashMap<>();
    public static void registerRenderers(java.util.Map<String, java.util.function.Supplier<DEItemRenderer>> factories) {
        factories.forEach((name,factory) -> {
            if(FACTORIES.putIfAbsent(name,factory)!=null) throw new IllegalArgumentException("Duplicate DE item renderer: "+name);
        });
    }
    private final DEItemRenderer renderer;
    private final Matrix4fc transform;
    private final SpecialModelRenderer<Argument> submission = new SpecialModelRenderer<>() {
        @Override public void submit(Argument argument, PoseStack poses, SubmitNodeCollector collector,
                int light, int overlay, boolean foil, int outline) {
            if (argument == null) return;
            renderer.submitItem(argument.stack, argument.context, argument.state, poses, collector, light, overlay, foil, outline);
        }
        @Override public Argument extractArgument(ItemStack stack) {
            return new Argument(stack.copy(), ItemDisplayContext.NONE, null);
        }
        @Override public void getExtents(Consumer<Vector3fc> output) {
            for (int x=0;x<2;x++) for(int y=0;y<2;y++) for(int z=0;z<2;z++) output.accept(new Vector3f(x,y,z));
        }
    };
    private record Argument(ItemStack stack, ItemDisplayContext context, Object state) {}
    public DEItemModel(DEItemRenderer renderer, Matrix4fc transform) {
        this.renderer=renderer; this.transform=new Matrix4f(transform);
    }
    @Override public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver,
            ItemDisplayContext context, ClientLevel level, ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        // GUI item caching must distinguish souls and other component-driven
        // variants of the same item model.
        output.appendModelIdentityElement(stack.getComponents());
        output.setAnimated();
        var layer=output.newLayer();
        layer.setUsesBlockLight(renderer.usesBlockLight());
        var particle=renderer.getParticleIcon();
        if(particle!=null) layer.setParticleMaterial(new net.minecraft.client.resources.model.sprite.Material.Baked(particle,false));
        layer.setExtents(() -> {
            var corners=new Vector3fc[8]; int i=0;
            for(int x=0;x<2;x++) for(int y=0;y<2;y++) for(int z=0;z<2;z++) corners[i++]=new Vector3f(x,y,z);
            return corners;
        });
        var authored=renderer.getModelState();
        var local=new Matrix4f().translation(.5F,.5F,.5F);
        if(authored!=null) local.mul(authored.getTransform(context).getMatrix());
        local.translate(-.5F,-.5F,-.5F).mul(transform);
        layer.setLocalTransform(local);
        layer.setupSpecialModel(submission,new Argument(stack.copy(),context,renderer.extractState(stack,context,level,owner)));
    }
    public record Unbaked(String renderer) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC=Codec.STRING.fieldOf("renderer").xmap(Unbaked::new,Unbaked::renderer);
        @Override public void resolveDependencies(ResolvableModel.Resolver resolver) {}
        @Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transform) {
            var factory=FACTORIES.get(renderer);
            if(factory==null) throw new IllegalArgumentException("Unknown DE item renderer: "+renderer);
            return new DEItemModel(factory.get(), transform);
        }
        @Override public MapCodec<Unbaked> type() { return CODEC; }
    }
}
