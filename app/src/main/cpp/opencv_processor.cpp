#include "opencv_processor.h"
#include <cstring>
#include <cmath>
#include <chrono>
#include <algorithm>

// Check if OpenCV is available at compile time
#ifdef HAVE_OPENCV
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
using namespace cv;
#endif

// Constructor
OpenCVProcessor::OpenCVProcessor()
    : mInitialized(false)
    , mOpenCVAvailable(false)
    , mCannyLowThreshold(50.0)
    , mCannyHighThreshold(150.0)
    , mCannyApertureSize(3)
    , mTotalFramesProcessed(0)
    , mTotalProcessingTimeMs(0)
    , mLastProcessingTimeMs(0)
{
    LOGI("OpenCVProcessor created");
}

// Destructor
OpenCVProcessor::~OpenCVProcessor() {
    release();
    LOGI("OpenCVProcessor destroyed");
}

// Initialize OpenCV
bool OpenCVProcessor::initialize() {
    if (mInitialized) {
        LOGW("OpenCVProcessor already initialized");
        return true;
    }

#ifdef HAVE_OPENCV
    try {
        // Test OpenCV availability by creating a small test matrix
        Mat testMat = Mat::zeros(2, 2, CV_8UC1);
        if (testMat.empty()) {
            LOGE("OpenCV test matrix creation failed");
            mOpenCVAvailable = false;
        } else {
            mOpenCVAvailable = true;
            LOGI("OpenCV initialized successfully (version %s)", CV_VERSION);
        }
    } catch (const std::exception& e) {
        LOGE("OpenCV initialization exception: %s", e.what());
        mOpenCVAvailable = false;
    }
#else
    LOGW("OpenCV not available at compile time - using fallback implementation");
    mOpenCVAvailable = false;
#endif

    mInitialized = true;
    return true;
}

// Check if OpenCV is available
bool OpenCVProcessor::isOpenCVAvailable() const {
    return mOpenCVAvailable;
}

// Process frame with specified mode
ProcessingMetrics OpenCVProcessor::processFrame(
    const uint8_t* inputData,
    int width,
    int height,
    ProcessingMode mode,
    uint8_t* outputData
) {
    ProcessingMetrics metrics = {0, width, height, mode, false};
    
    if (!mInitialized) {
        LOGE("Processor not initialized");
        return metrics;
    }
    
    if (inputData == nullptr || outputData == nullptr) {
        LOGE("Invalid input or output data pointer");
        return metrics;
    }
    
    if (width <= 0 || height <= 0) {
        LOGE("Invalid dimensions: %dx%d", width, height);
        return metrics;
    }
    
    int64_t startTime = getCurrentTimeMs();
    bool success = false;
    
    switch (mode) {
        case MODE_RAW:
            success = copyRawFrame(inputData, width, height, outputData);
            break;
            
        case MODE_EDGE:
            success = applyCannyEdge(inputData, width, height, outputData);
            break;
            
        case MODE_GRAYSCALE:
            success = convertToGrayscale(inputData, width, height, outputData);
            break;
            
        default:
            LOGE("Unknown processing mode: %d", mode);
            success = copyRawFrame(inputData, width, height, outputData);
            break;
    }
    
    int64_t endTime = getCurrentTimeMs();
    metrics.processingTimeMs = endTime - startTime;
    metrics.success = success;
    
    updateStatistics(metrics.processingTimeMs);
    
    return metrics;
}

