const express = require('express');
const cors = require('cors');
const crypto = require('crypto');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 8080;

// Gemini Model configuration (configurable via environment)
const GEMINI_MODEL = process.env.GEMINI_MODEL || 'gemini-1.5-flash';

// ScreenBuddy Level 10 System Instruction (Real-Time Conversation Assistant)
const SCREENBUDDY_SYSTEM_INSTRUCTION = `You are ScreenBuddy, a friendly, highly intelligent visual AI companion for Android.

You assist the user by visually inspecting their screen capture and providing helpful, structured, actionable assistance.

CORE PRINCIPLES:
1. Grounding: Analyze and reason ONLY about information that is actually visible in the provided image.
2. Honesty: If information is blurry, cropped, incomplete, or not visible, clearly state that. Do not invent missing facts or context.
3. Privacy: Do not unnecessarily extract, emphasize, or log sensitive personal data (such as passwords, full credit card numbers, OTPs).
4. Agency: Never claim you clicked a button, entered text, or took an action on the device. ScreenBuddy provides guidance; the user remains in control.
5. Tone: Friendly, concise, supportive, and clear. Avoid robotic fluff.

CONVERSATION ASSISTANT CAPABILITIES (LEVEL 10):
- When the screen displays a chat, direct message, or messaging app (such as WhatsApp, Instagram, Telegram, Messages, Messenger, Slack, Teams, SMS, Discord, etc.):
  1. Classify "screenType" as "CONVERSATION".
  2. Detect the app name and/or conversation partner if visible (e.g. "WhatsApp - Alex", "Instagram DM") for "screenTitle".
  3. Provide a brief 1-2 sentence "summary" of the recent message exchange.
  4. Provide a clear "mainInsight" explaining what the other person is asking or saying and the recommended response strategy.
  5. In "suggestedReplies", generate 3 to 5 realistic, natural, human-sounding reply suggestions across appropriate tones:
     - "Friendly": Warm, helpful, encouraging.
     - "Casual": Relaxed, natural, modern (subtle emojis if appropriate).
     - "Funny": Witty, lighthearted, playful.
     - "Professional": Clear, polite, respectful, business-appropriate.
     - "Short": Concise, direct, quick 1-sentence reply.
     Only include tones that fit the conversational context. Ensure replies sound natural and human, never robotic.
  6. In "suggestedActions", provide follow-up refinement chips:
     - {"label": "Make it more casual", "actionQuery": "Make the suggested replies more casual and relaxed."}
     - {"label": "Make it shorter", "actionQuery": "Make the suggested replies shorter and more concise."}
     - {"label": "Make it funnier", "actionQuery": "Make the suggested replies funnier with witty humor."}
     - {"label": "Make it more polite", "actionQuery": "Make the suggested replies more polite and respectful."}
     - {"label": "Give me another option", "actionQuery": "Suggest 3 different fresh reply options for this conversation."}

NON-CONVERSATION SCREENS:
- If the screen is NOT a conversation (e.g. WEBPAGE, FORM, CODE_OR_ERROR, FEED, DOCUMENT, SETTINGS, MEDIA):
  1. Classify accurately.
  2. Set "suggestedReplies" to an empty array [].
  3. Provide appropriate suggested actions such as Explain, Summarize, Translate, Troubleshoot, or Step-by-step fix.

OUTPUT FORMAT (MANDATORY JSON):
You MUST respond with a single valid JSON object with the following structure:
{
  "screenType": "CONVERSATION" | "WEBPAGE" | "FORM" | "CODE_OR_ERROR" | "FEED" | "DOCUMENT" | "SETTINGS" | "MEDIA" | "UNKNOWN",
  "screenTitle": "Short contextual screen title or app name (or null if unknown)",
  "summary": "1-2 sentence direct takeaway of what is visible",
  "mainInsight": "Core answer, explanation, or resolution directly addressing the user's question and intent",
  "suggestedReplies": [
    {
      "tone": "Friendly" | "Casual" | "Funny" | "Professional" | "Short",
      "text": "Actual suggested message text"
    }
  ],
  "suggestedActions": [
    {
      "label": "Short action label",
      "actionQuery": "Exact follow-up question ScreenBuddy should analyze if user taps this action"
    }
  ],
  "confidence": 0.95
}

RULES:
- Return ONLY the JSON object. Do NOT include markdown code blocks or extra text before/after.`;

