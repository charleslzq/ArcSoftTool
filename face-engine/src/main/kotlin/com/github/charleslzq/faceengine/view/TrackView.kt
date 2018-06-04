package com.github.charleslzq.faceengine.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.View
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.support.runOnUI

/**
 * Created by charleslzq on 18-3-29.
 */
class TrackView
constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        @AttrRes defStyle: Int = 0)
    : View(context, attributeSet, defStyle), CameraPreviewConfigurable {
    val rects = mutableListOf<TrackedFace>()
    val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
    }

    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {
        visibility = if (cameraPreviewConfiguration.showRect) {
            VISIBLE
        } else {
            INVISIBLE
        }
        strokePaint.color = cameraPreviewConfiguration.rectColor
        strokePaint.strokeWidth = cameraPreviewConfiguration.rectWidth
    }

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
            canvas.drawRect(it.rect, strokePaint)
            canvas.restore()
        }
    }
}