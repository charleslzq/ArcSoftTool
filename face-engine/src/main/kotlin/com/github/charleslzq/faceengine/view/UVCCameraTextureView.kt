package com.github.charleslzq.faceengine.view

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import com.serenegiant.usb.UVCCamera

class UVCCameraTextureView
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null, @AttrRes defStyle: Int = 0) : TextureView(context, attributeSet, defStyle) {

    var aspectRatio = UVCCamera.DEFAULT_PREVIEW_WIDTH.toDouble() / UVCCamera.DEFAULT_PREVIEW_HEIGHT
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException()
            }
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    fun setAspectRatio(width: Int, height: Int) {
        aspectRatio = width.toDouble() / height
    }

    override fun onMeasure(originalWidthMeasureSpec: Int, originalHeightMeasureSpec: Int) {
        var widthMeasureSpec = originalWidthMeasureSpec
        var heightMeasureSpec = originalHeightMeasureSpec

        if (aspectRatio > 0) {
            var initialWidth = View.MeasureSpec.getSize(widthMeasureSpec)
            var initialHeight = View.MeasureSpec.getSize(heightMeasureSpec)

            val horizPadding = paddingLeft + paddingRight
            val vertPadding = paddingTop + paddingBottom
            initialWidth -= horizPadding
            initialHeight -= vertPadding

            val viewAspectRatio = initialWidth.toDouble() / initialHeight
            val aspectDiff = aspectRatio / viewAspectRatio - 1

            if (Math.abs(aspectDiff) > 0.01) {
                if (aspectDiff > 0) {
                    initialHeight = (initialWidth / aspectRatio).toInt()
                } else {
                    initialWidth = (initialHeight * aspectRatio).toInt()
                }
                initialWidth += horizPadding
                initialHeight += vertPadding
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(initialWidth, View.MeasureSpec.EXACTLY)
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(initialHeight, View.MeasureSpec.EXACTLY)
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}