package com.example.snappyrulerset

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.snappyrulerset.tools.ToolType
import com.example.snappyrulerset.ui.DrawingView
import com.example.snappyrulerset.viewmodel.DrawingViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton



class MainActivity : ComponentActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var vm: DrawingViewModel
    private lateinit var hud: TextView
    private lateinit var fabToggleRuler: FloatingActionButton

    private val createFile = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri: Uri? ->
        uri?.let { drawingView.exportToPng(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vm = ViewModelProvider(this)[DrawingViewModel::class.java]
        drawingView = findViewById(R.id.drawingView)
        hud = findViewById(R.id.hud)
        fabToggleRuler = findViewById(R.id.fabToggleRuler)

        drawingView.replaceAllLines(vm.lines)
        drawingView.onNewLine = { line ->
            vm.addLine(line)
            drawingView.replaceAllLines(vm.lines)
        }
        drawingView.onHudUpdate = { hud.text = it }

        findViewById<ImageButton>(R.id.btnUndo).setOnClickListener {
            vm.undo(); drawingView.replaceAllLines(vm.lines)
        }
        findViewById<ImageButton>(R.id.btnRedo).setOnClickListener {
            vm.redo(); drawingView.replaceAllLines(vm.lines)
        }
        findViewById<ImageButton>(R.id.btnExport).setOnClickListener {
            createFile.launch("snappyruler_${System.currentTimeMillis()}.png")
        }

        // toolbar buttons
        findViewById<ImageButton>(R.id.btnPen).setOnClickListener {
            vm.setTool(ToolType.Pen); drawingView.setTool(ToolType.Pen)
        }
        findViewById<ImageButton>(R.id.btnRuler).setOnClickListener {
            vm.setTool(ToolType.Ruler); drawingView.setTool(ToolType.Ruler)
        }
        findViewById<ImageButton>(R.id.btnSetsq).setOnClickListener {
            vm.setTool(ToolType.SetSquare); drawingView.setTool(ToolType.SetSquare)
        }
        findViewById<ImageButton>(R.id.btnProtractor).setOnClickListener {
            vm.setTool(ToolType.Protractor); drawingView.setTool(ToolType.Protractor)
        }
        findViewById<ImageButton>(R.id.btnCompass).setOnClickListener {
            vm.setTool(ToolType.Compass); drawingView.setTool(ToolType.Compass)
        }

        fabToggleRuler.setOnClickListener {
            drawingView.setTool(ToolType.Ruler)

        }
    }
}
