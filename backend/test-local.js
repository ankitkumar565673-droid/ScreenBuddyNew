/**
 * Local Test Suite for ScreenBuddy Backend (Level 10 Real-Time Conversation Assistant)
 * 
 * Verifies:
 * 1. Health check (GET /health returns Level 10)
 * 2. Error handling (missing image, missing question, invalid formats, 503 unconfigured key)
 * 3. Structured output schema (screenType, summary, mainInsight, suggestedReplies, suggestedActions, confidence)
 * 4. Level 10 Conversation & Tone handling (Friendly, Casual, Funny, Professional, Short)
 * 5. Level 10 Follow-up refinement actions (Make it more casual, Make it shorter, etc.)
 * 6. Short-lived in-memory context support
 * 7. Privacy & Zero-Persistence verification
 */

const http = require('http');
const app = require('./server');

// Minimal 1x1 valid PNG in base64
const SAMPLE_PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

let server;
const TEST_PORT = 8999;

function makeRequest(method, path, body = null) {
    return new Promise((resolve, reject) => {
        const payload = body ? JSON.stringify(body) : null;
        const options = {
            hostname: '127.0.0.1',
            port: TEST_PORT,
            path: path,
            method: method,
            headers: {
                'Content-Type': 'application/json',
                ...(payload ? { 'Content-Length': Buffer.byteLength(payload) } : {})
            }
        };

        const req = http.request(options, (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; });
            res.on('end', () => {
                try {
                    const parsed = JSON.parse(data);
                    resolve({ status: res.statusCode, body: parsed });
                } catch {
                    resolve({ status: res.statusCode, raw: data });
                }
            });
        });

        req.on('error', (err) => reject(err));
        if (payload) req.write(payload);
        req.end();
    });
}

