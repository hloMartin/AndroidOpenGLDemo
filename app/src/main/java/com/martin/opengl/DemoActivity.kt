package com.martin.opengl

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class DemoActivity : AppCompatActivity() {

    private lateinit var gLView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var index = intent.getIntExtra("type", 1)
        gLView = DemoGLSurfaceView(this, index)
        setContentView(gLView)
    }
}