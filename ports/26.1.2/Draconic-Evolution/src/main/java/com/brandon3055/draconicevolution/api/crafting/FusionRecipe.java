package com.brandon3055.draconicevolution.api.crafting;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.DraconicAPI;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by brandon3055 on 24/11/20
 */
public class FusionRecipe implements IFusionRecipe {

    private final net.minecraft.world.item.ItemStackTemplate result;
    private final Ingredient catalyst;
    private final long totalEnergy;
    private final TechLevel techLevel;
    private final List<FusionIngredient> ingredients;

    public FusionRecipe(ItemStack result, Ingredient catalyst, long totalEnergy, TechLevel techLevel, List<FusionIngredient> ingredients) {
        this(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result),catalyst,totalEnergy,techLevel,ingredients);
    }
    public FusionRecipe(net.minecraft.world.item.ItemStackTemplate result, Ingredient catalyst, long totalEnergy, TechLevel techLevel, List<FusionIngredient> ingredients) {
        this.result = result;
        this.catalyst = catalyst;
        this.totalEnergy = totalEnergy;
        this.techLevel = techLevel;
        this.ingredients = ingredients;
    }

    @Override
    public TechLevel getRecipeTier() {
        return techLevel;
    }

    @Override
    public long getEnergyCost() {
        return totalEnergy;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients.stream().map(fusionIngredient -> fusionIngredient.ingredient).collect(Collectors.toCollection(NonNullList::create));
    }

    @Override
    public List<IFusionIngredient> fusionIngredients() {
        return ImmutableList.copyOf(ingredients);
    }

    @Override
    public Ingredient getCatalyst() {
        return catalyst;
    }

    @Override
    public ItemStack assemble(IFusionInventory inv, HolderLookup.Provider provider) {
        ItemStack stack = result.create();
        if (stack.getItem() instanceof IFusionDataTransfer) {
            ((IFusionDataTransfer) stack.getItem()).transferIngredientData(stack, inv, provider);
        }
        return stack;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return result.create();
    }

    @Override
    public RecipeSerializer<FusionRecipe> getSerializer() {
        return DraconicAPI.FUSION_RECIPE_SERIALIZER.get();
    }

    public static class FusionIngredient implements IFusionIngredient {
        private static final Codec<FusionIngredient> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                        FusionIngredientCodec.INSTANCE.fieldOf("ingredient").forGetter(e -> e.ingredient),
                        Codec.BOOL.fieldOf("consume").forGetter(e -> e.consume)
                ).apply(builder, FusionIngredient::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, FusionIngredient> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, e -> e.ingredient,
                ByteBufCodecs.BOOL, e -> e.consume,
                FusionIngredient::new
        );

        private final Ingredient ingredient;
        private final boolean consume;

        public FusionIngredient(Ingredient ingredient, boolean consume) {
            this.ingredient = ingredient;
            this.consume = consume;
        }

        @Override
        public Ingredient get() {
            return ingredient;
        }

        @Override
        public boolean consume() {
            return consume;
        }

//        protected void write(FriendlyByteBuf buffer) {
//            buffer.writeBoolean(consume);
//            ingredient.toNetwork(buffer);
//        }
//
//        protected static FusionIngredient read(FriendlyByteBuf buffer) {
//            boolean consume = buffer.readBoolean();
//            Ingredient ingredient = Ingredient.fromNetwork(buffer);
//            return new FusionIngredient(ingredient, consume);
//        }
    }

    private FusionRecipeData data(){
        return new FusionRecipeData(result,catalyst,totalEnergy,techLevel,ingredients.stream()
                .map(i->new FusionRecipeData.InjectorIngredient(i.ingredient,i.consume)).toList());
    }
    private static FusionRecipe fromData(FusionRecipeData data){
        return new FusionRecipe(data.resultTemplate(),data.catalyst(),data.totalEnergy(),data.techLevel(),data.ingredients().stream()
                .map(i->new FusionIngredient(i.ingredient(),i.consume())).toList());
    }
    public static final class Serializer {
        private Serializer(){}
        public static final MapCodec<FusionRecipe> CODEC=FusionRecipeData.CODEC.xmap(FusionRecipe::fromData,FusionRecipe::data);
        public static final StreamCodec<RegistryFriendlyByteBuf,FusionRecipe> STREAM_CODEC=FusionRecipeData.STREAM_CODEC.map(FusionRecipe::fromData,FusionRecipe::data);
        public static final RecipeSerializer<FusionRecipe> TYPE=new RecipeSerializer<>(CODEC,STREAM_CODEC);
    }
}
