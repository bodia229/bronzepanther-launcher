package community.bronzepanther.client;

import dev.emi.emi.api.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.*;
import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;

/** Show native recipe displays that have no existing EMI/JEI representation. */
@EmiEntrypoint
public final class RecipeCoverageEmiPlugin implements EmiPlugin {
    public static final Set<Identifier> coveredByEquivalent=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static EmiIngredient ingredient(SlotDisplay slot,net.minecraft.util.context.ContextMap context){
        return EmiIngredient.of(slot.resolveForStacks(context).stream().map(EmiStack::of).toList());
    }
    private static String signature(EmiRecipe recipe){
        var category=recipe.getCategory().getId();
        var inputs=recipe.getInputs().stream().filter(i->!i.isEmpty()).map(RecipeCoverageEmiPlugin::signature);
        boolean crafting=category.getPath().equals("crafting");
        return (crafting?"crafting":category)+"|"+(crafting?inputs.sorted().toList():inputs.toList())+"|"+recipe.getOutputs().stream().map(RecipeCoverageEmiPlugin::signature).toList();
    }
    private static String signature(EmiIngredient ingredient){
        return ingredient.getEmiStacks().stream().map(s->s.getId()+"@"+s.getAmount()+"@"+s.getComponentChanges()).sorted().toList().toString();
    }
    private static EmiIngredient decodeIngredient(JsonElement json,net.minecraft.resources.RegistryOps<JsonElement> ops){
        return nativeIngredient(Ingredient.CODEC.parse(ops,json).getOrThrow(),Minecraft.getInstance().level);
    }
    private static EmiIngredient nativeIngredient(Ingredient ingredient,net.minecraft.world.level.Level level){
        var samples=ingredient.display().resolveForStacks(SlotDisplayContext.fromLevel(level)).stream()
            .filter(ingredient::test).map(EmiStack::of).toList();
        return EmiIngredient.of(samples);
    }
    private static List<EmiIngredient> exampleIngredients(JsonObject data,net.minecraft.resources.RegistryOps<JsonElement> ops){
        var result=new ArrayList<EmiIngredient>(Collections.nCopies(9,EmiStack.EMPTY));
        if(data.has("input_cell")&&data.has("input_component")){
            result.set(0,decodeIngredient(data.get("input_cell"),ops));result.set(1,decodeIngredient(data.get("input_component"),ops));
        }else if(data.has("pattern")){
            var pattern=data.get("pattern");JsonObject key;JsonArray rows;
            if(pattern.isJsonObject()){key=pattern.getAsJsonObject().getAsJsonObject("key");rows=pattern.getAsJsonObject().getAsJsonArray("pattern");}
            else {key=data.getAsJsonObject("key");rows=pattern.getAsJsonArray();}
            if(rows.size()>3)return List.of();
            for(int y=0;y<rows.size();y++){
                String row=rows.get(y).getAsString();if(row.length()>3)return List.of();
                for(int x=0;x<row.length();x++)if(row.charAt(x)!=' ')result.set(y*3+x,decodeIngredient(key.get(String.valueOf(row.charAt(x))),ops));
            }
        }else if(data.has("ingredients")){
            var ingredients=data.getAsJsonArray("ingredients");if(ingredients.size()>9)return List.of();
            for(int i=0;i<ingredients.size();i++)result.set(i,decodeIngredient(ingredients.get(i),ops));
        }else if(data.has("input")&&(data.has("repair_ingredients")||data.has("repair_ingredient"))){
            result.set(0,decodeIngredient(data.get("input"),ops));
            if(data.has("repair_ingredients")){
                var repairs=data.getAsJsonArray("repair_ingredients");if(repairs.size()>8)return List.of();
                for(int i=0;i<repairs.size();i++)result.set(i+1,decodeIngredient(repairs.get(i),ops));
            }else result.set(1,decodeIngredient(data.get("repair_ingredient"),ops));
        }else if(data.has("target")&&data.has("dye")){
            result.set(0,decodeIngredient(data.get("target"),ops));result.set(1,decodeIngredient(data.get("dye"),ops));
        }else return List.of();
        return result;
    }
    private static EmiRecipe nativeCraftExample(RecipeHolder<?> holder,CraftingRecipe recipe,net.minecraft.client.multiplayer.ClientLevel level){
        var placement=recipe.placementInfo();
        if(!placement.isImpossibleToPlace()&&!placement.ingredients().isEmpty()){
            int width=3;boolean shapeless=true;
            try{
                width=(Integer)recipe.getClass().getMethod("getWidth").invoke(recipe);
                shapeless=false;
                try{shapeless=(Boolean)recipe.getClass().getMethod("isShapeless").invoke(recipe);}catch(NoSuchMethodException ignored){}
            }catch(ReflectiveOperationException ignored){}
            if(shapeless||width<1||width>3)width=3;
            var ingredients=new ArrayList<EmiIngredient>(Collections.nCopies(9,EmiStack.EMPTY));
            var slots=placement.slotsToIngredientIndex();boolean valid=true;
            for(int i=0;i<slots.size();i++){
                int position=shapeless?i:(i/width)*3+i%width;
                if(position>=9){valid=false;break;}
                int index=slots.getInt(i);
                if(index>=0)ingredients.set(position,nativeIngredient(placement.ingredients().get(index),level));
            }
            if(valid){var example=assembledExample(holder,recipe,ingredients,shapeless,level);if(example!=null)return example;}
        }
        var ops=net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
        var encoded=Recipe.CODEC.encodeStart(ops,recipe).result();if(encoded.isEmpty()||!encoded.get().isJsonObject())return null;
        var data=encoded.get().getAsJsonObject();
        var ingredients=exampleIngredients(data,ops);if(ingredients.isEmpty())return null;
        if(data.has("repair_ingredients")||data.has("repair_ingredient")){
            var target=ingredients.getFirst().getEmiStacks().getFirst().getItemStack().copy();
            if(target.isDamageableItem())target.setDamageValue(Math.max(1,target.getMaxDamage()-1));
            ingredients.set(0,EmiStack.of(target));
        }
        return assembledExample(holder,recipe,ingredients,!data.has("pattern"),level);
    }
    static EmiRecipe assembledExample(RecipeHolder<?> holder,CraftingRecipe recipe,List<EmiIngredient> ingredients,boolean shapeless,net.minecraft.client.multiplayer.ClientLevel level){
        var stacks=new ArrayList<ItemStack>();
        for(var ingredient:ingredients)stacks.add(ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getEmiStacks().getFirst().getItemStack().copy());
        var positioned=CraftingInput.ofPositioned(3,3,stacks);
        var input=positioned.input();
        // Actual native matching/assembly is mandatory: never invent a static result
        // for a component-dependent recipe from its JSON or item name.
        if(!recipe.matches(input,level))return null;
        var output=recipe.assemble(input);if(output.isEmpty())return null;
        var remainders=recipe.getRemainingItems(input);
        for(int i=0;i<remainders.size();i++){
            int slot=(i/input.width()+positioned.top())*3+i%input.width()+positioned.left();
            if(!remainders.get(i).isEmpty()&&!ingredients.get(slot).isEmpty())
                for(var stack:ingredients.get(slot).getEmiStacks())stack.setRemainder(EmiStack.of(remainders.get(i).copy()));
        }
        return new EmiCraftingRecipe(ingredients,EmiStack.of(output),holder.id().identifier(),shapeless){
            @Override public RecipeHolder<?> getBackingRecipe(){return holder;}
            @Override public boolean supportsRecipeTree(){return false;}
        };
    }
    @Override public void register(EmiRegistry registry){
        coveredByEquivalent.clear();
        var map=registry.getRecipeMap();
        var drying=new EmiRecipeCategory(Identifier.parse("bronzepanther:drying"),EmiStack.of(Items.LEATHER)){
            @Override public Component getName(){return Component.literal("Сушка · Сумеречный лес");}
        };
        registry.addCategory(drying);
        var mechanics=new EmiRecipeCategory(Identifier.parse("bronzepanther:mechanical_operations"),EmiStack.of(Items.CRAFTING_TABLE)){
            @Override public Component getName(){return Component.literal("Особые операции с предметами");}
        };
        registry.addCategory(mechanics);
        // Deferred registration lets author integrations go first. Never duplicate a
        // recipe ID/backing recipe or an identical crafting-grid presentation.
        registry.addDeferredRecipes(add->{
            var level=Minecraft.getInstance().level;if(level==null)return;
            var context=SlotDisplayContext.fromLevel(level);
            // EmiApi's manager still describes the previous bake during late registration.
            // Read the actual current batch through a checked, version-specific accessor.
            var existing=List.copyOf(community.bronzepanther.mixin.EmiRecipeListAccess.bp$registeredRecipes());
            var ids=new HashSet<Identifier>();var signatures=new HashSet<String>();
            for(var recipe:existing){
                if(recipe.getId()!=null)ids.add(recipe.getId());
                if(recipe.getBackingRecipe()!=null)ids.add(recipe.getBackingRecipe().id().identifier());
                signatures.add(signature(recipe));
            }
            int added=0,covered=0;
            for(var holder:map.values()){
                var id=holder.id().identifier();if(ids.contains(id))continue;
                var recipe=holder.value();
                int displayIndex=0;
                for(var display:recipe.display()){
                    List<EmiIngredient> ingredients=new ArrayList<>();boolean shapeless;
                    if(display instanceof ShapedCraftingRecipeDisplay shaped){
                        shapeless=false;
                        for(int y=0;y<3;y++)for(int x=0;x<3;x++)ingredients.add(x<shaped.width()&&y<shaped.height()?ingredient(shaped.ingredients().get(y*shaped.width()+x),context):EmiStack.EMPTY);
                    }else if(display instanceof ShapelessCraftingRecipeDisplay loose){
                        shapeless=true;
                        for(var slot:loose.ingredients())ingredients.add(ingredient(slot,context));
                    }else continue;
                    var output=display.result().resolveForFirstStack(context);if(output.isEmpty())continue;
                    var displayId=displayIndex++==0?id:Identifier.fromNamespaceAndPath("bronzepanther","display/"+id.getNamespace()+"/"+id.getPath()+"/"+displayIndex);
                    var emi=new EmiCraftingRecipe(ingredients,EmiStack.of(output),displayId,shapeless){
                        @Override public RecipeHolder<?> getBackingRecipe(){return holder;}
                    };
                    if(signatures.add(signature(emi))){add.accept(emi);added++;}else {covered++;coveredByEquivalent.add(id);}
                    ids.add(id);
                }
                if(!ids.contains(id)&&recipe instanceof CraftingRecipe crafting){
                    var emi=nativeCraftExample(holder,crafting,level);
                    if(emi==null)emi=ParametricCraftSamples.create(holder,crafting,map,level);
                    if(emi!=null){
                        if(signatures.add(signature(emi))){add.accept(emi);added++;}else {covered++;coveredByEquivalent.add(id);}
                        ids.add(id);
                    }
                }
                if(!ids.contains(id)){
                    var emi=mechanicalExample(mechanics,holder,level);
                    if(emi!=null){
                        if(signatures.add(signature(emi))){add.accept(emi);added++;}else {covered++;coveredByEquivalent.add(id);}
                        ids.add(id);
                    }
                }
                if(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).toString().equals("twilightforest:drying")&&recipe instanceof SingleItemRecipe single){
                    var choices=EmiIngredient.of(single.input());if(choices.isEmpty())continue;
                    var sample=new SingleRecipeInput(choices.getEmiStacks().getFirst().getItemStack());
                    if(!single.matches(sample,level))throw new IllegalStateException("Native drying input mismatch: "+id);
                    var output=single.assemble(sample);if(output.isEmpty())continue;
                    int ticks;
                    try{ticks=(Integer)recipe.getClass().getMethod("getDryingTime").invoke(recipe);}
                    catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot read native drying time",e);}
                    var emi=new Drying(drying,holder,choices,EmiStack.of(output),ticks);
                    if(signatures.add(signature(emi))){add.accept(emi);added++;}else {covered++;coveredByEquivalent.add(id);}
                    ids.add(id);
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("BronzePanther native recipe coverage: added {}, already equivalent {}",added,covered);
        });
    }
    private static EmiRecipe mechanicalExample(EmiRecipeCategory category,RecipeHolder<?> holder,net.minecraft.client.multiplayer.ClientLevel level){
        var recipe=holder.value();var type=BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).toString();
        try{
            String id=holder.id().identifier().toString();
            if(id.equals("sophisticatedcore:ender_linker_clear")||id.equals("sophisticatedcore:ender_linker_endpoint")){
                var linker=BuiltInRegistries.ITEM.getValue(Identifier.parse("sophisticatedcore:ender_linker"));
                if(linker==null||linker==Items.AIR)return null;
                if(id.endsWith("_clear"))return new Mechanical(category,holder,List.of(EmiStack.of(linker)),List.of(EmiStack.of(linker)),
                    List.of("Сброс привязки настроенного эндэр-связующего.","Поместите связующее отдельно в сетку верстака.","Старые настройки связи удаляются при крафте.","Это сброс настроек, а не создание связующего."));
                var backpack=BuiltInRegistries.ITEM.getValue(Identifier.parse("sophisticatedbackpacks:backpack"));
                return new Mechanical(category,holder,List.of(EmiStack.of(linker),EmiStack.of(backpack)),List.of(),
                    List.of("Связь с поддерживаемым контейнером.","Поместите связующее и контейнер в верстак.","Результат зависит от существующей привязки.","Права доступа и состояние проверяет сервер.","Обычный пустой контейнер не всегда подходит."));
            }
            if(type.equals("ae2:storage_cell_disassembly")){
                if(!(Boolean)recipe.getClass().getMethod("canDisassemble").invoke(recipe)){
                    // An empty disassembly definition disables this action; it is not a missing craft.
                    coveredByEquivalent.add(holder.id().identifier());return null;
                }
                var input=(Item)recipe.getClass().getMethod("storageCell").invoke(recipe);
                var outputs=new ArrayList<EmiStack>();
                for(var result:(List<?>)recipe.getClass().getMethod("cellDisassemblyItems").invoke(recipe))
                    outputs.add(EmiStack.of(((ItemStackTemplate)result).create()));
                return new Mechanical(category,holder,List.of(EmiStack.of(input)),outputs,
                    List.of("Сначала полностью опустошите ячейку.","Возьмите её в руку и нажмите Shift + ПКМ.","Компоненты возвращаются при разборе."));
            }
            if(type.equals("ae2:crafting_unit_transform")){
                var upgraded=(net.minecraft.world.level.block.Block)recipe.getClass().getMethod("getUpgradedBlock").invoke(recipe);
                var upgrade=(Item)recipe.getClass().getMethod("getUpgradeItem").invoke(recipe);
                var base=BuiltInRegistries.ITEM.getValue(Identifier.parse("ae2:crafting_unit"));
                return new Mechanical(category,holder,List.of(EmiStack.of(base),EmiStack.of(upgrade)),List.of(EmiStack.of(upgraded)),
                    List.of("Установите обычный сборочный блок.","Примените к нему предмет улучшения.","Это взаимодействие с блоком, не крафт."));
            }
            if(type.equals("ae2:matter_cannon")){
                var ops=net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
                var json=Recipe.CODEC.encodeStart(ops,recipe).getOrThrow().getAsJsonObject();
                var ammo=decodeIngredient(json.get("ammo"),ops);
                return new Mechanical(category,holder,List.of(ammo),List.of(),
                    List.of("Боеприпас для материальной пушки.","Масса боеприпаса: "+json.get("weight").getAsString()+".","Зарядите пушку через МЭ-хранилище.","Это характеристика боеприпаса, не рецепт."));
            }
            if(type.equals("twilightforest:uncrafting")){
                var input=(Ingredient)recipe.getClass().getMethod("getInput").invoke(recipe);
                int count=(Integer)recipe.getClass().getMethod("getCount").invoke(recipe);
                int cost=(Integer)recipe.getClass().getMethod("getCost").invoke(recipe);
                var placement=recipe.placementInfo();
                var outputs=new LinkedHashMap<Identifier,EmiStack>();
                for(int index:placement.slotsToIngredientIndex()){
                    if(index<0)continue;
                    var ingredient=EmiIngredient.of(placement.ingredients().get(index));
                    if(ingredient.isEmpty())continue;
                    var stack=ingredient.getEmiStacks().getFirst().copy();
                    var previous=outputs.get(stack.getId());
                    if(previous==null)outputs.put(stack.getId(),stack);else previous.setAmount(previous.getAmount()+stack.getAmount());
                }
                if(outputs.isEmpty())return null;
                return new Mechanical(category,holder,List.of(EmiIngredient.of(input,count)),List.copyOf(outputs.values()),
                    List.of("Разберите предмет на антиверстаке.","Стоимость: "+cost+" уровней опыта.","Состав зависит от исходного предмета."));
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Native operation API changed: "+holder.id(),e);}
        return null;
    }
    private record Mechanical(EmiRecipeCategory category,RecipeHolder<?> holder,List<EmiIngredient> inputs,List<EmiStack> outputs,List<String> notes) implements EmiRecipe {
        public EmiRecipeCategory getCategory(){return category;}
        public Identifier getId(){return holder.id().identifier();}
        public RecipeHolder<?> getBackingRecipe(){return holder;}
        public List<EmiIngredient> getInputs(){return inputs;}
        public List<EmiStack> getOutputs(){return outputs;}
        public boolean supportsRecipeTree(){return false;}
        public int getDisplayWidth(){return 166;}
        private List<net.minecraft.util.FormattedCharSequence> lines(){return notes.stream().flatMap(n->Minecraft.getInstance().font.split(Component.literal(n),164).stream()).toList();}
        private int slotsHeight(){return Math.max(1,(outputs.size()+2)/3)*20;}
        public int getDisplayHeight(){return slotsHeight()+8+lines().size()*10;}
        public void addWidgets(WidgetHolder widgets){
            for(int i=0;i<inputs.size();i++)widgets.addSlot(inputs.get(i),i*20,0);
            if(!outputs.isEmpty())widgets.addText(Component.literal("→"),64,5,0x404040,false);
            for(int i=0;i<outputs.size();i++)widgets.addSlot(outputs.get(i),104+(i%3)*20,(i/3)*20).recipeContext(this);
            int y=slotsHeight()+6;for(var line:lines()){widgets.addText(line,0,y,0x404040,false);y+=10;}
        }
    }
    private record Drying(EmiRecipeCategory category,RecipeHolder<?> backing,EmiIngredient input,EmiStack output,int ticks) implements EmiRecipe {
        public EmiRecipeCategory getCategory(){return category;}
        public Identifier getId(){return backing.id().identifier();}
        public RecipeHolder<?> getBackingRecipe(){return backing;}
        public List<EmiIngredient> getInputs(){return List.of(input);}
        public List<EmiStack> getOutputs(){return List.of(output);}
        public int getDisplayWidth(){return 166;}
        public int getDisplayHeight(){return 52;}
        public void addWidgets(WidgetHolder widgets){
            widgets.addSlot(input,14,0);widgets.addFillingArrow(60,1,Math.max(1000,ticks*50));
            widgets.addSlot(output,112,0).recipeContext(this);
            widgets.addText(Component.literal("Время: "+ticks+" тактов ("+(ticks/20.0)+" с)"),0,25,0x404040,false);
            widgets.addText(Component.literal("Разместите на сушильной стойке."),0,38,0x404040,false);
        }
    }
}
