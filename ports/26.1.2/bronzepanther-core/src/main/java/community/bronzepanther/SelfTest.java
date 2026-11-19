package community.bronzepanther;
import community.bronzepanther.mixin.EnergyAccess;
import dev.industrial.EuAcceptor;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class SelfTest {
    private static BlockEntity source,cell;
    private static int ticks;
    public static void register(){NeoForge.EVENT_BUS.addListener(SelfTest::started);NeoForge.EVENT_BUS.addListener(SelfTest::tick);}
    private static void require(boolean condition,String text){if(!condition)throw new IllegalStateException("BronzePanther self-test: "+text);}
    private static BlockEntity place(ServerLevel level,String id,BlockPos pos){level.getChunkAt(pos);level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,BuiltInRegistries.BLOCK.getValue(Identifier.parse(id)).defaultBlockState());return level.getBlockEntity(pos);}
    private static void started(ServerStartedEvent event){try{
        ServerLevel level=event.getServer().overworld();level.setChunkForced(0,0,true);
        CraftingInput grid=CraftingInput.of(2,2,List.of(new ItemStack(Items.COPPER_INGOT),new ItemStack(Items.COPPER_INGOT),new ItemStack(Items.COPPER_INGOT),new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("industrial:tin_ingot")))));
        ItemStack result=event.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).orElseThrow().value().assemble(grid);
        require(result.getCount()==4&&BuiltInRegistries.ITEM.getKey(result.getItem()).equals(Identifier.parse("industrial:bronze_ingot")),"bronze recipe");
        BlockEntity generator=place(level,"industrial:generator",new BlockPos(0,200,0));
        BlockEntity furnace=place(level,"industrial:electric_furnace",new BlockPos(3,200,0));
        require(generator instanceof EnergyAccess&&furnace instanceof EnergyAccess,"energy mixins");
        EnergyAccess gen=(EnergyAccess)generator,sink=(EnergyAccess)furnace;gen.bp$setEnergy(2000);sink.bp$setEnergy(0);
        EnergyHandler out=level.getCapability(Capabilities.Energy.BLOCK,generator.getBlockPos(),Direction.EAST),in=level.getCapability(Capabilities.Energy.BLOCK,furnace.getBlockPos(),Direction.WEST);
        require(out!=null&&in!=null,"FE capabilities");
        try(Transaction tx=Transaction.openRoot()){require(out.extract(37,tx)==37&&in.insert(37,tx)==37,"simulation amounts");}
        require(gen.bp$getEnergy()==2000&&sink.bp$getEnergy()==0,"rollback preserves both EU stores");
        try(Transaction tx=Transaction.openRoot()){require(out.extract(37,tx)==37&&in.insert(37,tx)==37,"transfer amounts");tx.commit();}
        require(gen.bp$getEnergy()==1963&&sink.bp$getEnergy()==37,"1:1 committed energy");
        try(Transaction tx=Transaction.openRoot()){require(in.insert(Integer.MAX_VALUE,tx)==in.getCapacityAsInt()-37,"capacity clamp");}
        require(sink.bp$getEnergy()==37,"capacity simulation rollback");
        require(((EuAcceptor)furnace).acceptEu(13)==13&&in.getAmountAsInt()==50,"native EU and FE use one store");
        source=place(level,"industrial:generator",new BlockPos(10,200,0));cell=place(level,"powah:energy_cell_starter",new BlockPos(11,200,0));
        require(cell!=null,"real Powah cell exists");EnergyHandler initial=level.getCapability(Capabilities.Energy.BLOCK,cell.getBlockPos(),Direction.WEST);try(Transaction tx=Transaction.openRoot()){initial.extract(Integer.MAX_VALUE,tx);tx.commit();}((EnergyAccess)source).bp$setEnergy(1234);try(Transaction tx=Transaction.openRoot()){System.out.println("BRONZEPANTHER_PROBE: capacity="+initial.getCapacityAsLong()+", insert="+initial.insert(1234,tx));}BronzePanther.pushForgeEnergy(level,source);System.out.println("BRONZEPANTHER_DIRECT: source="+((EnergyAccess)source).bp$getEnergy()+", powah="+initial.getAmountAsLong());try(Transaction tx=Transaction.openRoot()){initial.extract(Integer.MAX_VALUE,tx);tx.commit();}((EnergyAccess)source).bp$setEnergy(1234);System.out.println("BRONZEPANTHER_TEST_PHASE1_PASS: recipe, transactions, 1:1 FE/EU");
    }catch(Throwable error){error.printStackTrace();System.out.println("BRONZEPANTHER_TEST_FAIL");}}
    private static void tick(ServerTickEvent.Post event){if(source==null||++ticks!=100)return;try{EnergyHandler handler=source.getLevel().getCapability(Capabilities.Energy.BLOCK,cell.getBlockPos(),Direction.WEST);require(handler!=null,"Powah FE handler");System.out.println("BRONZEPANTHER_TEST_FLOW: source="+((EnergyAccess)source).bp$getEnergy()+", powah="+handler.getAmountAsLong());require(((EnergyAccess)source).bp$getEnergy()==0&&handler.getAmountAsLong()==1234,"automatic generator tick pushes exactly 1234 FE to Powah");System.out.println("BRONZEPANTHER_TEST_ALL_PASS");}catch(Throwable error){error.printStackTrace();System.out.println("BRONZEPANTHER_TEST_FAIL");}}
}

