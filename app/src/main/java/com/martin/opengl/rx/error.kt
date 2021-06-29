package com.martin.opengl.rx

import android.util.Log


/**
 * Created by li guofeng
 * Date: 1/21/21
 *
 */

inline fun <T> runOrLogError(fn: () -> T): T? {
    return try {
        fn()
    } catch (e: Throwable) {
        Log.w("runOrLogError", e)
        null
    }
}