// Enable CORS for mobile and web clients
app.use(cors());

// Configure JSON body parser with strict size limit (15MB for high-res screenshots)
app.use(express.json({ limit: '15mb' }));

// In-Memory rate limiting & request throttling
const requestCounts = new Map();
const RATE_LIMIT_WINDOW_MS = 60 * 1000;
const MAX_REQUESTS_PER_WINDOW = 60;

// Global Gemini Rate-Limit Cooldown Tracker
let geminiRateLimitUntil = 0;

// In-flight request deduplication & response caching (RAM only, 15 seconds TTL)
const inFlightRequests = new Map(); // key -> Promise
const responseCache = new Map();   // key -> { data, expiresAt }
const CACHE_TTL_MS = 15 * 1000;

app.use((req, res, next) => {
    const ip = req.ip || req.socket.remoteAddress || 'unknown';
    const now = Date.now();
    const windowStart = now - RATE_LIMIT_WINDOW_MS;

    const timestamps = (requestCounts.get(ip) || []).filter(t => t > windowStart);
    if (timestamps.length >= MAX_REQUESTS_PER_WINDOW) {
        return res.status(429).json({
            success: false,
            errorType: "RATE_LIMITED",
            error: "Too many requests. Please slow down and try again.",
            message: "Too many requests. Please slow down and try again.",
            retryAfterSeconds: 10
        });
    }

    timestamps.push(now);
    requestCounts.set(ip, timestamps);

    // Periodically clean up old entries
    if (requestCounts.size > 500) {
        for (const [key, list] of requestCounts.entries()) {
            if (list.every(t => t <= windowStart)) {
                requestCounts.delete(key);
            }
        }
    }

    if (responseCache.size > 200) {
        for (const [k, v] of responseCache.entries()) {
            if (v.expiresAt <= now) {
                responseCache.delete(k);
            }
        }
    }

    next();
});

/**
 * Health Check Endpoint
 * GET /health
 */
app.get('/health', (req, res) => {
    console.log('[AI-CONNECTION] HEALTH_CHECK received');
    res.status(200).json({
        status: 'ok',
        service: 'ScreenBuddy Vision AI Backend',
        version: '3.1.0',
        level: 'Level 10 - Real-Time Conversation Assistant',
        configuredModel: GEMINI_MODEL,
        rateLimited: Date.now() < geminiRateLimitUntil
    });
});

/**
 * Root Info Endpoint
 * GET /
 */
app.get('/', (req, res) => {
    res.status(200).json({
        service: "ScreenBuddy Vision AI Backend",
        version: "3.1.0",
        level: "Level 10",
        endpoints: {
            health: "GET /health",
            analyzeScreen: "POST /analyze-screen"
        }
    });
});

/**
 * Helper: Detect and validate MIME type from image base64 data and headers
 */
function detectMimeType(imageString) {
    if (typeof imageString !== 'string') return null;

    const dataUriMatch = imageString.match(/^data:(image\/(jpeg|jpg|png|webp));base64,/i);
    if (dataUriMatch) {
        const type = dataUriMatch[1].toLowerCase();
        return type === 'image/jpg' ? 'image/jpeg' : type;
    }

    const cleanPrefix = imageString.substring(0, 32);
    if (cleanPrefix.startsWith('/9j/')) {
        return 'image/jpeg';
    } else if (cleanPrefix.startsWith('iVBORw0KGgo')) {
        return 'image/png';
    } else if (cleanPrefix.startsWith('UklGR')) {
        return 'image/webp';
    }

    return 'image/jpeg';
}

/**
 * Helper: Clean and extract raw base64 string
 */
function extractRawBase64(imageString) {
    return imageString.replace(/^data:image\/\w+;base64,/i, '').replace(/\s+/g, '');
}

/**
 * Helper: Generate deterministic cache key from image + prompt parameters
 */
function generateRequestKey(rawBase64, question, intent, recentContext) {
    const hash = crypto.createHash('sha256');
    // Sample head, middle, and tail of base64 + length to hash efficiently without full buffer clone
    const len = rawBase64.length;
    const sample = `${len}:${rawBase64.substring(0, 200)}:${rawBase64.substring(Math.floor(len / 2), Math.floor(len / 2) + 200)}:${rawBase64.substring(len - 200)}`;
    hash.update(sample);
    hash.update(`|${question || ''}|${intent || ''}|${recentContext || ''}`);
    return hash.digest('hex');
}

