package com.brandon3055.draconicevolution.lib;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Server-authoritative recipes; clients only see recipe types explicitly synchronized by NeoForge. */
public final class RecipeLookup {
    private static volatile RecipeMap clientRecipes = RecipeMap.EMPTY;
    private RecipeLookup() {}
    public static void updateClientRecipes(RecipeMap recipes) { clientRecipes = recipes; }
    public static void clearClientRecipes() { clientRecipes = RecipeMap.EMPTY; }
    public static RecipeMap forLevel(Level level) {
        return level instanceof ServerLevel server ? server.recipeAccess().recipeMap() : clientRecipes;
    }
    public static Optional<RecipeHolder<?>> byId(Level level, Identifier id) {
        return Optional.ofNullable(forLevel(level).byKey(ResourceKey.create(Registries.RECIPE, id)));
    }
    public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> find(RecipeType<T> type, I input, Level level) {
        return forLevel(level).getRecipesFor(type, input, level).findFirst();
    }
    public static <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> all(RecipeType<T> type, Level level) {
        return forLevel(level).byType(type);
    }
}
