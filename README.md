# 🧪 Android Edge Detection Viewer - RnD Assessment

A real-time edge detection application demonstrating integration of Android, OpenCV C++, OpenGL ES, JNI, and TypeScript web viewer.

## 📋 Project Overview

This project implements a camera-based edge detection system with the following components:
- **Android App**: Camera feed capture and UI
- **OpenCV C++**: Real-time image processing via JNI
- **OpenGL ES 2.0**: Hardware-accelerated rendering
- **TypeScript Web Viewer**: Debug tool for viewing processed frames

## 🏗️ Architecture

```
┌─────────────┐
│   Camera    │
│  (CameraX)  │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Android   │
│  (Kotlin)   │
└──────┬──────┘
       │ JNI
       ▼
┌─────────────┐
│   OpenCV    │
│    (C++)    │ ──► Edge Detection (Canny)
└──────┬──────┘     Grayscale Filter
       │
       ▼
┌─────────────┐
│  OpenGL ES  │
│   Renderer  │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Display   │
└─────────────┘
```

## 📁 Project Structure

```
flam/
├── app/                              # Android application
│   ├── src/main/
│   │   ├── java/com/flam/edgedetector/
│   │   │   ├── MainActivity.kt       # Main activity with camera
│   │   │   ├── camera/
│   │   │   │   └── CameraManager.kt  # Camera feed handler
│   │   │   ├── gl/
│   │   │   │   ├── GLRenderer.kt     # OpenGL ES renderer
│   │   │   │   ├── GLTextureView.kt  # Custom GLSurfaceView
│   │   │   │   └── Shader.kt         # GLSL shader utilities
│   │   │   └── NativeLib.kt          # JNI interface
│   │   ├── cpp/                      # Native C++ code
│   │   │   ├── native-lib.cpp        # JNI implementation
│   │   │   ├── opencv_processor.cpp  # OpenCV processing
│   │   │   └── opencv_processor.h    # Header file
│   │   ├── res/                      # Android resources
│   │   └── AndroidManifest.xml       # App manifest
│   ├── build.gradle.kts              # App build config
│   └── CMakeLists.txt                # CMake for native build
├── web/                              # TypeScript web viewer
│   ├── src/
│   │   ├── index.ts                  # Main TypeScript code
│   │   └── FrameViewer.ts            # Frame viewer component
│   ├── public/
│   │   └── index.html                # Web page
│   ├── package.json                  # NPM dependencies
│   └── tsconfig.json                 # TypeScript config
├── build.gradle.kts                  # Root build file
├── settings.gradle.kts               # Gradle settings
└── README.md                         # This file
```

## 🔧 Tech Stack

- **Android SDK**: API 24+ (Android 7.0)
- **Language**: Kotlin 1.9+
- **NDK**: r25c+
- **OpenCV**: 4.8.0 (C++)
- **OpenGL ES**: 2.0+
- **JNI**: Native interface layer
- **TypeScript**: 5.0+
- **Build System**: Gradle 8.0+, CMake 3.22+

## 🚀 Setup Instructions

### Prerequisites

1. **Android Studio**: Arctic Fox or later
2. **NDK**: Install via SDK Manager
3. **CMake**: Install via SDK Manager
4. **Node.js**: v16+ (for web viewer)
5. **OpenCV Android SDK**: Download from opencv.org

### Android App Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd flam
   ```

2. **Download OpenCV Android SDK**
   - Download OpenCV 4.8.0 Android SDK from https://opencv.org/releases/
   - Extract and note the path to `sdk/native/jni/include` and `sdk/native/libs`

3. **Configure OpenCV Path**
   - Edit `app/CMakeLists.txt`
   - Update `OPENCV_DIR` to point to your OpenCV SDK location

4. **Open in Android Studio**
   - Open the `flam` directory
   - Let Gradle sync complete
   - Connect Android device or start emulator

5. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   # Or use Android Studio's Run button
   ```

