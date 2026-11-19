package com.brandon3055.draconicevolution.api.crafting;

import com.brandon3055.brandonscore.api.TechLevel;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.ArrayList;
import java.util.List;

/** Registry-aware fusion recipe payload, retaining existing JSON fields and long energy costs. */
public record FusionRecipeData(ItemStackTemplate resultTemplate,Ingredient catalyst,long totalEnergy,TechLevel techLevel,List<InjectorIngredient> ingredients) {
    public record InjectorIngredient(Ingredient ingredient,boolean consume){
        public static final Codec<InjectorIngredient> CODEC=RecordCodecBuilder.create(i->i.group(
                FusionIngredientCodec.INSTANCE.fieldOf("ingredient").forGetter(InjectorIngredient::ingredient),
                Codec.BOOL.fieldOf("consume").forGetter(InjectorIngredient::consume)).apply(i,InjectorIngredient::new));
        public static final StreamCodec<RegistryFriendlyByteBuf,InjectorIngredient> STREAM_CODEC=StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC,InjectorIngredient::ingredient,ByteBufCodecs.BOOL,InjectorIngredient::consume,InjectorIngredient::new);
    }
    public static final MapCodec<FusionRecipeData> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(FusionRecipeData::resultTemplate),
            FusionIngredientCodec.INSTANCE.fieldOf("catalyst").forGetter(FusionRecipeData::catalyst),
            Codec.LONG.validate(value->value>=0?com.mojang.serialization.DataResult.success(value):com.mojang.serialization.DataResult.error(()->"Fusion energy cannot be negative")).fieldOf("totalEnergy").forGetter(FusionRecipeData::totalEnergy),
            TechLevel.CODEC.fieldOf("techLevel").forGetter(FusionRecipeData::techLevel),
            InjectorIngredient.CODEC.listOf(0,255).fieldOf("ingredients").forGetter(FusionRecipeData::ingredients)).apply(i,FusionRecipeData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,FusionRecipeData> STREAM_CODEC=StreamCodec.of(FusionRecipeData::write,FusionRecipeData::read);
    public FusionRecipeData {
        if(totalEnergy<0||ingredients.size()>255)throw new IllegalArgumentException("Invalid fusion recipe payload");
        ingredients=List.copyOf(ingredients);
    }
    public FusionRecipeData(ItemStack result,Ingredient catalyst,long totalEnergy,TechLevel techLevel,List<InjectorIngredient> ingredients) {
        this(ItemStackTemplate.fromNonEmptyStack(result),catalyst,totalEnergy,techLevel,ingredients);
    }
    public ItemStack result(){return resultTemplate.create();}
    private static FusionRecipeData read(RegistryFriendlyByteBuf buffer){
        var result=ItemStackTemplate.STREAM_CODEC.decode(buffer);var catalyst=Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        int count=buffer.readUnsignedByte();var ingredients=new ArrayList<InjectorIngredient>(count);
        for(int i=0;i<count;i++)ingredients.add(InjectorIngredient.STREAM_CODEC.decode(buffer));
        long energy=buffer.readLong();int tier=buffer.readUnsignedByte();
        if(tier>=TechLevel.VALUES.length)throw new IllegalArgumentException("Invalid fusion technology tier");
        return new FusionRecipeData(result,catalyst,energy,TechLevel.VALUES[tier],ingredients);
    }
    private static void write(RegistryFriendlyByteBuf buffer,FusionRecipeData data){
        ItemStackTemplate.STREAM_CODEC.encode(buffer,data.resultTemplate);Ingredient.CONTENTS_STREAM_CODEC.encode(buffer,data.catalyst);
        buffer.writeByte(data.ingredients.size());for(var ingredient:data.ingredients)InjectorIngredient.STREAM_CODEC.encode(buffer,ingredient);
        buffer.writeLong(data.totalEnergy);buffer.writeByte(data.techLevel.index);
    }
}
