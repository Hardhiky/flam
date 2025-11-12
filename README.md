# Edge Detector - Real-Time Camera Processing with Live Web Streaming

A high-performance Android application that performs real-time edge detection using OpenCV and OpenGL ES, with live frame streaming to a web viewer via WebSocket.

## Project Overview

This project demonstrates advanced mobile computer vision by combining:
- **OpenCV** - Edge detection and image processing
- **OpenGL ES** - Hardware-accelerated rendering
- **CameraX** - Modern Android camera API
- **WebSocket** - Real-time frame streaming
- **TypeScript/Node.js** - Web viewer application

## Features

- Real-time camera processing with multiple modes:
  - RAW - Original camera feed
  - EDGE - Canny edge detection
  - GRAYSCALE - Grayscale conversion
- Live frame streaming from Android to web browser
- Hardware-accelerated rendering with OpenGL ES 2.0
- Performance monitoring (FPS, processing time)
- Auto-reconnect WebSocket with failover
- Multi-client support (multiple viewers can watch same stream)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Android Application                       │
│  ┌────────────┐    ┌──────────┐    ┌────────────────────┐   │
│  │  CameraX   │───▶│  OpenCV  │───▶│   OpenGL ES 2.0    │   │
│  │            │    │  C++/JNI │    │   Renderer         │   │
│  └────────────┘    └──────────┘    └────────────────────┘   │
│         │                                    │                │
│         │                                    │                │
│         └────────────────┬───────────────────┘                │
│                          │                                    │
│                   ┌──────▼─────────┐                          │
│                   │   WebSocket    │                          │
│                   │    Client      │                          │
│                   └────────────────┘                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │ ws://
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                    Node.js Server                            │
│  ┌────────────────┐         ┌─────────────────────────┐     │
│  │  HTTP Server   │         │   WebSocket Server      │     │
│  │  (Port 3000)   │         │   (Port 8080)           │     │
│  └────────────────┘         └─────────────────────────┘     │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │ http://
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                     Web Browser                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         TypeScript Web Viewer                        │   │
│  │  - Real-time frame display                           │   │
│  │  - Performance statistics                            │   │
│  │  - WebSocket client                                  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Prerequisites

### For Android Development
- Android Studio Arctic Fox or later
- Android SDK API 24+ (Android 7.0)
- NDK (for native C++ code)
- CMake 3.22.1+
- Kotlin 1.8+
- An Android device or emulator with camera support

### For Web Viewer
- Node.js 14+ and npm
- Modern web browser (Chrome, Firefox, Edge)

### Network Requirements
- Both Android device and computer must be on the same network
- Firewall must allow connections on ports 3000 and 8080

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd flam
```

### 2. Setup OpenCV Android SDK

Download OpenCV Android SDK from https://opencv.org/releases/

```bash
# Example for OpenCV 4.8.0
wget https://github.com/opencv/opencv/releases/download/4.8.0/opencv-4.8.0-android-sdk.zip
unzip opencv-4.8.0-android-sdk.zip
mv opencv-4.8.0-android-sdk OpenCV-android-sdk
```

The OpenCV SDK should be placed in the project root as `OpenCV-android-sdk/`.

### 3. Install Web Viewer Dependencies

```bash
cd web
npm install
```

### 4. Build TypeScript

```bash
npm run build
```

## Configuration

### Network Setup

#### Step 1: Find Your Computer's IP Address

**On Linux/Mac:**
```bash
ifconfig
# or
ip addr show
```

**On Windows:**
```bash
ipconfig
```

Look for your local network IP (usually starts with 192.168.x.x or 10.x.x.x).
Example: `192.168.1.101`

#### Step 2: Update Android App Configuration

Edit the file: `app/src/main/java/com/flam/edgedetector/MainActivity.kt`

Find the line (around line 129):
```kotlin
val serverUrl = "ws://192.168.1.100:8080"
```

Replace with your computer's IP address:
```kotlin
val serverUrl = "ws://192.168.1.101:8080"  // Use YOUR IP here
```

#### Step 3: Configure Firewall

Allow incoming connections on ports 3000 and 8080.

**Linux (ufw):**
```bash
sudo ufw allow 3000
sudo ufw allow 8080
```

**Linux (firewalld):**
```bash
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

