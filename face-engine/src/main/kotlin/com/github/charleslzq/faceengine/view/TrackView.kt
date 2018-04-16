package com.github.charleslzq.faceengine.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.View
import com.github.charleslzq.faceengine.core.R
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.support.runOnUI
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by charleslzq on 18-3-29.
 */
fun TypedArray.extract(setup: TypedArray.() -> Unit) {
    setup(this)
    recycle()
}

class TrackView
constructor(context: Context, attributeSet: AttributeSet? = null, @AttrRes defStyle: Int = 0) : View(context, attributeSet, defStyle) {
    val rects = mutableListOf<TrackedFace>()
    val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
    }
    private val _track = AtomicBoolean(true)
    val track = _track.get()

    init {
        attributeSet?.let { context.obtainStyledAttributes(it, R.styleable.FaceDetectView) }?.extract {
            strokePaint.color = getColor(R.styleable.FaceDetectView_rectColor, DEFAULT_COLOR)
            strokePaint.strokeWidth = getDimension(R.styleable.FaceDetectView_rectWidth, DEFAULT_WIDTH)
            _track.set(getBoolean(R.styleable.FaceDetectView_showTrackRect, DEFAULT_TRACK))
            visibility = if (track) {
                VISIBLE
            } else {
                INVISIBLE
            }
        }
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

    companion object {
        const val DEFAULT_TRACK = true
        const val DEFAULT_COLOR = Color.RED
        const val DEFAULT_WIDTH = 1f
    }
}