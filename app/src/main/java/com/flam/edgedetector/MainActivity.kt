package com.flam.edgedetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.flam.edgedetector.camera.CameraManager
import com.flam.edgedetector.gl.GLRenderer
import com.google.android.material.button.MaterialButton
import android.opengl.GLSurfaceView
import android.widget.ProgressBar
import android.widget.TextView
import java.util.Locale

/**
 * Main activity - Real-time edge detection viewer
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_REQUEST = 100
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    // UI Components
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var fpsTextView: TextView
    private lateinit var processingTimeTextView: TextView
    private lateinit var resolutionTextView: TextView
    private lateinit var modeTextView: TextView
    private lateinit var btnRawMode: MaterialButton
    private lateinit var btnEdgeMode: MaterialButton
    private lateinit var btnGrayscaleMode: MaterialButton
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var statusTextView: TextView

    // Core components
    private lateinit var glRenderer: GLRenderer
    private lateinit var cameraManager: CameraManager

    private var isOpenGLInitialized = false
    private var isCameraInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i(TAG, "MainActivity created")

        initializeViews()
        initializeNativeLib()
        checkPermissions()
    }

    /**
     * Initialize UI views
     */
    private fun initializeViews() {
        glSurfaceView = findViewById(R.id.glSurfaceView)
        fpsTextView = findViewById(R.id.fpsTextView)
        processingTimeTextView = findViewById(R.id.processingTimeTextView)
        resolutionTextView = findViewById(R.id.resolutionTextView)
        modeTextView = findViewById(R.id.modeTextView)
        btnRawMode = findViewById(R.id.btnRawMode)
        btnEdgeMode = findViewById(R.id.btnEdgeMode)
        btnGrayscaleMode = findViewById(R.id.btnGrayscaleMode)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        statusTextView = findViewById(R.id.statusTextView)

        // Setup button click listeners
        btnRawMode.setOnClickListener { setProcessingMode(NativeLib.MODE_RAW) }
        btnEdgeMode.setOnClickListener { setProcessingMode(NativeLib.MODE_EDGE) }
        btnGrayscaleMode.setOnClickListener { setProcessingMode(NativeLib.MODE_GRAYSCALE) }

        // Initial UI state
        showLoading(true, getString(R.string.opencv_init_success))
    }

    /**
     * Initialize native library
     */
    private fun initializeNativeLib() {
        if (!NativeLib.isLoaded()) {
            Log.e(TAG, "Native library not loaded")
            showError(getString(R.string.error_native_lib_load))
            return
        }

        if (!NativeLib.initialize()) {
            Log.e(TAG, "Failed to initialize native library")
            showError(getString(R.string.opencv_init_failed))
            return
        }

        val isOpenCVAvailable = NativeLib.isInitializedSuccessfully()
        Log.i(TAG, "Native library initialized, OpenCV available: $isOpenCVAvailable")

        if (!isOpenCVAvailable) {
            Toast.makeText(
                this,
                getString(R.string.opencv_not_available),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Check and request camera permissions
     */
    private fun checkPermissions() {
        if (allPermissionsGranted()) {
            initializeOpenGL()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    /**
     * Check if all required permissions are granted
     */
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (allPermissionsGranted()) {
                initializeOpenGL()
            } else {
                showError(getString(R.string.camera_permission_denied))
                Toast.makeText(
                    this,
                    getString(R.string.camera_permission_required),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    /**
     * Initialize OpenGL renderer
     */
    private fun initializeOpenGL() {
        try {
            showLoading(true, "Initializing OpenGL...")

            // Configure GLSurfaceView
            glSurfaceView.setEGLContextClientVersion(2)

            // Create and set renderer
            glRenderer = GLRenderer()
            glRenderer.onFpsUpdate = { fps ->
                runOnUiThread {
                    updateFps(fps)
                }
            }

            glSurfaceView.setRenderer(glRenderer)
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            isOpenGLInitialized = true
            Log.i(TAG, getString(R.string.opengl_init_success))

            // Initialize camera after OpenGL
            initializeCamera()
        } catch (e: Exception) {
            Log.e(TAG, "OpenGL initialization failed: ${e.message}")
            showError(getString(R.string.opengl_init_failed))
        }
    }

    /**
     * Initialize camera manager
     */
    private fun initializeCamera() {
        try {
            showLoading(true, "Initializing camera...")

            cameraManager = CameraManager(this)

            // Set camera callbacks
            cameraManager.onFrameProcessed = { frameData, width, height, processingTime ->
                if (isOpenGLInitialized) {
                    // Update OpenGL texture with processed frame
                    glRenderer.setFrameData(frameData, width, height)

                    // Update UI on main thread
                    runOnUiThread {
                        updateProcessingTime(processingTime)
                        updateResolution(width, height)
                    }
                }
            }

            cameraManager.onError = { error ->
                runOnUiThread {
                    showError(error)
                }
            }

            // Start camera
            cameraManager.initialize(this) { success ->
                runOnUiThread {
                    if (success) {
                        isCameraInitialized = true
                        showLoading(false)
                        Log.i(TAG, "Camera initialized successfully")
                        Toast.makeText(
                            this,
                            "Camera ready - Processing at ${cameraManager.getFrameWidth()}x${cameraManager.getFrameHeight()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        showError(getString(R.string.camera_error))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed: ${e.message}")
            showError(getString(R.string.error_camera_unavailable))
        }
    }

    /**
     * Set processing mode
     */
    private fun setProcessingMode(mode: Int) {
        if (!isCameraInitialized) {
            Log.w(TAG, "Camera not initialized yet")
            return
        }

        cameraManager.setProcessingMode(mode)
        updateModeButtons(mode)
        updateModeText(mode)

        Log.i(TAG, "Processing mode set to: ${NativeLib.getModeName(mode)}")
    }

    /**
     * Update mode buttons visual state
     */
    private fun updateModeButtons(selectedMode: Int) {
        val activeColor = ContextCompat.getColor(this, R.color.button_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.button_inactive)

        btnRawMode.setBackgroundColor(
            if (selectedMode == NativeLib.MODE_RAW) activeColor else inactiveColor
        )
        btnEdgeMode.setBackgroundColor(
            if (selectedMode == NativeLib.MODE_EDGE) activeColor else inactiveColor
        )
        btnGrayscaleMode.setBackgroundColor(
            if (selectedMode == NativeLib.MODE_GRAYSCALE) activeColor else inactiveColor
        )
    }

    /**
     * Update FPS display
     */
    private fun updateFps(fps: Float) {
        fpsTextView.text = String.format(Locale.US, "FPS: %.1f", fps)
    }

    /**
     * Update processing time display
     */
    private fun updateProcessingTime(timeMs: Long) {
        processingTimeTextView.text = String.format(
            Locale.US,
            "Processing: %dms",
            timeMs
        )
    }

    /**
     * Update resolution display
     */
    private fun updateResolution(width: Int, height: Int) {
        resolutionTextView.text = String.format(
            Locale.US,
            "Resolution: %dx%d",
            width,
            height
        )
    }

    /**
     * Update mode text display
     */
    private fun updateModeText(mode: Int) {
        modeTextView.text = String.format(
            Locale.US,
            "Mode: %s",
            NativeLib.getModeName(mode)
        )
    }

    /**
     * Show loading indicator
     */
    private fun showLoading(show: Boolean, message: String = "") {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show && message.isNotEmpty()) {
            statusTextView.text = message
            statusTextView.visibility = View.VISIBLE
        } else {
            statusTextView.visibility = View.GONE
        }
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        loadingIndicator.visibility = View.GONE
        statusTextView.text = message
        statusTextView.visibility = View.VISIBLE
        Log.e(TAG, "Error: $message")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        glSurfaceView.onPause()
        cameraManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        // Release camera
        if (::cameraManager.isInitialized) {
            cameraManager.release()
        }

        // Release OpenGL renderer
        if (::glRenderer.isInitialized) {
            glRenderer.release()
        }

        // Release native library
        NativeLib.release()

        Log.i(TAG, "MainActivity destroyed")
    }

    /**
     * Print debug information
     */
    private fun printDebugInfo() {
        Log.d(TAG, "=== Debug Info ===")
        Log.d(TAG, "Native library loaded: ${NativeLib.isLoaded()}")
        Log.d(TAG, "Native library initialized: ${NativeLib.isInitializedSuccessfully()}")
        Log.d(TAG, "OpenGL initialized: $isOpenGLInitialized")
        Log.d(TAG, "Camera initialized: $isCameraInitialized")
        if (::cameraManager.isInitialized) {
            Log.d(TAG, cameraManager.getCameraInfo())
        }
        if (::glRenderer.isInitialized) {
            Log.d(TAG, "Renderer FPS: ${glRenderer.getCurrentFps()}")
        }
        Log.d(TAG, "Native stats: ${NativeLib.getStats()}")
        Log.d(TAG, "==================")
    }
}