**Windows:**
```powershell
netsh advfirewall firewall add rule name="WebViewer HTTP" dir=in action=allow protocol=TCP localport=3000
netsh advfirewall firewall add rule name="WebViewer WebSocket" dir=in action=allow protocol=TCP localport=8080
```

**Mac:**
Go to System Preferences > Security & Privacy > Firewall > Firewall Options
Add Node.js to allowed applications.

## Running the Application

### Start the Web Server

In a terminal:
```bash
cd web
node server.js
```

Expected output:
```
============================================================
Edge Detector Web Viewer Server
============================================================
HTTP Server:      http://localhost:3000
WebSocket Server: ws://localhost:8080
============================================================
```

Keep this terminal running.

### Build and Install Android App

#### Option A: Using Gradle (Command Line)

```bash
cd flam
./gradlew assembleDebug
```

The APK will be created at:
```
app/build/outputs/apk/debug/app-debug.apk
```

Install on device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Option B: Using Android Studio

1. Open the `flam` project in Android Studio
2. Connect your Android device via USB
3. Enable USB Debugging on your device:
   - Settings > About Phone
   - Tap "Build Number" 7 times to enable Developer Options
   - Settings > Developer Options > Enable "USB Debugging"
4. Click the green "Run" button in Android Studio
5. Select your device

### Open Web Viewer

In your web browser, navigate to:
```
http://localhost:3000
```

