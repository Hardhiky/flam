package com.flam.edgedetector

import android.graphics.Bitmap
import android.util.Log

/**
 * JNI interface for native OpenCV processing
 */
object NativeLib {

    private const val TAG = "NativeLib"

    // Processing modes
    const val MODE_RAW = 0
    const val MODE_EDGE = 1
    const val MODE_GRAYSCALE = 2

    private var isLibraryLoaded = false
    private var isInitialized = false

    init {
        try {
            System.loadLibrary("edgedetector")
            isLibraryLoaded = true
            Log.i(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library: ${e.message}")
            isLibraryLoaded = false
        }
    }

    /**
     * Check if native library is loaded
     */
    fun isLoaded(): Boolean = isLibraryLoaded

    /**
     * Initialize OpenCV native library
     * @return true if initialization successful
     */
    fun initialize(): Boolean {
        if (!isLibraryLoaded) {
            Log.e(TAG, "Cannot initialize - library not loaded")
            return false
        }

        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }

        try {
            isInitialized = initOpenCV()
            if (isInitialized) {
                Log.i(TAG, "OpenCV initialized successfully")
                Log.i(TAG, "OpenCV available: ${isOpenCVAvailable()}")
            } else {
                Log.e(TAG, "OpenCV initialization failed")
            }
            return isInitialized
        } catch (e: Exception) {
            Log.e(TAG, "Exception during initialization: ${e.message}")
            return false
        }
    }

    /**
     * Check if initialized
     */
    fun isInitializedSuccessfully(): Boolean = isInitialized

    /**
     * Process frame using byte arrays
     * @param inputData Input RGBA frame data
     * @param width Frame width
     * @param height Frame height
     * @param mode Processing mode (MODE_RAW, MODE_EDGE, MODE_GRAYSCALE)
     * @param outputData Output processed frame data (pre-allocated)
     * @return Processing time in milliseconds, or -1 on error
     */
    fun processFrameBytes(
        inputData: ByteArray,
        width: Int,
        height: Int,
        mode: Int,
        outputData: ByteArray
    ): Long {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized")
            return -1
        }

        val expectedSize = width * height * 4
        if (inputData.size < expectedSize) {
            Log.e(TAG, "Input data too small: ${inputData.size}, expected: $expectedSize")
            return -1
        }

        if (outputData.size < expectedSize) {
            Log.e(TAG, "Output data too small: ${outputData.size}, expected: $expectedSize")
            return -1
        }

        return try {
            processFrame(inputData, width, height, mode, outputData)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during frame processing: ${e.message}")
            -1
        }
    }

    /**
     * Process frame using bitmaps
     * @param inputBitmap Input bitmap (RGBA_8888)
     * @param mode Processing mode
     * @param outputBitmap Output bitmap (RGBA_8888, same size as input)
     * @return Processing time in milliseconds, or -1 on error
     */
    fun processFrameBitmaps(
        inputBitmap: Bitmap,
        mode: Int,
        outputBitmap: Bitmap
    ): Long {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized")
            return -1
        }

        if (inputBitmap.width != outputBitmap.width || inputBitmap.height != outputBitmap.height) {
            Log.e(TAG, "Input and output bitmaps must have same dimensions")
            return -1
        }

        if (inputBitmap.config != Bitmap.Config.ARGB_8888 ||
            outputBitmap.config != Bitmap.Config.ARGB_8888) {
            Log.e(TAG, "Bitmaps must be in ARGB_8888 format")
            return -1
        }

        return try {
            processFrameBitmap(inputBitmap, mode, outputBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during bitmap processing: ${e.message}")
            -1
        }
    }

    /**
     * Set Canny edge detection thresholds
     * @param lowThreshold Low threshold (default: 50)
     * @param highThreshold High threshold (default: 150)
     */
    fun updateCannyThresholds(lowThreshold: Double = 50.0, highThreshold: Double = 150.0) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized")
            return
        }

        try {
            setCannyThresholds(lowThreshold, highThreshold)
            Log.i(TAG, "Canny thresholds updated: low=$lowThreshold, high=$highThreshold")
        } catch (e: Exception) {
            Log.e(TAG, "Exception setting Canny thresholds: ${e.message}")
        }
    }

    /**
     * Get processing statistics
     * @return Statistics string
     */
    fun getStats(): String {
        if (!isInitialized) {
            return "Not initialized"
        }

        return try {
            getStatistics()
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting statistics: ${e.message}")
            "Error: ${e.message}"
        }
    }

    /**
     * Convert YUV420 to RGBA
     * @param yPlane Y plane data
     * @param uPlane U plane data
     * @param vPlane V plane data
     * @param width Image width
     * @param height Image height
     * @param yRowStride Y plane row stride
     * @param uvRowStride UV plane row stride
     * @param uvPixelStride UV plane pixel stride
     * @param outputData Output RGBA data (pre-allocated)
     */
    fun convertYuvToRgba(
        yPlane: ByteArray,
        uPlane: ByteArray,
        vPlane: ByteArray,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        outputData: ByteArray
    ) {
        if (!isLibraryLoaded) {
            Log.e(TAG, "Library not loaded")
            return
        }

        try {
            yuv420ToRgba(
                yPlane, uPlane, vPlane,
                width, height,
                yRowStride, uvRowStride, uvPixelStride,
                outputData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during YUV to RGBA conversion: ${e.message}")
        }
    }

    /**
     * Release native resources
     */
    fun release() {
        if (!isInitialized) {
            return
        }

        try {
            releaseOpenCV()
            isInitialized = false
            Log.i(TAG, "Native resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during release: ${e.message}")
        }
    }

    /**
     * Get mode name string
     */
    fun getModeName(mode: Int): String {
        return when (mode) {
            MODE_RAW -> "RAW"
            MODE_EDGE -> "EDGE"
            MODE_GRAYSCALE -> "GRAYSCALE"
            else -> "UNKNOWN"
        }
    }

    // Native method declarations
    private external fun initOpenCV(): Boolean
    private external fun isOpenCVAvailable(): Boolean
    private external fun processFrame(
        inputArray: ByteArray,
        width: Int,
        height: Int,
        mode: Int,
        outputArray: ByteArray
    ): Long
    private external fun processFrameBitmap(
        inputBitmap: Bitmap,
        mode: Int,
        outputBitmap: Bitmap
    ): Long
    private external fun setCannyThresholds(lowThreshold: Double, highThreshold: Double)
    private external fun getStatistics(): String
    private external fun releaseOpenCV()
    private external fun yuv420ToRgba(
        yPlane: ByteArray,
        uPlane: ByteArray,
        vPlane: ByteArray,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        outputArray: ByteArray
    )
}
