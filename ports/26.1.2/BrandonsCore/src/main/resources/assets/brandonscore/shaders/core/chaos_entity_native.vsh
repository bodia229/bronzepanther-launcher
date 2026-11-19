#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <brandonscore:native_settings.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:light.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#moj_import <minecraft:fog.glsl>


in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;











out vec3 fPos;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec4 normal;
out vec2 posMod;

void main() {
    fPos = (ModelViewMat * ModelMat * vec4(Position, 1.0)).xyz;
    gl_Position = ProjMat * ModelViewMat * ModelMat * vec4(Position, 1.0);

    vec3 fogPos = (ModelMat * vec4(Position, 1.0)).xyz;
    sphericalVertexDistance = fog_spherical_distance(fogPos);
    cylindricalVertexDistance = fog_cylindrical_distance(fogPos);
    if (DisableLight) {
        vertexColor = Color;
        lightMapColor = vec4(1.0);
    } else if (SimpleLight) {
        vertexColor = Color * sample_lightmap(Sampler2, UV2);
        lightMapColor = vec4(1.0);
    } else {
        vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
        lightMapColor = sample_lightmap(Sampler2, UV2);
    }
    if (DisableOverlay) {
        overlayColor = vec4(1.0);
    } else {
        overlayColor = texelFetch(Sampler1, UV1, 0);
    }
    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * ModelMat * vec4(Normal, 0.0);
    posMod = normalize(normal).xy / 100;
}
