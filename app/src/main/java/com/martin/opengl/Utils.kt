package com.martin.opengl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import android.view.View
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

const val SIZEOF_FLOAT = 4

const val LOG_TAG = "martin"

//简单做一下不同 demo 下的图片的切换处理
var image01 = R.drawable.img11
var image02 = R.drawable.img12

fun createFloatBuffer(coords: FloatArray): FloatBuffer {
    return ByteBuffer.allocateDirect(coords.size * SIZEOF_FLOAT).run {
        order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply {
                    put(coords)
                    position(0)
                }
    }
}

fun loadVertexShader(shaderCode: String): Int {
    return loadShader(GLES20.GL_VERTEX_SHADER, shaderCode)
}

fun loadFragmentShader(shaderCode: String): Int {
    return loadShader(GLES20.GL_FRAGMENT_SHADER, shaderCode)
}

fun loadShader(type: Int, shaderCode: String): Int {
    return GLES20.glCreateShader(type).also { shader ->
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        var compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            log_e("load shader error... type:$type  code:$shaderCode")
            GLES20.glDeleteShader(shader)
            return 0
        }
        log_d("load shader succ....")
    }
}

fun createProgram(vertexShader: Int, fragmentShader: Int): Int {
    return GLES20.glCreateProgram().apply {
        GLES20.glAttachShader(this, vertexShader)
        GLES20.glAttachShader(this, fragmentShader)
        GLES20.glLinkProgram(this)
        var linkStatus = IntArray(1)
        GLES20.glGetProgramiv(this, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            log_e("create Program failed....")
            GLES20.glDeleteProgram(this)
            return 0
        }
    }
}

fun loadImageTexture(data: ByteBuffer, width: Int, height: Int, format: Int): Int {
    var textureHandles = IntArray(1)

    // 1. 创建纹理对象
    GLES20.glGenTextures(1, textureHandles, 0)
    var textureHandle = textureHandles[0]
    // 2. 将纹理绑定到OpenGL对象上
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)
    // 3. 设置纹理过滤参数:解决纹理缩放过程中的锯齿问题。若不设置，则会导致纹理为黑色
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
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
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
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

/**
 * 根据资源ID获取相应的OpenGL纹理ID，若加载失败则返回0
 * 必须在GL线程中调用
 */
fun loadImageTexture(context: Context, resourceId: Int): Int {
    val textureObjectIds = IntArray(1)
    // 1. 创建纹理对象
    GLES20.glGenTextures(1, textureObjectIds, 0)
    val textureHandle = textureObjectIds[0]
    if (textureObjectIds[0] == 0) {
        log_e("Could not generate a new OpenGL texture object.")
        return 0
    }

    val options = BitmapFactory.Options()
    options.inScaled = false

    val bitmap = BitmapFactory.decodeResource(
            context.resources, resourceId, options)

    if (bitmap == null) {
        log_e("Resource ID $resourceId could not be decoded.")
        // 加载Bitmap资源失败，删除纹理Id
        GLES20.glDeleteTextures(1, textureObjectIds, 0)
        return 0
    }
    // 2. 将纹理绑定到OpenGL对象上
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0])
    // 3. 设置纹理过滤参数:解决纹理缩放过程中的锯齿问题。若不设置，则会导致纹理为黑色
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
    // 4. 通过OpenGL对象读取Bitmap数据，并且绑定到纹理对象上，之后就可以回收Bitmap对象
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    // 5. 生成Mip位图
    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
    // 6. 这里可以对Bitmap对象进行回收
    // 7. 将纹理从OpenGL对象上解绑
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    // 所以整个流程中，OpenGL对象类似一个容器或者中间者的方式，将Bitmap数据转移到OpenGL纹理上
    return textureHandle
}

/**
 * 根据资源ID获取相应的OpenGL纹理ID，若加载失败则返回0
 * 必须在GL线程中调用
 */
fun loadImageTexture(context: Context, resourceId: Int, setSize: (Int, Int) -> Unit): Int {
    val textureObjectIds = IntArray(1)
    // 1. 创建纹理对象
    GLES20.glGenTextures(1, textureObjectIds, 0)
    val textureHandle = textureObjectIds[0]
    if (textureObjectIds[0] == 0) {
        log_e("Could not generate a new OpenGL texture object.")
        return 0
    }

    val options = BitmapFactory.Options()
    options.inScaled = false

    val bitmap = BitmapFactory.decodeResource(
            context.resources, resourceId, options)

    if (bitmap == null) {
        log_e("Resource ID $resourceId could not be decoded.")
        // 加载Bitmap资源失败，删除纹理Id
        GLES20.glDeleteTextures(1, textureObjectIds, 0)
        return 0
    }
    // 2. 将纹理绑定到OpenGL对象上
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0])
    // 3. 设置纹理过滤参数:解决纹理缩放过程中的锯齿问题。若不设置，则会导致纹理为黑色
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
    // 4. 通过OpenGL对象读取Bitmap数据，并且绑定到纹理对象上，之后就可以回收Bitmap对象
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    // 5. 生成Mip位图
    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
    // 6. 这里可以对Bitmap对象进行回收
    setSize(bitmap.width, bitmap.height)
    // 7. 将纹理从OpenGL对象上解绑
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    // 所以整个流程中，OpenGL对象类似一个容器或者中间者的方式，将Bitmap数据转移到OpenGL纹理上
    return textureHandle
}

fun log_d(msg: String?) {
    msg?.run {
        Log.d(LOG_TAG, msg)
    }
}

fun log_e(msg: String?) {
    msg?.run {
        Log.e(LOG_TAG, msg)
    }
}