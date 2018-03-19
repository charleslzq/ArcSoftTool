package com.github.charleslzq.arcsofttools.kotlin.support

import android.content.res.Resources
import com.github.charleslzq.arcsofttools.R
import com.github.charleslzq.faceengine.support.StringXMLResource

/**
 * Created by charleslzq on 18-3-19.
 */
class ArcSoftSettingWithWebSocket(resources: Resources) : ArcSoftSetting(resources) {
    val webSocketUrl by StringXMLResource(resources, R.string.ArcSoft_WebSocket_url)
}