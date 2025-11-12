package com.flam.edgedetector.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 2.0 Renderer for displaying processed camera frames
 */
class GLRenderer : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "GLRenderer"

        // Vertex coordinates (full screen quad)
        private val VERTEX_COORDS = floatArrayOf(
            -1.0f, -1.0f,  // Bottom left
            1.0f, -1.0f,   // Bottom right
            -1.0f, 1.0f,   // Top left
            1.0f, 1.0f     // Top right
        )

        // Texture coordinates (flipped vertically for camera)
        private val TEXTURE_COORDS = floatArrayOf(
            0.0f, 1.0f,  // Bottom left
            1.0f, 1.0f,  // Bottom right
            0.0f, 0.0f,  // Top left
            1.0f, 0.0f   // Top right
        )

        private const val COORDS_PER_VERTEX = 2
        private const val COORDS_PER_TEXTURE = 2
    }

    private var shader: Shader? = null
    private var textureId: Int = 0

    private var vertexBuffer: FloatBuffer
    private var textureBuffer: FloatBuffer

    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    private var frameData: ByteArray? = null
    private var frameWidth: Int = 0
    private var frameHeight: Int = 0
    private var frameUpdated: Boolean = false

    private val frameLock = Any()

    // FPS tracking
    private var frameCount: Long = 0
    private var lastFpsTime: Long = 0
    private var currentFps: Float = 0f

    // Callback for FPS updates
    var onFpsUpdate: ((Float) -> Unit)? = null

    init {
        // Initialize vertex buffer
        val vb = ByteBuffer.allocateDirect(VERTEX_COORDS.size * 4)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer.put(VERTEX_COORDS)
        vertexBuffer.position(0)

        // Initialize texture coordinate buffer
        val tb = ByteBuffer.allocateDirect(TEXTURE_COORDS.size * 4)
        tb.order(ByteOrder.nativeOrder())
        textureBuffer = tb.asFloatBuffer()
        textureBuffer.put(TEXTURE_COORDS)
        textureBuffer.position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i(TAG, "Surface created")

        // Set clear color to black
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Initialize shader
        shader = Shader()
        if (!shader!!.initDefault()) {
            Log.e(TAG, "Failed to initialize shader")
            return
        }

        // Create texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        if (textureId == 0) {
            Log.e(TAG, "Failed to generate texture")
            return
        }

        // Bind and configure texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        Shader.checkGLError("onSurfaceCreated")

        Log.i(TAG, "OpenGL initialization complete")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(TAG, "Surface changed: ${width}x${height}")

        surfaceWidth = width
        surfaceHeight = height

        GLES20.glViewport(0, 0, width, height)

        Shader.checkGLError("onSurfaceChanged")
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Update texture if new frame available
        synchronized(frameLock) {
            if (frameUpdated && frameData != null && frameWidth > 0 && frameHeight > 0) {
                updateTexture()
                frameUpdated = false
            }
        }

        // Check if we have a valid texture and shader
        if (textureId == 0 || shader == null || !shader!!.isValid()) {
            return
        }

        // Use shader program
        shader!!.use()

        // Get attribute locations
        val positionHandle = shader!!.getAttribLocation("aPosition")
        val texCoordHandle = shader!!.getAttribLocation("aTexCoord")
        val textureHandle = shader!!.getUniformLocation("uTexture")

        if (positionHandle < 0 || texCoordHandle < 0 || textureHandle < 0) {
            Log.e(TAG, "Failed to get shader locations")
            return
        }

        // Enable vertex attributes
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // Set vertex data
        GLES20.glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            COORDS_PER_VERTEX * 4,
            vertexBuffer
        )

        // Set texture coordinate data
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            COORDS_PER_TEXTURE,
            GLES20.GL_FLOAT,
            false,
            COORDS_PER_TEXTURE * 4,
            textureBuffer
        )

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Disable vertex attributes
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)

        Shader.checkGLError("onDrawFrame")

        // Update FPS
        updateFps()
    }

    /**
     * Update texture with current frame data
     */
    private fun updateTexture() {
        val data = frameData ?: return

        if (data.isEmpty() || frameWidth <= 0 || frameHeight <= 0) {
            Log.w(TAG, "Invalid frame data")
            return
        }

        // Allocate buffer for texture data
        val buffer = ByteBuffer.allocateDirect(data.size)
        buffer.put(data)
        buffer.position(0)

        // Bind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Upload texture data
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            frameWidth,
            frameHeight,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            buffer
        )

        Shader.checkGLError("updateTexture")
    }

    /**
     * Set new frame data for rendering
     * @param data Frame data in RGBA format
     * @param width Frame width
     * @param height Frame height
     */
    fun setFrameData(data: ByteArray, width: Int, height: Int) {
        synchronized(frameLock) {
            if (frameData == null || frameData!!.size != data.size) {
                frameData = ByteArray(data.size)
            }
            System.arraycopy(data, 0, frameData!!, 0, data.size)
            frameWidth = width
            frameHeight = height
            frameUpdated = true
        }
    }

    /**
     * Update FPS counter
     */
    private fun updateFps() {
        frameCount++
        val currentTime = System.currentTimeMillis()

        if (lastFpsTime == 0L) {
            lastFpsTime = currentTime
        }

        val elapsed = currentTime - lastFpsTime
        if (elapsed >= 1000) {
            currentFps = (frameCount * 1000.0f) / elapsed
            frameCount = 0
            lastFpsTime = currentTime

            // Notify FPS update on UI thread
            onFpsUpdate?.invoke(currentFps)
        }
    }

    /**
     * Get current FPS
     */
    fun getCurrentFps(): Float = currentFps

    /**
     * Release OpenGL resources
     */
    fun release() {
        Log.i(TAG, "Releasing renderer resources")

        synchronized(frameLock) {
            frameData = null
            frameUpdated = false
        }

        // Delete texture
        if (textureId != 0) {
            val textures = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, textures, 0)
            textureId = 0
        }

        // Release shader
        shader?.release()
        shader = null

        Log.i(TAG, "Renderer resources released")
    }

    /**
     * Get surface dimensions
     */
    fun getSurfaceWidth(): Int = surfaceWidth
    fun getSurfaceHeight(): Int = surfaceHeight
}
