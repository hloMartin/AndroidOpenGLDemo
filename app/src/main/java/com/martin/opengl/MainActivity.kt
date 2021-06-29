package com.martin.opengl

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)
    }

    fun clickDemo1(view: View) {
        start(1)
    }

    fun clickDemo2(view: View) {
        start(2)
    }

    fun clickDemo3(view: View) {
        start(3)
    }

    fun clickDemo4(view: View) {
        start(4)
    }

    fun clickDemo5(view: View) {
        start(5)
    }

    fun clickDemo6(view: View) {
        start(6)
    }

    fun clickDemo7(view: View) {
        start(7)
    }

    fun clickDemo8(view: View) {
        start(8)
    }

    private fun start(index: Int) {
        startActivity(Intent(this, OpenGLActivity::class.java).apply {
            putExtra("type", index)
        })
    }


}