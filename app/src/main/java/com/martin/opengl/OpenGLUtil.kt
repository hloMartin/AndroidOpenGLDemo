package com.martin.opengl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GlUniformBean(val name: String, val value: Any)
class GlTextureBean(val textureName: String, val bitmap: Bitmap) {
    var enable = true
}

/**
 * 使用着色器代码，创建并使用着色器程序。
 * NOTE：方法需要保证在 GL 线程环境中调用
 */
fun glCreateAndUseProgramWithShader(vertexShaderSource: String, fragmentShaderSource: String): Int {
    //顶点着色器
    var vertexShader = createShader(vertexShaderSource, GLES20.GL_VERTEX_SHADER)
    //片段着色器
    var fragmentShader = createShader(fragmentShaderSource, GLES20.GL_FRAGMENT_SHADER)
    //创建着色器程序
    val program = GLES20.glCreateProgram()
    //加载着色器
    GLES20.glAttachShader(program, vertexShader)
    GLES20.glAttachShader(program, fragmentShader)
    //连接着色器
    GLES20.glLinkProgram(program)
    //检测着色器程序连接状态
    checkProgramLinkStatus(program)
    //使用着色器
    GLES20.glUseProgram(program)
    //删除着色器，它们已经链接到我们的程序中了，已经不再需要了
    GLES20.glDeleteShader(vertexShader)
    GLES20.glDeleteShader(fragmentShader)
    return program
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
    // 计算方法：stride = positionByteCount * sizeOf(type) * 属性个数。 这里 type=GLES20.GL_FLOAT
    GLES20.glVertexAttribPointer(location, positionByteCount, GLES20.GL_FLOAT, false, 0, bufferData)
    //启用顶点属性；顶点属性默认是禁用的
    GLES20.glEnableVertexAttribArray(location)
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
private fun checkProgramLinkStatus(program: Int) {
    val compileStatus = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, compileStatus, 0)
    if (compileStatus[0] != GLES20.GL_TRUE) {
        log_e("create Program failed....${GLES20.glGetProgramInfoLog(program)}")
    } else {
        log_d("create Program success")
    }
}

/**
 * 创建并编译着色器
 */
private fun createShader(shaderSource: String, shaderType: Int): Int {
    var shader = GLES20.glCreateShader(shaderType)
    GLES20.glShaderSource(shader, shaderSource)
    GLES20.glCompileShader(shader)
    //获取状态，查看编译编译是否成功
    val compileStatus = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
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
    return GLES20.glGetAttribLocation(program, name)
}

/**
 * 获取 shader 中 uniform 变量的 location
 * NOTE：方法需要保证在 GL 线程环境中调用
 */
fun glGetUniformLocation(program: Int, name: String): Int {
    return GLES20.glGetUniformLocation(program, name)
}

/**
 * 设置着色器的输入参数 | float 类型
 * NOTE：方法需要保证在 GL 线程环境中调用
 */
fun glSetFloat(location: Int, vararg values: Float) {
    when (values.size) {
        1 -> GLES20.glUniform1f(location, values[0])
        2 -> GLES20.glUniform2f(location, values[0], values[1])
        3 -> GLES20.glUniform3f(location, values[0], values[1], values[2])
        4 -> GLES20.glUniform4f(location, values[0], values[1], values[2], values[3])
        else -> log_e("[glSetFloat] wrong params, $values")
    }
}

/**
 * 设置着色器的输入参数 | Int 类型
 * NOTE：方法需要保证在 GL 线程环境中调用
 */
fun glSetInt(location: Int, vararg values: Int) {
    when (values.size) {
        1 -> GLES20.glUniform1i(location, values[0])
        2 -> GLES20.glUniform2i(location, values[0], values[1])
        3 -> GLES20.glUniform3i(location, values[0], values[1], values[2])
        4 -> GLES20.glUniform4i(location, values[0], values[1], values[2], values[3])
        else -> log_e("[glSetInt] wrong params, $values")
    }
}

/**
 * 释放纹理
 */
fun glReleaseTexture(vararg textures: Int) {
    GLES20.glDeleteTextures(textures.size, textures, 0)
}

fun loadImageTexture(data: ByteBuffer, width: Int, height: Int, format: Int): Int {
    var textureHandles = IntArray(1)

    // 1. 创建纹理对象
    GLES20.glGenTextures(1, textureHandles, 0)
    var textureHandle = textureHandles[0]
    // 2. 将纹理绑定到OpenGL对象上
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)
    // 3. 设置纹理过滤参数:解决纹理缩放过程中的锯齿问题。若不设置，则会导致纹理为黑色
    GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D,
        GLES20.GL_TEXTURE_MIN_FILTER,
        GLES20.GL_LINEAR_MIPMAP_LINEAR
    )
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
    // 4. 通过OpenGL对象读取Bitmap数据，并且绑定到纹理对象上，之后就可以回收Bitmap对象
    GLES20.glTexImage2D(
        GLES20.GL_TEXTURE_2D,
        0,
        format,
        width,
        height,
        0,
        format,
        GLES20.GL_UNSIGNED_BYTE,
        data
    )
    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    return textureHandle
}

/**
 * 通过 OpenGl 对象生成图片纹理对象
 */
fun loadImageTexture(bitmap: Bitmap?): Int {
    bitmap ?: return 0
    var textureHandles = IntArray(1)
    // 1. 创建纹理对象
    GLES20.glGenTextures(1, textureHandles, 0)
    var textureHandle = textureHandles[0]
    if (textureHandles[0] == 0) {
        log_e("Could not generate a new OpenGL texture object.")
        return 0
    }
    // 2. 将纹理绑定到OpenGL对象上
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)
    // 3. 设置纹理过滤参数:解决纹理缩放过程中的锯齿问题。若不设置，则会导致纹理为黑色
    GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D,
        GLES20.GL_TEXTURE_MIN_FILTER,
        GLES20.GL_LINEAR_MIPMAP_LINEAR
    )
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
    // 4. 通过OpenGL对象读取Bitmap数据，并且绑定到纹理对象上，之后就可以回收Bitmap对象
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    //生成 MIP 贴图
    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    return textureHandle
}