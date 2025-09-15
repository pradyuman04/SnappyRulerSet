package com.example.snappyrulerset.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.*

class RulerOverlay {
    var center = PointF(400f, 400f)
    var length = 800f
    var angleDeg = 0f
    var visible = false

    private val overlayPaint: Paint = Paint().apply {
        color = 0x8033B5E5.toInt()
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    fun toggle() { visible = !visible }

    fun set(center: PointF, angleDeg: Float, length: Float) {
        this.center = center; this.angleDeg = angleDeg; this.length = length
    }

    fun draw(canvas: Canvas) {
        if (!visible) return
        val rad = Math.toRadians(angleDeg.toDouble())
        val hx = cos(rad).toFloat() * length / 2f
        val hy = sin(rad).toFloat() * length / 2f
        val x1 = center.x - hx; val y1 = center.y - hy
        val x2 = center.x + hx; val y2 = center.y + hy
        canvas.drawLine(x1, y1, x2, y2, overlayPaint)
    }

    fun projectPointToRuler(p: PointF): PointF {
        val rad = Math.toRadians(angleDeg.toDouble())
        val dx = cos(rad).toFloat(); val dy = sin(rad).toFloat()
        val vx = p.x - center.x; val vy = p.y - center.y
        val t = vx * dx + vy * dy
        val clamped = max(-length/2f, min(length/2f, t))
        return PointF(center.x + dx*clamped, center.y + dy*clamped)
    }
}
