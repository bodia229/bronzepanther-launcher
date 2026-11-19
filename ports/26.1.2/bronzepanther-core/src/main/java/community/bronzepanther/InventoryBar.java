package community.bronzepanther;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ModListScreen;

/** One row beside EMI, independent of FTB's automatic grid compaction. */
public final class InventoryBar {
    public static void init(ScreenEvent.Init.Post event){
        if(!(event.getScreen() instanceof AbstractContainerScreen<?> screen))return;
        String[] labels={"Р","К","Т","М"};
        String[] tips={"Все рецепты","Книга заданий","Моя команда","Настройки модов"};
        for(int i=0;i<labels.length;i++){
            final int action=i;
            Button button=Button.builder(Component.literal(labels[i]),b->{try{
                switch(action){
                    case 0 -> Class.forName("dev.emi.emi.api.EmiApi").getMethod("displayAllRecipes").invoke(null);
                    case 1 -> clickFtb("ftbquests:quests");
                    case 2 -> clickFtb("ftbteams:my_team");
                    case 3 -> Minecraft.getInstance().setScreen(new ModListScreen(screen));
                }
            }catch(ReflectiveOperationException e){com.mojang.logging.LogUtils.getLogger().error("BronzePanther inventory action {} failed",action,e);Minecraft.getInstance().gui.setOverlayMessage(Component.literal("Открыть меню не удалось. Повтори после загрузки мира."),false);}})
                .bounds(screen.getGuiLeft()>=120?48+i*18:2+i*18,screen.height-(screen.getGuiLeft()>=120?20:40),16,16).tooltip(Tooltip.create(Component.literal(tips[i]))).build();
            event.addListener(button);
        }
    }
    static void clickFtb(String id)throws ReflectiveOperationException{
        Class<?> manager=Class.forName("dev.ftb.mods.ftblibrary.sidebar.SidebarButtonManager");
        Object instance=manager.getField("INSTANCE").get(null);
        Collection<?> buttons=(Collection<?>)manager.getMethod("getButtons").invoke(instance);
        for(Object registered:buttons){if(id.equals(registered.getClass().getMethod("getId").invoke(registered).toString())){
            registered.getClass().getMethod("clickButton",boolean.class).invoke(registered,false);return;
        }}
        throw new NoSuchMethodException("FTB sidebar action not registered: "+id);
    }
}
