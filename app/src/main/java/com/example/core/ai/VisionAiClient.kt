package com.example.core.ai

import android.content.Context

/**
 * Singleton provider for VisionAiService.
 * Enables switching between AI providers or mocks without changing UI layer code.
 */
object VisionAiClient {
    private var serviceInstance: VisionAiService? = null

    fun getService(context: Context? = null): VisionAiService {
        return serviceInstance ?: synchronized(this) {
            serviceInstance ?: GeminiVisionAiService(context?.applicationContext).also {
                serviceInstance = it
            }
        }
    }

    /**
     * Allows injecting a custom or mock service for testing.
     */
    fun setServiceForTesting(service: VisionAiService) {
        serviceInstance = service
    }
}
