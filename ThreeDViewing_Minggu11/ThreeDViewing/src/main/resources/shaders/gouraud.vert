#version 330 core
// Gouraud shading: pencahayaan dihitung PER VERTEX, hasilnya diinterpolasi.
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;

out vec3 vAmbient;
out vec3 vDirect;                 // diffuse + specular
out vec3 FragNormal;
out vec2 TexCoords;
out vec4 FragPosLightSpace;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat3 normalMatrix;
uniform mat4 lightSpaceMatrix;
uniform vec3 lightDir;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform bool  useBlinnPhong;
uniform float shininess;
uniform float blinnScale;
uniform float ambientStrength;
uniform float specularStrength;

void main() {
    vec4 worldPos = model * vec4(aPos, 1.0);
    vec3 norm = normalize(normalMatrix * aNormal);
    vec3 viewDir = normalize(viewPos - worldPos.xyz);

    float diff = max(dot(norm, lightDir), 0.0);
    float spec = 0.0;
    if (diff > 0.0) {
        if (useBlinnPhong) {
            vec3 halfDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfDir), 0.0), shininess * blinnScale);
        } else {
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
        }
    }

    vAmbient = ambientStrength * lightColor;
    vDirect  = diff * lightColor + specularStrength * spec * lightColor;
    FragNormal = norm;
    TexCoords = aTexCoords;
    FragPosLightSpace = lightSpaceMatrix * worldPos;
    gl_Position = projection * view * worldPos;
}
