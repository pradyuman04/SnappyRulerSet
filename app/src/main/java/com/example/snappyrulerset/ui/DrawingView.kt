package com.example.snappyrulerset.ui

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.snappyrulerset.tools.ToolType
import com.example.snappyrulerset.utils.GeometryUtils
import com.example.snappyrulerset.utils.Quadtree
import com.example.snappyrulerset.utils.QuadtreePoint
import com.example.snappyrulerset.utils.RectFNode
import com.example.snappyrulerset.viewmodel.LineShape
import kotlin.math.*

/**
 * DrawingView
 *
 * - Backwards-compatible public API for MainActivity (replaceAllLines, onNewLine callback).
 * - Adds Compass & Protractor shapes (resizable with pinch). They are persistent shapes on the canvas.
 * - Local undo/redo for shapes; calls onNewLine(...) for LineShapes (so ViewModel code that manages lines keeps working).
 *
 * Additions are marked with `// Add`
 */
class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ----------------------------
    // Public callbacks & backward-compatible API
    // ----------------------------
    var onNewLine: ((LineShape) -> Unit)? = null                                    // Preserve existing callback
    var onHudUpdate: ((String) -> Unit)? = null
    var onNewShape: ((Any) -> Unit)? = null                                         // Add: optional callback for shapes (Compass/Protractor)

    // Backward-compatible helper for MainActivity: replace lines
    fun replaceAllLines(newLines: List<LineShape>) {                                   // Add
        // Convert incoming lines into internal shapes (LineShape objects)
        shapes.clear()
        shapes.addAll(newLines.map { Shape.Line(it.start, it.end) })
        rebuildSpatialIndex()
        invalidate()
    }

    // ----------------------------
    // Internal model: shapes on canvas (lines, compasses, protractors)
    // ----------------------------
    private val shapes = mutableListOf<Shape>()                                       // Add: persistent shapes list
    private val undoStack = ArrayDeque<List<Shape>>()                                 // Add: local undo stack
    private val redoStack = ArrayDeque<List<Shape>>()                                 // Add: local redo stack

    // ----------------------------
    // Gestures / Camera
    // ----------------------------
    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f

    // ----------------------------
    // Drawing state / preview
    // ----------------------------
    private var currentStart: PointF? = null
    private var currentEnd: PointF? = null
    private var isDrawing = false

    // active tool (selected from MainActivity via drawingView.setTool)
    private var activeTool: ToolType = ToolType.Pen

    // ----------------------------
    // Visuals / paints
    // ----------------------------
    private val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 6f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val previewPaint = Paint().apply { color = Color.RED; strokeWidth = 6f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val gridPaint = Paint().apply { color = Color.parseColor("#EAEAEA"); strokeWidth = 1f }
    private val hudPaint = Paint().apply { color = Color.DKGRAY; textSize = 36f; isAntiAlias = true }
    private val snapDotPaint = Paint().apply { color = Color.BLUE; style = Paint.Style.FILL; isAntiAlias = true }
    private val rulerPaint = Paint().apply { color = 0x8033B5E5.toInt(); strokeWidth = 6f; style = Paint.Style.STROKE; isAntiAlias = true }

    // ----------------------------
    // Snapping + grid
    // ----------------------------
    private val allowedAngles = listOf(0f, 30f, 45f, 60f, 90f, 120f, 135f, 150f, 180f)
    private var snappingEnabled = true
    private var snapRadiusPx = 36f
    private var gridSizePx = 40f

    // ----------------------------
    // Spatial index for point snapping
    // ----------------------------
    private var quadtree: Quadtree = Quadtree(RectFNode(-5000f, -5000f, 5000f, 5000f))
    private var quadtreeNeedsRebuild = true

    // Ruler overlay (existing)
    private val rulerOverlay = RulerOverlay()

    // active shape being manipulated with pinch (Add)
    private var activeShape: Shape? = null                                              // Add: currently selected/created shape for resizing

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    // ----------------------------
    // Public API (backwards compatible)
    // ----------------------------
    fun setTool(tool: ToolType) {
        activeTool = tool
        // Keep ruler overlay visible only when ruler is selected
        rulerOverlay.visible = (tool == ToolType.Ruler)
        // When selecting a tool, clear preview start/end (so HUD will show latest completed shape)
        currentStart = null
        currentEnd = null
        isDrawing = false
        activeShape = null
        invalidate()
    }

    // Undo/Redo for shapes (local). These are independent of your ViewModel undo/redo; lines will still call onNewLine.
    fun undoLocal() {                                                           // Add
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(shapes.map { it.copyShape() })
            val prev = undoStack.removeLast()
            shapes.clear()
            shapes.addAll(prev.map { it.copyShape() })
            quadtreeNeedsRebuild = true
            invalidate()
        }
    }

    fun redoLocal() {                                                           // Add
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(shapes.map { it.copyShape() })
            val next = redoStack.removeLast()
            shapes.clear()
            shapes.addAll(next.map { it.copyShape() })
            quadtreeNeedsRebuild = true
            invalidate()
        }
    }

    private fun pushUndoSnapshot() {                                             // Add
        undoStack.addLast(shapes.map { it.copyShape() })
        if (undoStack.size > 80) undoStack.removeFirst()
        redoStack.clear()
    }

    // ----------------------------
    // Drawing & render
    // ----------------------------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor)

        // Grid (restored)
        drawGrid(canvas)

        // Draw persistent shapes
        for (s in shapes) {
            when (s) {
                is Shape.Line -> {
                    canvas.drawLine(s.start.x, s.start.y, s.end.x, s.end.y, linePaint)
                }
                is Shape.Compass -> {
                    val paint = Paint().apply { color = 0xFF8E24AA.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE; isAntiAlias = true }
                    canvas.drawCircle(s.center.x, s.center.y, s.radius, paint)
                }
                is Shape.Protractor -> {
                    val paint = Paint().apply { color = 0xFF2E7D32.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
                    canvas.drawArc(RectF(s.center.x - s.radius, s.center.y - s.radius, s.center.x + s.radius, s.center.y + s.radius), 180f, 180f, false, paint)
                    // major angles only (Add): 0,30,45,60,90,120,135,150,180
                    val major = listOf(0, 30, 45, 60, 90, 120, 135, 150, 180)
                    for (angle in major) {
                        val rad = Math.toRadians(angle.toDouble())
                        val x = s.center.x + cos(rad).toFloat() * s.radius
                        val y = s.center.y + sin(rad).toFloat() * s.radius
                        canvas.drawLine(s.center.x, s.center.y, x, y, paint)
                    }
                }
            }
        }

        // Preview (line) while drawing
        currentStart?.let { s ->
            currentEnd?.let { e ->
                // If drawing a line-like tool, show preview
                if (activeTool == ToolType.Pen || activeTool == ToolType.SetSquare || activeTool == ToolType.SETSQUARE_30_60 || activeTool == ToolType.Ruler) {
                    canvas.drawLine(s.x, s.y, e.x, e.y, previewPaint)
                }
                // Compass preview shows circle
                if (activeTool == ToolType.Compass) {
                    val r = GeometryUtils.distance(s, e)
                    val paint = Paint().apply { color = 0x66FF1744; strokeWidth = 4f; style = Paint.Style.STROKE; isAntiAlias = true }
                    canvas.drawCircle(s.x, s.y, r, paint)
                }
                // Protractor preview draws half-circle with major angles
                if (activeTool == ToolType.Protractor) {
                    val r = GeometryUtils.distance(s, e)
                    val paint = Paint().apply { color = 0x668E24AA.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
                    canvas.drawArc(RectF(s.x - r, s.y - r, s.x + r, s.y + r), 180f, 180f, false, paint)
                }
            }
        }

        // Ruler overlay
        if (rulerOverlay.visible) rulerOverlay.draw(canvas)

        canvas.restore()

        // HUD update and draw bottom HUD
        val hudText = composeHudText()
        if (!hudText.isNullOrEmpty()) {
            onHudUpdate?.invoke(hudText)
            // draw HUD at bottom (screen coords)
            canvas.drawText(hudText, 16f, (height - 32f).toFloat(), hudPaint)
        }
    }

    // Draw small-square grid (restored)
    private fun drawGrid(canvas: Canvas) {                                                   // Add: restored grid
        val w = (width / scaleFactor) + abs(translateX / scaleFactor)
        val h = (height / scaleFactor) + abs(translateY / scaleFactor)
        val startX = -translateX / scaleFactor
        val startY = -translateY / scaleFactor

        var x = startX - (startX % gridSizePx)
        while (x < startX + w) {
            canvas.drawLine(x, startY - 1000f, x, startY + h + 1000f, gridPaint)
            x += gridSizePx
        }
        var y = startY - (startY % gridSizePx)
        while (y < startY + h) {
            canvas.drawLine(startX - 1000f, y, startX + w + 1000f, y, gridPaint)
            y += gridSizePx
        }
    }

    // Compose HUD text per active tool & preview
    private fun composeHudText(): String {
        // If drawing a line-like tool, show length and angle
        currentStart?.let { s ->
            currentEnd?.let { e ->
                val dx = e.x - s.x
                val dy = e.y - s.y
                val lenPx = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val angle = (atan2(dy, dx).toDegrees() + 360f) % 360f
                val snapped = if (snappingEnabled) GeometryUtils.snapAngle(angle, allowedAngles, 3f) else angle
                val lenCm = pxToCm(lenPx)
                return when (activeTool) {
                    ToolType.Compass -> "Compass radius: %.2f cm".format(pxToCm(GeometryUtils.distance(s, e)))
                    ToolType.Protractor -> "Protractor size: %.2f cm".format(pxToCm(GeometryUtils.distance(s, e)))
                    else -> "Len: %.2f cm  Angle: %.1f° (snap: %.1f°)".format(lenCm, angle, snapped)
                }
            }
        }

        // If there's an active shape selected (recently created), show its metrics persistently
        activeShape?.let { s ->
            when (s) {
                is Shape.Compass -> return "Compass: radius %.2f cm".format(pxToCm(s.radius))
                is Shape.Protractor -> return "Protractor: size %.2f cm".format(pxToCm(s.radius))
                else -> {}
            }
        }

        // If nothing drawing, show currently selected tool
        return "Tool: ${activeTool.name}"
    }

    private fun pxToCm(px: Float): Float {
        val metrics = resources.displayMetrics
        val xdpi = metrics.xdpi
        if (xdpi <= 0f) return px / 37.7952755906f
        val pixelsPerCm = xdpi / 2.54f
        return px / pixelsPerCm
    }

    // ----------------------------
    // Touch handling
    // ----------------------------
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        gestureDetector.onTouchEvent(ev)

        val pointerCount = ev.pointerCount

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val p = screenToWorld(ev.x, ev.y)
                when (activeTool) {
                    ToolType.Pen, ToolType.SetSquare, ToolType.SETSQUARE_30_60 -> {
                        isDrawing = true
                        currentStart = PointF(p.x, p.y); currentEnd = PointF(p.x, p.y)
                    }
                    ToolType.Ruler -> {
                        rulerOverlay.visible = true; rulerOverlay.center = PointF(p.x, p.y)
                    }
                    ToolType.Compass -> {
                        // Create a persistent compass shape (Add)
                        val c = Shape.Compass(PointF(p.x, p.y), 60f)
                        shapes.add(c)
                        pushUndoSnapshot()
                        activeShape = c
                        // inform optional callback
                        onNewShape?.invoke(c)
                    }
                    ToolType.Protractor -> {
                        val pr = Shape.Protractor(PointF(p.x, p.y), 80f)
                        shapes.add(pr)
                        pushUndoSnapshot()
                        activeShape = pr
                        onNewShape?.invoke(pr)
                    }
                    else -> {
                        // default fallback
                        isDrawing = true
                        currentStart = PointF(p.x, p.y); currentEnd = PointF(p.x, p.y)
                    }
                }
                lastPanX = ev.x; lastPanY = ev.y
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val p = screenToWorld(ev.x, ev.y)
                when (activeTool) {
                    ToolType.Pen, ToolType.SetSquare, ToolType.SETSQUARE_30_60 -> {
                        if (isDrawing) {
                            currentEnd = PointF(p.x, p.y)
                            // snapping: grid + points
                            if (quadtreeNeedsRebuild) rebuildSpatialIndex()
                            val nearby = mutableListOf<QuadtreePoint>()
                            quadtree.queryRange(RectFNode(p.x - snapRadiusPx, p.y - snapRadiusPx, p.x + snapRadiusPx, p.y + snapRadiusPx), nearby)
                            if (nearby.isNotEmpty()) {
                                val nearest = nearby.minByOrNull { GeometryUtils.distance(it.point, p) }
                                nearest?.let { currentEnd = PointF(it.point.x, it.point.y) }
                            } else if (currentStart != null) {
                                // angle snapping for setsquare
                                val angle = GeometryUtils.angleDegrees(currentStart!!, currentEnd!!)
                                val snapped = when (activeTool) {
                                    ToolType.SetSquare -> GeometryUtils.snapAngle(angle, listOf(0f, 45f, 90f, 135f, 180f), 4f)
                                    ToolType.SETSQUARE_30_60 -> GeometryUtils.snapAngle(angle, listOf(0f, 30f, 60f, 90f, 120f, 150f, 180f), 4f)
                                    else -> GeometryUtils.snapAngle(angle, allowedAngles, 4f)
                                }
                                if (abs(((angle - snapped + 180f + 360f) % 360f) - 180f) <= 4f) {
                                    val rad = Math.toRadians(snapped.toDouble())
                                    val length = GeometryUtils.distance(currentStart!!, currentEnd!!)
                                    currentEnd = PointF(currentStart!!.x + (cos(rad) * length).toFloat(), currentStart!!.y + (sin(rad) * length).toFloat())
                                }
                            }
                        }
                    }
                    ToolType.Ruler -> {
                        if (pointerCount >= 2) {
                            // rotate ruler with two fingers
                            val pa = screenToWorld(ev.getX(0), ev.getY(0))
                            val pb = screenToWorld(ev.getX(1), ev.getY(1))
                            val angle = atan2(pb.y - pa.y, pb.x - pa.x).toDegrees()
                            rulerOverlay.angleDeg = angle
                        }
                    }
                    ToolType.Compass -> {
                        // if active shape is the most recently created compass, allow radius drag
                        val a = activeShape
                        if (a is Shape.Compass) {
                            val r = GeometryUtils.distance(a.center, p)
                            a.radius = r
                        }
                    }
                    ToolType.Protractor -> {
                        val a = activeShape
                        if (a is Shape.Protractor) {
                            val r = GeometryUtils.distance(a.center, p)
                            a.radius = r
                        }
                    }
                    else -> {
                        // pan
                        if (!isDrawing && pointerCount == 1) {
                            val dx = ev.x - lastPanX
                            val dy = ev.y - lastPanY
                            translateX += dx; translateY += dy
                            lastPanX = ev.x; lastPanY = ev.y
                        }
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // finalize line drawing — call onNewLine to keep ViewModel in sync (backwards-compatible)
                if (isDrawing && currentStart != null && currentEnd != null) {
                    if (GeometryUtils.distance(currentStart!!, currentEnd!!) > 6f) {
                        val line = LineShape(currentStart!!, currentEnd!!)
                        // notify existing callback so MainActivity / VM behave unchanged
                        onNewLine?.invoke(line)
                        // Also store as a persistent shape in local shapes list if desired
                        shapes.add(Shape.Line(line.start, line.end))
                        pushUndoSnapshot()
                        quadtreeNeedsRebuild = true
                    }
                }
                // clear previewing
                isDrawing = false
                currentStart = null; currentEnd = null
                // clear active shape selection (we allow shapes to persist; activeShape remains to show HUD if needed)
                activeShape = null
                invalidate()
            }
        }

        return true
    }

    // ----------------------------
    // Spatial index rebuild for snapping
    // ----------------------------
    private fun rebuildSpatialIndex() {
        quadtree = Quadtree(RectFNode(-5000f, -5000f, 5000f, 5000f))
        val lineList = shapes.mapNotNull { if (it is Shape.Line) LineShape(it.start, it.end) else null }
        lineList.forEachIndexed { idx, l ->
            quadtree.insert(QuadtreePoint("e${idx}_s", l.start))
            quadtree.insert(QuadtreePoint("e${idx}_e", l.end))
            val mid = PointF((l.start.x + l.end.x) / 2f, (l.start.y + l.end.y) / 2f)
            quadtree.insert(QuadtreePoint("m${idx}", mid))
        }
        // intersections (naive O(n^2), ok for small counts)
        for (i in 0 until lineList.size) {
            for (j in i+1 until lineList.size) {
                val a = lineList[i]; val b = lineList[j]
                GeometryUtils.intersectionOfLines(a.start, a.end, b.start, b.end)?.let { quadtree.insert(QuadtreePoint("int$i$j", it)) }
            }
        }
        quadtreeNeedsRebuild = false
    }

    // ----------------------------
    // Utility / helpers
    // ----------------------------
    private fun screenToWorld(sx: Float, sy: Float): PointF = PointF((sx - translateX) / scaleFactor, (sy - translateY) / scaleFactor)
    private fun Float.toDegrees(): Float = this * 180f / Math.PI.toFloat()
    private fun atan2(y: Float, x: Float) = kotlin.math.atan2(y.toDouble(), x.toDouble()).toFloat()

    // very small conversion helper


    // ----------------------------
    // Gesture support for pinch resizing active shapes
    // ----------------------------
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // If the user is currently manipulating an active shape with pinch, scale its radius/size
            val a = activeShape
            if (a is Shape.Compass) {
                a.radius *= detector.scaleFactor
                if (a.radius < 4f) a.radius = 4f
                invalidate()
                return true
            } else if (a is Shape.Protractor) {
                a.radius *= detector.scaleFactor
                if (a.radius < 8f) a.radius = 8f
                invalidate()
                return true
            }
            // if not manipulating a shape, allow zoom (camera)
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(0.25f, min(scaleFactor, 5f))
            invalidate()
            return true
        }
    }
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            snappingEnabled = false
            postDelayed({ snappingEnabled = true }, 600)
        }

        override fun onDown(e: MotionEvent): Boolean = true
    }


    // ----------------------------
    // Shapes sealed class (internal to view)
    // ----------------------------
    private sealed class Shape {
        data class Line(val start: PointF, val end: PointF) : Shape()
        data class Compass(var center: PointF, var radius: Float) : Shape()
        data class Protractor(var center: PointF, var radius: Float) : Shape()
    }

    private fun Shape.copyShape(): Shape = when (this) {
        is Shape.Line -> Shape.Line(PointF(this.start.x, this.start.y), PointF(this.end.x, this.end.y))
        is Shape.Compass -> Shape.Compass(PointF(this.center.x, this.center.y), this.radius)
        is Shape.Protractor -> Shape.Protractor(PointF(this.center.x, this.center.y), this.radius)
    }

    // ----------------------------
    // Exports
    // ----------------------------
    fun exportToPng(uri: Uri) {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        draw(c)
        context.contentResolver.openOutputStream(uri)?.use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
    }

    // Expose ruler overlay for external tweaking (backwards compatibility)
    fun getRulerOverlay(): RulerOverlay = rulerOverlay

    // Expose shapes (read-only copy) - Add
    fun getShapesSnapshot(): List<Any> = shapes.map { it.copyShape() }

    // Save snapshot helper invoked whenever we added a shape

    private fun pushUndoSnapshotInternal() {
        undoStack.addLast(shapes.map { it.copyShape() })
        if (undoStack.size > 80) undoStack.removeFirst()
        redoStack.clear()
    }

    // keep last pan values
    private var lastPanX = 0f
    private var lastPanY = 0f



}
