package com.brandon3055.brandonscore.client.model;

import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.client.render.ExtractedEquipmentGeometry;
import com.brandon3055.brandonscore.handlers.contributor.ContributorHandler;
import com.brandon3055.brandonscore.items.EquippedModelItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import java.util.*;
import java.util.function.BiConsumer;

/** Extract legacy custom models immediately; submit only detached geometry later. */
public class EquippedItemModelLayer<S extends HumanoidRenderState,M extends HumanoidModel<S>> extends RenderLayer<S,M> {
    private static final ContextKey<ExtractedEquipmentGeometry> EQUIPMENT=new ContextKey<>(Identifier.fromNamespaceAndPath("brandonscore","equipment_geometry"));
    private static final Map<LivingEntityRenderer<?,?,?>,EquippedItemModelLayer<?,?>> LAYERS=new IdentityHashMap<>();
    private final boolean slim;
    private final ContributorModel<?> contributorModel=new ContributorModel<>();
    public EquippedItemModelLayer(LivingEntityRenderer<?,S,M> renderer,boolean slim){super(renderer);this.slim=slim;LAYERS.put(renderer,this);}
    public static void clearLayers(){LAYERS.clear();}
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void registerModifiers(RegisterRenderStateModifiersEvent event){
        event.<LivingEntity,LivingEntityRenderState>registerEntityModifier(
                new com.google.common.reflect.TypeToken<LivingEntityRenderer<?,?,?>>() {},EquippedItemModelLayer::extract);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void extract(LivingEntity entity,LivingEntityRenderState state){
        if(!(state instanceof HumanoidRenderState humanoid))return;
        var renderer=Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        var layer=LAYERS.get(renderer);if(layer==null)return;
        // Legacy model hooks share shader staging; serialize their extraction, never their deferred draws.
        synchronized(LAYERS){((EquippedItemModelLayer)layer).capture(entity,humanoid);}
    }
    private void capture(LivingEntity entity,S state){
        getParentModel().setupAnim(state);
        var extracted=ExtractedEquipmentGeometry.capture(buffers->{
            var poses=new PoseStack();
            for(var slot:List.of(EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.HEAD))
                captureItem(entity,entity.getItemBySlot(slot).copy(),slot,poses,buffers,state.lightCoords,state.partialTick);
            if(BrandonsCore.equipmentManager!=null){
                var stack=BrandonsCore.equipmentManager.findMatchingItem(item->item.getItem() instanceof EquippedModelItem,entity);
                if(!stack.isEmpty())captureItem(entity,stack.copy(),null,poses,buffers,state.lightCoords,state.partialTick);
            }
            if(entity instanceof Player player){
                var props=ContributorHandler.getProps(player);
                if(props.isContributor()){
                    ClientHooks.copyModelProperties(getParentModel(),contributorModel);contributorModel.props=props;
                    contributorModel.render(entity,poses,buffers,ItemStack.EMPTY,state.lightCoords,OverlayTexture.NO_OVERLAY,state.partialTick);
                }
            }
        });
        state.setRenderData(EQUIPMENT,extracted);
    }
    private void captureItem(LivingEntity entity,ItemStack stack,EquipmentSlot slot,PoseStack poses,MultiBufferSource buffers,int light,float partial){
        if(!(stack.getItem() instanceof EquippedModelItem item)||!item.shouldRenderInSlot(entity,stack,slot))return;
        var equippable=stack.get(DataComponents.EQUIPPABLE);
        if(slot!=null&&equippable!=null&&equippable.slot()!=slot)return;
        var model=Objects.requireNonNull(item.getExtendedModel(entity,stack,slot,getParentModel(),slim),"Missing equipped item model");
        model.render(entity,poses,buffers,stack,light,OverlayTexture.NO_OVERLAY,partial);
        com.brandon3055.brandonscore.client.render.RenderUtils.endBatch(buffers);
    }
    @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,S state,float yaw,float pitch){
        var geometry=state.getRenderData(EQUIPMENT);if(geometry!=null)geometry.submit(poses,collector);
    }
}
