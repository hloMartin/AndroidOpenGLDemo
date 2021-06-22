package com.martin.opengl

import android.content.Context
import android.content.res.Resources
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class DemoRenderer(val context: Context, fragmentShader: String) : GLSurfaceView.Renderer {

    //顶点着色器
    private var VETEXT_SHADER = """
                        attribute vec4 a_Position;
                        attribute vec2 a_TexCoord;
                        varying vec2 v_TexCoord;
                        void main() {
                            v_TexCoord = a_TexCoord;
                            gl_Position =  a_Position;
                        }
                        """

    //片段着色器
    private var FRAGMENT_SHADER = ""

    //顶点坐标
    private val VERTEX_POINT_DATA = floatArrayOf(
            -1f, -1f,
            -1f, 1f,
            1f, 1f,
            1f, -1f
    )

    //纹理坐标
    private val TEXTURE_POINT_DATA = floatArrayOf(
            0f, 1f,
            0f, 0f,
            1f, 0f,
            1f, 1f
    )

    //vec2：2个 | vec3：3个 | vec4：4个
    //一个顶点坐标占用了几个 float 数。
    private val VERTEX_COUNT = 2

    //一个纹理坐标占用几个 float 数。
    private val TEXTURE_COUNT = 2

    private var mVertexData: FloatBuffer
    private var mTexVertexBuffer: FloatBuffer

    //纹理1
    private var texture1 = 0
    //纹理2
    private var texture2 = 0
    //辅助纹理
    private var displacement = 0

    //纹理对象
    private var texture1Location = -1
    private var texture2Location = -1
    private var displacementLocation = -1
    private var resolutionLocation = -1
    private var progressLocation = -1

    private var mProgram = 0

    //进度控制
    private var mProgress = 0f

    init {
        FRAGMENT_SHADER = fragmentShader
        mVertexData = createFloatBuffer(VERTEX_POINT_DATA)
        mTexVertexBuffer = createFloatBuffer(TEXTURE_POINT_DATA)
    }


    /**
     * 更新进度
     */
    fun updateProgress(progress: Float) {
        mProgress = progress
        if (mProgress == 1f) {
            //动画结束之后，交换纹理，简单实现动画的循环
            mProgress = 0f
            val temp = texture1
            texture1 = texture2
            texture2 = temp
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        //创建并编译顶点着色器
        var vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vertexShader, VETEXT_SHADER)
        GLES20.glCompileShader(vertexShader)
        //获取状态
        var compileStatus = IntArray(1)
        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            log_e("load vertex shader error... code:$VETEXT_SHADER")
        }else{
            log_d("load vertex shader succ....")
        }
        //创建并编译片段着色器
        var fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(fragmentShader, FRAGMENT_SHADER)
        GLES20.glCompileShader(fragmentShader)
        //获取状态
        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            log_e("load fragment shader error... code:$FRAGMENT_SHADER")
        }else{
            log_d("load fragment shader succ....")
        }

        //创建着色器程序
        mProgram = GLES20.glCreateProgram()
        //加载着色器，并将它们链接起来
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)
        //检测状态
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, compileStatus, 0)
        if (compileStatus[0] != GLES20.GL_TRUE) {
            log_e("create Program failed....${GLES20.glGetProgramInfoLog(mProgram)}")
        }else{
            log_d("create Program succ")
        }
        //使用着色器程序
        GLES20.glUseProgram(mProgram)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        //获取着色器内的参数位置
        //顶点位置
        val vertexPositionLocation = GLES20.glGetAttribLocation(mProgram, "a_Position")
        //纹理位置
        val texturePositionLocation = GLES20.glGetAttribLocation(mProgram, "a_TexCoord")
        texture1Location = GLES20.glGetUniformLocation(mProgram, "u_TextureUnit")
        texture2Location = GLES20.glGetUniformLocation(mProgram, "u_TextureUnit1")
        progressLocation = GLES20.glGetUniformLocation(mProgram, "progress")
        displacementLocation = GLES20.glGetUniformLocation(mProgram, "displacement")
        resolutionLocation = GLES20.glGetUniformLocation(mProgram, "resolution")

        log_d("vertexPositionLocation:$vertexPositionLocation  texturePositionLocation:$texturePositionLocation texture1Location:$texture1Location displacementLocation:$displacementLocation")

        //传入顶点坐标
        mVertexData.position(0)
        //stride 传 0，方法会去自动计算 stride，但是要保证当前数组只有一个属性，多个属性需要手动计算。
        GLES20.glVertexAttribPointer(vertexPositionLocation, VERTEX_COUNT, GLES20.GL_FLOAT, false, 0, mVertexData)
        GLES20.glEnableVertexAttribArray(vertexPositionLocation)

        //传入纹理坐标
        mTexVertexBuffer.position(0)
        GLES20.glVertexAttribPointer(texturePositionLocation, TEXTURE_COUNT, GLES20.GL_FLOAT, false, 0, mTexVertexBuffer)
        GLES20.glEnableVertexAttribArray(texturePositionLocation)

        //将图片数据加载到纹理
        texture1 = loadImageTexture(context, image01, this::setSize)
        texture2 = loadImageTexture(context, image02)
        if(displacementLocation != -1){
            displacement = loadImageTexture(context, R.drawable.disp1)
        }
    }

    private fun setSize(imageWidth: Int, imageHeight: Int){
        if(resolutionLocation != -1){
            val displayMetrics = Resources.getSystem().displayMetrics
            val imageAspect = imageHeight.toFloat() / imageWidth
            var z: Float
            var w: Float
            if ((displayMetrics.heightPixels.toFloat() / displayMetrics.widthPixels) > imageAspect) {
                z = displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels * imageAspect
                w = 1f
            } else {
                z = 1f
                w = displayMetrics.heightPixels.toFloat() / displayMetrics.widthPixels.toFloat() / imageAspect
            }
            GLES20.glUniform4f(resolutionLocation, displayMetrics.widthPixels.toFloat(), displayMetrics.heightPixels.toFloat(), z, w)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        //更新进度
        GLES20.glUniform1f(progressLocation, mProgress)

        //设置当前活动的纹理单元为纹理单元0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture1)
        GLES20.glUniform1i(texture1Location, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2)
        GLES20.glUniform1i(texture2Location, 1)

        if (displacementLocation != -1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, displacement)
            GLES20.glUniform1i(displacementLocation, 2)
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, TEXTURE_POINT_DATA.size / TEXTURE_COUNT)
    }

}