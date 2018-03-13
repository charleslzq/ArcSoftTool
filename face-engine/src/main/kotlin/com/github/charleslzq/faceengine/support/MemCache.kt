package com.github.charleslzq.pacsdemo.support

import android.util.LruCache

/**
 * Created by charleslzq on 17-12-22.
 * @param storageType 缓存中对象的类型
 * @param size 缓存大小
 * @param updateWhen 更新缓存时对象所需满足的前提条件
 * @param keyGenerator 将参数列表拼成一个string作为缓存的key
 */
class MemCache<T>(
        private val storageType: Class<T>,
        size: Int,
        private val updateWhen: (T) -> Boolean = { true },
        private val keyGenerator: (Array<String>) -> String = { it.joinToString("#") }
) {
    private val cache = LruCache<String, T>(size)

    fun contains(vararg args: String) = cache[keyGenerator(arrayOf(*args))] != null

    /**
     * @param args 参数列表
     * @param onMiss 未命中时的操作
     */
    fun load(vararg args: String, onMiss: (Array<String>) -> T?): T? {
        val arguments = arrayOf(*args)
        val objectKey = keyGenerator(arguments)
        val objectInCache = storageType.cast(cache[objectKey])
        return objectInCache ?: onMiss(arguments)?.also {
            if (updateWhen(it)) {
                cache.put(objectKey, it)
            }
        }
    }

    fun update(vararg args: String, data: T) = cache.put(keyGenerator(arrayOf(*args)), data)

    fun clear(vararg args: String) = cache.remove(keyGenerator(arrayOf(*args)))

    /**
     * 清除缓存
     */
    fun clear() = cache.evictAll()
}