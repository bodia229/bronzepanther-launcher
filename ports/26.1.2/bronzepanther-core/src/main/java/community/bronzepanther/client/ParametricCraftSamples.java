package community.bronzepanther.client;

import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.util.*;

/** Verified examples for recipes whose serializers intentionally contain no ingredient data. */
final class ParametricCraftSamples {
    private static ItemStack item(String id){
        var key=Identifier.parse(id);
        return BuiltInRegistries.ITEM.containsKey(key)?new ItemStack(BuiltInRegistries.ITEM.getValue(key)):ItemStack.EMPTY;
    }
    private static List<EmiIngredient> grid(ItemStack... stacks){
        var result=new ArrayList<EmiIngredient>(Collections.nCopies(9,EmiStack.EMPTY));
        for(int i=0;i<stacks.length;i++)result.set(i,stacks[i].isEmpty()?EmiStack.EMPTY:EmiStack.of(stacks[i]));
        return result;
    }
    private static RecipeHolder<?> holder(RecipeMap map,String id){
        return map.byKey(ResourceKey.create(Registries.RECIPE,Identifier.parse(id)));
    }
    @SuppressWarnings("unchecked")
    static EmiRecipe create(RecipeHolder<?> holder,CraftingRecipe recipe,RecipeMap map,ClientLevel level){
        String id=holder.id().identifier().toString();
        List<EmiIngredient> inputs=null;boolean shaped=false;
        try{
            switch(id){
                case "ae2:special/facade" -> {
                    inputs=grid(ItemStack.EMPTY,item("ae2:cable_anchor"),ItemStack.EMPTY,item("ae2:cable_anchor"),new ItemStack(Items.STONE),item("ae2:cable_anchor"),ItemStack.EMPTY,item("ae2:cable_anchor"),ItemStack.EMPTY);shaped=true;
                }
                case "ae2:add_item_upgrade" -> inputs=grid(item("ae2:wireless_terminal"),item("ae2:energy_card"));
                case "ae2:remove_item_upgrade" -> {
                    var add=holder(map,"ae2:add_item_upgrade");
                    if(add==null||!(add.value() instanceof CraftingRecipe upgrade))return null;
                    var sample=CraftingInput.ofPositioned(2,1,List.of(item("ae2:wireless_terminal"),item("ae2:energy_card"))).input();
                    if(!upgrade.matches(sample,level))return null;
                    inputs=grid(upgrade.assemble(sample));
                }
                case "sophisticatedbackpacks:backpack_dye" -> inputs=grid(item("sophisticatedbackpacks:backpack"),new ItemStack(Items.RED_DYE));
                case "sophisticatedcore:upgrade_clear" -> inputs=grid(item("sophisticatedbackpacks:pickup_upgrade"));
                case "minecraft:repair_item" -> {
                    var first=new ItemStack(Items.IRON_PICKAXE);first.setDamageValue(200);
                    var second=new ItemStack(Items.IRON_PICKAXE);second.setDamageValue(200);
                    inputs=grid(first,second);
                }
                case "twilightforest:travellers_vest_gloves_merge_recipe" -> inputs=grid(item("twilightforest:travellers_vest"),item("twilightforest:travellers_gloves"));
                case "twilightforest:emperors_cloth_recipe" -> inputs=grid(item("twilightforest:emperors_cloth"),new ItemStack(Items.DIAMOND_CHESTPLATE));
                case "twilightforest:casket_repair_recipe" -> {
                    var casket=item("twilightforest:keepsake_casket");
                    var damage=(net.minecraft.core.component.DataComponentType<Integer>)BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse("twilightforest:casket_damage"));
                    if(damage==null)return null;casket.set(damage,1);
                    var repair=EmiIngredient.of(net.minecraft.tags.TagKey.create(Registries.ITEM,Identifier.parse("twilightforest:keepsake_casket_repair_items")));
                    if(repair.isEmpty())return null;
                    inputs=grid(casket,repair.getEmiStacks().getFirst().getItemStack().copy());
                }
                case "thaumaturge:label_filter" -> {
                    var key=(ResourceKey<?>)Class.forName("com.leclowndu93150.thaumaturge.api.aspect.TTAspects").getField("AER").get(null);
                    ResourceKey<net.minecraft.core.Registry<Object>> registry=ResourceKey.createRegistryKey(key.registry());
                    var aspect=level.registryAccess().lookupOrThrow(registry).getOrThrow(ResourceKey.create(registry,key.identifier()));
                    var phial=(ItemStack)Class.forName("com.leclowndu93150.thaumaturge.content.item.PhialItem").getMethod("makeFilled",net.minecraft.core.Holder.class).invoke(null,aspect);
                    inputs=grid(item("thaumaturge:label"),phial);
                }
                default -> {}
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Native parametric craft API changed: "+id,e);}
        if(inputs==null&&id.startsWith("minecraft:")&&id.endsWith("_banner_duplicate")){
            var blank=item(id.substring(0,id.length()-"_duplicate".length()));
            if(!blank.isEmpty()){
                var patterned=blank.copy();
                var pattern=level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(net.minecraft.world.level.block.entity.BannerPatterns.CROSS);
                patterned.set(net.minecraft.core.component.DataComponents.BANNER_PATTERNS,new net.minecraft.world.level.block.entity.BannerPatternLayers.Builder().add(pattern,DyeColor.RED).build());
                inputs=grid(patterned,blank);
            }
        }
        return inputs==null?null:RecipeCoverageEmiPlugin.assembledExample(holder,recipe,inputs,!shaped,level);
    }
}
