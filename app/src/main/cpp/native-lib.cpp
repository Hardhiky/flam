#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <cstring>
#include <string>
#include "opencv_processor.h"

#define LOG_TAG "NativeLib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Global processor instance
static OpenCVProcessor* g_processor = nullptr;

// JNI method to initialize OpenCV
extern "C" JNIEXPORT jboolean JNICALL
Java_com_flam_edgedetector_NativeLib_initOpenCV(
    JNIEnv* env,
    jobject /* this */
) {
    LOGI("Initializing OpenCV native library");
    
    if (g_processor != nullptr) {
        LOGW("Processor already initialized");
        return JNI_TRUE;
    }
    
    try {
        g_processor = new OpenCVProcessor();
        if (g_processor->initialize()) {
            LOGI("OpenCV processor initialized successfully");
            if (g_processor->isOpenCVAvailable()) {
                LOGI("OpenCV is available and ready");
            } else {
                LOGW("OpenCV not available - using fallback implementation");
            }
            return JNI_TRUE;
        } else {
            LOGE("Failed to initialize OpenCV processor");
            delete g_processor;
            g_processor = nullptr;
            return JNI_FALSE;
        }
    } catch (const std::exception& e) {
        LOGE("Exception during initialization: %s", e.what());
        if (g_processor != nullptr) {
            delete g_processor;
            g_processor = nullptr;
        }
        return JNI_FALSE;
    }
}

// JNI method to check if OpenCV is available
extern "C" JNIEXPORT jboolean JNICALL
Java_com_flam_edgedetector_NativeLib_isOpenCVAvailable(
    JNIEnv* env,
    jobject /* this */
) {
    if (g_processor == nullptr) {
        return JNI_FALSE;
    }
    return g_processor->isOpenCVAvailable() ? JNI_TRUE : JNI_FALSE;
}

// JNI method to process frame
extern "C" JNIEXPORT jlong JNICALL
Java_com_flam_edgedetector_NativeLib_processFrame(
    JNIEnv* env,
    jobject /* this */,
    jbyteArray inputArray,
    jint width,
    jint height,
    jint mode,
    jbyteArray outputArray
) {
    if (g_processor == nullptr) {
        LOGE("Processor not initialized");
        return -1;
    }
    
    if (inputArray == nullptr || outputArray == nullptr) {
        LOGE("Input or output array is null");
        return -1;
    }
    
    // Get input array
    jsize inputLength = env->GetArrayLength(inputArray);
    jsize expectedLength = width * height * 4; // RGBA format
    
    if (inputLength < expectedLength) {
        LOGE("Input array too small: %d, expected: %d", inputLength, expectedLength);
        return -1;
    }
    
    // Get output array
    jsize outputLength = env->GetArrayLength(outputArray);
    if (outputLength < expectedLength) {
        LOGE("Output array too small: %d, expected: %d", outputLength, expectedLength);
        return -1;
    }
    
    // Get byte array elements
    jbyte* inputBytes = env->GetByteArrayElements(inputArray, nullptr);
    jbyte* outputBytes = env->GetByteArrayElements(outputArray, nullptr);
    
    if (inputBytes == nullptr || outputBytes == nullptr) {
        LOGE("Failed to get byte array elements");
        if (inputBytes != nullptr) {
            env->ReleaseByteArrayElements(inputArray, inputBytes, JNI_ABORT);
        }
        if (outputBytes != nullptr) {
            env->ReleaseByteArrayElements(outputArray, outputBytes, JNI_ABORT);
        }
        return -1;
    }
    
    // Process frame
    ProcessingMode processingMode = static_cast<ProcessingMode>(mode);
    ProcessingMetrics metrics = g_processor->processFrame(
        reinterpret_cast<const uint8_t*>(inputBytes),
        width,
        height,
        processingMode,
        reinterpret_cast<uint8_t*>(outputBytes)
    );
    
    // Release arrays
    env->ReleaseByteArrayElements(inputArray, inputBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(outputArray, outputBytes, 0);
    
    if (!metrics.success) {
        LOGE("Frame processing failed");
        return -1;
    }
    
    return metrics.processingTimeMs;
}

// JNI method to process frame from bitmap
extern "C" JNIEXPORT jlong JNICALL
Java_com_flam_edgedetector_NativeLib_processFrameBitmap(
    JNIEnv* env,
    jobject /* this */,
    jobject inputBitmap,
    jint mode,
    jobject outputBitmap
) {
    if (g_processor == nullptr) {
        LOGE("Processor not initialized");
        return -1;
    }
    
    if (inputBitmap == nullptr || outputBitmap == nullptr) {
        LOGE("Input or output bitmap is null");
        return -1;
    }
    
    // Get bitmap info
    AndroidBitmapInfo inputInfo;
    AndroidBitmapInfo outputInfo;
    
    if (AndroidBitmap_getInfo(env, inputBitmap, &inputInfo) < 0) {
        LOGE("Failed to get input bitmap info");
        return -1;
    }
    
    if (AndroidBitmap_getInfo(env, outputBitmap, &outputInfo) < 0) {
        LOGE("Failed to get output bitmap info");
        return -1;
    }
    
    // Verify bitmap format
    if (inputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Input bitmap format not supported: %d", inputInfo.format);
        return -1;
    }
    
    if (outputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Output bitmap format not supported: %d", outputInfo.format);
        return -1;
    }
    
    // Lock bitmaps
    void* inputPixels;
    void* outputPixels;
    
    if (AndroidBitmap_lockPixels(env, inputBitmap, &inputPixels) < 0) {
        LOGE("Failed to lock input bitmap");
        return -1;
    }
    
    if (AndroidBitmap_lockPixels(env, outputBitmap, &outputPixels) < 0) {
        LOGE("Failed to lock output bitmap");
        AndroidBitmap_unlockPixels(env, inputBitmap);
        return -1;
    }
    
    // Process frame
    ProcessingMode processingMode = static_cast<ProcessingMode>(mode);
    ProcessingMetrics metrics = g_processor->processFrame(
        static_cast<const uint8_t*>(inputPixels),
        inputInfo.width,
        inputInfo.height,
        processingMode,
        static_cast<uint8_t*>(outputPixels)
    );
    
    // Unlock bitmaps
    AndroidBitmap_unlockPixels(env, inputBitmap);
    AndroidBitmap_unlockPixels(env, outputBitmap);
    
    if (!metrics.success) {
        LOGE("Bitmap processing failed");
        return -1;
    }
    
    return metrics.processingTimeMs;
}

// JNI method to set Canny thresholds
extern "C" JNIEXPORT void JNICALL
Java_com_flam_edgedetector_NativeLib_setCannyThresholds(
    JNIEnv* env,
    jobject /* this */,
    jdouble lowThreshold,
    jdouble highThreshold
) {
    if (g_processor != nullptr) {
        g_processor->setCannyThresholds(lowThreshold, highThreshold);
        LOGI("Canny thresholds set: low=%.1f, high=%.1f", lowThreshold, highThreshold);
    } else {
        LOGE("Processor not initialized");
    }
}

// JNI method to get statistics
extern "C" JNIEXPORT jstring JNICALL
Java_com_flam_edgedetector_NativeLib_getStatistics(
    JNIEnv* env,
    jobject /* this */
) {
    if (g_processor == nullptr) {
        return env->NewStringUTF("Processor not initialized");
    }
    
    std::string stats = g_processor->getStatistics();
    return env->NewStringUTF(stats.c_str());
}

// JNI method to release OpenCV
extern "C" JNIEXPORT void JNICALL
Java_com_flam_edgedetector_NativeLib_releaseOpenCV(
    JNIEnv* env,
    jobject /* this */
) {
    LOGI("Releasing OpenCV native library");
    
    if (g_processor != nullptr) {
        g_processor->release();
        delete g_processor;
        g_processor = nullptr;
        LOGI("Processor released");
    }
}

// JNI_OnLoad - called when native library is loaded
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("Native library loaded");
    return JNI_VERSION_1_6;
}