Or from another device on the same network:
```
http://192.168.1.101:3000
```
(Replace with your computer's IP)

### Launch Android App

1. Open the "Edge Detector" app on your Android device
2. Grant camera permission when prompted
3. Wait 2-3 seconds for initialization
4. You should see a toast message: "Connected to Web Viewer"
5. Look at the web browser - you should see live frames streaming!

## Usage

### Android App Controls

- **RAW Button** - Display original camera feed
- **EDGE Button** - Apply Canny edge detection
- **GRAYSCALE Button** - Convert to grayscale

### Web Viewer Keyboard Shortcuts

- **L** - Load sample frame (local test)
- **R** - Refresh current frame
- **C** - Clear display
- **W** - Toggle WebSocket connection
- **S** - Start simulation mode
- **D** - Download current frame
- **I** - Print debug info to console

### Web Viewer Console API

Open browser console (F12) and use:

```javascript
// Print debug information
edgeDetectorApp.printInfo()

// Send ping to server
edgeDetectorApp.sendPing()

// Request server statistics
edgeDetectorApp.requestStats()

// Send test frame
edgeDetectorApp.sendTestFrame()
```

## Testing

### Test Without Android Device

You can test the web viewer using the simulator:

```bash
cd web
npm run test:android
```

This will:
- Connect to the WebSocket server
- Send simulated frames at 10 FPS
- Cycle through RAW, EDGE, and GRAYSCALE modes
- Display frames in the web viewer

### Verify WebSocket Connection

Check WebSocket server status:
```bash
curl http://localhost:3000/api/websocket/info
```

Expected response:
```json
{
  "available": true,
  "message": "WebSocket server is running",
  "endpoint": "ws://localhost:8080",
  "status": "active",
  "connectedClients": 1,
  "totalFramesProcessed": 150
}
```

### WebSocket Test Page

Open the dedicated test page:
```
http://localhost:3000/ws-test.html
```

This provides a comprehensive WebSocket testing interface with:
- Connection controls
- Message sending/receiving
- Real-time statistics
- Debug console

## Monitoring and Debugging

### Server Logs

Monitor server activity:
```bash
cd web
node server.js | tee server.log
```

Or if running in background:
```bash
tail -f server.log
```

### Android Logs

View Android application logs:
```bash
adb logcat | grep -E "MainActivity|WebSocket|CameraManager"
```

Filter for specific tags:
```bash
adb logcat MainActivity:I WebSocketStreamer:I *:S
```

### Performance Monitoring

**In Web Browser Console:**
```javascript
// View current statistics
edgeDetectorApp.printInfo()
```

**In Android Logcat:**
Look for lines containing:
- "Frame received" - Shows processing time
- "WebSocket connected" - Connection status
- "FPS" - Frames per second

## Performance Tuning

### Reduce Frame Size

Edit `app/src/main/java/com/flam/edgedetector/camera/CameraManager.kt`:

```kotlin
private const val TARGET_WIDTH = 640   // Default: 1280
private const val TARGET_HEIGHT = 480  // Default: 720
```

### Adjust JPEG Quality

Edit `app/src/main/java/com/flam/edgedetector/network/WebSocketFrameStreamer.kt`:

```kotlin
private const val JPEG_QUALITY = 60  // Default: 80 (range: 0-100)
```

Lower quality = smaller size = faster transmission

### Limit Frame Rate

Modify frame interval in `WebSocketFrameStreamer.kt`:

```kotlin
private const val MAX_FRAME_SIZE_KB = 500  // Increase if needed
```

## Troubleshooting

### Problem: WebSocket Connection Failed

**Symptoms:**
- Android app shows "Disconnected from Web Viewer"
- Web viewer status indicator is red
- No frames appearing

**Solutions:**
1. Verify both devices are on the same WiFi network
2. Check IP address in `MainActivity.kt` is correct
3. Ensure server is running (`node server.js`)
4. Check firewall allows ports 3000 and 8080
5. Test connection: `curl http://YOUR_IP:3000/api/websocket/info`

### Problem: Frames Not Displaying

**Solutions:**
1. Check browser console (F12) for JavaScript errors
2. Verify WebSocket indicator is green
3. Reload the web page
4. Restart the server
5. Check Android logs: `adb logcat | grep WebSocket`

### Problem: Poor Performance / Lag

**Solutions:**
1. Use 5GHz WiFi instead of 2.4GHz
2. Reduce frame resolution (see Performance Tuning)
3. Lower JPEG quality
4. Move closer to WiFi router
5. Close other applications on phone
6. Check network bandwidth

### Problem: Build Errors

**Common Issues:**

**NDK not found:**
```bash
# In Android Studio: File > Project Structure > SDK Location
# Set NDK location
```

**OpenCV not found:**
```bash
# Ensure OpenCV-android-sdk folder is in project root
ls -l OpenCV-android-sdk/
```

**Gradle sync failed:**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Problem: Camera Permission Denied

**Solution:**
1. Uninstall the app
2. Reinstall
3. Grant camera permission when prompted
4. Or: Settings > Apps > Edge Detector > Permissions > Enable Camera

## API Documentation

### WebSocket Message Format

#### Frame Message (Android to Server)

```json
{
  "type": "frame",
  "imageData": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "width": 1280,
  "height": 720,
  "mode": 1,
  "processingTime": 23,
  "timestamp": 1234567890,
  "frameNumber": 42
}
```

#### Connection Message (Server to Client)

```json
{
  "type": "connection",
  "status": "connected",
  "message": "Connected to Edge Detector WebSocket server",
  "timestamp": 1234567890,
  "clientCount": 1
}
```

#### Ping/Pong Messages

```json
{
  "type": "ping",
  "timestamp": 1234567890
}
```

```json
{
  "type": "pong",
  "timestamp": 1234567890
}
```

### REST API Endpoints

**GET /** - Main web viewer page

**GET /api/websocket/info** - WebSocket server status
```json
{
  "available": true,
  "endpoint": "ws://localhost:8080",
  "status": "active",
  "connectedClients": 2
}
```

**GET /api/frame/sample** - Get sample frame data

**GET /api/stats** - Get processing statistics

**POST /api/frame** - Submit frame (alternative to WebSocket)
```json
{
  "imageData": "data:image/jpeg;base64,...",
  "width": 1280,
  "height": 720,
  "mode": 1,
  "processingTime": 25
}
```

## Project Structure

```
flam/
├── app/                          # Android application
│   ├── src/
│   │   ├── main/
│   │   │   ├── cpp/             # Native C++ code (OpenCV processing)
│   │   │   ├── java/com/flam/edgedetector/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── NativeLib.kt
│   │   │   │   ├── camera/      # CameraX integration
│   │   │   │   ├── gl/          # OpenGL rendering
│   │   │   │   └── network/     # WebSocket client
│   │   │   └── res/             # Resources
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── CMakeLists.txt           # Native build configuration
├── web/                         # Web viewer application
│   ├── public/
│   │   ├── index.html           # Main viewer page
│   │   └── ws-test.html         # WebSocket test page
│   ├── src/
│   │   ├── index.ts             # Main TypeScript entry
│   │   └── FrameViewer.ts       # Frame display logic
│   ├── test/
│   │   └── simulate-android.js  # Android simulator
│   ├── dist/                    # Compiled JavaScript
│   ├── server.js                # Express + WebSocket server
│   ├── package.json
│   └── tsconfig.json
├── OpenCV-android-sdk/          # OpenCV Android SDK (download separately)
├── gradle/                      # Gradle wrapper
├── build.gradle.kts             # Root build configuration
└── settings.gradle.kts
```

## Technology Stack

### Android
- **Language:** Kotlin
- **Build System:** Gradle with Kotlin DSL
- **Camera:** CameraX
- **Graphics:** OpenGL ES 2.0
- **Native:** C++ with JNI
- **Computer Vision:** OpenCV 4.x
- **Networking:** OkHttp WebSocket
- **JSON:** Gson

### Web Viewer
- **Runtime:** Node.js
- **Language:** TypeScript
- **Server:** Express.js
- **WebSocket:** ws library
- **Build:** TypeScript Compiler (tsc)

## Performance Characteristics

### Expected Performance

**Good WiFi (5GHz, close to router):**
- Frame rate: 15-30 FPS
- Latency: < 100ms
- Resolution: 1280x720

**Medium WiFi (2.4GHz):**
- Frame rate: 8-15 FPS
- Latency: 100-300ms
- Resolution: 640x480

**Poor WiFi:**
- Frame rate: < 5 FPS
- Latency: > 300ms
- Consider reducing resolution

### Processing Times

- Edge detection: 15-35ms (depends on device)
- Grayscale conversion: 5-15ms
- Frame encoding (JPEG): 10-25ms
- Network transmission: 20-100ms (WiFi dependent)

## Development

### Building from Source

```bash
# Clean build
cd flam
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build (requires signing)
./gradlew assembleRelease

# Run tests
./gradlew test

# Generate APK and install
./gradlew installDebug
```

### TypeScript Development

```bash
cd web

# Watch mode (auto-compile on changes)
npm run watch

# Build production
npm run build

# Run linter
npm run lint

# Format code
npm run format
```

### Live Development

Terminal 1 - Server:
```bash
cd web
npm run dev
```

Terminal 2 - TypeScript watch:
```bash
cd web
npm run watch
```

Terminal 3 - Android logs:
```bash
adb logcat | grep -E "MainActivity|WebSocket"
```

## Contributing

### Code Style

- **Kotlin:** Follow Android Kotlin style guide
- **TypeScript:** Use Prettier formatting
- **C++:** Follow Google C++ style guide

### Commit Messages

Format: `[component] brief description`

Examples:
- `[android] Add WebSocket reconnection logic`
- `[web] Improve frame display performance`
- `[native] Optimize edge detection algorithm`

## License

MIT License - See LICENSE file for details

## Credits

- OpenCV - Computer vision library
- CameraX - Android camera API
- OkHttp - HTTP and WebSocket client
- Express.js - Web server framework
- ws - WebSocket server library

## Support

For issues, questions, or contributions:
1. Check existing issues in the repository
2. Review troubleshooting section
3. Check server and Android logs
4. Create detailed issue with logs and steps to reproduce

## Version History

**v1.0.0** (Current)
- Initial release
- Real-time edge detection
- WebSocket live streaming
- Multi-client support
- Performance monitoring
- Auto-reconnect functionality

## Roadmap

Planned features:
- [ ] Recording functionality
- [ ] Multiple camera support (front/back)
- [ ] Additional filters (blur, sharpen, etc.)
- [ ] Histogram display
- [ ] Frame buffering for smoother playback
- [ ] H.264 video encoding
- [ ] Peer-to-peer WebRTC support
- [ ] Mobile web viewer (view on phone browser)