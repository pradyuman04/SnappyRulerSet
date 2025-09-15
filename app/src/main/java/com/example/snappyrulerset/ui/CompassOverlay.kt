package com.example.snappyrulerset.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CompassOverlay {
    var center = PointF(500f, 500f)
    var radius = 150f
    var visible = false

    private val paint: Paint = Paint().apply {
        color = 0x8033B5E5.toInt()
        strokeWidth = 5f
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
        canvas.drawCircle(center.x, center.y, radius, paint)
    }

    /** Project a point to lie on the circle boundary */
    fun projectPointOnCircle(p: PointF): PointF {
        val dx = p.x - center.x
        val dy = p.y - center.y
        val len = sqrt(dx*dx + dy*dy)
        if (len == 0f) return PointF(center.x + radius, center.y)
        val factor = radius / len
        return PointF(center.x + dx*factor, center.y + dy*factor)
    }
}
