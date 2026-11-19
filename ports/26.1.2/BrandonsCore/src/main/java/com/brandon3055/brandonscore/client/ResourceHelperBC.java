//package com.brandon3055.brandonscore.client;
//
//import com.brandon3055.brandonscore.BrandonsCore;
//import net.minecraft.client.Minecraft;
//import net.minecraft.resources.Identifier;
//
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Map;
//
///**
// * Created by brandon3055 on 29/3/2016.
// * Helper function to bind and cache resource locations.
// */
//@Deprecated
//public class ResourceHelperBC {
//
//    private static Identifier vanillaParticles;
//    private static Map<String, Identifier> cachedResources = new HashMap<String, Identifier>();
//    public static final String RESOURCE_PREFIX = BrandonsCore.MODID.toLowerCase(Locale.ENGLISH) + ":";
//
//    public static void bindTexture(Identifier texture) {
//        Minecraft.getInstance().textureManager.bindForSetup(texture);
//    }
//
//    public static Identifier getResource(String rs) {
//        if (!cachedResources.containsKey(rs)) cachedResources.put(rs, Identifier.parse(RESOURCE_PREFIX + rs));
//        return cachedResources.get(rs);
//    }
//
//    public static Identifier getResourceRAW(String rs) {
//        if (!cachedResources.containsKey(rs)) cachedResources.put(rs, Identifier.parse(rs));
//        return cachedResources.get(rs);
//    }
//
//    public static void bindTexture(String rs) {
//        bindTexture(getResource(rs));
//    }
//}
