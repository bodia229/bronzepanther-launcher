package com.brandon3055.draconicevolution.init;

import com.brandon3055.brandonscore.api.TechLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;

/**
 * Created by brandon3055 on 05/05/2022
 */
public class TechProperties extends Item.Properties {

    private final TechLevel techLevel;
    private boolean modularAttributes;

    public TechProperties(TechLevel techLevel) {
        this.techLevel = techLevel;
//        this.component(ItemData.TECH_LEVEL, techLevel);
    }

    @Override
    public Item.Properties durability(int p_41504_) {
        this.component(DataComponents.MAX_STACK_SIZE, 1);
        return this;
    }

    public TechProperties copy() {
        TechProperties copy = new TechProperties(techLevel);
        copy.componentInitializer=componentInitializer;
        copy.craftingRemainingItem=craftingRemainingItem;
        copy.requiredFeatures=requiredFeatures;
        copy.id=id;
        copy.descriptionId=descriptionId;
        copy.canCombineRepair=canCombineRepair;
        copy.modularAttributes=modularAttributes;
        return copy;
    }

    /** Keep stack-sensitive module attributes instead of freezing vanilla tool attributes. */
    public TechProperties modularAttributes(){
        modularAttributes=true;
        componentInitializer=componentInitializer.andThen((builder,context,key)->builder.set(DataComponents.ATTRIBUTE_MODIFIERS, null));
        return this;
    }
    @Override public Item.Properties attributes(net.minecraft.world.item.component.ItemAttributeModifiers attributes){
        return modularAttributes?this:super.attributes(attributes);
    }

    @Deprecated
    public TechLevel getTechLevel() {
        return techLevel;
    }
}
