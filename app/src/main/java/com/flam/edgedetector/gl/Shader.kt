package com.flam.edgedetector.gl

import android.opengl.GLES20
import android.util.Log

/**
 * Utility class for managing OpenGL ES shaders
 */
class Shader {

    companion object {
        private const val TAG = "Shader"

        /**
         * Load and compile a shader
         * @param type Shader type (GLES20.GL_VERTEX_SHADER or GLES20.GL_FRAGMENT_SHADER)
         * @param shaderCode Shader source code
         * @return Shader handle, or 0 on error
         */
        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            if (shader == 0) {
                Log.e(TAG, "Error creating shader")
                return 0
            }

            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)

            if (compiled[0] == 0) {
                val info = GLES20.glGetShaderInfoLog(shader)
                Log.e(TAG, "Could not compile shader $type: $info")
                GLES20.glDeleteShader(shader)
                return 0
            }

            return shader
        }

        /**
         * Create and link a shader program
         * @param vertexShaderCode Vertex shader source code
         * @param fragmentShaderCode Fragment shader source code
         * @return Program handle, or 0 on error
         */
        fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            if (vertexShader == 0) {
                return 0
            }

            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
            if (fragmentShader == 0) {
                GLES20.glDeleteShader(vertexShader)
                return 0
            }

            val program = GLES20.glCreateProgram()
            if (program == 0) {
                Log.e(TAG, "Error creating program")
                GLES20.glDeleteShader(vertexShader)
                GLES20.glDeleteShader(fragmentShader)
                return 0
            }

            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)

            if (linkStatus[0] == 0) {
                val info = GLES20.glGetProgramInfoLog(program)
                Log.e(TAG, "Could not link program: $info")
                GLES20.glDeleteProgram(program)
                GLES20.glDeleteShader(vertexShader)
                GLES20.glDeleteShader(fragmentShader)
                return 0
            }

            // Shaders are no longer needed after linking
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)

            return program
        }

        /**
         * Check for OpenGL errors
         * @param operation Operation name for logging
         */
        fun checkGLError(operation: String) {
            var error: Int
            while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "$operation: glError $error")
            }
        }

        /**
         * Default vertex shader for texture rendering
         */
        val DEFAULT_VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;

            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        /**
         * Default fragment shader for texture rendering
         */
        val DEFAULT_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;

            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """.trimIndent()

        /**
         * Fragment shader with grayscale effect
         */
        val GRAYSCALE_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;

            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                gl_FragColor = vec4(gray, gray, gray, color.a);
            }
        """.trimIndent()

        /**
         * Fragment shader with invert effect
         */
        val INVERT_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;

            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                gl_FragColor = vec4(1.0 - color.rgb, color.a);
            }
        """.trimIndent()

        /**
         * Fragment shader with brightness adjustment
         */
        val BRIGHTNESS_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            uniform float uBrightness;

            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                gl_FragColor = vec4(color.rgb + uBrightness, color.a);
            }
        """.trimIndent()

        /**
         * Fragment shader with contrast adjustment
         */
        val CONTRAST_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            uniform float uContrast;

            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                vec3 adjusted = ((color.rgb - 0.5) * uContrast) + 0.5;
                gl_FragColor = vec4(adjusted, color.a);
            }
        """.trimIndent()

        /**
         * Fragment shader with edge enhancement
         */
        val EDGE_ENHANCE_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            uniform vec2 uTexelSize;

            void main() {
                vec2 tc = vTexCoord;
                vec4 center = texture2D(uTexture, tc);
                vec4 left = texture2D(uTexture, tc - vec2(uTexelSize.x, 0.0));
                vec4 right = texture2D(uTexture, tc + vec2(uTexelSize.x, 0.0));
                vec4 top = texture2D(uTexture, tc - vec2(0.0, uTexelSize.y));
                vec4 bottom = texture2D(uTexture, tc + vec2(0.0, uTexelSize.y));

                vec4 edges = abs(center - left) + abs(center - right) +
                             abs(center - top) + abs(center - bottom);

                gl_FragColor = vec4(edges.rgb, 1.0);
            }
        """.trimIndent()

        /**
         * Fragment shader with vignette effect
         */
        val VIGNETTE_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            uniform float uVignetteStrength;

            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                vec2 center = vTexCoord - vec2(0.5, 0.5);
                float dist = length(center);
                float vignette = 1.0 - (dist * uVignetteStrength);
                gl_FragColor = vec4(color.rgb * vignette, color.a);
            }
        """.trimIndent()
    }

    private var program: Int = 0
    private val uniforms = mutableMapOf<String, Int>()
    private val attributes = mutableMapOf<String, Int>()

    /**
     * Initialize shader with custom code
     */
    fun init(vertexShaderCode: String, fragmentShaderCode: String): Boolean {
        program = createProgram(vertexShaderCode, fragmentShaderCode)
        return program != 0
    }

    /**
     * Initialize shader with default shaders
     */
    fun initDefault(): Boolean {
        return init(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER)
    }

    /**
     * Use this shader program
     */
    fun use() {
        if (program != 0) {
            GLES20.glUseProgram(program)
        }
    }

    /**
     * Get attribute location
     */
    fun getAttribLocation(name: String): Int {
        if (!attributes.containsKey(name)) {
            val location = GLES20.glGetAttribLocation(program, name)
            if (location >= 0) {
                attributes[name] = location
            } else {
                Log.w(TAG, "Attribute $name not found in shader")
            }
            return location
        }
        return attributes[name] ?: -1
    }

    /**
     * Get uniform location
     */
    fun getUniformLocation(name: String): Int {
        if (!uniforms.containsKey(name)) {
            val location = GLES20.glGetUniformLocation(program, name)
            if (location >= 0) {
                uniforms[name] = location
            } else {
                Log.w(TAG, "Uniform $name not found in shader")
            }
            return location
        }
        return uniforms[name] ?: -1
    }

    /**
     * Set uniform integer value
     */
    fun setUniform(name: String, value: Int) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES20.glUniform1i(location, value)
        }
    }

    /**
     * Set uniform float value
     */
    fun setUniform(name: String, value: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES20.glUniform1f(location, value)
        }
    }

    /**
     * Set uniform vec2 value
     */
    fun setUniform(name: String, x: Float, y: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES20.glUniform2f(location, x, y)
        }
    }

    /**
     * Set uniform vec3 value
     */
    fun setUniform(name: String, x: Float, y: Float, z: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES20.glUniform3f(location, x, y, z)
        }
    }

    /**
     * Set uniform vec4 value
     */
    fun setUniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES20.glUniform4f(location, x, y, z, w)
        }
    }

    /**
     * Release shader resources
     */
    fun release() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
            uniforms.clear()
            attributes.clear()
            Log.d(TAG, "Shader released")
        }
    }

    /**
     * Check if shader is valid
     */
    fun isValid(): Boolean = program != 0
}
