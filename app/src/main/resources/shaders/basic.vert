#version 300 es
precision highp float;

layout(location = 0) in vec3 a_position;
layout(location = 1) in vec3 a_normal;
layout(location = 2) in vec3 a_color;
layout(location = 3) in float a_flag;

uniform mat4 u_mvp;
uniform mat4 u_model;
uniform mat3 u_normalMat;
uniform vec3 u_lightDir;
uniform vec3 u_viewPos;

out vec3 v_color;
out vec3 v_normal;
out vec3 v_viewDir;
out float v_flag;

void main() {
    vec4 worldPos = u_model * vec4(a_position, 1.0);
    gl_Position = u_mvp * vec4(a_position, 1.0);
    
    v_color = a_color;
    v_normal = u_normalMat * a_normal;
    v_viewDir = u_viewPos - worldPos.xyz;
    v_flag = a_flag;
}
