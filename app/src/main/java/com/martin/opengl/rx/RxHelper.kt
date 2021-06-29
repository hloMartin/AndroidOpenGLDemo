package com.martin.opengl.rx

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers


object RxHelper {
    @JvmField
    val MAIN_THREAD: Scheduler = AndroidSchedulers.mainThread()

    @JvmField
    val IO_THREAD: Scheduler = Schedulers.io()

    @JvmField
    val CPU_THREAD: Scheduler = Schedulers.computation()

    @JvmField
    val TERMINATED: Disposable = object : Disposable {
        override fun isDisposed(): Boolean = true
        override fun dispose() {}
    }

    @JvmField
    val UN_DISPOSABLE: Disposable = object : Disposable {
        override fun isDisposed(): Boolean = false
        override fun dispose() {}
    }
}