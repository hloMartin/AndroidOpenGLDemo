package com.martin.opengl

import android.util.Log

const val LOG_TAG = "martin"

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