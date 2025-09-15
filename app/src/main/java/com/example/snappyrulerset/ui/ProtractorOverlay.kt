package com.example.snappyrulerset.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class ProtractorOverlay {
    var center = PointF(600f, 600f)
    var radius = 200f
    var visible = false

    private val paint: Paint = Paint().apply {
        color = 0x80FF8800.toInt()
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    fun toggle() { visible = !visible }

    fun set(center: PointF, radius: Float) {
        this.center = center
        this.radius = radius
    }

    fun draw(canvas: Canvas) {
        if (!visible) return
        // draw half-circle for protractor
        val steps = 180
        for (i in 0..steps) {
            val angle = i * PI / steps
            val x = (center.x + radius * cos(angle)).toFloat()
            val y = (center.y - radius * sin(angle)).toFloat()
            canvas.drawPoint(x, y, paint)
        }
    }
}
