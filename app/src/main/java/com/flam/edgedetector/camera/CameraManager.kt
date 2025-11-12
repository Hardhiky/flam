package com.flam.edgedetector.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.flam.edgedetector.NativeLib
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Camera manager for CameraX integration and frame processing
 */
class CameraManager(private val context: Context) {

    companion object {
        private const val TAG = "CameraManager"
        private const val TARGET_WIDTH = 1280
        private const val TARGET_HEIGHT = 720
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService? = null

    private var isInitialized = false
    private var processingMode: Int = NativeLib.MODE_EDGE

    // Frame callback
    var onFrameProcessed: ((ByteArray, Int, Int, Long) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    // Processing buffer
    private var processedFrameBuffer: ByteArray? = null
    private var frameWidth: Int = 0
    private var frameHeight: Int = 0

    // Performance tracking
    private var lastProcessTime: Long = 0
    private var frameCount: Long = 0

    /**
     * Initialize camera
     */
    fun initialize(lifecycleOwner: LifecycleOwner, callback: (Boolean) -> Unit) {
        if (isInitialized) {
            Log.w(TAG, "Camera already initialized")
            callback(true)
            return
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                startCamera(lifecycleOwner)
                isInitialized = true
                Log.i(TAG, "Camera initialized successfully")
                callback(true)
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed: ${e.message}")
                onError?.invoke("Camera initialization failed: ${e.message}")
                callback(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Start camera with image analysis
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera(lifecycleOwner: LifecycleOwner) {
        val provider = cameraProvider ?: return

        // Unbind all use cases before rebinding
        provider.unbindAll()

        // Configure image analysis
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(TARGET_WIDTH, TARGET_HEIGHT))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                    processImage(imageProxy)
                }
            }

        // Select back camera
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Bind use cases to camera
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )

            Log.i(TAG, "Camera started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed: ${e.message}")
            onError?.invoke("Camera binding failed: ${e.message}")
        }
    }

    /**
     * Process camera image frame
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(imageProxy: ImageProxy) {
        try {
            val startTime = System.currentTimeMillis()

            // Get image dimensions
            val width = imageProxy.width
            val height = imageProxy.height

            // Initialize buffer if needed
            val bufferSize = width * height * 4 // RGBA
            if (processedFrameBuffer == null ||
                frameWidth != width ||
                frameHeight != height
            ) {
                processedFrameBuffer = ByteArray(bufferSize)
                frameWidth = width
                frameHeight = height
                Log.i(TAG, "Frame buffer allocated: ${width}x${height}")
            }

            // Get image data
            val buffer = imageProxy.planes[0].buffer
            val inputData = ByteArray(buffer.remaining())
            buffer.get(inputData)

            // Process frame with native code
            val processingTime = NativeLib.processFrameBytes(
                inputData,
                width,
                height,
                processingMode,
                processedFrameBuffer!!
            )

            if (processingTime >= 0) {
                lastProcessTime = processingTime
                frameCount++

                // Notify callback with processed frame
                onFrameProcessed?.invoke(
                    processedFrameBuffer!!,
                    width,
                    height,
                    processingTime
                )

                // Log performance every 100 frames
                if (frameCount % 100 == 0L) {
                    val totalTime = System.currentTimeMillis() - startTime
                    Log.d(TAG, "Frame $frameCount processed in ${totalTime}ms (native: ${processingTime}ms)")
                }
            } else {
                Log.e(TAG, "Frame processing failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image: ${e.message}")
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Set processing mode
     */
    fun setProcessingMode(mode: Int) {
        if (mode != processingMode) {
            processingMode = mode
            Log.i(TAG, "Processing mode changed to: ${NativeLib.getModeName(mode)}")
        }
    }

    /**
     * Get current processing mode
     */
    fun getProcessingMode(): Int = processingMode

    /**
     * Get last processing time
     */
    fun getLastProcessingTime(): Long = lastProcessTime

    /**
     * Get frame count
     */
    fun getFrameCount(): Long = frameCount

    /**
     * Get frame dimensions
     */
    fun getFrameWidth(): Int = frameWidth
    fun getFrameHeight(): Int = frameHeight

    /**
     * Check if camera has flashlight
     */
    fun hasFlashUnit(): Boolean {
        return camera?.cameraInfo?.hasFlashUnit() ?: false
    }

    /**
     * Toggle flashlight
     */
    fun toggleFlashlight(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    /**
     * Get camera info
     */
    fun getCameraInfo(): String {
        val cam = camera ?: return "Camera not initialized"
        val info = cam.cameraInfo

        return buildString {
            append("Camera Info:\n")
            append("- Sensor Rotation: ${info.sensorRotationDegrees}\n")
            append("- Has Flash: ${info.hasFlashUnit()}\n")
            append("- Frame: ${frameWidth}x${frameHeight}\n")
            append("- Mode: ${NativeLib.getModeName(processingMode)}\n")
            append("- Frames Processed: $frameCount\n")
            append("- Last Process Time: ${lastProcessTime}ms")
        }
    }

    /**
     * Stop camera
     */
    fun stop() {
        Log.i(TAG, "Stopping camera")
        cameraProvider?.unbindAll()
        camera = null
        imageAnalysis = null
    }

    /**
     * Release resources
     */
    fun release() {
        Log.i(TAG, "Releasing camera resources")

        stop()

        cameraExecutor?.shutdown()
        cameraExecutor = null

        processedFrameBuffer = null
        frameWidth = 0
        frameHeight = 0

        isInitialized = false

        Log.i(TAG, "Camera resources released")
    }

    /**
     * Check if camera is initialized
     */
    fun isReady(): Boolean = isInitialized && camera != null
}
