package com.martin.opengl

import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GlUniformBean(val name: String, val value: Any)
class GlTextureBean(val textureName: String, val bitmap: Bitmap) {
    var enable = true
}


/**
 * 输入内存中的坐标数据转化成 GLSL 中顶点坐标参数
 * NOTE：方法需要保证在 GL 线程环境中调用
 */
fun glTransformPositionData(
    program: Int,
    name: String,
    pointArr: FloatArray,
    positionByteCount: Int
) {
    val bufferData = createFloatBuffer(pointArr)
    var location = glGetAttrLocation(program, name)
    bufferData.position(0)
    // stride 传 0，方法会去自动计算 stride，但是要保证当前数组只有一个属性，多个属性需要手动计算。
    // 计算方法：stride = positionByteCount * sizeOf(type) * 属性个数。 这里 type=GLES31.GL_FLOAT
    GLES31.glVertexAttribPointer(location, positionByteCount, GLES31.GL_FLOAT, false, 0, bufferData)
    //启用顶点属性；顶点属性默认是禁用的
    GLES31.glEnableVertexAttribArray(location)
}

const val SIZEOF_FLOAT = 4

private fun createFloatBuffer(coords: FloatArray): FloatBuffer {
    return ByteBuffer.allocateDirect(coords.size * SIZEOF_FLOAT).run {
        order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(coords)
                position(0)
            }
    }
}

/**
 * 检测着色器程序连接状态，并输出日志
 */
fun checkProgramLinkStatus(program: Int) {
    val compileStatus = IntArray(1)
    GLES31.glGetProgramiv(program, GLES31.GL_LINK_STATUS, compileStatus, 0)
    if (compileStatus[0] != GLES31.GL_TRUE) {
        log_e("create Program failed....${GLES31.glGetProgramInfoLog(program)}")
    } else {
        log_d("create Program success")
    }
}

/**
 * 创建并编译着色器
 */
fun createShader(shaderSource: String, shaderType: Int): Int {
    var shader = GLES31.glCreateShader(shaderType)
    GLES31.glShaderSource(shader, shaderSource)
    GLES31.glCompileShader(shader)
    //获取状态，查看编译编译是否成功
    val compileStatus = IntArray(1)
    GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, compileStatus, 0)
    if (compileStatus[0] == 0) {
        log_e("load shader error, type:$shaderType, code:$shaderSource")
    } else {
        log_d("load shader success, type:$shaderType")
    }
    return shader
}

/**
 * 获取 vertex shader 中 attribute 变量的 location
 * NOTE：attribute 变量只能在 vertex 中使用
 * NOTE：方法需要保证在 GL 线程环境中调用
 */
fun glGetAttrLocation(program: Int, name: String): Int {
    return GLES31.glGetAttribLocation(program, name)
}

/**
 * 释放纹理
 */
fun glReleaseTexture(vararg textures: Int) {
    GLES31.glDeleteTextures(textures.size, textures, 0)
}

fun loadImageTexture(data: ByteBuffer, width: Int, height: Int, format: Int): Int {
    var textureHandles = IntArray(1)

    // 1. 创建纹理对象
    GLES31.glGenTextures(1, textureHandles, 0)
    var textureHandle = textureHandles[0]
    // 2. 将纹理绑定到OpenGL对象上
    GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureHandle)
    // 3. 设置纹理过滤参数:解决纹理缩放过程中的锯齿问题。若不设置，则会导致纹理为黑色
    GLES31.glTexParameteri(
        GLES31.GL_TEXTURE_2D,
        GLES31.GL_TEXTURE_MIN_FILTER,
        GLES31.GL_LINEAR_MIPMAP_LINEAR
    )
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_REPEAT)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_REPEAT)
    // 4. 通过OpenGL对象读取Bitmap数据，并且绑定到纹理对象上，之后就可以回收Bitmap对象
    GLES31.glTexImage2D(
        GLES31.GL_TEXTURE_2D,
        0,
        format,
        width,
        height,
        0,
        format,
        GLES31.GL_UNSIGNED_BYTE,
        data
    )
    GLES31.glGenerateMipmap(GLES31.GL_TEXTURE_2D)
    GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
    return textureHandle
}

/**
 * 通过 OpenGl 对象生成图片纹理对象
 */
fun loadImageTexture(bitmap: Bitmap?): Int {
    bitmap ?: return 0
    var textureHandles = IntArray(1)
    // 1. 创建纹理对象
    GLES31.glGenTextures(1, textureHandles, 0)
    var textureHandle = textureHandles[0]
    if (textureHandles[0] == 0) {
        log_e("Could not generate a new OpenGL texture object.")
        return 0
    }
    // 2. 将纹理绑定到OpenGL对象上
    GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureHandle)
    // 3. 设置纹理过滤参数:解决纹理缩放过程中的锯齿问题。若不设置，则会导致纹理为黑色
    GLES31.glTexParameteri(
        GLES31.GL_TEXTURE_2D,
        GLES31.GL_TEXTURE_MIN_FILTER,
        GLES31.GL_LINEAR_MIPMAP_LINEAR
    )
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_REPEAT)
    GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_REPEAT)
    // 4. 通过OpenGL对象读取Bitmap数据，并且绑定到纹理对象上，之后就可以回收Bitmap对象
    GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)
    //生成 MIP 贴图
    GLES31.glGenerateMipmap(GLES31.GL_TEXTURE_2D)
    GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
    return textureHandle
}