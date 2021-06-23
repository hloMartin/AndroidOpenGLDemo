package com.martin.opengl

import android.content.Context
import android.content.res.Resources
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import io.reactivex.rxjava3.subjects.PublishSubject
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

    //纹理1
    private var texture1 = -1
    //纹理2
    private var texture2 = -1
    //辅助纹理
    private var displacement = -1

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
        // 构建着色器程序，并使用。
        mProgram = glCreateAndUseProgramWithShader(VETEXT_SHADER, FRAGMENT_SHADER)
        // 从内存中读取顶点数据设置到 GLSL 中
        glTransformPositionData(mProgram, "a_Position", VERTEX_POINT_DATA, VERTEX_COUNT)
        glTransformPositionData(mProgram, "a_TexCoord", TEXTURE_POINT_DATA, TEXTURE_COUNT)

        texture1Location = glGetUniformLocation(mProgram, "u_TextureUnit")
        texture2Location = glGetUniformLocation(mProgram, "u_TextureUnit1")
        progressLocation = glGetUniformLocation(mProgram, "progress")
        displacementLocation = glGetUniformLocation(mProgram, "displacement")
        resolutionLocation = glGetUniformLocation(mProgram, "resolution")

        glSetInt(texture1Location, 0)
        glSetInt(texture2Location, 1)
        if(displacementLocation != -1){
            glSetInt(displacementLocation, 2)
        }

        texture1 = loadImageTexture(context, image01, this::setSize)
        texture2 = loadImageTexture(context, image02)
        //先判断 GLSL 中是否有定义辅助纹理。
        if(displacementLocation != -1){
            displacement = loadImageTexture(context, R.drawable.disp1)
        }
    }

    private fun setSize(imageWidth: Int, imageHeight: Int){
        //先判断 GLSL 中是否有定义分辨率参数。
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
            glSetFloat(resolutionLocation, displayMetrics.widthPixels.toFloat(), displayMetrics.heightPixels.toFloat(), z, w)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口大小，告诉 OpenGL 可以用来渲染的 surface 的大小
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        //更新进度
        glSetFloat(progressLocation, mProgress)
        if(mProgress == 0f){
            //切换纹理单位绑定的纹理，实现简单的循环切换
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture1)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2)

            if(displacementLocation != -1){
                GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, displacement)
            }
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, TEXTURE_POINT_DATA.size / TEXTURE_COUNT)
    }

}