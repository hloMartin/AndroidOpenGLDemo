package com.martin.opengl

import android.opengl.GLES31

class Shader(vertexShaderSource: String, fragmentShaderSource: String) {

    var programId = -1

    private var locationMap = hashMapOf<String, Int>()

    init {
        //顶点着色器
        var vertexShader = createShader(vertexShaderSource, GLES31.GL_VERTEX_SHADER)
        //片段着色器
        var fragmentShader = createShader(fragmentShaderSource, GLES31.GL_FRAGMENT_SHADER)
        //创建着色器程序
        programId = GLES31.glCreateProgram()
        //加载着色器
        GLES31.glAttachShader(programId, vertexShader)
        GLES31.glAttachShader(programId, fragmentShader)
        //连接着色器
        GLES31.glLinkProgram(programId)
        //检测着色器程序连接状态
        checkProgramLinkStatus(programId)
        //删除着色器，它们已经链接到我们的程序中了，已经不再需要了
        GLES31.glDeleteShader(vertexShader)
        GLES31.glDeleteShader(fragmentShader)
    }

    fun use(){
        GLES31.glUseProgram(programId)
    }

    fun getUniformLocation(name: String): Int {
        if (locationMap.containsKey(name)) {
            locationMap[name]?.let { return it }
        }
        GLES31.glGetUniformLocation(programId, name).let {
            locationMap[name] = it
            return it
        }
    }

    fun setUniform(name: String, value: Any) {
        when (value) {
            is Int -> setInt(name, value)
            is Float -> setFloat(name, value)
            is IntArray -> setInt(name, *value)
            is FloatArray -> setFloat(name, *value)
        }
    }

    private fun setFloat(name: String, vararg values: Float) {
        val location = getUniformLocation(name)
        when (values.size) {
            1 -> GLES31.glUniform1f(location, values[0])
            2 -> GLES31.glUniform2f(location, values[0], values[1])
            3 -> GLES31.glUniform3f(location, values[0], values[1], values[2])
            4 -> GLES31.glUniform4f(location, values[0], values[1], values[2], values[3])
            else -> log_e("[setFloat] wrong params, $values")
        }
    }

    private fun setInt(name: String, vararg values: Int) {
        val location = getUniformLocation(name)
        when (values.size) {
            1 -> GLES31.glUniform1i(location, values[0])
            2 -> GLES31.glUniform2i(location, values[0], values[1])
            3 -> GLES31.glUniform3i(location, values[0], values[1], values[2])
            4 -> GLES31.glUniform4i(location, values[0], values[1], values[2], values[3])
            else -> log_e("[glSetInt] wrong params, $values")
        }
    }

}