/**
 * Helper: Build intent-specific prompt text
 */
function buildPromptText(userQuestion, intent, recentContext, textContext) {
    const parts = [];

    if (textContext && typeof textContext === 'string' && textContext.trim().length > 0) {
        parts.push(`EXTRACTED SCREEN TEXT / CONTEXT:\n"${textContext.trim()}"\n`);
    }

    if (recentContext && typeof recentContext === 'string' && recentContext.trim().length > 0) {
        parts.push(`PREVIOUS CONVERSATION CONTEXT (In-Memory):\n"${recentContext.trim()}"\n`);
    }

    if (intent && typeof intent === 'string') {
        const normalizedIntent = intent.trim().toUpperCase();
        switch (normalizedIntent) {
            case 'REPLY':
                parts.push("TASK INTENT: Real-Time Conversation Assistant. Analyze the visible chat/message thread on screen or provided text context. Identify the latest incoming message, sender, context, and tone. Provide 3-5 distinct, natural reply suggestions across appropriate tones (Friendly, Casual, Funny, Professional, Short). Provide follow-up refinement action chips (Make it more casual, Make it shorter, Make it funnier, Make it more polite, Give me another option).");
                break;
            case 'EXPLAIN':
                parts.push("TASK INTENT: Explain what is shown on this screen or text context in simple, clear, easy-to-understand terms.");
                break;
            case 'SUMMARIZE':
                parts.push("TASK INTENT: Summarize the important visible information into concise, high-value key takeaways.");
                break;
            case 'TRANSLATE':
                parts.push("TASK INTENT: Identify non-English or foreign text on this screen and translate it accurately into clear English.");
                break;
            case 'TROUBLESHOOT':
                parts.push("TASK INTENT: Diagnose the visible error, crash log, or issue and provide step-by-step troubleshooting advice.");
                break;
            case 'LEARN':
                parts.push("TASK INTENT: Provide an educational breakdown and core concepts shown on this screen.");
                break;
            case 'COMPARE':
                parts.push("TASK INTENT: Compare the visible options, items, or prices and highlight pros/cons.");
                break;
            case 'EXTRACT':
                parts.push("TASK INTENT: Extract relevant data, key values, dates, or contact details accurately.");
                break;
            default:
                parts.push(`TASK INTENT: ${normalizedIntent}`);
                break;
        }
    }

    parts.push(`USER QUESTION / REQUEST:\n${userQuestion}`);

    return parts.join("\n\n");
}

/**
 * Helper: Safely parse structured JSON from Gemini response text
 */
function parseGeminiStructuredResponse(rawText) {
    if (!rawText || typeof rawText !== 'string') {
        return createFallbackStructuredResponse("I couldn't analyze the screen content.");
    }

    const trimmed = rawText.trim();

    // 1. Try direct parse
    try {
        const directObj = JSON.parse(trimmed);
        if (directObj && typeof directObj === 'object') {
            return sanitizeStructuredObject(directObj);
        }
    } catch (_) {
        // Continue to extraction techniques
    }

    // 2. Try markdown code block extraction (```json ... ``` or ``` ... ```)
    const codeBlockMatch = trimmed.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
    if (codeBlockMatch && codeBlockMatch[1]) {
        try {
            const blockObj = JSON.parse(codeBlockMatch[1].trim());
            if (blockObj && typeof blockObj === 'object') {
                return sanitizeStructuredObject(blockObj);
            }
        } catch (_) {
            // Continue
        }
    }

    // 3. Try finding first '{' and last '}'
    const firstBrace = trimmed.indexOf('{');
    const lastBrace = trimmed.lastIndexOf('}');
    if (firstBrace !== -1 && lastBrace > firstBrace) {
        try {
            const substringJson = trimmed.substring(firstBrace, lastBrace + 1);
            const subObj = JSON.parse(substringJson);
            if (subObj && typeof subObj === 'object') {
                return sanitizeStructuredObject(subObj);
            }
        } catch (_) {
            // Continue
        }
    }

    // 4. Graceful fallback for non-JSON or plain-text response
    return createFallbackStructuredResponse(trimmed);
}

/**
 * Helper: Sanitize and guarantee valid fields on structured response object
 */
