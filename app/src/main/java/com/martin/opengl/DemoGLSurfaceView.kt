package com.martin.opengl

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class DemoGLSurfaceView(context: Context, type: Int) : GLSurfaceView(context) {

    val ANIMATION_TIME = 1500L

    private val renderer: DemoRenderer

    private var isChanging = false

    init {
        setEGLContextClientVersion(2)

        renderer = DemoRenderer(context, getDemoFragmentShader(type))
        setRenderer(renderer)

        //设置 View 的更新需要主动调用 requestRender()
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                startTrans()
            }
        }
        return true
    }

    private fun startTrans() {
        if (isChanging) {
            return
        }
        isChanging = true
        ValueAnimator.ofFloat(0f, 1f)
                .apply {
                    addUpdateListener {
                        renderer.updateProgress(it.animatedValue as Float)
                        requestRender()
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            isChanging = false
                        }
                    })
                    duration = ANIMATION_TIME
                    start()
                }
    }
}