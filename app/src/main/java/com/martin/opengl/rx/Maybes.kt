package com.martin.opengl.rx

import android.os.Build
import androidx.annotation.RequiresApi
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeObserver
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.atomic.AtomicBoolean

object Maybes {
    inline fun <T> new(crossinline onSubscribe: (observer: MaybeObserver<in T>) -> Unit): Maybe<T> {
        return object : Maybe<T>() {
            override fun subscribeActual(observer: MaybeObserver<in T>) {
                onSubscribe(observer)
            }
        }
    }

    fun <T> fromCallOnIoThread(call: (signal: Disposable) -> T?): Maybe<T> {
        return fromCall(call)
            .subscribeOn(RxHelper.IO_THREAD)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun <T> fromCall(call: (signal: Disposable) -> T?): Maybe<T> {
        return new<T> { observer ->
            val signal = Signal()
            observer.onSubscribe(signal)

            val r = try {
                call(signal)
            } catch (e: Throwable) {
                if (!signal.isDisposed) {
                    observer.onError(e)
                }
                return@new
            }

            if (signal.isDisposed) {
                if (r is AutoCloseable) {
                    runOrLogError {
                        r.close()
                    }
                }
            } else {
                if (r !== null)
                    observer.onSuccess(r)
                else
                    observer.onComplete()
            }
        }
    }

    private class Signal : AtomicBoolean(), Disposable {
        override fun dispose() {
            compareAndSet(false, true)
        }

        override fun isDisposed(): Boolean = get()
    }
}