### Web Viewer Setup

1. **Navigate to web directory**
   ```bash
   cd web
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Build TypeScript**
   ```bash
   npm run build
   ```

4. **Run development server**
   ```bash
   npm run dev
   ```

5. **Open in browser**
   - Navigate to `http://localhost:3000`

## 📱 Features

### ✅ Core Features

- [x] Real-time camera feed capture using CameraX
- [x] JNI bridge to native C++ code
- [x] OpenCV Canny edge detection
- [x] OpenCV grayscale conversion
- [x] OpenGL ES 2.0 rendering with textures
- [x] Smooth performance (15+ FPS)
- [x] TypeScript web viewer for debug frames

### ⭐ Bonus Features

- [x] Toggle between raw and processed feed
- [x] FPS counter display
- [x] Frame processing time logger
- [x] GLSL shader effects (grayscale, invert)
- [x] HTTP endpoint for web viewer integration
- [x] Modular architecture with clean separation

## 🎮 Usage

### Android App

1. **Launch the app** - Grant camera permissions when prompted
2. **View camera feed** - Real-time processed output displays
3. **Toggle processing** - Tap the toggle button to switch between:
   - Raw camera feed
   - Edge-detected output
   - Grayscale output
4. **Monitor performance** - FPS counter shown in top-left

### Web Viewer

1. **Open web page** - Displays sample processed frames
2. **View stats** - Shows resolution and FPS information
3. **Auto-refresh** - Simulates real-time frame updates

## 🧪 Testing

### Android Unit Tests
```bash
./gradlew test
```

### Android Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Web Tests
```bash
cd web
npm test
```

## 🔍 Technical Details

### JNI Interface

The native interface exposes:
- `processFrame(ByteArray, width, height, mode)` - Process camera frame
- `initOpenCV()` - Initialize OpenCV native library
- `releaseOpenCV()` - Cleanup resources

### Processing Modes

1. **RAW** (0): No processing
2. **EDGE** (1): Canny edge detection
3. **GRAYSCALE** (2): Grayscale conversion

### OpenGL Shaders

- **Vertex Shader**: Standard texture mapping
- **Fragment Shader**: Texture sampling with optional effects

### Performance Optimization

- Frame processing on background thread
- Double buffering for texture updates
- Efficient memory management with native buffers
- Camera resolution capped at 1280x720 for performance

## 📊 Performance Metrics

- **Target FPS**: 30 FPS
- **Minimum FPS**: 15 FPS
- **Average Processing Time**: 20-30ms per frame
- **Memory Usage**: ~50-80MB

## 🐛 Known Issues

- High-resolution cameras may impact FPS on older devices
- Edge detection parameters tuned for general use (may need adjustment)
- Web viewer currently uses static frames (WebSocket integration planned)

## 🔮 Future Improvements

- [ ] Real-time WebSocket streaming to web viewer
- [ ] Multiple edge detection algorithms (Sobel, Laplacian)
- [ ] Recording and playback functionality
- [ ] Advanced shader effects library
- [ ] GPU-accelerated processing with OpenGL compute shaders
- [ ] ML-based edge enhancement

## 📝 Development Notes

### Git Workflow

This project follows a structured commit pattern:
- `feat:` New features
- `fix:` Bug fixes
- `refactor:` Code restructuring
- `docs:` Documentation updates
- `perf:` Performance improvements

### Code Style

- **Kotlin**: Official Kotlin style guide
- **C++**: Google C++ style guide
- **TypeScript**: Airbnb TypeScript style guide

## 📄 License

This project is created for assessment purposes.

## 👤 Author

RnD Intern Assessment Project

## 🙏 Acknowledgments

- OpenCV Community
- Android Camera Samples
- OpenGL ES Documentation

---

**Assessment Duration**: 3 Days  
**Submission**: Ensure all code is committed with meaningful messages  
**Repository**: Must be public or shareable for evaluation