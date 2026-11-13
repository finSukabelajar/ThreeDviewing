#version 330 core
out vec4 FragColor;

in vec3 vAmbient;
in vec3 vDirect;
in vec3 FragNormal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightDir;
uniform vec3 objectColor;
uniform sampler2D textureSampler;
uniform sampler2D shadowMap;
uniform bool  useTexture;
uniform bool  useShadow;
uniform bool  usePCF;
uniform bool  unlit;
uniform float biasMax;
uniform float biasMin;

float ShadowCalculation(vec4 fragPosLightSpace, vec3 normal) {
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    if (projCoords.z > 1.0) return 0.0;

    float currentDepth = projCoords.z;
    float bias = max(biasMax * (1.0 - dot(normal, lightDir)), biasMin);

    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    if (usePCF) {
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
                shadow += (currentDepth - bias > pcfDepth) ? 1.0 : 0.0;
            }
        }
        shadow /= 9.0;
    } else {
        float closestDepth = texture(shadowMap, projCoords.xy).r;
        shadow = (currentDepth - bias > closestDepth) ? 1.0 : 0.0;
    }
    return shadow;
}

void main() {
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    if (unlit) {
        FragColor = vec4(objectColor, 1.0);
        return;
    }
    float shadow = useShadow ? ShadowCalculation(FragPosLightSpace, normalize(FragNormal)) : 0.0;
    FragColor = vec4((vAmbient + (1.0 - shadow) * vDirect) * baseColor, 1.0);
}