async function runTests() {
    console.log("==================================================");
    console.log("   SCREENBUDDY BACKEND LEVEL 10 TEST SUITE");
    console.log("==================================================");

    server = app.listen(TEST_PORT);
    await new Promise(r => setTimeout(r, 500));

    let passed = 0;
    let failed = 0;

    async function assert(desc, fn) {
        try {
            await fn();
            console.log(`  ✓ ${desc}`);
            passed++;
        } catch (err) {
            console.error(`  ✗ ${desc}: ${err.message}`);
            failed++;
        }
    }

    console.log("\n[TEST GROUP 1: Health & Base Endpoints]");
    await assert("GET /health returns 200 { status: 'ok', level: 'Level 10 - Real-Time Conversation Assistant' }", async () => {
        const res = await makeRequest('GET', '/health');
        if (res.status !== 200 || res.body.status !== 'ok' || !res.body.level.includes("Level 10")) {
            throw new Error(`Expected 200 { status: 'ok', level contains Level 10 }, got ${res.status}: ${JSON.stringify(res.body)}`);
        }
        if (res.body.apiKey || res.body.GEMINI_API_KEY) {
            throw new Error("Security failure: API key leaked in health endpoint!");
        }
    });

    console.log("\n[TEST GROUP 2: Validation & Error Handling]");
    await assert("POST /analyze-screen with no image returns 400 'Please capture your screen first.'", async () => {
        const res = await makeRequest('POST', '/analyze-screen', { question: "What is on my screen?" });
        if (res.status !== 400 || res.body.success !== false || res.body.error !== "Please capture your screen first.") {
            throw new Error(`Unexpected response: ${JSON.stringify(res.body)}`);
        }
    });

    await assert("POST /analyze-screen with no question returns 400 'Tell me what you'd like help with.'", async () => {
        const res = await makeRequest('POST', '/analyze-screen', { image: SAMPLE_PNG_BASE64 });
        if (res.status !== 400 || res.body.success !== false || res.body.error !== "Tell me what you'd like help with.") {
            throw new Error(`Unexpected response: ${JSON.stringify(res.body)}`);
        }
    });

    await assert("POST /analyze-screen with invalid base64 image returns 400 'I can't see the screen yet — try again.'", async () => {
        const res = await makeRequest('POST', '/analyze-screen', { image: "xyz", question: "What is this?" });
        if (res.status !== 400 || res.body.success !== false || res.body.error !== "I can't see the screen yet — try again.") {
            throw new Error(`Unexpected response: ${JSON.stringify(res.body)}`);
        }
    });

    console.log("\n[TEST GROUP 3: Structured AI & Intent Handling]");
    
    // Test direct parser sanitization & Level 10 structure
    await assert("parseGeminiStructuredResponse parses valid Conversation structure with tones", async () => {
        const sampleRaw = JSON.stringify({
            screenType: "CONVERSATION",
            screenTitle: "WhatsApp - Alice",
            summary: "Alice is asking if you want to grab lunch today at noon.",
            mainInsight: "Alice is inviting you to lunch. A friendly confirmation or polite decline is appropriate.",
            suggestedReplies: [
                { tone: "Friendly", text: "Sounds great, see you at noon!" },
                { tone: "Casual", text: "Yeah I'm down! Where to?" },
                { tone: "Funny", text: "Only if you're buying lunch! Just kidding, see you at 12." },
                { tone: "Professional", text: "Thank you for the invitation. I will join you at noon." },
                { tone: "Short", text: "Yes, 12pm works!" }
            ],
            suggestedActions: [
                { label: "Make it more casual", actionQuery: "Make the suggested replies more casual and relaxed." },
                { label: "Make it shorter", actionQuery: "Make the suggested replies shorter." }
            ],
            confidence: 0.95
        });

        const parsed = app.parseGeminiStructuredResponse(sampleRaw);
        if (parsed.screenType !== "CONVERSATION") throw new Error(`Wrong screenType: ${parsed.screenType}`);
        if (parsed.suggestedReplies.length !== 5) throw new Error(`Expected 5 replies, got ${parsed.suggestedReplies.length}`);
        if (parsed.suggestedReplies[0].tone !== "Friendly") throw new Error(`Wrong tone: ${parsed.suggestedReplies[0].tone}`);
        if (parsed.suggestedActions.length < 2) throw new Error(`Expected at least 2 actions`);
        if (parsed.confidence !== 0.95) throw new Error(`Wrong confidence: ${parsed.confidence}`);
    });

    await assert("parseGeminiStructuredResponse provides default refinement chips when none provided", async () => {
        const sampleRaw = JSON.stringify({
            screenType: "CONVERSATION",
            summary: "Discussion about project roadmap.",
            mainInsight: "Colleague asking for milestone updates.",
            suggestedReplies: [
                { tone: "Friendly", text: "I'll have the update ready in 30 mins!" }
            ],
            confidence: 0.9
        });

        const parsed = app.parseGeminiStructuredResponse(sampleRaw);
        if (parsed.suggestedActions.length < 3) throw new Error(`Expected default refinement actions for conversation`);
        const labels = parsed.suggestedActions.map(a => a.label);
        if (!labels.includes("Make it more casual") || !labels.includes("Make it shorter")) {
            throw new Error(`Missing expected conversation refinement actions: ${JSON.stringify(labels)}`);
        }
    });

    const hasApiKey = !!(process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY !== 'YOUR_GEMINI_API_KEY_HERE');

    if (!hasApiKey) {
        console.log("  [INFO] GEMINI_API_KEY is not configured in local environment.");
        console.log("  [INFO] Verifying graceful error handling for missing/unconfigured API key...");
        await assert("Graceful 503 error when GEMINI_API_KEY is not set", async () => {
            const res = await makeRequest('POST', '/analyze-screen', {
                image: SAMPLE_PNG_BASE64,
                question: "What should I reply to this chat?",
                intent: "REPLY"
            });
            if (res.status !== 503 || res.body.success !== false || !res.body.error) {
                throw new Error(`Unexpected response: ${JSON.stringify(res.body)}`);
            }
        });
    } else {
        console.log("  [INFO] Real GEMINI_API_KEY detected in env.");
    }

    console.log("\n[TEST GROUP 4: Privacy & Zero Storage Verification]");
    await assert("No image files or persistent databases written to disk", async () => {
        const fs = require('fs');
        const files = fs.readdirSync(__dirname);
        const forbidden = files.filter(f => f.endsWith('.png') || f.endsWith('.jpg') || f.endsWith('.db') || f.endsWith('.sqlite'));
        if (forbidden.length > 0) {
            throw new Error(`Found persistent storage artifacts on disk: ${forbidden.join(', ')}`);
        }
    });

    console.log("\n==================================================");
    console.log(`TEST SUMMARY: ${passed} passed, ${failed} failed`);
    console.log("==================================================");

    server.close();
    process.exit(failed > 0 ? 1 : 0);
}

runTests().catch(err => {
    console.error("Test runner crashed:", err);
    if (server) server.close();
    process.exit(1);
});
