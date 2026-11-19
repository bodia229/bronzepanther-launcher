package com.brandon3055.draconicevolution.client.shader;

import com.brandon3055.brandonscore.client.shader.BCShader;
import com.brandon3055.brandonscore.client.shader.BCUniform;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Native effect pipelines keep the authored parameter names in a per-draw settings block. */
public final class EffectShader extends BCShader<EffectShader> {
    private static final Set<String> BUILT_INS = Set.of("ModelViewMat", "ProjMat", "ColorModulator", "FogStart", "FogEnd", "FogColor", "FogShape");

    public EffectShader(String path, VertexFormat format) {
        super(Identifier.fromNamespaceAndPath("draconicevolution", path), format, layout(path), path.equals("chaos_block"));
    }

    public BCUniform getUniform(String name) { return uniform(name); }

    private static Map<String, Integer> layout(String path) {
        String resource = "/assets/draconicevolution/shaders/core/" + path + ".json";
        try (InputStream stream = EffectShader.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing effect metadata " + resource);
            var metadata = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Integer> offsets = new LinkedHashMap<>();
            int cursor = 0;
            for (JsonElement entry : metadata.getAsJsonArray("uniforms")) {
                var uniform = entry.getAsJsonObject();
                String name = uniform.get("name").getAsString();
                if (BUILT_INS.contains(name)) continue;
                String type = uniform.get("type").getAsString().toLowerCase(Locale.ROOT);
                int count = uniform.get("count").getAsInt();
                int alignment = switch (type) {
                    case "float", "int" -> 4;
                    case "vec2" -> 8;
                    case "vec3", "vec4" -> 16;
                    default -> throw new IllegalArgumentException("Unsupported effect parameter " + name + ": " + type);
                };
                if (count < 1 || count > 4) throw new IllegalArgumentException("Unsupported effect parameter width " + name);
                cursor = (cursor + alignment - 1) / alignment * alignment;
                if (offsets.put(name, cursor) != null) throw new IllegalArgumentException("Duplicate effect parameter " + name);
                cursor += alignment == 16 ? 16 : count * 4;
            }
            if (cursor > SETTINGS_BYTES) throw new IllegalArgumentException("Effect parameters exceed native settings capacity: " + path);
            return Map.copyOf(offsets);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot read effect metadata " + resource, error);
        }
    }
}
