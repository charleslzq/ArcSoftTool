package com.github.charleslzq.face.baidu

import android.content.res.Resources
import com.github.charleslzq.faceengine.support.StringXMLResource

class BaiduSetting(resources: Resources) {
    val baseUrl by StringXMLResource(
            resources,
            R.string.Baidu_Base
    )
}

fun String.toSafeRetrofitUrl() = takeIf { endsWith("/") } ?: this+"/"