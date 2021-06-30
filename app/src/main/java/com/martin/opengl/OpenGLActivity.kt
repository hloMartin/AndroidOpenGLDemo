package com.martin.opengl

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.Function3
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject

class OpenGLActivity : AppCompatActivity() {

    private lateinit var root:FrameLayout
    private lateinit var gLView: MyGLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        setContentView(root)

        var index = intent.getIntExtra("type", 1)
        val fragmentShaderSource = getDemoFragmentShader(index)

        addGLView(fragmentShaderSource)
    }

    private fun addGLView(fragmentShaderSource:String){
        val firstFrameSubject = PublishSubject.create<Boolean>()
        firstFrameSubject.subscribe {
            log_d("first draw....")
        }
        Observable.zip(
            loadImageBitmap(image01),
            loadImageBitmap(image02),
            loadImageBitmap(R.drawable.disp1),
            Function3<Bitmap, Bitmap, Bitmap, Triple<Bitmap, Bitmap, Bitmap>> { b1, b2, b3 ->
                Triple(b1, b2, b3)
            }).observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                log_d("bitmap:${it.first} ${it.second} ${it.third}")
                gLView = MyGLSurfaceView.GLBuilder(this)
                    .setFragmentShaderSource(fragmentShaderSource)
                    .addUniform("resolution", floatArrayOf(1f, 1f, 1f, 1f))
                    .addUniform("progress", 0f)
                    .addTextureInfo("u_TextureUnit", it.first)
                    .addTextureInfo("u_TextureUnit1", it.second)
                    .addTextureInfo("displacement", it.third)
                    .setFirstFrameSubject(firstFrameSubject)
                    .build()
                root.addView(gLView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

                gLView.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_UP -> {
                            startAnima()
                        }
                    }
                    true
                }
            }
    }

    private val ANIMATION_TIME = 1500L
    private var isPlaying = false

    private fun startAnima(){
        if (isPlaying) {
            return
        }
        isPlaying = true
        ValueAnimator.ofFloat(0f, 1f)
            .apply {
                addUpdateListener {
                    gLView.updateUniform("progress", it.animatedValue as Float)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        isPlaying = false
                        gLView.exchangeTexture("u_TextureUnit", "u_TextureUnit1")
                        gLView.updateUniform("progress", 0f)
                    }
                })
                duration = ANIMATION_TIME
                start()
            }
    }

    private fun loadImageBitmap(resId: Int): Observable<Bitmap> {
        return Observable.fromCallable {
            BitmapFactory.decodeResource(this.resources, resId, null)
        }.subscribeOn(Schedulers.io())
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }
}