// Apply Canny edge detection
bool OpenCVProcessor::applyCannyEdge(
    const uint8_t* inputData,
    int width,
    int height,
    uint8_t* outputData
) {
#ifdef HAVE_OPENCV
    if (mOpenCVAvailable) {
        try {
            // Create input Mat from RGBA data
            Mat inputMat(height, width, CV_8UC4, (void*)inputData);
            
            // Convert to grayscale
            Mat grayMat;
            cvtColor(inputMat, grayMat, COLOR_RGBA2GRAY);
            
            // Apply Gaussian blur to reduce noise
            Mat blurredMat;
            GaussianBlur(grayMat, blurredMat, Size(5, 5), 1.5);
            
            // Apply Canny edge detection
            Mat edgesMat;
            Canny(blurredMat, edgesMat, mCannyLowThreshold, mCannyHighThreshold, mCannyApertureSize);
            
            // Convert edges back to RGBA (white edges on black background)
            Mat outputMat(height, width, CV_8UC4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    uint8_t edgeValue = edgesMat.at<uint8_t>(y, x);
                    outputMat.at<Vec4b>(y, x) = Vec4b(edgeValue, edgeValue, edgeValue, 255);
                }
            }
            
            // Copy to output buffer
            std::memcpy(outputData, outputMat.data, width * height * 4);
            
            return true;
        } catch (const std::exception& e) {
            LOGE("OpenCV Canny edge detection failed: %s", e.what());
            return applyCannyEdgeFallback(inputData, width, height, outputData);
        }
    }
#endif
    
    // Use fallback if OpenCV is not available
    return applyCannyEdgeFallback(inputData, width, height, outputData);
}

// Convert to grayscale
bool OpenCVProcessor::convertToGrayscale(
    const uint8_t* inputData,
    int width,
    int height,
    uint8_t* outputData
) {
#ifdef HAVE_OPENCV
    if (mOpenCVAvailable) {
        try {
            // Create input Mat from RGBA data
            Mat inputMat(height, width, CV_8UC4, (void*)inputData);
            
            // Convert to grayscale
            Mat grayMat;
            cvtColor(inputMat, grayMat, COLOR_RGBA2GRAY);
            
            // Convert back to RGBA format (grayscale in all channels)
            Mat outputMat(height, width, CV_8UC4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    uint8_t grayValue = grayMat.at<uint8_t>(y, x);
                    outputMat.at<Vec4b>(y, x) = Vec4b(grayValue, grayValue, grayValue, 255);
                }
            }
            
            // Copy to output buffer
            std::memcpy(outputData, outputMat.data, width * height * 4);
            
            return true;
        } catch (const std::exception& e) {
            LOGE("OpenCV grayscale conversion failed: %s", e.what());
            return convertToGrayscaleFallback(inputData, width, height, outputData);
        }
    }
#endif
    
    // Use fallback if OpenCV is not available
    return convertToGrayscaleFallback(inputData, width, height, outputData);
}

// Copy raw frame
bool OpenCVProcessor::copyRawFrame(
    const uint8_t* inputData,
    int width,
    int height,
    uint8_t* outputData
) {
    size_t dataSize = width * height * 4; // RGBA format
    std::memcpy(outputData, inputData, dataSize);
    return true;
}

// Set Canny thresholds
void OpenCVProcessor::setCannyThresholds(double lowThreshold, double highThreshold) {
    mCannyLowThreshold = lowThreshold;
    mCannyHighThreshold = highThreshold;
    LOGI("Canny thresholds updated: low=%.1f, high=%.1f", lowThreshold, highThreshold);
}

// Get statistics
std::string OpenCVProcessor::getStatistics() const {
    char buffer[256];
    double avgTime = mTotalFramesProcessed > 0 
        ? static_cast<double>(mTotalProcessingTimeMs) / mTotalFramesProcessed 
        : 0.0;
    
    snprintf(buffer, sizeof(buffer),
        "Frames: %llu, Avg Time: %.2fms, Last Time: %lldms, OpenCV: %s",
        static_cast<unsigned long long>(mTotalFramesProcessed),
        avgTime,
        static_cast<long long>(mLastProcessingTimeMs),
        mOpenCVAvailable ? "Yes" : "No");
    
    return std::string(buffer);
}

// Release resources
void OpenCVProcessor::release() {
    if (mInitialized) {
        LOGI("Releasing OpenCVProcessor resources");
        LOGI("Final statistics: %s", getStatistics().c_str());
        mInitialized = false;
    }
}