function sanitizeStructuredObject(obj) {
    const validScreenTypes = [
        "CONVERSATION", "WEBPAGE", "FORM", "CODE_OR_ERROR", 
        "FEED", "DOCUMENT", "SETTINGS", "MEDIA", "UNKNOWN"
    ];

    let screenType = typeof obj.screenType === 'string' ? obj.screenType.trim().toUpperCase() : "UNKNOWN";
    if (!validScreenTypes.includes(screenType)) {
        screenType = "UNKNOWN";
    }

    const screenTitle = typeof obj.screenTitle === 'string' && obj.screenTitle.trim().length > 0
        ? obj.screenTitle.trim()
        : null;

    const summary = typeof obj.summary === 'string' ? obj.summary.trim() : "";
    const mainInsight = typeof obj.mainInsight === 'string' && obj.mainInsight.trim().length > 0
        ? obj.mainInsight.trim()
        : (summary || "Analysis complete.");

    // Sanitize suggested replies
    const validTones = ["Friendly", "Casual", "Funny", "Professional", "Short", "Natural"];
    const suggestedReplies = [];
    if (Array.isArray(obj.replies) && screenType === "CONVERSATION") {
        for (const item of obj.replies) {
            if (typeof item === 'string' && item.trim().length > 0) {
                suggestedReplies.push({ tone: "Natural", text: item.trim() });
            }
        }
    }
    if (Array.isArray(obj.suggestedReplies) && screenType === "CONVERSATION") {
        for (const item of obj.suggestedReplies) {
            if (item && typeof item === 'object') {
                let tone = typeof item.tone === 'string' ? item.tone.trim() : "Friendly";
                const matchedTone = validTones.find(t => t.toLowerCase() === tone.toLowerCase()) || tone;
                const text = typeof item.text === 'string' ? item.text.trim() : "";
                if (text.length > 0) {
                    suggestedReplies.push({ tone: matchedTone, text });
                }
            } else if (typeof item === 'string' && item.trim().length > 0) {
                suggestedReplies.push({ tone: "Friendly", text: item.trim() });
            }
        }
    }

    // Sanitize suggested actions
    const suggestedActions = [];
    if (Array.isArray(obj.suggestedActions)) {
        for (const item of obj.suggestedActions) {
            if (item && typeof item === 'object') {
                const label = typeof item.label === 'string' ? item.label.trim() : "";
                const actionQuery = typeof item.actionQuery === 'string' ? item.actionQuery.trim() : label;
                if (label.length > 0) {
                    suggestedActions.push({ label, actionQuery });
                }
            } else if (typeof item === 'string' && item.trim().length > 0) {
                suggestedActions.push({ label: item.trim(), actionQuery: item.trim() });
            }
        }
    }

    // Supply helpful defaults based on screenType if none provided
    if (suggestedActions.length === 0) {
        if (screenType === "CONVERSATION") {
            suggestedActions.push(
                { label: "Make it more casual", actionQuery: "Make the suggested replies more casual and relaxed." },
                { label: "Make it shorter", actionQuery: "Make the suggested replies shorter and more concise." },
                { label: "Make it funnier", actionQuery: "Make the suggested replies funnier with witty humor." },
                { label: "Make it more polite", actionQuery: "Make the suggested replies more polite and respectful." },
                { label: "Give me another option", actionQuery: "Suggest 3 different fresh reply options for this conversation." }
            );
        } else if (screenType === "FORM") {
            suggestedActions.push(
                { label: "Explain required fields", actionQuery: "Explain what each required field on this form means." },
                { label: "Check for errors", actionQuery: "Are there any visible validation errors on this form?" }
            );
        } else if (screenType === "CODE_OR_ERROR") {
            suggestedActions.push(
                { label: "Step-by-step fix", actionQuery: "Give me step-by-step instructions to fix this error." },
                { label: "Explain root cause", actionQuery: "What caused this error and how can I prevent it?" }
            );
        } else {
            suggestedActions.push(
                { label: "Explain more", actionQuery: "Explain what is shown on this screen in more detail." },
                { label: "Summarize", actionQuery: "Summarize the key points of this screen in bullet points." },
                { label: "Translate", actionQuery: "Identify important text and translate it into English." }
            );
        }
    }

    const rawConfidence = typeof obj.confidence === 'number' ? obj.confidence : 0.90;
    const confidence = Math.max(0.0, Math.min(1.0, rawConfidence));

    return {
        screenType,
        screenTitle,
        summary,
        mainInsight,
        suggestedReplies,
        suggestedActions,
        confidence
    };
}

/**
 * Helper: Create clean fallback object when AI output is unstructured
 */
