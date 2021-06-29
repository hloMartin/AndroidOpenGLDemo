package com.martin.opengl

//默认的顶点着色器代码
const val GL_DEFAULT_SHADER_VERTEX = """
    attribute vec4 a_Position;
    attribute vec2 a_TexCoord;
    varying vec2 v_TexCoord;
    void main() {
        v_TexCoord = a_TexCoord;
        gl_Position =  a_Position;
    }
"""

const val GL_DEFAULT_SHADER_FRAGMENT = """
    precision mediump float;
    varying vec2 v_TexCoord;
    uniform sampler2D u_TextureUnit;
    uniform sampler2D u_TextureUnit1;
    uniform float progress;
        
    void main(){
        vec4 t1 = texture2D(u_TextureUnit, v_TexCoord);
        vec4 t2 = texture2D(u_TextureUnit1, v_TexCoord);
        gl_FragColor = mix(t1, t2, progress);
    }
"""

//默认的顶点坐标
//要确定几个数据为一组确定一个点的坐标（vec2：2个 | vec3：3个 | vec4：4个）
val VERTEX_POINT_DATA = floatArrayOf(
    -1f, -1f,
    -1f, 1f,
    1f, 1f,
    1f, -1f
)

val VERTEX_POINT_DATA_COUNT = 2

val TEXTURE_POTION_DATA_COUNT = 2

//默认的纹理坐标
val TEXTURE_POINT_DATA = floatArrayOf(
    0f, 1f,
    0f, 0f,
    1f, 0f,
    1f, 1f
)