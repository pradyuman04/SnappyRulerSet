package com.example.snappyrulerset.utils

import android.graphics.PointF
import kotlin.math.*

object GeometryUtils {
    fun distance(a: PointF, b: PointF): Float = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    fun angleDegrees(a: PointF, b: PointF): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return ((atan2(dy.toDouble(), dx.toDouble()) * 180.0 / Math.PI).toFloat() + 360f) % 360f
    }

    fun projectPointOnLine(pt: PointF, a: PointF, b: PointF): PointF {
        val vx = b.x - a.x
        val vy = b.y - a.y
        val wx = pt.x - a.x
        val wy = pt.y - a.y
        val c1 = vx * wx + vy * wy
        val c2 = vx * vx + vy * vy
        val t = if (c2 == 0f) 0f else (c1 / c2)
        return PointF(a.x + vx * t, a.y + vy * t)
    }

    fun intersectionOfLines(a1: PointF, a2: PointF, b1: PointF, b2: PointF): PointF? {
        val x1 = a1.x; val y1 = a1.y
        val x2 = a2.x; val y2 = a2.y
        val x3 = b1.x; val y3 = b1.y
        val x4 = b2.x; val y4 = b2.y
        val denom = (y4 - y3)*(x2 - x1) - (x4 - x3)*(y2 - y1)
        if (abs(denom) < 1e-6) return null
        val ua = ((x4 - x3)*(y1 - y3) - (y4 - y3)*(x1 - x3)) / denom
        return PointF(x1 + ua*(x2 - x1), y1 + ua*(y2 - y1))
    }

    fun snapAngle(angle: Float, allowed: List<Float>, threshold: Float): Float {
        var nearest = angle; var best = Float.MAX_VALUE
        for (a in allowed) {
            val d = abs(((angle - a + 180f + 360f) % 360f) - 180f)
            if (d < best) { best = d; nearest = a }
        }
        return if (best <= threshold) nearest else angle
    }
}