// Get current time in milliseconds
int64_t OpenCVProcessor::getCurrentTimeMs() const {
    auto now = std::chrono::steady_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());
    return duration.count();
}

// Update statistics
void OpenCVProcessor::updateStatistics(int64_t processingTimeMs) {
    mTotalFramesProcessed++;
    mTotalProcessingTimeMs += processingTimeMs;
    mLastProcessingTimeMs = processingTimeMs;
    
    // Log every 100 frames
    if (mTotalFramesProcessed % 100 == 0) {
        LOGD("Statistics: %s", getStatistics().c_str());
    }
}

// Fallback Canny edge detection
bool OpenCVProcessor::applyCannyEdgeFallback(
    const uint8_t* inputData,
    int width,
    int height,
    uint8_t* outputData
) {
    // Allocate temporary grayscale buffer
    uint8_t* grayData = new uint8_t[width * height];
    
    // Convert to grayscale
    for (int i = 0; i < width * height; i++) {
        int idx = i * 4;
        grayData[i] = ImageUtils::rgbaToGray(
            inputData[idx],
            inputData[idx + 1],
            inputData[idx + 2]
        );
    }
    
    // Apply simple edge detection
    uint8_t* edgeData = new uint8_t[width * height];
    ImageUtils::simpleEdgeDetection(grayData, width, height, edgeData);
    
    // Convert back to RGBA
    for (int i = 0; i < width * height; i++) {
        int idx = i * 4;
        uint8_t edgeValue = edgeData[i];
        outputData[idx] = edgeValue;
        outputData[idx + 1] = edgeValue;
        outputData[idx + 2] = edgeValue;
        outputData[idx + 3] = 255;
    }
    
    delete[] grayData;
    delete[] edgeData;
    
    return true;
}

// Fallback grayscale conversion
bool OpenCVProcessor::convertToGrayscaleFallback(
    const uint8_t* inputData,
    int width,
    int height,
    uint8_t* outputData
) {
    for (int i = 0; i < width * height; i++) {
        int idx = i * 4;
        uint8_t gray = ImageUtils::rgbaToGray(
            inputData[idx],
            inputData[idx + 1],
            inputData[idx + 2]
        );
        outputData[idx] = gray;
        outputData[idx + 1] = gray;
        outputData[idx + 2] = gray;
        outputData[idx + 3] = 255;
    }
    return true;
}

// ImageUtils namespace implementation
namespace ImageUtils {
    void simpleEdgeDetection(
        const uint8_t* grayscale,
        int width,
        int height,
        uint8_t* output
    ) {
        // Sobel kernels
        const int sobelX[3][3] = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
        };
        
        const int sobelY[3][3] = {
            {-1, -2, -1},
            { 0,  0,  0},
            { 1,  2,  1}
        };
        
        // Initialize output to zero
        std::memset(output, 0, width * height);
        
        // Apply Sobel operator
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int gx = 0;
                int gy = 0;
                
                // Convolve with Sobel kernels
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = grayscale[(y + ky) * width + (x + kx)];
                        gx += pixel * sobelX[ky + 1][kx + 1];
                        gy += pixel * sobelY[ky + 1][kx + 1];
                    }
                }
                
                // Calculate gradient magnitude
                int magnitude = static_cast<int>(std::sqrt(gx * gx + gy * gy));
                magnitude = std::min(255, magnitude);
                
                output[y * width + x] = static_cast<uint8_t>(magnitude);
            }
        }
        
        // Apply threshold to create binary edges
        const uint8_t threshold = 50;
        for (int i = 0; i < width * height; i++) {
            output[i] = output[i] > threshold ? 255 : 0;
        }
    }
    
    void applyThreshold(
        const uint8_t* input,
        int width,
        int height,
        uint8_t threshold,
        uint8_t* output
    ) {
        for (int i = 0; i < width * height; i++) {
            output[i] = input[i] > threshold ? 255 : 0;
        }
    }
}