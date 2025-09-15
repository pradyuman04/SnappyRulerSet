package com.example.snappyrulerset

import android.app.Activity
import android.graphics.PointF
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity

class CalibrationActivity : ComponentActivity() {

    private var firstPoint: PointF? = null
    private var secondPoint: PointF? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        val etKnown = findViewById<EditText>(R.id.etKnown)
        val btnStart = findViewById<Button>(R.id.btnStartCal)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        btnStart.setOnClickListener {
            tvStatus.text = "Tap two points on the screen to mark object edge."
            findViewById<TextView>(R.id.calibRoot).setOnTouchListener { v, ev ->
                if (ev.action == android.view.MotionEvent.ACTION_DOWN) {
                    val p = PointF(ev.x, ev.y)
                    if (firstPoint == null) {
                        firstPoint = p; tvStatus.text = "First point recorded. Tap second point."
                    } else if (secondPoint == null) {
                        secondPoint = p; tvStatus.text = "Second point recorded."
                        val knownStr = etKnown.text.toString()
                        val knownCm = knownStr.toFloatOrNull()
                        if (knownCm == null || knownCm <= 0f) {
                            tvStatus.text = "Enter valid known length in cm."
                        } else {
                            val pixelDist = kotlin.math.hypot((firstPoint!!.x - secondPoint!!.x).toDouble(), (firstPoint!!.y - secondPoint!!.y).toDouble()).toFloat()
                            val pixelsPerCm = pixelDist / knownCm
                            val res = intent
                            res.putExtra("pixelsPerCm", pixelsPerCm)
                            setResult(Activity.RESULT_OK, res)
                            finish()
                        }
                    }
                }
                true
            }
        }
    }
}
