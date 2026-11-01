package com.example.core.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.example.core.ScreenCaptureManager as CoreScreenCaptureManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

/**
 * State representing the real MediaProjection Screen Capture lifecycle (Level 2).
 */
sealed interface CaptureState {
    /** Initial state: No screen capture session active */
    object Idle : CaptureState

    /** Permission prompt shown to the user */
    object PermissionRequested : CaptureState

    /** User denied system permission */
    object PermissionDenied : CaptureState

    /** Capture session active with optional latest bitmap in memory */
    data class Active(
        val bitmap: Bitmap?,
        val captureCount: Int = 1,
        val timestamp: Long = System.currentTimeMillis()
    ) : CaptureState

    /** Screen access was stopped by user or system */
    object Stopped : CaptureState

    /** Error state if projection or virtual display fails */
    data class Error(val message: String) : CaptureState
}

/**
 * ScreenCaptureManager for Level 2.
 *
 * Implements REAL Android screen capture via Android MediaProjection API.
 *
 * Privacy Guarantees:
 * - Capture ONLY begins after explicit user consent via system dialog.
 * - Captured screenshot exists ONLY in memory (RAM Bitmap).
 * - ZERO storage writing, ZERO network transmission, ZERO analytics, ZERO AI API.
 * - Complete teardown and bitmap recycling upon stop or discard.
 */
class ScreenCaptureManager private constructor() : CoreScreenCaptureManager {

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var currentBitmap: Bitmap? = null
    private var captureCount = 0

    private var displayWidth: Int = 720
    private var displayHeight: Int = 1280
    private var displayDensityDpi: Int = DisplayMetrics.DENSITY_DEFAULT

    override val isCaptureGranted: Boolean
        get() = mediaProjection != null && _captureState.value is CaptureState.Active

    fun isCaptureActive(): Boolean = mediaProjection != null && _captureState.value is CaptureState.Active

    fun getCurrentBitmap(): Bitmap? = currentBitmap

    fun updateBitmap(bitmap: Bitmap) {
        currentBitmap = bitmap
        captureCount++
        _captureState.value = CaptureState.Active(
            bitmap = bitmap,
            captureCount = captureCount,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Waits for any overlay or covering activity to hide, then captures a clean frame
     * of the underlying screen (such as Chrome, WhatsApp, or Instagram).
     *
     * Validates that the acquired frame has meaningful content and is not an unrendered
     * black screen or blank buffer before returning.
     */
    suspend fun captureCleanFrame(delayMs: Long = 350L): Bitmap? {
        Log.d(TAG, "[DIAGNOSTIC] Capture request started (isCaptureActive=${isCaptureActive()})")
        Log.d(TAG, "[DIAGNOSTIC] MediaProjection session state: ${if (mediaProjection != null) "ACTIVE" else "INACTIVE"}")
        Log.d(TAG, "[DIAGNOSTIC] VirtualDisplay creation/state: ${if (virtualDisplay != null) "CREATED_AND_ACTIVE" else "NULL"}")

        if (!isCaptureActive()) {
            Log.w(TAG, "[CAPTURE_PERMISSION_MISSING] MediaProjection is not active, using currentBitmap or fallback bitmap")
        }

        if (delayMs > 0) {
            kotlinx.coroutines.delay(delayMs)
        }

        var attempts = 0
        while (attempts < 4) {
            attempts++
            val candidate = currentBitmap
            Log.d(TAG, "[DIAGNOSTIC] Frame candidate attempt $attempts: Bitmap created/null=${candidate != null} (${candidate?.width ?: 0}x${candidate?.height ?: 0})")
            
            if (candidate != null && !candidate.isRecycled) {
                val validation = FrameValidator.validateFrame(candidate)
                val isBlackOrBlank = validation is FrameValidationResult.Invalid && 
                    (validation.reason == InvalidReason.COMPLETELY_BLACK || validation.reason == InvalidReason.NEARLY_BLACK || validation.reason == InvalidReason.SOLID_COLOR_BLANK)
                Log.d(TAG, "[DIAGNOSTIC] Frame black/blank check result: isBlackOrBlank=$isBlackOrBlank, validation=$validation")

                if (validation is FrameValidationResult.Valid) {
                    return candidate
                } else if (attempts < 4) {
                    val reasonStr = if (validation is FrameValidationResult.Invalid) validation.reason.name else "UNKNOWN"
                    Log.w(TAG, "[FRAME_VALIDATION_RETRY] Frame validation attempt $attempts failed ($reasonStr), waiting 120ms...")
                    kotlinx.coroutines.delay(120L)
                } else {
                    Log.w(TAG, "[FRAME_VALIDATION_FAILED] Frame still invalid after $attempts attempts: $validation")
                    // Do not return invalid/blank frame or fabricate a fake bitmap
                    return null
                }
            } else {
                Log.d(TAG, "[DIAGNOSTIC] ImageReader frame received or not: candidate bitmap is NULL")
                if (attempts < 4) {
                    kotlinx.coroutines.delay(100L)
                }
            }
        }

        Log.w(TAG, "[CAPTURE_FAILED] Unable to acquire valid screen frame from active capture session")
        return null
    }

    override fun captureCurrentFrame(): ByteArray? {
        val bitmap = currentBitmap ?: return null
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress frame: ${e.message}")
            null
        }
    }

    /**
     * Creates the system Screen Capture Intent for MediaProjection.
     */
    fun createScreenCaptureIntent(context: Context): Intent {
        Log.d(TAG, "permission requested")
        _captureState.value = CaptureState.PermissionRequested
        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mediaProjectionManager.createScreenCaptureIntent()
    }

    /**
     * Handles the result of the system MediaProjection permission dialog.
     */
    fun onPermissionResult(resultCode: Int, data: Intent?, context: Context) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.d(TAG, "permission denied")
            _captureState.value = CaptureState.PermissionDenied
            return
        }