// JNI_OnUnload - called when native library is unloaded
JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved) {
    LOGI("Native library unloaded");
    
    // Clean up global processor if it still exists
    if (g_processor != nullptr) {
        g_processor->release();
        delete g_processor;
        g_processor = nullptr;
    }
}

// Utility function to convert YUV to RGBA (for camera preview)
extern "C" JNIEXPORT void JNICALL
Java_com_flam_edgedetector_NativeLib_yuv420ToRgba(
    JNIEnv* env,
    jobject /* this */,
    jbyteArray yPlane,
    jbyteArray uPlane,
    jbyteArray vPlane,
    jint width,
    jint height,
    jint yRowStride,
    jint uvRowStride,
    jint uvPixelStride,
    jbyteArray outputArray
) {
    // Get array elements
    jbyte* yData = env->GetByteArrayElements(yPlane, nullptr);
    jbyte* uData = env->GetByteArrayElements(uPlane, nullptr);
    jbyte* vData = env->GetByteArrayElements(vPlane, nullptr);
    jbyte* outData = env->GetByteArrayElements(outputArray, nullptr);
    
    if (yData == nullptr || uData == nullptr || vData == nullptr || outData == nullptr) {
        LOGE("Failed to get YUV array elements");
        if (yData) env->ReleaseByteArrayElements(yPlane, yData, JNI_ABORT);
        if (uData) env->ReleaseByteArrayElements(uPlane, uData, JNI_ABORT);
        if (vData) env->ReleaseByteArrayElements(vPlane, vData, JNI_ABORT);
        if (outData) env->ReleaseByteArrayElements(outputArray, outData, JNI_ABORT);
        return;
    }
    
    // Convert YUV420 to RGBA
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int yIndex = y * yRowStride + x;
            int uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride;
            
            int Y = yData[yIndex] & 0xFF;
            int U = uData[uvIndex] & 0xFF;
            int V = vData[uvIndex] & 0xFF;
            
            // YUV to RGB conversion
            int C = Y - 16;
            int D = U - 128;
            int E = V - 128;
            
            int R = (298 * C + 409 * E + 128) >> 8;
            int G = (298 * C - 100 * D - 208 * E + 128) >> 8;
            int B = (298 * C + 516 * D + 128) >> 8;
            
            // Clamp values
            R = R < 0 ? 0 : (R > 255 ? 255 : R);
            G = G < 0 ? 0 : (G > 255 ? 255 : G);
            B = B < 0 ? 0 : (B > 255 ? 255 : B);
            
            // Write RGBA
            int outIndex = (y * width + x) * 4;
            outData[outIndex] = static_cast<jbyte>(R);
            outData[outIndex + 1] = static_cast<jbyte>(G);
            outData[outIndex + 2] = static_cast<jbyte>(B);
            outData[outIndex + 3] = static_cast<jbyte>(255);
        }
    }
    
    // Release arrays
    env->ReleaseByteArrayElements(yPlane, yData, JNI_ABORT);
    env->ReleaseByteArrayElements(uPlane, uData, JNI_ABORT);
    env->ReleaseByteArrayElements(vPlane, vData, JNI_ABORT);
    env->ReleaseByteArrayElements(outputArray, outData, 0);
}