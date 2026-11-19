package community.bronzepanther;

import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.CloudStatus;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class PerformanceClient {
    private static KeyMapping toggle;
    private static final PerformancePolicy policy = new PerformancePolicy();
    private static String mode = "auto";
    private static int ceiling = -1, applied = -1;
    private static long sampleAt, warmupUntil;
    private static Object lastLevel;
    private static Path file() { return Minecraft.getInstance().gameDirectory.toPath().resolve("config/bronzepanther-performance.json"); }
    public static void register(IEventBus bus) {
        bus.addListener(PerformanceClient::keys);
        NeoForge.EVENT_BUS.addListener(PerformanceClient::tick);
    }
    private static void keys(RegisterKeyMappingsEvent event) {
        toggle = new KeyMapping("key.bronzepanther.performance", GLFW.GLFW_KEY_F8, KeyMapping.Category.MISC);
        event.register(toggle);
        try {
            if (Files.exists(file())) {
                String saved = JsonParser.parseString(Files.readString(file())).getAsJsonObject().get("mode").getAsString();
                if (saved.equals("auto") || saved.equals("potato") || saved.equals("manual")) mode = saved;
            }
        } catch (Exception e) { System.out.println("BronzePanther: using default performance profile"); }
    }
    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime() / 1_000_000_000L;
        if (mc.level != lastLevel) {
            lastLevel = mc.level; policy.reset(); warmupUntil = now + 45; sampleAt = now + 5;
            if (mc.level != null) { ceiling = mc.options.renderDistance().get(); applied = ceiling; }
        }
        if (mc.player == null || toggle == null) return;
        while (toggle.consumeClick()) {
            mode = mode.equals("auto") ? "potato" : mode.equals("potato") ? "manual" : "auto";
            policy.reset(); warmupUntil = now + 30;
            if (mode.equals("potato")) {
                ceiling = 6;
                setDistance(mc, 6);
                mc.options.entityDistanceScaling().set(0.5);
                mc.options.particles().set(ParticleStatus.MINIMAL);
                mc.options.cloudStatus().set(CloudStatus.OFF);
                mc.options.entityShadows().set(false);
                mc.options.save();
            } else if (mode.equals("manual")) {
                // Return the automatic distance reduction; all other graphics remain player-controlled.
                setDistance(mc, Math.max(4, ceiling));
            } else { ceiling = mc.options.renderDistance().get(); applied = ceiling; }
            try {
                Files.createDirectories(file().getParent());
                Files.writeString(file(), "{\"mode\":\"" + mode + "\"}\n");
            } catch (Exception e) { System.out.println("BronzePanther: could not save performance mode"); }
            mc.gui.setOverlayMessage(Component.translatable("message.bronzepanther.performance", Component.translatable("bronzepanther.mode." + mode)), false);
        }
        if (mode.equals("manual") || mc.screen != null || mc.isPaused() || !mc.isWindowActive() || now < warmupUntil || now < sampleAt) return;
        sampleAt = now + 5;
        int current = mc.options.renderDistance().get();
        if (current != applied) { ceiling = current; applied = current; policy.reset(); warmupUntil = now + 30; return; }
        int next = policy.sample(mc.getFps(), current, ceiling, now);
        if (next != current) setDistance(mc, next);
    }
    private static void setDistance(Minecraft mc, int distance) {
        mc.options.renderDistance().set(distance);
        mc.options.broadcastOptions();
        applied = distance;
    }
}
