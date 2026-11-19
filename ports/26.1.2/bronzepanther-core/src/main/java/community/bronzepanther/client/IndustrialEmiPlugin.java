package community.bronzepanther.client;

import dev.emi.emi.api.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.industrial.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ItemLike;
import java.util.*;

/** Read Industrial Neo's own process tables; no server recipes or balances are changed. */
@EmiEntrypoint
public final class IndustrialEmiPlugin implements EmiPlugin {
    private final Map<String,EmiRecipeCategory> categories=new LinkedHashMap<>();
    private final Set<Identifier> ids=new HashSet<>();
    private EmiRegistry registry;
    private static ItemStack stack(ItemLike item){return new ItemStack(item);}
    private static ItemStack stack(ItemLike item,int count){return new ItemStack(item,count);}
    private static ItemStack cell(CellFluid fluid){
        if(fluid.filledCell()!=null)return stack(fluid.filledCell());
        return FluidCellItem.filled(fluid);
    }
    private void category(String id,ItemLike machine,String title){
        var key=Identifier.fromNamespaceAndPath("bronzepanther","industrial/"+id);
        var category=new EmiRecipeCategory(key,EmiStack.of(machine)){
            @Override public Component getName(){return Component.translatable(title);}
        };
        categories.put(id,category);registry.addCategory(category);registry.addWorkstation(category,EmiStack.of(machine));
    }
    private void add(String category,String key,List<ItemStack> inputs,List<ItemStack> outputs,int ticks,String... notes){
        var id=Identifier.fromNamespaceAndPath("bronzepanther","industrial/"+category+"/"+key.replace(':','/'));
        if(!ids.add(id))throw new IllegalStateException("Duplicate Industrial EMI process "+id);
        registry.addRecipe(new Process(categories.get(category),id,inputs,outputs,ticks,List.of(notes)));
    }
    private void add(String category,String key,List<ItemStack> inputs,ItemStack output,int ticks,String... notes){
        add(category,key,inputs,List.of(output),ticks,notes);
    }
    private static String key(Item item){return BuiltInRegistries.ITEM.getKey(item).toString();}
    private void table(String category,Map<Item,ProcessingBlockEntity.Recipe> table){
        table.forEach((input,recipe)->add(category,key(input),List.of(stack(input,recipe.count())),recipe.output().copy(),300,"2 EU/такт · 15 с"));
    }
    @Override public void register(EmiRegistry registry){
        this.registry=registry;categories.clear();ids.clear();
        registry.addAlias(EmiStack.of(Industrial.STEEL_INGOT.get()),Component.literal("Сталь · стальной слиток"));
        registry.addAlias(EmiStack.of(Industrial.CARBON_PLATE.get()),Component.literal("Углепластик · углепластина"));
        category("macerating",Industrial.MACERATOR_ITEM.get(),"block.industrial.macerator");
        category("extracting",Industrial.EXTRACTOR_ITEM.get(),"block.industrial.extractor");
        category("compressing",Industrial.COMPRESSOR_ITEM.get(),"block.industrial.compressor");
        for(int mode=0;mode<3;mode++)category("forming_"+mode,Industrial.METAL_FORMER_ITEM.get(),new String[]{"rei.industrial.extruding","rei.industrial.rolling","rei.industrial.cutting"}[mode]);
        category("blasting",Industrial.BLAST_FURNACE_ITEM.get(),"block.industrial.blast_furnace");
        category("washing",Industrial.ORE_WASHING_ITEM.get(),"block.industrial.ore_washing");
        category("centrifuging",Industrial.THERMAL_CENTRIFUGE_ITEM.get(),"block.industrial.thermal_centrifuge");
        category("block_cutting",Industrial.BLOCK_CUTTER_ITEM.get(),"block.industrial.block_cutter");
        category("canning",Industrial.CANNING_MACHINE_ITEM.get(),"block.industrial.canning_machine");
        category("solid_canning",Industrial.SOLID_CANNING_MACHINE_ITEM.get(),"block.industrial.solid_canning_machine");
        category("bottling",Industrial.FLUID_BOTTLER_ITEM.get(),"block.industrial.fluid_bottler");
        category("pumping",Industrial.PUMP_ITEM.get(),"block.industrial.pump");
        category("heating",Industrial.FLUID_HEAT_GENERATOR_ITEM.get(),"block.industrial.fluid_heat_generator");
        category("exchanging",Industrial.LIQUID_HEAT_EXCHANGER_ITEM.get(),"block.industrial.liquid_heat_exchanger");
        category("fermenting",Industrial.FERMENTER_ITEM.get(),"block.industrial.fermenter");
        category("recycling",Industrial.RECYCLER_ITEM.get(),"block.industrial.recycler");
        category("scrapbox",Industrial.SCRAPBOX.get(),"item.industrial.scrapbox");
        category("electrolysis",Industrial.ELECTROLYZER_ITEM.get(),"block.industrial.electrolyzer");
        category("replicating",Industrial.REPLICATOR_ITEM.get(),"block.industrial.replicator");
        category("distilling",Industrial.SOLAR_DESTILLER_ITEM.get(),"block.industrial.solar_destiller");
        category("condensing",Industrial.CONDENSER_ITEM.get(),"block.industrial.condenser");
        category("lathing",Industrial.LATHE_ITEM.get(),"block.industrial.lathe");
        MaceratorBlockEntity.recipes().forEach((input,output)->add("macerating",key(input),List.of(stack(input,MaceratorBlockEntity.required(input))),output.copy(),300,"2 EU/такт · 15 с"));
        table("extracting",ExtractorBlockEntity.recipes());table("compressing",CompressorBlockEntity.recipes());
        for(int mode=0;mode<3;mode++)table("forming_"+mode,MetalFormerBlockEntity.recipes(mode));
        BlastFurnaceBlockEntity.inputs().stream().sorted(Comparator.comparing(IndustrialEmiPlugin::key)).forEach(input->
            add("blasting",key(input),List.of(stack(input),stack(Industrial.AIR_CELL.get(),6)),
                List.of(stack(Industrial.STEEL_INGOT.get()),stack(Industrial.SLAG.get()),stack(Industrial.EMPTY_CELL.get(),6)),6000,
                "Нужен нагрев до 50 000 HU.","300 с · EU не требуется.","Воздушные капсулы возвращаются пустыми."));
        OreRecipes.washing().forEach((input,recipe)->add("washing",key(input),List.of(stack(input),stack(Industrial.WATER_CELL.get())),recipe.outputs(),500,"1 000 мБ воды · 16 EU/такт · 25 с"));
        OreRecipes.centrifuge().forEach((input,recipe)->add("centrifuging",key(input),List.of(stack(input,recipe.inputCount())),recipe.outputs(),500,"Нагрев: "+recipe.heat()+" HU","48 EU/такт · 25 с"));
        BlockCutterBlockEntity.cuts().forEach((input,cut)->add("block_cutting",key(input),List.of(stack(input,cut.count())),cut.output().copy(),900,"Твёрдость лезвия: не ниже "+cut.hardness(),"48 EU/такт · 45 с"));
        int n=0;
        for(var recipe:CanningMachineBlockEntity.enrichRecipes())add("canning","enrich_"+n++,List.of(cell(recipe.in()),stack(recipe.input())),cell(recipe.out()),200,
            "Обогащение: "+recipe.inAmount()+" мБ → "+recipe.outAmount()+" мБ","4 EU/такт · 10 с");
        for(var fuel:List.of(Industrial.ENRICHED_URANIUM.get(),Industrial.MOX_FUEL.get()))add("canning",key(fuel),List.of(stack(Industrial.FUEL_ROD.get()),stack(fuel)),CanningMachineBlockEntity.bottleResult(stack(Industrial.FUEL_ROD.get()),stack(fuel)),200,"Режим заполнения топливных стержней.","4 EU/такт · 10 с");
        for(var fluid:CellFluid.values()){
            if(fluid==CellFluid.NONE)continue;
            String name=fluid.name().toLowerCase(Locale.ROOT);
            if(fluid.filledCell()!=null){
                if(!fluid.fluid().defaultFluidState().createLegacyBlock().isAir())add("pumping",name,List.of(stack(Industrial.EMPTY_CELL.get())),cell(fluid),20,"Источник жидкости перед насосом.","1 000 мБ · 1 EU/такт · 1 с");
                add("bottling",name+"_empty",List.of(cell(fluid)),stack(Industrial.EMPTY_CELL.get()),100,"Слив 1 000 мБ в резервуар.","2 EU/такт · 5 с");
                add("bottling",name+"_fill",List.of(stack(Industrial.EMPTY_CELL.get())),cell(fluid),100,"Заполнение из резервуара: 1 000 мБ.","2 EU/такт · 5 с");
            }
            if(FluidHeatGeneratorBlockEntity.isFuel(fluid))add("heating",name,List.of(cell(fluid)),stack(Industrial.EMPTY_CELL.get()),20,
                FluidHeatGeneratorBlockEntity.burnPerSecond(fluid)+" мБ/с → "+FluidHeatGeneratorBlockEntity.heatPerTick(fluid)+" HU/такт");
            var cooled=LiquidHeatExchangerBlockEntity.cooled(fluid);
            if(cooled!=CellFluid.NONE)add("exchanging",name,List.of(cell(fluid)),cell(cooled),20,"Охлаждение жидкости: 20 HU на мБ.");
        }
        for(var item:BuiltInRegistries.ITEM){
            int cans=SolidCanningMachineBlockEntity.nutrition(stack(item));
            if(cans>0)add("solid_canning",key(item),List.of(stack(item),stack(Industrial.TIN_CAN.get(),cans)),stack(Industrial.FILLED_TIN_CAN.get(),cans),200,"1 порция еды + "+cans+" пустых консервов.","2 EU/такт");
        }
        add("fermenting","biogas",List.of(stack(Industrial.BIOMASS_CELL.get())),stack(Industrial.BIOGAS_CELL.get()),100,"10 мБ биомассы + 8 000 HU → 200 мБ биогаза.");
        add("fermenting","fertilizer",List.of(stack(Industrial.BIOMASS_CELL.get())),stack(Industrial.FERTILIZER.get()),100,"После расхода 500 мБ биомассы.");
        add("recycling","scrap",List.of(stack(Items.COBBLESTONE)),stack(Industrial.SCRAP.get()),45,"Предметы, кроме панели, палки и снега.","Шанс утильсырья: 1 из 8 · 1 EU/такт");
        var drops=ScrapboxItem.dropTable();
        for(int i=0;i<drops.size();i++)add("scrapbox","drop_"+i,List.of(stack(Industrial.SCRAPBOX.get())),drops.get(i).copy(),0,"Открытие коробки; шанс: "+String.format(Locale.ROOT,"%.2f",ScrapboxItem.chanceOf(i)*100)+" %");
        add("electrolysis","charge",List.of(stack(Industrial.WATER_CELL.get())),stack(Industrial.ELECTROLYZED_WATER_CELL.get()),0,"Рядом с накопителем, заряд ≥ 70 %.","Расход: 20 000 EU из накопителя.");
        add("electrolysis","discharge",List.of(stack(Industrial.ELECTROLYZED_WATER_CELL.get())),stack(Industrial.WATER_CELL.get()),0,"При заряде накопителя ≤ 30 %.","Возврат: 12 000 + 2 000 × уровень EU.");
        add("replicating","iridium",List.of(cell(CellFluid.UU_MATTER),stack(Industrial.CRYSTAL_MEMORY.get())),stack(Industrial.IRIDIUM_ORE.get()),1200,
            "Отсканируйте иридий и сохраните шаблон.","Репликатор ставится рядом с хранилищем.","120 мБ материи · 512 EU/такт · около минуты.");
        add("distilling","water",List.of(stack(Industrial.WATER_CELL.get())),cell(CellFluid.DISTILLED_WATER),72,"1 мБ за 72 такта на солнце, без EU.","Жаркий биом: 36; холодный: 144 такта.");
        add("condensing","water",List.of(stack(Industrial.EMPTY_CELL.get())),cell(CellFluid.DISTILLED_WATER),0,"10 000 мБ пара → 100 мБ воды.","Теплоотвод: +100 мБ пара/такт, 2 EU/такт.");
        add("lathing","blank",List.of(stack(Industrial.IRON_TURNING_BLANK.get())),stack(Industrial.TURNED_BLANK_43434.get()),0,"Пять кнопок изменяют профиль заготовки.","1 000 KU и 1 ед. износа за нажатие.","Профили: 43434, 32323, 54321, 43222.");
        com.mojang.logging.LogUtils.getLogger().info("BronzePanther Industrial EMI processes: {} in {} categories",ids.size(),categories.size());
    }
    private record Process(EmiRecipeCategory category,Identifier id,List<ItemStack> inputs,List<ItemStack> outputs,int ticks,List<String> notes) implements EmiRecipe {
        public EmiRecipeCategory getCategory(){return category;}
        public Identifier getId(){return id;}
        public List<EmiIngredient> getInputs(){return inputs.stream().map(s->(EmiIngredient)EmiStack.of(s.copy())).toList();}
        public List<EmiStack> getOutputs(){
            float chance=1;
            if(id.getPath().startsWith("industrial/recycling/"))chance=0.125F;
            if(id.getPath().startsWith("industrial/scrapbox/drop_"))chance=ScrapboxItem.chanceOf(Integer.parseInt(id.getPath().substring(id.getPath().lastIndexOf('_')+1)));
            final float probability=chance;
            return outputs.stream().map(s->EmiStack.of(s.copy()).setChance(probability)).toList();
        }
        private List<net.minecraft.util.FormattedCharSequence> lines(){
            return notes.stream().flatMap(text->Minecraft.getInstance().font.split(Component.literal(text),164).stream()).toList();
        }
        public int getDisplayWidth(){return 166;}
        public int getDisplayHeight(){return 28+lines().size()*10;}
        public void addWidgets(WidgetHolder widgets){
            var in=getInputs();var out=getOutputs();
            for(int i=0;i<in.size();i++)widgets.addSlot(in.get(i),i*20,0);
            widgets.addFillingArrow(62,1,Math.max(1000,ticks*50));
            for(int i=0;i<out.size();i++)widgets.addSlot(out.get(i),104+i*20,0).recipeContext(this);
            int y=26;for(var line:lines()){widgets.addText(line,0,y,0x404040,false);y+=10;}
        }
        // Processes involving fluids, random drops or machine conditions must not
        // pretend that a displayed filled cell is an unconditional crafting input.
        public boolean supportsRecipeTree(){return Set.of("macerating","extracting","compressing","forming_0","forming_1","forming_2").stream().anyMatch(s->id.getPath().startsWith("industrial/"+s+"/"));}
    }
}
