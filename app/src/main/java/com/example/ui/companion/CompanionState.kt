package com.example.ui.companion

/**
 * Expressive states for ScreenBuddy cute warm yellow companion.
 * Matching the exact design specification sheet:
 * - IDLE (blink & float)
 * - LOOKING (scanning screen)
 * - THINKING (processing with thought bubble)
 * - HAPPY (answer ready, star spark in eyes)
 * - SURPRISED (wow! wide eyes)
 * - CONFUSED (not sure with question mark)
 * - LIVE_VIEW (active with red indicator dot)
 * - SLEEPY / SLEEPING (resting with Zzz)
 * - ERROR (something wrong with sweat drop)
 * - EXCITED / LISTENING (bonus interactive states)
 */
enum class CompanionState {
    IDLE,       // Gentle floating & blinking, round expressive eyes
    LOOKING,    // Scanning screen, downward focused gaze
    READING,    // Attentive reading of conversation / text on screen
    THINKING,   // Processing AI request, upward gaze with thought bubble
    SUGGESTING, // Preparing & delivering smart reply suggestions
    HAPPY,      // Answer ready, upward sparkle/star in eyes, warm glow
    SURPRISED,  // Wide wow! eyes
    CONFUSED,   // Puzzled glance with floating question mark (?)
    LIVE_VIEW,  // Active real-time stream with floating red indicator dot
    SLEEPY,     // Closed resting curved eyes (˘ ˘) with floating z Z z
    SLEEPING,   // Alias for Sleepy
    ERROR,      // Empathetic worried gaze with blue sweat drop
    EXCITED,    // Star sparkles and radiant pulse
    LISTENING   // Attentive listening gaze
}

