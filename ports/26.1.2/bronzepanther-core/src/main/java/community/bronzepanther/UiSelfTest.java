package community.bronzepanther;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Disposable-world visual fixtures. Never registered during an ordinary launch. */
public final class UiSelfTest {
    private static int ticks,stage;
    public static void register(){NeoForge.EVENT_BUS.addListener(UiSelfTest::tick);}
    private static void tick(ClientTickEvent.Post event){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)return;
        if(++ticks<200)return;
        int relative=ticks-200;
        if(relative%100==0){try{
            var inventory=mc.player.getInventory();
            switch(stage){
                case 0 -> mc.setScreen(new InventoryScreen(mc.player));
                case 1 -> mc.setScreen(new CraftingScreen(new CraftingMenu(100,inventory),inventory,Component.translatable("container.crafting")));
                case 2 -> mc.setScreen(new ContainerScreen(ChestMenu.sixRows(101,inventory),inventory,Component.translatable("container.chestDouble")));
                case 3 -> mc.setScreen(new FurnaceScreen(new FurnaceMenu(102,inventory),inventory,Component.translatable("container.furnace")));
                case 4 -> Class.forName("dev.emi.emi.api.EmiApi").getMethod("displayAllRecipes").invoke(null);
                case 5 -> InventoryBar.clickFtb("ftbquests:quests");
                case 6 -> {mc.setScreen(new InventoryScreen(mc.player));InventoryBar.clickFtb("ftbteams:my_team");}
                case 7 -> {mc.options.guiScale().set(4);mc.resizeGui();mc.setScreen(new InventoryScreen(mc.player));}
                default -> {return;}
            }
            System.out.println("BRONZEPANTHER_UI_OPEN="+stage+":"+mc.screen.getClass().getName());
        }catch(Throwable e){e.printStackTrace();System.out.println("BRONZEPANTHER_UI_FAIL="+stage);}}
        if(relative%100==75&&stage<8){
            if(stage==5||stage==6){try{Object gui=mc.screen.getClass().getMethod("getGui").invoke(mc.screen);String expected=stage==5?"QuestScreen":"MyTeamScreen";if(!gui.getClass().getSimpleName().equals(expected))throw new IllegalStateException("Expected "+expected+", got "+gui.getClass().getName());System.out.println("BRONZEPANTHER_SIDEBAR_PASS="+expected);}catch(Throwable e){e.printStackTrace();System.out.println("BRONZEPANTHER_UI_FAIL="+stage);}}
            String name="bronzepanther-ui-"+stage+".png";
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),1,message->System.out.println("BRONZEPANTHER_UI_CAPTURE="+name));
            stage++;
            if(stage==8)System.out.println("BRONZEPANTHER_UI_COMPLETE");
        }
    }
}
