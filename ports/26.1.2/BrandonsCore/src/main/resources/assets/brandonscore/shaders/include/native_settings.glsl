layout(std140) uniform BCSettings {
    mat4 ModelMat;
    vec4 BaseColor;
    ivec2 UV1Override;
    ivec2 UV2Override;
    float Time;
    float Decay;
    float Transition;
    float Hue;
    float Yaw;
    float Pitch;
    float Alpha;
    bool SimpleLight;
    bool DisableLight;
    bool DisableOverlay;
    float Activation;
};
