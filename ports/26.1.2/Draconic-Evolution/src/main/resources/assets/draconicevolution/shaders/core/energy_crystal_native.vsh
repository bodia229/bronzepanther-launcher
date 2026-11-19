#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <draconicevolution:energy_crystal_settings.glsl>


in vec3 Position;
in vec2 UV0;



out vec3 fPos;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    fPos = (ModelViewMat * vec4(Position, 1.0)).xyz;
    texCoord0 = UV0;
}
