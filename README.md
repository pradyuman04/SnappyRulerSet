# Snappy Ruler Set — Android

![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-blue)
[![Download APK](https://img.shields.io/badge/Download-APK-orange?logo=android)](https://drive.google.com/file/d/14jxX9wt-mGAb4vkuqeL6xwyotD9wGSx-/view?usp=sharing)

A simple Android drawing app that simulates geometry tools with *snappy* interactions for accurate constructions.

---

## ✨ Features
- **Pen** – freehand drawing with finger/stylus  
- **Ruler** – draggable, rotatable; draw straight lines; snaps to 0°/30°/45°/60°/90° and line endpoints/midpoints  
- **Set Squares** – 45° and 30°–60° variants; snap to grid + segments  
- **Protractor** – measure angles between rays; snap readout to 1°, hard snap at 30/45/60/90/120/135/150/180°  
- **Compass** – set radius by drag; draw arcs/circles; snap to intersections/points; adjustable circle size  
- **Undo/Redo** – 20+ steps  
- **Export** – save drawing as PNG/JPEG  

---

## 🏗 Architecture Overview
- **UI** → `MainActivity`, tool buttons, export/undo/redo  
- **Canvas** → `DrawingView` (custom view for rendering + gestures)  
- **Tools** → Overlays (Ruler, Set Square, Protractor, Compass)  
- **State** → Shapes + Tools stored in ViewModel; Undo/Redo via snapshot stack  
- **Utils** → Geometry helpers (angles, intersections, snapping) + Quadtree for fast hit-testing  

---

## 🎯 Snapping Strategy
- Grid (configurable, default 5 mm)  
- Points (endpoints, midpoints, centers, intersections)  
- Angles (0°, 30°, 45°, 60°, 90° …)  
- **Dynamic snap radius** → larger at low zoom, smaller at high zoom  
- Visual hint + gentle haptic on snap  

---

## ⚡ Performance
- Target: 60 fps on mid-range device (6 GB RAM)  
- Optimizations:  
  - Cache paints + objects in `onDraw`  
  - Quadtree for O(log n) spatial queries  
  - Recompute intersections only when shapes change  

---

## 📏 Calibration
- Default: `xdpi / 2.54` → px to cm  
- Optional: User calibration (draw 10 cm line, adjust scale)  
- Accuracy: Length ±1 mm, Angle ±0.5°  

---

## 🛠 Tech Stack & Tools Used
- **Language**: Kotlin  
- **UI Framework**: Android Views + Custom Canvas (`DrawingView`)  
- **Architecture**: MVVM (ViewModel + LiveData/State handling)  
- **Graphics**: Android Canvas APIs, Paint caching  
- **Data Handling**: Snapshot stack for Undo/Redo, Quadtree for hit-testing  
- **Export**: Bitmap → PNG/JPEG  
- **IDE**: Android Studio  
- **Version Control**: Git + GitHub  
- **Testing & Debugging**: Android Emulator, Physical device testing  

---

## 📸 Screenshots
🖼️ [Demo Image]  

<img width=25% height=35% alt="Demo Image" src="https://github.com/user-attachments/assets/6e37e7e5-ec54-4a12-be1b-0ea4f459ba21" />  

---

## 🎥 Demo Video
[Watch Demo Video](https://github.com/user-attachments/assets/8469cffb-c402-4091-b4bf-0368ce77baf9)  

---
