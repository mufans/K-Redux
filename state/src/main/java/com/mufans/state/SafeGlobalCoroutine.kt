package com.mufans.state

import android.os.Looper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// 保存 CoroutineScope
private var scopeRef: AtomicReference<Any> = AtomicReference()

// 自定义的 CoroutineScope
val appGlobalScope: CoroutineScope
    get() {
        while (true) {
            val existing = scopeRef.get() as CoroutineScope?
            if (existing != null) {
                return existing
            }
            val newScope = SafeCoroutineScope(Dispatchers.Main.immediate)
            if (scopeRef.compareAndSet(null, newScope)) {
                return newScope
            }
        }
    }

// 不会崩溃的 CoroutineScope
private class SafeCoroutineScope(context: CoroutineContext) : CoroutineScope, Closeable {
    override val coroutineContext: CoroutineContext =
        SupervisorJob() + context + UncaughtCoroutineExceptionHandler()

    override fun close() {
        coroutineContext.cancelChildren()
    }
}

// 自定义 CoroutineExceptionHandler
private class UncaughtCoroutineExceptionHandler : CoroutineExceptionHandler,
    AbstractCoroutineContextElement(CoroutineExceptionHandler) {
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        // 处理异常
        exception.printStackTrace()
    }
}


val isOnMainThread: Boolean
    get() = Looper.myLooper() == Looper.getMainLooper()

fun runOnMainThread(block: suspend CoroutineScope.() -> Unit): Job {
    return appGlobalScope.launch(if (isOnMainThread) EmptyCoroutineContext else Dispatchers.Main) { block() }
}