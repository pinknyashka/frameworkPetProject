#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform vec2 u_offset;
varying vec2 v_texCoord0;

void main() {
    vec4 sum = texture2D(u_texture, v_texCoord0) * 0.227027;
    sum += texture2D(u_texture, v_texCoord0 + u_offset * 1.0) * 0.1945946;
    sum += texture2D(u_texture, v_texCoord0 - u_offset * 1.0) * 0.1945946;
    sum += texture2D(u_texture, v_texCoord0 + u_offset * 2.0) * 0.1216216;
    sum += texture2D(u_texture, v_texCoord0 - u_offset * 2.0) * 0.1216216;
    sum += texture2D(u_texture, v_texCoord0 + u_offset * 3.0) * 0.054054;
    sum += texture2D(u_texture, v_texCoord0 - u_offset * 3.0) * 0.054054;
    sum += texture2D(u_texture, v_texCoord0 + u_offset * 4.0) * 0.016216;
    sum += texture2D(u_texture, v_texCoord0 - u_offset * 4.0) * 0.016216;
    gl_FragColor = sum;
}
