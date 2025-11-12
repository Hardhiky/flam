# Minimum CMake versiocmake
# Sets the minimum version of CMake required.
# Use a version compatible with your Android Gradle Plugin, like 3.18.1 or 3.22.1.
cmake_minimum_required(VERSION 3.18.1)

# Set the path to your OpenCV Android SDK.
# This points CMake to the correct directory to find the OpenCV build files.
# The path is constructed relative to this CMakeLists.txt file's location.
set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/../OpenCV-android-sdk/sdk/native/jni)

# Find the OpenCV package using the path specified above.
# This command is crucial. It finds the libraries and sets up variables like OpenCV_LIBS.
find_package(OpenCV REQUIRED)

# Log the found OpenCV libraries to the build output for debugging.
message(STATUS "OpenCV library status:")
message(STATUS "    version: ${OpenCV_VERSION}")
message(STATUS "    libraries: ${OpenCV_LIBS}")

# Add your C++ source files and define your native library.
# The library will be named 'libedgedetector.so'.
add_library(edgedetector
            SHARED
            src/main/cpp/native-lib.cpp
            src/main/cpp/opencv_processor.cpp)

# Find the Android logging library, which allows you to use __android_log_print.
find_library(log-lib
             log)

# Link your library with OpenCV and other necessary Android system libraries.
# This is the most important step for fixing your error.
# ${OpenCV_LIBS} is a variable created by find_package() that contains all the OpenCV modules.
target_link_libraries(
        # The library you are building.
        edgedetector

        # --- THIS IS THE FIX ---
        # Link against all the OpenCV modules found by find_package.
        ${OpenCV_LIBS}

        # Link against standard Android libraries.
        android
        jnigraphics
        GLESv2
        EGL
        atomic
        m
        ${log-lib}
        )
