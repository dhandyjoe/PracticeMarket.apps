package com.automation.practice.feature.shop

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Pinch zoom 50–300%, double tap 1× ↔ 2×, geser saat diperbesar, dan lempar horizontal
 * untuk pindah gambar saat zoom ≤ 1× (FR-SHOP-13). Anak pertama yang diskalakan.
 */
class ZoomLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    var zoom = 1f
        private set
    var onZoomChanged: ((Float) -> Unit)? = null

    /** -1 = gambar sebelumnya, +1 = berikutnya. */
    var onSwipe: ((Int) -> Unit)? = null

    private val content get() = getChildAt(0)

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            setZoom(zoom * detector.scaleFactor)
            return true
        }
    })

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onDoubleTap(e: MotionEvent): Boolean {
            setZoom(if (zoom == 1f) DOUBLE_TAP_ZOOM else 1f)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (zoom <= 1f) return false
            content.translationX = clampPan(content.translationX - dx, content.width, width)
            content.translationY = clampPan(content.translationY - dy, content.height, height)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (zoom > 1f || abs(vx) < FLING_MIN_VELOCITY || abs(vx) < abs(vy)) return false
            onSwipe?.invoke(if (vx < 0) 1 else -1)
            return true
        }
    })

    fun setZoom(value: Float) {
        zoom = value.coerceIn(MIN_ZOOM, MAX_ZOOM)
        content.scaleX = zoom
        content.scaleY = zoom
        content.translationX = clampPan(content.translationX, content.width, width)
        content.translationY = clampPan(content.translationY, content.height, height)
        onZoomChanged?.invoke(zoom)
    }

    fun reset() {
        content.translationX = 0f
        content.translationY = 0f
        setZoom(1f)
    }

    private fun clampPan(value: Float, contentSize: Int, viewSize: Int): Float {
        val limit = ((contentSize * zoom - viewSize) / 2f).coerceAtLeast(0f)
        return value.coerceIn(-limit, limit)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        scaleDetector.onTouchEvent(event)
        if (!scaleDetector.isInProgress) gestures.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick() = super.performClick()

    private companion object {
        const val MIN_ZOOM = 0.5f
        const val MAX_ZOOM = 3f
        const val DOUBLE_TAP_ZOOM = 2f
        const val FLING_MIN_VELOCITY = 800f
    }
}
