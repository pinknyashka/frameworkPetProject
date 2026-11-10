#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform sampler2D u_normalMap;
uniform sampler2D u_shadowMap;

uniform vec2 u_screenSize;

uniform int u_lightCount;
uniform vec3 u_lightPosition[4];
uniform vec3 u_lightColor[4];
uniform vec3 u_lightParams[4];
uniform float u_ambient;

uniform float u_shadowStrength;
uniform int u_shadowsEnabled;
uniform int u_receiveShadows;

uniform int u_hasNormalMap;
uniform float u_deriveNormals;
uniform float u_heightScale;
uniform float u_normalStep;
uniform float u_specular;
uniform float u_shininess;

varying vec4 v_color;
varying vec2 v_texCoord0;
varying vec2 v_worldPos;

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec4 albedo = texture2D(u_texture, v_texCoord0) * v_color;
    if (albedo.a <= 0.004) {
        discard;
    }

    vec3 normal = vec3(0.0, 0.0, 1.0);

    if (u_hasNormalMap > 0) {
        vec3 mapped = texture2D(u_normalMap, v_texCoord0).rgb * 2.0 - 1.0;
        normal = normalize(mapped);
    } else if (u_deriveNormals > 0.5) {
        float left = luminance(texture2D(u_texture, v_texCoord0 - vec2(u_normalStep, 0.0)).rgb);
        float right = luminance(texture2D(u_texture, v_texCoord0 + vec2(u_normalStep, 0.0)).rgb);
        float up = luminance(texture2D(u_texture, v_texCoord0 - vec2(0.0, u_normalStep)).rgb);
        float down = luminance(texture2D(u_texture, v_texCoord0 + vec2(0.0, u_normalStep)).rgb);
        normal = normalize(vec3((left - right) * u_heightScale, (up - down) * u_heightScale, 1.0));
    }

    vec3 diffuse = vec3(0.0);
    vec3 specular = vec3(0.0);
    vec3 viewDir = vec3(0.0, 0.0, 1.0);

    for (int i = 0; i < 4; i++) {
        if (i < u_lightCount) {
            vec3 lightPos = u_lightPosition[i];
            vec3 lightColor = u_lightColor[i];
            float radius = u_lightParams[i].x;
            float intensity = u_lightParams[i].y;

            vec3 delta = lightPos - vec3(v_worldPos, 0.0);
            float distance2 = dot(delta, delta);
            float attenuation = clamp(1.0 - distance2 / (radius * radius), 0.0, 1.0);
            vec3 lightDir = normalize(vec3(delta.xy, max(delta.z, 0.001)));

            float ndl = max(dot(normal, lightDir), 0.0);
            diffuse += lightColor * ndl * attenuation;

            vec3 halfway = normalize(lightDir + viewDir);
            float spec = pow(max(dot(normal, halfway), 0.0), u_shininess);
            specular += lightColor * spec * attenuation;
        }
    }

    float shadow = 0.0;
    if (u_shadowsEnabled > 0 && u_receiveShadows > 0) {
        vec2 shadowUv = gl_FragCoord.xy / u_screenSize;
        shadow = texture2D(u_shadowMap, shadowUv).r;
    }

    vec3 lighting = vec3(u_ambient) + diffuse;
    lighting *= (1.0 - shadow * u_shadowStrength);

    vec3 color = albedo.rgb * lighting + specular * u_specular * albedo.rgb;
    gl_FragColor = vec4(color, albedo.a);
}
