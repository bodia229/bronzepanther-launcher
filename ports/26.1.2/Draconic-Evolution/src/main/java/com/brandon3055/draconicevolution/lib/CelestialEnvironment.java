package com.brandon3055.draconicevolution.lib;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;

/** Manipulate the native sky clock without advancing game ticks or changing the clock rate. */
public final class CelestialEnvironment {
    private CelestialEnvironment() {}
    public static long dayTime(Level level) { return level.getOverworldClockTime(); }
    public static int advance(ServerLevel level, int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("Negative celestial time advance");
        var clock = level.registryAccess().get(WorldClocks.OVERWORLD);
        if (ticks == 0 || clock.isEmpty()) return 0;
        level.clockManager().addTicks(clock.get(), ticks);
        return ticks;
    }
    public static void weather(ServerLevel level, boolean rain, boolean storm, int duration) {
        if (duration < 0) throw new IllegalArgumentException("Negative weather duration");
        var data = level.getWeatherData();
        data.setRaining(rain);
        data.setThundering(rain && storm);
        data.setRainTime(rain ? duration : 0);
        data.setThunderTime(rain && storm ? duration : 0);
        data.setClearWeatherTime(rain ? 0 : duration);
    }
}
