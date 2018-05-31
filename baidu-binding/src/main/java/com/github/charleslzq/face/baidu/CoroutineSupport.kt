@file:JvmName("CoroutineSupport")

package com.github.charleslzq.face.baidu

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.runBlocking


fun <T> blockingGet(result: Deferred<T>) = runBlocking { result.await() }
