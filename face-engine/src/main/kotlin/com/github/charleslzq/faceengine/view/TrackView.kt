package com.github.charleslzq.faceengine.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.View
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.support.runOnUI
import com.github.charleslzq.faceengine.view.config.CameraPreviewConfiguration

/**
 * Created by charleslzq on 18-3-29.
 */
class TrackView
constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        @AttrRes defStyle: Int = 0)
    : View(context, attributeSet, defStyle) {
    val rects = mutableListOf<TrackedFace>()
    val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
    }
    var getConfiguration: () -> CameraPreviewConfiguration = { CameraPreviewConfiguration() }

    fun resetRects(newFaces: Collection<TrackedFace>) {
        rects.clear()
        rects.addAll(newFaces)

        runOnUI {
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        rects.forEach {
            canvas.save()
            canvas.rotate(-it.degree.toFloat())
            canvas.drawRect(it.rect, strokePaint.apply {
                getConfiguration().let {
                    color = it.rectColor
                    strokeWidth = it.rectWidth
                }
            })
            canvas.restore()
        }
    }
}