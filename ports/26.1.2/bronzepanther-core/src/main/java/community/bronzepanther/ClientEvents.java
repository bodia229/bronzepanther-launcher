package community.bronzepanther;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientEvents {
    private static int proofFrames;
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("bronzepanther", "textures/gui/title_background.png");
    public static void register() { NeoForge.EVENT_BUS.addListener(ClientEvents::background); NeoForge.EVENT_BUS.addListener(InventoryBar::init); }
    private static void background(ScreenEvent.Render.Background event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        GuiGraphicsExtractor gui = event.getGuiGraphics();
        int width = gui.guiWidth(), height = gui.guiHeight();
        double imageAspect = 1672.0 / 941.0, screenAspect = (double) width / height;
        float u = screenAspect < imageAspect ? (float) ((1 - screenAspect / imageAspect) / 2) : 0;
        float v = screenAspect > imageAspect ? (float) ((1 - imageAspect / screenAspect) / 2) : 0;
        gui.blit(BACKGROUND, 0, 0, width, height, u, 1 - u, v, 1 - v);
        gui.fill(0, 0, width, height, 0x18000000);
        if (Boolean.getBoolean("bronzepanther.screenshot") && ++proofFrames == 120) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            System.out.println("BRONZEPANTHER_MENU_RU=" + net.minecraft.locale.Language.getInstance().getOrDefault("block.industrial.generator"));
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, "bronzepanther-menu.png", mc.getMainRenderTarget(), 1, message -> System.out.println("BRONZEPANTHER_SCREENSHOT_SAVED"));
        }
    }
}
