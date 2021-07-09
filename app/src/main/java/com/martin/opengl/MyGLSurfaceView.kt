package com.martin.opengl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import io.reactivex.rxjava3.subjects.PublishSubject
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLSurfaceView(context: Context, private val builder: GLBuilder) : GLSurfaceView(context),GLSurfaceView.Renderer {

    private var program = -1

    private var locationMap = hashMapOf<String, Int>()
    //记录 GLSL uniform 变量绑定的纹理对象 ID
    private var textureIdMap = hashMapOf<String, Int>()
    //记录 GLSL uniform 变量对应的纹理单元
    private var textureUnitMap = hashMapOf<String, Int>()

    private var hasEverDrawFrame = false

    init {
        setEGLContextClientVersion(3)
        setRenderer(this)
        //设置 View 的更新需要主动调用 requestRender()
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun updateUniform(name: String, value: Any){
        queueEvent {
            setUniform(name, value)
            requestRender()
        }
    }

    /**
     * 交换纹理，实际操作就是交换 GLSL 中两个 uniform 类型的纹理变量绑定的纹理对象
     */
    fun exchangeTexture(texture1: String, texture2: String) {
        queueEvent {
            val unit1 = textureUnitMap[texture1] ?: return@queueEvent
            val unit2 = textureUnitMap[texture2] ?: return@queueEvent
            val t1 = textureIdMap[texture1] ?: return@queueEvent
            val t2 = textureIdMap[texture2] ?: return@queueEvent

            textureIdMap[texture1] = t2
            textureIdMap[texture2] = t1

            //重新激活并绑定纹理
            GLES31.glActiveTexture(unit1)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, t2)
            GLES31.glActiveTexture(unit2)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, t1)
        }
    }

    private fun setUniform(name: String, value: Any) {
        locationMap[name]?.let { location ->
            when (value) {
                is Int -> glSetInt(location, value)
                is Float -> glSetFloat(location, value)
                is IntArray -> glSetInt(location, *value)
                is FloatArray -> glSetFloat(location, *value)
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 构建着色器程序，并使用。
        program = glCreateAndUseProgramWithShader(builder.vertexShaderSource, builder.fragmentShaderSource)
        // 从内存中读取顶点数据设置到 GLSL 中
        glTransformPositionData(program, "a_Position", builder.vertexPointArr, builder.vertexPointSize)
        glTransformPositionData(program, "a_TexCoord", builder.texturePointArr, builder.texturePointSize)

        //获取 uniform 参数的 location，并设置对应的值
        for(index in 0 until builder.uniforms.size){
            val uniformInfo = builder.uniforms[index]
            val location = glGetUniformLocation(program, uniformInfo.name)
            if (location == -1) {
                continue
            }
            locationMap[uniformInfo.name]  = location
            setUniform(uniformInfo.name, uniformInfo.value)
        }
        /*
        * 绑定过程分为 3 步
        * 1：通过 OpenGL 生成纹理对象
        * 2：激活指定纹理
        * 3：绑定纹理对象至当前激活的纹理上
        * NOTE:测试发现，多个纹理的情况下，需要先统一生成纹理，然后统一进行激活绑定。
        */
        //生成一个数组用来存储纹理对象
        val textureIdAttr = IntArray(builder.textures.size)
        //先统一加载纹理，并且设置对应纹理在 GLSL 中的变量值
        for (index in 0 until builder.textures.size) {
            val textureInfo = builder.textures[index]
            //获取纹理变量的 location
            val location = glGetUniformLocation(program, textureInfo.textureName)
            if (location == -1) {
                //说明对应的纹理变量在 GLSL 中没有定义。
                textureInfo.enable = false
                continue
            }
            //加载纹理
            val texture = loadImageTexture(textureInfo.bitmap)
            textureIdAttr[index] = texture
            //设置纹理变量的值
            glSetInt(location, texture - 1)
            //添加纹理对象至集合，方便后续释放
            textureIdMap[textureInfo.textureName] = texture
        }
        //开始逐个激活和绑定纹理对象至对应的纹理单元上。
        for (index in 0 until builder.textures.size) {
            var textureInfo = builder.textures[index]
            if (!textureInfo.enable) {
                //说明对应的纹理变量在 GLSL 中没有定义。
                continue
            }
            val textureId = textureIdAttr[index]
            val textUnit = GLES31.GL_TEXTURE0 + textureId - 1
            //记录 uniform 变量对应的纹理单元
            textureUnitMap[textureInfo.textureName] = textUnit
            //激活并绑定纹理
            GLES31.glActiveTexture(textUnit)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口大小，告诉 OpenGL 可以用来渲染的 surface 的大小
        GLES31.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        log_d("[onDrawFrame]")
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, builder.texturePointCount())
        if(!hasEverDrawFrame){
            hasEverDrawFrame = true
            builder.firstFrameSubject?.onNext(true)
            builder.firstFrameSubject?.onComplete()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //释放纹理，防止 OOM
        queueEvent {
            var textures = mutableListOf<Int>()
            textureIdMap.forEach {
                textures.add(it.value)
            }
            glReleaseTexture(*textures.toIntArray())
        }
    }

    class GLBuilder(private val context: Context) {
        //顶掉着色器的坐标数据
        var vertexPointArr = VERTEX_POINT_DATA
            private set
        //每个顶点坐标占用的数组长度
        var vertexPointSize = VERTEX_POINT_DATA_COUNT
            private set
        //片段着色器的坐标数据
        var texturePointArr = TEXTURE_POINT_DATA
            private set
        //片段着色器坐标占用的数组长度
        var texturePointSize = TEXTURE_POTION_DATA_COUNT
            private set
        //顶点着色器 GLSL 代码
        var vertexShaderSource = GL_DEFAULT_SHADER_VERTEX
            private set
        //片段着色器 GLSL 代码
        var fragmentShaderSource = GL_DEFAULT_SHADER_FRAGMENT
            private set
        // GLSL 代码中的 uniform 变量。（对 GLSL 来说是一个常量，只能在外部程序中修改，GLSL 中无法修改）
        var uniforms = mutableListOf<GlUniformBean>()
            private set
        // GLSL 的纹理对象（uniform 变量名，以及对应绑定的用来生成纹理的 bitmap）
        var textures = mutableListOf<GlTextureBean>()
            private set
        var firstFrameSubject:PublishSubject<Boolean>? = null

        fun texturePointCount():Int{
            return texturePointArr.size / texturePointSize
        }

        fun setVertexPointArr(arr: FloatArray) = apply {
            this.vertexPointArr = arr
        }
        fun setVertexPointSize(size: Int) = apply {
            this.vertexPointSize = size
        }
        fun setTexturePointArr(arr:FloatArray) = apply {
            this.texturePointArr = arr
        }
        fun setTexturePointSize(size: Int) = apply {
            this.texturePointSize = size
        }
        fun setVertexShaderSource(shaderSource:String) = apply {
            this.vertexShaderSource = shaderSource
        }
        fun setFragmentShaderSource(shaderSource: String) = apply {
            this.fragmentShaderSource = shaderSource
        }
        fun addUniform(name: String, value: Any) = apply{
            this.uniforms.add(GlUniformBean(name, value))
        }
        fun addTextureInfo(textureName:String, bitmap: Bitmap) = apply {
            this.textures.add(GlTextureBean(textureName, bitmap))
        }
        fun setFirstFrameSubject(subject: PublishSubject<Boolean>) = apply {
            this.firstFrameSubject = subject
        }

        fun build(): MyGLSurfaceView {
            return MyGLSurfaceView(context, this)
        }
    }

}