package com.glasscc

import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

/** Glas-Karte: transparente Füllung + Glanz oben + Kantenlicht (Specular-Rim) */
open class GlassCard(c: Context, protected val r: Float) : FrameLayout(c) {
    var active = false
        set(v) { field = v; invalidate() }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gloss = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    protected val box = RectF()

    init { setWillNotDraw(false) }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        box.set(2f, 2f, w - 2f, h - 2f)
        gloss.shader = LinearGradient(0f, 0f, 0f, h * .65f, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        edge.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xEEFFFFFF.toInt(), 0x18FFFFFF, 0x18FFFFFF, 0xAAFFFFFF.toInt()),
            floatArrayOf(0f, .4f, .6f, 1f), Shader.TileMode.CLAMP
        )
    }

    protected open fun inner(cv: Canvas) {}

    override fun onDraw(cv: Canvas) {
        fill.color = if (active) 0x77FFFFFF else 0x30FFFFFF
        cv.drawRoundRect(box, r, r, fill)
        inner(cv)
        cv.drawRoundRect(box, r, r, gloss)
        cv.drawRoundRect(box, r, r, edge)
    }
}

/** Vertikaler Glas-Slider wie im iOS-Kontrollzentrum */
class GlassSlider(c: Context, r: Float, emoji: String, private var v: Float, private val onChange: (Float) -> Unit) : GlassCard(c, r) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEEFFFFFF.toInt() }
    private val clip = Path()

    init {
        val tv = TextView(c)
        tv.text = emoji; tv.textSize = 22f
        val lp = FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        lp.bottomMargin = c.dp(16)
        addView(tv, lp)
    }

    override fun inner(cv: Canvas) {
        clip.reset(); clip.addRoundRect(box, r, r, Path.Direction.CW)
        cv.save(); cv.clipPath(clip)
        cv.drawRect(0f, height * (1 - v), width.toFloat(), height.toFloat(), p)
        cv.restore()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        v = (1 - e.y / height).coerceIn(0f, 1f)
        invalidate(); onChange(v)
        return true
    }
}
