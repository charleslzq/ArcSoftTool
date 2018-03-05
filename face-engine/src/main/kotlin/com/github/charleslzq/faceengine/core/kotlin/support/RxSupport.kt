package com.github.charleslzq.faceengine.core.kotlin.support

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

/**
 * Created by charleslzq on 18-3-5.
 */
fun <T> callOnIo(callable: () -> T) = callOn(Schedulers.io(), callable)

fun <T : Any> callNullableOnIo(callable: () -> T?) = callNullableOn(Schedulers.io(), callable)

fun runOnIo(runnable: () -> Unit) = runOn(Schedulers.io(), runnable)

fun <T> callOnCompute(callable: () -> T) = callOn(Schedulers.computation(), callable)

fun <T : Any> callNullableOnCompute(callable: () -> T?) =
    callNullableOn(Schedulers.computation(), callable)

fun runOnCompute(runnable: () -> Unit) = runOn(Schedulers.computation(), runnable)

fun <T> callOn(scheduler: Scheduler = Schedulers.trampoline(), callable: () -> T): T {
    return Observable.just(1).observeOn(scheduler).map { callable() }.blockingSingle()
}

fun <T : Any> callNullableOn(
    scheduler: Scheduler = Schedulers.trampoline(),
    callable: () -> T?
): T? {
    return Observable.just(1).observeOn(scheduler).map { listOfNotNull(callable()) }
        .blockingSingle().firstOrNull()
}

fun runOn(scheduler: Scheduler = Schedulers.trampoline(), runnable: () -> Unit) {
    Observable.just(1).observeOn(scheduler).subscribe { runnable() }
}