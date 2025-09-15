package com.example.snappyrulerset.utils

import android.graphics.PointF

class Quadtree(private val bounds: RectFNode, private val capacity: Int = 8) {
    private val points = mutableListOf<QuadtreePoint>()
    private var divided = false
    private var nw: Quadtree? = null
    private var ne: Quadtree? = null
    private var sw: Quadtree? = null
    private var se: Quadtree? = null

    fun insert(pt: QuadtreePoint): Boolean {
        if (!bounds.contains(pt.point.x, pt.point.y)) return false
        if (points.size < capacity) { points.add(pt); return true }
        else {
            if (!divided) subdivide()
            if (nw!!.insert(pt)) return true
            if (ne!!.insert(pt)) return true
            if (sw!!.insert(pt)) return true
            if (se!!.insert(pt)) return true
        }
        return false
    }

    fun queryRange(range: RectFNode, found: MutableList<QuadtreePoint>) {
        if (!bounds.intersects(range)) return
        for (p in points) if (range.contains(p.point.x, p.point.y)) found.add(p)
        if (divided) {
            nw!!.queryRange(range, found)
            ne!!.queryRange(range, found)
            sw!!.queryRange(range, found)
            se!!.queryRange(range, found)
        }
    }

    private fun subdivide() {
        val x = bounds.left; val y = bounds.top
        val w = bounds.width() / 2f; val h = bounds.height() / 2f
        nw = Quadtree(RectFNode(x, y, x + w, y + h), capacity)
        ne = Quadtree(RectFNode(x + w, y, x + 2*w, y + h), capacity)
        sw = Quadtree(RectFNode(x, y + h, x + w, y + 2*h), capacity)
        se = Quadtree(RectFNode(x + w, y + h, x + 2*w, y + 2*h), capacity)
        divided = true
    }
}

data class QuadtreePoint(val id: String, val point: PointF)
data class RectFNode(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(x: Float, y: Float): Boolean = x >= left && x <= right && y >= top && y <= bottom
    fun intersects(other: RectFNode): Boolean = !(other.left > right || other.right < left || other.top > bottom)
    fun width(): Float = right - left
    fun height(): Float = bottom - top
    companion object { fun fromCenter(cx: Float, cy: Float, halfW: Float, halfH: Float) = RectFNode(cx - halfW, cy - halfH, cx + halfW, cy + halfH) }
}
