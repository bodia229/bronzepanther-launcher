package community.bronzepanther.client;

import com.google.gson.*;
import dev.emi.emi.api.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Display acquisition data without registering crafting recipes or changing loot. */
@EmiEntrypoint
public final class AcquisitionEmiPlugin implements EmiPlugin {
    public static final Map<String,Integer> publishedCounts=new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String,EmiRecipeCategory> categories=new HashMap<>();
    private final Set<Identifier> ids=new HashSet<>();
    private void category(EmiRegistry registry,String key,String name,Item icon){
        var category=new EmiRecipeCategory(Identifier.parse("bronzepanther:acquisition/"+key),EmiStack.of(icon)){
            @Override public Component getName(){return Component.literal(name);}
        };
        categories.put(key,category);registry.addCategory(category);
    }
    private static Item item(Identifier id){return BuiltInRegistries.ITEM.containsKey(id)?BuiltInRegistries.ITEM.getValue(id):Items.AIR;}
    private static EmiStack mobIcon(EntityType<?> type){
        return EmiStack.of(SpawnEggItem.byId(type).map(net.minecraft.core.Holder::value).orElse(Items.SKELETON_SKULL));
    }
    private void add(EmiRegistry registry,String kind,String key,EmiStack icon,EmiStack output,List<Component> notes){
        var id=Identifier.parse("bronzepanther:acquisition/"+kind+"/"+key);
        if(!ids.add(id))throw new IllegalStateException("Duplicate acquisition source "+id);
        registry.addRecipe(new Source(categories.get(kind),id,icon,output,List.copyOf(notes)));
        publishedCounts.merge(kind,1,Integer::sum);
    }
    @Override public void register(EmiRegistry registry){
        categories.clear();ids.clear();publishedCounts.clear();
        category(registry,"mob","Добыча с мобов",Items.ROTTEN_FLESH);
        category(registry,"chest","Находки и награды",Items.CHEST);
        category(registry,"block","Добыча блоков и руд",Items.IRON_PICKAXE);
        category(registry,"fishing","Рыбалка",Items.FISHING_ROD);
        category(registry,"bartering","Обмен с пиглинами",Items.GOLD_INGOT);
        category(registry,"soul","Души мобов · Жнец",Items.ENCHANTED_BOOK);
        category(registry,"special","Особые способы получения",Items.EMERALD);
        var mc=Minecraft.getInstance();
        try(var reader=new InputStreamReader(mc.getResourceManager().open(Identifier.parse("bronzepanther:emi/acquisition.json")),StandardCharsets.UTF_8)){
            var data=JsonParser.parseReader(reader).getAsJsonObject();
            if(data.get("schema").getAsInt()!=1)throw new IllegalStateException("Unsupported acquisition schema");
            for(var element:data.getAsJsonArray("entries")){
                var entry=element.getAsJsonObject();
                var outputId=Identifier.parse(entry.get("item").getAsString());
                var output=item(outputId);if(output==Items.AIR)continue; // Optional-mod loot conditions.
                String kind=entry.get("kind").getAsString();
                var source=Identifier.parse(entry.get("source").getAsString());
                var notes=new ArrayList<Component>();EmiStack icon;
                switch(kind){
                    case "mob" -> {
                        if(!BuiltInRegistries.ENTITY_TYPE.containsKey(source))continue;
                        var type=BuiltInRegistries.ENTITY_TYPE.getValue(source);
                        icon=mobIcon(type);notes.add(Component.literal("Возможная добыча: ").append(type.getDescription()));
                    }
                    case "block" -> {
                        var blockItem=item(source);if(blockItem==Items.AIR)continue;
                        icon=EmiStack.of(blockItem);notes.add(Component.literal("Разрушение блока: ").append(new ItemStack(blockItem).getHoverName()));
                        notes.add(Component.literal("Нужен подходящий для блока инструмент."));
                    }
                    case "chest" -> {
                        icon=EmiStack.of(Items.CHEST);notes.add(Component.literal(entry.get("label").getAsString()));
                        notes.add(Component.literal("Возможная награда, не гарантированная находка."));
                    }
                    case "fishing" -> {
                        icon=EmiStack.of(Items.FISHING_ROD);notes.add(Component.literal("Возможный улов при рыбалке."));
                        if(source.getPath().endsWith("treasure"))notes.add(Component.literal("Сокровища требуют открытого водоёма."));
                    }
                    case "bartering" -> {
                        icon=EmiStack.of(Items.GOLD_INGOT);notes.add(Component.literal("Отдайте золотой слиток взрослому пиглину."));
                        notes.add(Component.literal("Результат обмена выбирается случайно."));
                    }
                    case "special" -> {
                        icon=EmiStack.of(Items.EMERALD);
                        switch(source.getPath()){
                            case "gameplay/sniffer_digging" -> {icon=mobIcon(EntityType.SNIFFER);notes.add(Component.literal("Взрослый нюхач выкапывает семена на подходящем грунте."));}
                            case "gameplay/cat_morning_gift" -> {icon=mobIcon(EntityType.CAT);notes.add(Component.literal("Прирученный кот может принести подарок после сна рядом с хозяином."));}
                            case "gameplay/chicken_lay" -> {icon=mobIcon(EntityType.CHICKEN);notes.add(Component.literal("Курица периодически несёт яйца."));}
                            case "gameplay/armadillo_shed" -> {icon=mobIcon(EntityType.ARMADILLO);notes.add(Component.literal("Броненосец периодически сбрасывает щиток; также можно вычесать кистью."));}
                            case "gameplay/panda_sneeze" -> {icon=mobIcon(EntityType.PANDA);notes.add(Component.literal("Детёныш панды иногда роняет слизь при чихании."));}
                            case "gameplay/turtle_grow" -> {icon=mobIcon(EntityType.TURTLE);notes.add(Component.literal("Детёныш черепахи роняет щиток, когда становится взрослым."));}
                            default -> {
                                String profession=source.getPath().substring(source.getPath().lastIndexOf('/')+1).replace("_gift","");
                                if(profession.equals("unemployed"))profession="none";
                                notes.add(Component.literal("Житель может подарить предмет при эффекте «Герой деревни»."));
                                notes.add(Component.literal("Профессия: ").append(profession.equals("baby")?Component.literal("ребёнок"):Component.translatable("entity.minecraft.villager."+profession)));
                            }
                        }
                    }
                    default -> throw new IllegalStateException("Unknown acquisition kind "+kind);
                }
                for(var note:entry.getAsJsonArray("notes"))notes.add(Component.literal(note.getAsString()));
                var displayed=new ItemStack(output);
                String variant="";
                if(entry.has("components")){
                    if(mc.level==null)throw new IllegalStateException("Loot components require the connected registry context");
                    var stackJson=new JsonObject();stackJson.addProperty("id",outputId.toString());stackJson.add("components",entry.get("components"));
                    displayed=ItemStack.CODEC.parse(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,mc.level.registryAccess()),stackJson).getOrThrow();
                    variant="/variant_"+entry.get("variant").getAsString();
                }
                add(registry,kind,source.getNamespace()+"/"+source.getPath()+"/"+outputId.getNamespace()+"/"+outputId.getPath()+variant,icon,EmiStack.of(displayed),notes);
            }
        }catch(IOException e){throw new IllegalStateException("Missing frozen-pack acquisition index",e);}
        addSouls(registry,mc);
        addRuntimeSources(registry);
        var internal=item(Identifier.parse("draconicevolution:structure_block"));
        if(internal!=Items.AIR)registry.removeEmiStacks(EmiStack.of(internal));
        com.mojang.logging.LogUtils.getLogger().info("BronzePanther acquisition entries: {}",ids.size());
    }
    private void addRuntimeSources(EmiRegistry registry){
        var heart=item(Identifier.parse("draconicevolution:dragon_heart"));
        if(heart!=Items.AIR)add(registry,"special","draconicevolution/dragon_heart",mobIcon(EntityType.ENDER_DRAGON),EmiStack.of(heart),
            List.of(Component.literal("Победите дракона Энда в настоящей битве в Энде."),
                    Component.literal("Сердце появляется над выходным порталом."),
                    Component.literal("Оно остаётся висеть над порталом, пока его не заберут.")));
        var shard=item(Identifier.parse("draconicevolution:chaos_shard"));
        var crystal=item(Identifier.parse("draconicevolution:chaos_crystal"));
        if(shard!=Items.AIR&&crystal!=Items.AIR)add(registry,"special","draconicevolution/chaos_shard",EmiStack.of(crystal),EmiStack.of(shard),
            List.of(Component.literal("Найдите остров Хаоса в Энде."),
                    Component.literal("Сначала победите Хранителя Хаоса."),
                    Component.literal("Затем добудьте центральный кристалл Хаоса."),
                    Component.literal("До победы над стражем кристалл восстанавливается.")));
        var resin=item(Identifier.parse("industrial:sticky_resin"));
        var tap=item(Identifier.parse("industrial:treetap"));
        if(resin!=Items.AIR&&tap!=Items.AIR)add(registry,"special","industrial/sticky_resin",EmiStack.of(tap),EmiStack.of(resin),
            List.of(Component.literal("Найдите каучуковое дерево."),
                    Component.literal("Используйте краник на выделении смолы на стволе."),
                    Component.literal("Не нужно рубить дерево для сбора смолы.")));
    }
    private void addSouls(EmiRegistry registry,Minecraft mc){
        if(mc.level==null)return;
        var soul=item(Identifier.parse("draconicevolution:mob_soul"));if(soul==Items.AIR)return;
        try{
            @SuppressWarnings("unchecked") var soulId=(net.minecraft.core.component.DataComponentType<Identifier>)BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse("draconicevolution:soul_id"));
            if(soulId==null)throw new IllegalStateException("Missing native soul ID component");
            registry.setDefaultComparison(soul,Comparison.compareData(stack->stack.getOrDefault(soulId,Identifier.withDefaultNamespace("pig"))));
            registry.removeEmiStacks(stack->stack.getEmiStacks().stream().anyMatch(s->s.getId().equals(Identifier.parse("draconicevolution:mob_soul"))&&s.get(soulId)==null));
            var setEntity=soul.getClass().getMethod("setEntity",Identifier.class,ItemStack.class);
            var eligibility=Class.forName("com.brandon3055.draconicevolution.handlers.LootEventHandler").getDeclaredMethod("canEntityDropSoul",LivingEntity.class);
            eligibility.setAccessible(true);
            for(var type:BuiltInRegistries.ENTITY_TYPE){
                if(type==EntityType.PLAYER)continue;
                var entity=type.create(mc.level,EntitySpawnReason.LOAD);
                if(!(entity instanceof Mob))continue;
                var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);
                var output=new ItemStack(soul);setEntity.invoke(soul,id,output);
                registry.addEmiStack(EmiStack.of(output));
                boolean enabled=(Boolean)eligibility.invoke(null,entity);
                var notes=new ArrayList<Component>();
                notes.add(Component.literal("Душа моба: ").append(type.getDescription()));
                if(enabled){
                    notes.add(Component.literal("Добейте моба личным ударом."));
                    notes.add(Component.literal("Обычный меч: нужно зачарование «Жнец»."));
                    notes.add(Component.literal("У оружия Виверны и Драконика есть базовый шанс."));
                    notes.add(Component.literal("«Жнец» повышает шанс; максимум — V."));
                    notes.add(Component.literal("Душа выпадает редко, не при каждом убийстве."));
                }else notes.add(Component.literal("Выпадение души этого моба отключено настройками."));
                add(registry,"soul",id.getNamespace()+"/"+id.getPath(),mobIcon(type),EmiStack.of(output),notes);
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot read native mob soul acquisition",e);}
    }
    private record Source(EmiRecipeCategory category,Identifier id,EmiStack source,EmiStack output,List<Component> notes) implements EmiRecipe {
        public EmiRecipeCategory getCategory(){return category;}
        public Identifier getId(){return id;}
        public List<EmiIngredient> getInputs(){return List.of();}
        public List<EmiIngredient> getCatalysts(){return List.of(source);}
        public List<EmiStack> getOutputs(){return List.of(output);}
        public boolean supportsRecipeTree(){return false;}
        public boolean hideCraftable(){return true;}
        public int getDisplayWidth(){return 166;}
        private List<net.minecraft.util.FormattedCharSequence> lines(){return notes.stream().flatMap(note->Minecraft.getInstance().font.split(note,164).stream()).toList();}
        public int getDisplayHeight(){return 26+lines().size()*10;}
        public void addWidgets(WidgetHolder widgets){
            widgets.addSlot(source,10,0);widgets.addText(Component.literal("→"),70,5,0x404040,false);
            widgets.addSlot(output,136,0).recipeContext(this);
            int y=25;for(var line:lines()){widgets.addText(line,0,y,0x404040,false);y+=10;}
        }
    }
}
