#version 300 es
precision highp float;

in vec3 v_color;
in vec3 v_normal;
in vec3 v_viewDir;
in float v_flag;

uniform vec3 u_lightDir;
uniform vec3 u_sunColor;
uniform vec3 u_ambientColor;

out vec4 fragColor;

void main() {
    vec3 n = normalize(v_normal);
    vec3 l = normalize(u_lightDir);
    vec3 v = normalize(v_viewDir);
    
    float diff = max(dot(n, l), 0.0);
    vec3 diffuse = diff * u_sunColor * v_color;
    vec3 ambient = u_ambientColor * v_color;
    
    // Simple specular for metal/glass
    vec3 spec = vec3(0.0);
    if (v_flag > 0.5) {
        vec3 h = normalize(l + v);
        float specPower = v_flag > 2.5 ? 64.0 : 32.0; // Glass is smoother
        float specInt = v_flag > 1.5 ? 0.8 : 0.4;
        spec = pow(max(dot(n, h), 0.0), specPower) * u_sunColor * specInt;
    }
    
    // Emissive override
    if (v_flag > 1.5 && v_flag < 2.5) {
        fragColor = vec4(v_color, 1.0);
    } else {
        fragColor = vec4(ambient + diffuse + spec, 1.0);
    }
}
