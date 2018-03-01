package com.github.charleslzq.arcsofttools.kotlin.support

import android.content.res.Resources
import kotlin.reflect.KProperty

/**
 * Created by charleslzq on 18-2-28.
 */
abstract class XMLResourceDelegate<out T>(
    private val resources: Resources,
    private val resourceId: Int,
    private val getter: (Resources, Int) -> T
) {
    operator fun getValue(thisRef: Any, property: KProperty<*>) = getter(resources, resourceId)
}

class BooleanXMLResource(
    resources: Resources,
    resourceId: Int
) : XMLResourceDelegate<Boolean>(resources, resourceId, Resources::getBoolean)

class IntXMLResource(
    resources: Resources,
    resourceId: Int
) : XMLResourceDelegate<Int>(resources, resourceId, Resources::getInteger)

class StringXMLResource(
    resources: Resources,
    resourceId: Int
) : XMLResourceDelegate<String>(resources, resourceId, Resources::getString)