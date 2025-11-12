#ifndef EDGEDETECTOR_OPENCV_PROCESSOR_H
#define EDGEDETECTOR_OPENCV_PROCESSOR_H

#include <jni.h>
#include <android/log.h>
#include <cstdint>
#include <string>

// Logging macro
#define LOG_TAG "OpenCVProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Processing modes
enum ProcessingMode {
    MODE_RAW = 0,       // No processing
    MODE_EDGE = 1,      // Canny edge detection
    MODE_GRAYSCALE = 2  // Grayscale conversion
};

// Performance metrics structure
struct ProcessingMetrics {
    int64_t processingTimeMs;
    int width;
    int height;
    int mode;
    bool success;
};

class OpenCVProcessor {
public:
    OpenCVProcessor();
    ~OpenCVProcessor();

    /**
     * Initialize OpenCV processor
     * @return true if initialization successful
     */
    bool initialize();

    /**
     * Check if OpenCV is available
     * @return true if OpenCV is properly loaded
     */
    bool isOpenCVAvailable() const;

    /**
     * Process frame with specified mode
     * @param inputData Input RGBA frame data
     * @param width Frame width
     * @param height Frame height
     * @param mode Processing mode
     * @param outputData Output processed frame data (must be pre-allocated)
     * @return Processing metrics
     */
    ProcessingMetrics processFrame(
        const uint8_t* inputData,
        int width,
        int height,
        ProcessingMode mode,
        uint8_t* outputData
    );

    /**
     * Apply Canny edge detection
     * @param inputData Input RGBA frame data
     * @param width Frame width
     * @param height Frame height
     * @param outputData Output edge-detected frame (RGBA)
     * @return true if successful
     */
    bool applyCannyEdge(
        const uint8_t* inputData,
        int width,
        int height,
        uint8_t* outputData
    );

    /**
     * Convert frame to grayscale
     * @param inputData Input RGBA frame data
     * @param width Frame width
     * @param height Frame height
     * @param outputData Output grayscale frame (RGBA format but grayscale)
     * @return true if successful
     */
    bool convertToGrayscale(
        const uint8_t* inputData,
        int width,
        int height,
        uint8_t* outputData
    );

    /**
     * Copy frame without processing
     * @param inputData Input RGBA frame data
     * @param width Frame width
     * @param height Frame height
     * @param outputData Output frame data
     * @return true if successful
     */
    bool copyRawFrame(
        const uint8_t* inputData,
        int width,
        int height,
        uint8_t* outputData
    );

    /**
     * Get Canny edge detection thresholds
     */
    void setCannyThresholds(double lowThreshold, double highThreshold);

    /**
     * Get current processing statistics
     */
    std::string getStatistics() const;

    /**
     * Release resources
     */
    void release();

private:
    bool mInitialized;
    bool mOpenCVAvailable;
    
    // Canny edge detection parameters
    double mCannyLowThreshold;
    double mCannyHighThreshold;
    int mCannyApertureSize;
    
    // Statistics
    uint64_t mTotalFramesProcessed;
    uint64_t mTotalProcessingTimeMs;
    int64_t mLastProcessingTimeMs;
    
    // Helper methods
    int64_t getCurrentTimeMs() const;
    void updateStatistics(int64_t processingTimeMs);
    
    // Fallback processing methods (when OpenCV is not available)
    bool applyCannyEdgeFallback(
        const uint8_t* inputData,
        int width,
        int height,
        uint8_t* outputData
    );
    
    bool convertToGrayscaleFallback(
        const uint8_t* inputData,
        int width,
        int height,
        uint8_t* outputData
    );
};

// Utility functions
namespace ImageUtils {
    /**
     * Convert RGBA to grayscale using standard luminance formula
     */
    inline uint8_t rgbaToGray(uint8_t r, uint8_t g, uint8_t b) {
        return static_cast<uint8_t>(0.299f * r + 0.587f * g + 0.114f * b);
    }
    
    /**
     * Simple edge detection using Sobel-like operator (fallback)
     */
    void simpleEdgeDetection(
        const uint8_t* grayscale,
        int width,
        int height,
        uint8_t* output
    );
    
    /**
     * Apply threshold to grayscale image
     */
    void applyThreshold(
        const uint8_t* input,
        int width,
        int height,
        uint8_t threshold,
        uint8_t* output
    );
}

#endif // EDGEDETECTOR_OPENCV_PROCESSOR_H