function createFallbackStructuredResponse(plainText) {
    const isReplyLike = plainText.toLowerCase().includes("reply:") || plainText.toLowerCase().includes("option 1");
    const screenType = isReplyLike ? "CONVERSATION" : "UNKNOWN";

    return {
        screenType,
        screenTitle: null,
        summary: "",
        mainInsight: plainText,
        suggestedReplies: [],
        suggestedActions: [
            { label: "Explain more", actionQuery: "Explain what is shown on this screen in more detail." },
            { label: "Summarize", actionQuery: "Summarize the key points of this screen in bullet points." }
        ],
        confidence: 0.85
    };
}

/**
 * Core Gemini Model Invocation with cascading model fallback
 */
async function callGeminiVisionApi(apiKey, geminiPayload) {
    const candidateModels = [
        GEMINI_MODEL,
        'gemini-1.5-flash',
        'gemini-2.0-flash',
        'gemini-1.5-flash-8b',
        'gemini-1.5-pro'
    ].filter((m, i, arr) => m && arr.indexOf(m) === i);

    let lastError = null;

    for (const modelCandidate of candidateModels) {
        const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(modelCandidate)}:generateContent?key=${apiKey}`;
        const abortController = new AbortController();
        const timeoutId = setTimeout(() => abortController.abort(), 25000);

        console.log(`[AI-CONNECTION] GEMINI_REQUEST_START model=${modelCandidate}`);

        try {
            const response = await fetch(geminiUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(geminiPayload),
                signal: abortController.signal
            });

            clearTimeout(timeoutId);

            console.log(`[AI-CONNECTION] GEMINI_RESPONSE status=${response.status} model=${modelCandidate}`);

            if (response.ok) {
                const data = await response.json();
                const answerText = data.candidates?.[0]?.content?.parts?.[0]?.text;
                if (answerText && answerText.trim().length > 0) {
                    return { success: true, rawText: answerText.trim() };
                } else {
                    console.warn(`[AI-CONNECTION] PARSE_ERROR empty candidate parts from ${modelCandidate}`);
                }
            } else {
                const status = response.status;
                const errBody = await response.text();
                lastError = { status, body: errBody };

                if (status === 429) {
                    console.warn(`[AI-CONNECTION] REQUEST_FAILED reason=RATE_LIMITED model=${modelCandidate}`);
                    continue;
                } else if (status === 404 || status === 400) {
                    console.warn(`[AI-CONNECTION] REQUEST_FAILED reason=HTTP_${status} model=${modelCandidate}, checking next...`);
                    continue;
                } else if (status === 401 || status === 403) {
                    console.error(`[AI-CONNECTION] REQUEST_FAILED reason=AUTH_ERROR model=${modelCandidate}`);
                    return { success: false, status, errorType: "CONFIG_ERROR", message: "AI service configuration error." };
                }
            }
        } catch (fetchErr) {
            clearTimeout(timeoutId);
            const isTimeout = fetchErr.name === 'AbortError';
            if (isTimeout) {
                console.warn(`[AI-CONNECTION] TIMEOUT model=${modelCandidate}`);
            } else {
                console.warn(`[AI-CONNECTION] REQUEST_FAILED reason=FETCH_ERROR error=${fetchErr.message}`);
            }
            lastError = { status: isTimeout ? 504 : 500, body: fetchErr.message, isTimeout };
        }
    }

    if (lastError?.status === 429) {
        // All models returned 429 -> set 30s global cooldown
        geminiRateLimitUntil = Date.now() + 30000;
        console.warn("[AI-CONNECTION] REQUEST_FAILED reason=ALL_MODELS_RATE_LIMITED. Setting 30s cooldown.");
        return {
            success: false,
            status: 429,
            errorType: "RATE_LIMITED",
            retryAfterSeconds: 30,
            message: "AI service is temporarily busy."
        };
    }

    if (lastError?.isTimeout) {
        return {
            success: false,
            status: 504,
            errorType: "TIMEOUT_ERROR",
            retryAfterSeconds: 5,
            message: "AI took too long to respond."
        };
    }

    return {
        success: false,
        status: lastError?.status || 503,
        errorType: "BACKEND_REACHABLE_GEMINI_FAILED",
        retryAfterSeconds: 5,
        message: "AI service is temporarily busy."
    };
}

/**
 * Screen Analysis Core Handler
 * POST /analyze-screen (and POST /api/analyze-screen)
 */
async function handleAnalyzeScreen(req, res) {
    const startTime = Date.now();
    console.log(`[AI-CONNECTION] REQUEST_START mode=${req.body?.image ? 'VISION' : 'TEXT'}`);

    // 1. Validate JSON request body
    if (!req.body || typeof req.body !== 'object') {
        console.warn('[AI-CONNECTION] REQUEST_FAILED reason=INVALID_REQUEST_BODY');
        return res.status(400).json({
            success: false,
            errorType: "INVALID_REQUEST",
            error: "Invalid request format.",
            message: "Invalid request format."
        });
    }

    const { image, question, intent, recentContext, textContext } = req.body;

    const userQuestion = (question && typeof question === 'string' && question.trim().length > 0)
        ? question.trim()
        : "What is on this screen?";

    const hasImage = image && typeof image === 'string' && image.trim().length > 0;
    const hasTextContext = textContext && typeof textContext === 'string' && textContext.trim().length > 0;

    // 2. Validate payload presence (must have image OR textContext OR user question)
    if (!hasImage && !hasTextContext) {
        console.warn('[AI-CONNECTION] REQUEST_FAILED reason=MISSING_INPUT');
        return res.status(400).json({
            success: false,
            errorType: "MISSING_INPUT",
            error: "Please provide either screen context or a screen capture.",
            message: "Please provide either screen context or a screen capture."
        });
    }

    let mimeType = null;
    let rawBase64 = "";

    if (hasImage) {
        mimeType = detectMimeType(image);
        rawBase64 = extractRawBase64(image);

        if (!mimeType || rawBase64.length < 16) {
            console.warn('[AI-CONNECTION] REQUEST_FAILED reason=INVALID_IMAGE');
            return res.status(400).json({
                success: false,
                errorType: "INVALID_IMAGE",
                error: "I can't see the screen yet — try again.",
                message: "I can't see the screen yet — try again."
            });
        }

        try {
            const buffer = Buffer.from(rawBase64, 'base64');
            if (buffer.length < 50) {
                console.warn('[AI-CONNECTION] REQUEST_FAILED reason=IMAGE_BUFFER_TOO_SMALL');
                return res.status(400).json({
                    success: false,
                    errorType: "INVALID_IMAGE",
                    error: "I can't see the screen yet — try again.",
                    message: "I can't see the screen yet — try again."
                });
            }
        } catch {
            console.warn('[AI-CONNECTION] REQUEST_FAILED reason=IMAGE_DECODE_ERROR');
            return res.status(400).json({
                success: false,
                errorType: "INVALID_IMAGE",
                error: "I can't see the screen yet — try again.",
                message: "I can't see the screen yet — try again."
            });
        }
    }

    // 5. Check Server Gemini API Key configuration
    const apiKey = process.env.GEMINI_API_KEY || process.env.AI_API_KEY;
    if (!apiKey || apiKey === 'YOUR_GEMINI_API_KEY_HERE') {
        console.error('[AI-CONNECTION] REQUEST_FAILED reason=CONFIG_ERROR');
        return res.status(503).json({
            success: false,
            errorType: "CONFIG_ERROR",
            error: "AI service configuration error.",
            message: "AI service configuration error."
        });
    }

    // 6. Active Gemini Rate-Limit Cooldown Check
    const now = Date.now();
    if (now < geminiRateLimitUntil) {
        const remainingSeconds = Math.max(1, Math.ceil((geminiRateLimitUntil - now) / 1000));
        console.log(`[AI] Cooldown active. Rejecting request (${remainingSeconds}s remaining).`);
        return res.status(429).json({
            success: false,
            errorType: "RATE_LIMITED",
            error: "AI is taking a tiny break ☕ Please try again shortly.",
            message: "AI is taking a tiny break ☕ Please try again shortly.",
            retryAfterSeconds: remainingSeconds
        });
    }

    // 7. Request Deduplication & Response Caching (15s TTL)
    const requestKey = generateRequestKey(rawBase64, userQuestion, intent, recentContext);

    const cached = responseCache.get(requestKey);
    if (cached && cached.expiresAt > now) {
        console.log(`[AI] Serving duplicate request from in-memory cache. Latency: ${Date.now() - startTime}ms`);
        return res.status(200).json({
            ...cached.data,
            cached: true
        });
    }

    // 8. In-Flight Request Deduplication
    if (inFlightRequests.has(requestKey)) {
        console.log(`[AI] Awaiting existing in-flight analysis for duplicate frame.`);
        try {
            const result = await inFlightRequests.get(requestKey);
            if (result.success) {
                return res.status(200).json(result.data);
            } else {
                return res.status(result.status || 500).json(result.errorPayload);
            }
        } catch (e) {
            // In-flight promise failed, proceed with fresh execution
        }
    }

    // 9. Build Gemini Request
    const fullPromptText = buildPromptText(userQuestion, intent, recentContext, textContext);
    const userParts = [{ text: fullPromptText }];
    if (hasImage && mimeType && rawBase64) {
        userParts.push({ inlineData: { mimeType: mimeType, data: rawBase64 } });
    }

    const geminiPayload = {
        contents: [
            {
                role: "user",
                parts: userParts
            }
        ],
        systemInstruction: {
            parts: [{ text: SCREENBUDDY_SYSTEM_INSTRUCTION }]
        },
        generationConfig: {
            temperature: 0.2,
            topP: 0.95,
            maxOutputTokens: 1536,
            responseMimeType: "application/json"
        }
    };

    console.log(`[AI] Starting analysis. Intent: ${intent || 'NONE'}, Question length: ${userQuestion.length}`);

    // Create In-Flight promise
    const executionPromise = (async () => {
        const result = await callGeminiVisionApi(apiKey, geminiPayload);
        if (result.success) {
            const structured = parseGeminiStructuredResponse(result.rawText);
            const responseData = {
                success: true,
                answer: structured.mainInsight,
                screenType: structured.screenType,
                screenTitle: structured.screenTitle,
                summary: structured.summary,
                mainInsight: structured.mainInsight,
                suggestedReplies: structured.suggestedReplies,
                suggestedActions: structured.suggestedActions,
                confidence: structured.confidence
            };
            // Cache successful result
            responseCache.set(requestKey, {
                data: responseData,
                expiresAt: Date.now() + CACHE_TTL_MS
            });
            return { success: true, data: responseData };
        } else {
            return {
                success: false,
                status: result.status,
                errorPayload: {
                    success: false,
                    errorType: result.errorType || "GENERIC_ERROR",
                    error: result.message,
                    message: result.message,
                    retryAfterSeconds: result.retryAfterSeconds || 0
                }
            };
        }
    })();

    inFlightRequests.set(requestKey, executionPromise);

    try {
        const executionResult = await executionPromise;
        const duration = Date.now() - startTime;
        if (executionResult.success) {
            console.log(`[AI-CONNECTION] RESPONSE_RECEIVED duration=${duration}ms screenType=${executionResult.data.screenType || 'UNKNOWN'}`);
            return res.status(200).json(executionResult.data);
        } else {
            console.warn(`[AI-CONNECTION] REQUEST_FAILED status=${executionResult.status} errorType=${executionResult.errorPayload.errorType}`);
            return res.status(executionResult.status || 500).json(executionResult.errorPayload);
        }
    } finally {
        inFlightRequests.delete(requestKey);
    }
}

// Register primary and alias routes
app.post('/analyze-screen', handleAnalyzeScreen);
app.post('/api/analyze-screen', handleAnalyzeScreen);

// Catch-all 404 handler
app.use((req, res) => {
    res.status(404).json({
        success: false,
        errorType: "NOT_FOUND",
        error: "Endpoint not found.",
        message: "Endpoint not found."
    });
});

// Global error handler
app.use((err, req, res, next) => {
    if (err.type === 'entity.too.large') {
        return res.status(413).json({
            success: false,
            errorType: "PAYLOAD_TOO_LARGE",
            error: "Captured image is too large. Please retry with a compressed image.",
            message: "Captured image is too large. Please retry with a compressed image."
        });
    }
    return res.status(500).json({
        success: false,
        errorType: "INTERNAL_ERROR",
        error: "An unexpected server error occurred.",
        message: "An unexpected server error occurred."
    });
});

// Start listening when executed directly
if (require.main === module) {
    app.listen(PORT, () => {
        console.log(`[ScreenBuddy Backend] Level 10 Structured Vision AI Backend running on port ${PORT}`);
    });
}

// Attach test helper functions to exported app
app.parseGeminiStructuredResponse = parseGeminiStructuredResponse;
app.sanitizeStructuredObject = sanitizeStructuredObject;
app.buildPromptText = buildPromptText;

module.exports = app;