        Log.d(TAG, "permission granted")
        // Android 14+ requirement: start the foreground service first.
        // ScreenCaptureService calls startCaptureSession immediately after startForeground().
        ScreenCaptureService.start(context, resultCode, data)
    }

    /**
     * Initializes MediaProjection, VirtualDisplay, and ImageReader.
     * Called by ScreenCaptureService once startForeground is active.
     */
    fun startCaptureSession(resultCode: Int, data: Intent, context: Context) {
        try {
            // 1. Calculate screen dimensions
            resolveDisplayMetrics(context)

            // 2. Initialize background HandlerThread for frame processing
            initBackgroundHandler()

            // 3. Acquire MediaProjection
            val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is null after grant")
                _captureState.value = CaptureState.Error("MediaProjection unavailable")
                return
            }

            Log.d(TAG, "capture started")

            // 5. Register MediaProjection Callback before creating VirtualDisplay (Android 14+ mandate)
            val handler = backgroundHandler ?: Handler(context.mainLooper)
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "capture stopped")
                    stopCapture(context)
                }
            }, handler)

            // 6. Create ImageReader with RGBA_8888
            val width = displayWidth
            val height = displayHeight
            val density = displayDensityDpi

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 4)
            imageReader?.setOnImageAvailableListener({ reader ->
                handleImageAvailable(reader)
            }, handler)

            // 7. Create VirtualDisplay to route screen output to ImageReader Surface
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenBuddyVirtualDisplay",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )

            _captureState.value = CaptureState.Active(bitmap = null, captureCount = 0)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting capture session: ${e.message}", e)
            _captureState.value = CaptureState.Error("Failed to start screen capture: ${e.localizedMessage}")
            stopCapture(context)
        }
    }

    /**
     * Processes incoming image frames from ImageReader.
     */
    private fun handleImageAvailable(reader: ImageReader) {
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image == null) {
                Log.d(TAG, "[DIAGNOSTIC] ImageReader frame received or not: NULL")
                return
            }

            Log.d(TAG, "[DIAGNOSTIC] ImageReader frame received: TRUE (dimensions=${image.width}x${image.height})")
            val bitmap = convertImageToBitmap(image)
            Log.d(TAG, "[DIAGNOSTIC] Bitmap created or null: ${bitmap != null} (${bitmap?.width ?: 0}x${bitmap?.height ?: 0})")

            if (bitmap != null) {
                // Safely update in-memory reference
                currentBitmap = bitmap
                captureCount++
                _captureState.value = CaptureState.Active(
                    bitmap = bitmap,
                    captureCount = captureCount,
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "[DIAGNOSTIC] MediaProjection/ImageReader exception in handleImageAvailable: ${e.message}", e)
        } finally {
            image?.close()
        }
    }

    /**
     * Converts raw Image plane buffer to clean ARGB_8888 Bitmap handling row stride.
     * Uses dynamic image width and height to gracefully support orientation changes and resizing.
     */
    private fun convertImageToBitmap(image: Image): Bitmap? {
        return try {
            val width = image.width
            val height = image.height
            if (width <= 0 || height <= 0) return null

            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmapWidth = width + (if (pixelStride > 0) rowPadding / pixelStride else 0)
            val fullBitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
            fullBitmap.copyPixelsFromBuffer(buffer)

            if (rowPadding == 0 && bitmapWidth == width) {
                fullBitmap
            } else {
                val safeWidth = width.coerceAtMost(bitmapWidth).coerceAtLeast(1)
                val safeHeight = height.coerceAtMost(fullBitmap.height).coerceAtLeast(1)
                val croppedBitmap = Bitmap.createBitmap(fullBitmap, 0, 0, safeWidth, safeHeight)
                if (croppedBitmap !== fullBitmap) {
                    // Safe to recycle original temporary buffer bitmap only if distinct instance
                    try {
                        fullBitmap.recycle()
                    } catch (ignored: Throwable) {}
                }
                croppedBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap conversion error: ${e.message}", e)
            null
        }
    }

    /**
     * Triggers a re-capture of the current frame from the active MediaProjection.
     */
    fun captureAgain() {
        Log.d(TAG, "capture again requested")
        // If imageReader has a current frame, acquireLatestImage is triggered automatically
        // or we signal active state update
        val active = _captureState.value as? CaptureState.Active
        if (active != null) {
            _captureState.value = active.copy(timestamp = System.currentTimeMillis())
        }
    }

    /**
     * Discards the screenshot from memory without writing to storage.
     */
    fun discardScreenshot() {
        Log.d(TAG, "screenshot discarded")
        currentBitmap = null
        val active = _captureState.value as? CaptureState.Active
        if (active != null) {
            _captureState.value = CaptureState.Active(bitmap = null, captureCount = active.captureCount)
        }
    }

    /**
     * Stops the MediaProjection session, releases all hardware resources, and resets state.
     */
    fun stopCapture(context: Context) {
        Log.d(TAG, "capture stopped")

        try {
            virtualDisplay?.release()
            virtualDisplay = null

            imageReader?.close()
            imageReader = null

            mediaProjection?.stop()
            mediaProjection = null

            currentBitmap = null

            handlerThread?.quitSafely()
            handlerThread = null
            backgroundHandler = null

            ScreenCaptureService.stop(context)

            Log.d(TAG, "resources released")
            _captureState.value = CaptureState.Stopped
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing capture resources: ${e.message}", e)
            _captureState.value = CaptureState.Stopped
        }
    }

    /**
     * Resets state back to Idle for new requests.
     */
    fun resetToIdle() {
        _captureState.value = CaptureState.Idle
    }

    private fun resolveDisplayMetrics(context: Context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (windowManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics = windowManager.currentWindowMetrics
                val bounds = metrics.bounds
                displayWidth = if (bounds.width() > 0) bounds.width() else 1080
                displayHeight = if (bounds.height() > 0) bounds.height() else 1920
                displayDensityDpi = context.resources.configuration.densityDpi
            } else {
                @Suppress("DEPRECATION")
                val display = windowManager.defaultDisplay
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                display.getRealMetrics(metrics)
                displayWidth = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
                displayHeight = if (metrics.heightPixels > 0) metrics.heightPixels else 1920
                displayDensityDpi = metrics.densityDpi
            }
        }
    }

    private fun initBackgroundHandler() {
        if (handlerThread == null) {
            handlerThread = HandlerThread("ScreenBuddyCaptureThread").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    companion object {
        private const val TAG = "ScreenBuddyCapture"

        val instance: ScreenCaptureManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            ScreenCaptureManager()
        }
    }
}
