# 3D SUBHAN Renderer (Java2D)

This project renders a fully animated, colorful 3D version of the name **SUBHAN** using pure Java and Swing. Each letter is made of extruded and smoothed blocks ("bubble letters"), rendered with lighting, shading, and automatic rotation — no external libraries or OpenGL required.

## ✨ Features

- 🧊 **Rounded 3D Letters**: Each character is built from bevelled, extruded blocks.
- 💡 **Soft Lighting**: Ambient + directional lighting gives smooth shading effects.
- 🌈 **Per-Letter Colors**: Each character is colored uniquely (red, orange, yellow, etc).
- 🌀 **Auto Rotation**: Letters rotate continuously around X and Y axes.
- 🎨 **Anti-Aliased Rendering**: High-quality image rendering via Java2D.

## 📸 Preview

| Feature | Description |
|--------|-------------|
| 📐 | Grid-based block design for each letter |
| 💡 | Gouraud-style soft shading with light vector facing camera |

## 🛠️ How to Run

### Prerequisites

- Java JDK 8+ installed
- An IDE like VSCode or IntelliJ **OR** terminal access

### Compile & Run

```bash
javac render.java
java render
