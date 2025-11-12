package com.flam.edgedetector.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * WebSocket Frame Streamer - Sends processed frames to web viewer in real-time
 */
class WebSocketFrameStreamer(
    private val serverUrl: String = "ws://192.168.1.100:8080" // Change to your PC's IP
) {
    companion object {
        private const val TAG = "WebSocketStreamer"
        private const val RECONNECT_INTERVAL_MS = 5000L
        private const val PING_INTERVAL_MS = 30000L
        private const val MAX_FRAME_SIZE_KB = 500 // Limit frame size
        private const val JPEG_QUALITY = 80 // JPEG compression quality (0-100)
    }

    private var webSocket: WebSocket? = null
    private val okHttpClient: OkHttpClient
    private val gson = Gson()
    private var isConnecting = false
    private var shouldReconnect = true
    private var frameCounter = 0L
    private var bytesSent = 0L

    // Callbacks
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onMessageReceived: ((String) -> Unit)? = null

    init {
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(PING_INTERVAL_MS, TimeUnit.MILLISECONDS)
            .build()

        Log.i(TAG, "WebSocket Frame Streamer initialized for: $serverUrl")
    }

    /**
     * Connect to WebSocket server
     */
    fun connect() {
        if (webSocket != null || isConnecting) {
            Log.w(TAG, "Already connected or connecting")
            return
        }

        isConnecting = true
        shouldReconnect = true
        Log.i(TAG, "Connecting to WebSocket server: $serverUrl")

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnecting = false
                Log.i(TAG, " WebSocket connected successfully")
                onConnected?.invoke()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, " Received message: $text")
                onMessageReceived?.invoke(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnecting = false
                this@WebSocketFrameStreamer.webSocket = null
                Log.i(TAG, " WebSocket closed: $code - $reason")
                onDisconnected?.invoke()

                // Auto-reconnect if needed
                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnecting = false
                this@WebSocketFrameStreamer.webSocket = null
                val errorMsg = "WebSocket error: ${t.message}"
                Log.e(TAG, " $errorMsg", t)
                onError?.invoke(errorMsg)
                onDisconnected?.invoke()

                // Auto-reconnect on failure
                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }
        })
    }

    /**
     * Schedule reconnection attempt
     */
    private fun scheduleReconnect() {
        if (!shouldReconnect) return

        Log.i(TAG, " Reconnecting in ${RECONNECT_INTERVAL_MS / 1000} seconds...")
        Thread {
            Thread.sleep(RECONNECT_INTERVAL_MS)
            if (shouldReconnect && webSocket == null && !isConnecting) {
                connect()
            }
        }.start()
    }

    /**
     * Disconnect from WebSocket server
     */
    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        isConnecting = false
        Log.i(TAG, "Disconnected from WebSocket server")
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return webSocket != null
    }

    /**
     * Send frame data to web viewer
     * @param frameData RGBA byte array
     * @param width Frame width
     * @param height Frame height
     * @param mode Processing mode (0=RAW, 1=EDGE, 2=GRAYSCALE)
     * @param processingTime Processing time in milliseconds
     */
    fun sendFrame(
        frameData: ByteArray,
        width: Int,
        height: Int,
        mode: Int,
        processingTime: Long
    ) {
        val ws = webSocket
        if (ws == null) {
            Log.w(TAG, "️ WebSocket not connected, skipping frame")
            return
        }

        try {
            // Convert RGBA byte array to Bitmap
            val bitmap = rgbaToBitmap(frameData, width, height)
            if (bitmap == null) {
                Log.e(TAG, "Failed to create bitmap from frame data")
                return
            }

            // Compress bitmap to JPEG for efficient transmission
            val imageData = bitmapToBase64(bitmap, JPEG_QUALITY)
            bitmap.recycle() // Free memory

            // Check size limit
            val sizeKb = imageData.length / 1024
            if (sizeKb > MAX_FRAME_SIZE_KB) {
                Log.w(TAG, "️ Frame too large: ${sizeKb}KB, skipping")
                return
            }

            // Create frame message
            val frameMessage = FrameMessage(
                type = "frame",
                imageData = imageData,
                width = width,
                height = height,
                mode = mode,
                processingTime = processingTime,
                timestamp = System.currentTimeMillis(),
                frameNumber = ++frameCounter
            )

            // Serialize to JSON
            val json = gson.toJson(frameMessage)

            // Send via WebSocket
            val success = ws.send(json)
            if (success) {
                bytesSent += json.length
                Log.d(TAG, " Frame #$frameCounter sent (${sizeKb}KB, ${processingTime}ms)")
            } else {
                Log.w(TAG, " Failed to send frame #$frameCounter")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending frame: ${e.message}", e)
        }
    }

    /**
     * Convert RGBA byte array to Bitmap
     */
    private fun rgbaToBitmap(rgba: ByteArray, width: Int, height: Int): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Convert RGBA to ARGB (Android bitmap format)
            val argb = IntArray(width * height)
            for (i in argb.indices) {
                val idx = i * 4
                if (idx + 3 < rgba.size) {
                    val r = rgba[idx].toInt() and 0xFF
                    val g = rgba[idx + 1].toInt() and 0xFF
                    val b = rgba[idx + 2].toInt() and 0xFF
                    val a = rgba[idx + 3].toInt() and 0xFF
                    argb[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                }
            }

            bitmap.setPixels(argb, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error converting RGBA to Bitmap: ${e.message}", e)
            null
        }
    }

    /**
     * Convert Bitmap to Base64 encoded JPEG string
     */
    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    /**
     * Send ping to keep connection alive
     */
    fun sendPing() {
        val ws = webSocket
        if (ws != null) {
            val pingMessage = mapOf(
                "type" to "ping",
                "timestamp" to System.currentTimeMillis()
            )
            val json = gson.toJson(pingMessage)
            ws.send(json)
            Log.d(TAG, " Ping sent")
        }
    }

    /**
     * Get connection statistics
     */
    fun getStats(): String {
        return "Frames sent: $frameCounter, Bytes sent: ${bytesSent / 1024}KB, Connected: ${isConnected()}"
    }

    /**
     * Release resources
     */
    fun release() {
        shouldReconnect = false
        disconnect()
        okHttpClient.dispatcher.executorService.shutdown()
        okHttpClient.connectionPool.evictAll()
        Log.i(TAG, "WebSocket Frame Streamer released")
    }

    /**
     * Data class for frame message
     */
    private data class FrameMessage(
        val type: String,
        val imageData: String,
        val width: Int,
        val height: Int,
        val mode: Int,
        val processingTime: Long,
        val timestamp: Long,
        val frameNumber: Long
    )
}
