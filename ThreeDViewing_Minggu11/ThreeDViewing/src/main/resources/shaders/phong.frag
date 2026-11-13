#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightDir;            // arah MENUJU cahaya (ternormalisasi)
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler; // unit 0
uniform sampler2D shadowMap;      // unit 1

uniform bool  useTexture;
uniform bool  useBlinnPhong;
uniform bool  useShadow;
uniform bool  usePCF;
uniform bool  unlit;
uniform float shininess;
uniform float blinnScale;         // pengali eksponen Blinn-Phong (4.0 = setara Phong)
uniform float ambientStrength;
uniform float specularStrength;
uniform float biasMax;
uniform float biasMin;

float ShadowCalculation(vec4 fragPosLightSpace, vec3 normal) {
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;               // NDC [-1,1] -> [0,1]
    if (projCoords.z > 1.0) return 0.0;                // di luar far plane cahaya

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
    if (unlit) {                       // penanda posisi cahaya
        FragColor = vec4(objectColor, 1.0);
        return;
    }

    vec3 norm = normalize(Normal);
    if (!gl_FrontFacing) norm = -norm; // pencahayaan dua sisi saat culling dimatikan
    vec3 viewDir = normalize(viewPos - FragPos);

    vec3 ambient = ambientStrength * lightColor;

    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    float spec = 0.0;
    if (diff > 0.0) {                  // tidak ada highlight di sisi membelakangi cahaya
        if (useBlinnPhong) {
            vec3 halfDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfDir), 0.0), shininess * blinnScale);
        } else {
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
        }
    }
    vec3 specular = specularStrength * spec * lightColor;

    float shadow = useShadow ? ShadowCalculation(FragPosLightSpace, norm) : 0.0;

    // Ambient tidak ikut digelapkan oleh bayangan
    vec3 result = (ambient + (1.0 - shadow) * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
