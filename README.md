# ScreenBuddy 🤖

**ScreenBuddy** is a personal, privacy-first AI screen assistant for Android featuring a floating AI companion (**Mira**), real-time OCR, Smart Reply, and zero-latency local intelligence.

## Core Product Principle
> **"ScreenBuddy only looks when you ask."**

- ScreenBuddy **never** continuously monitors or records your screen.
- Screen captures are performed only on explicit user request (tapping "Look at my screen", "Live View", "Smart Reply", or "Ask AI").
- Screenshots and query data are kept strictly in ephemeral memory and discarded immediately after processing.
- No analytics tracking, OCR logging, or image persistence.

---

## Level 16: Complete Multi-Level Architecture

ScreenBuddy is built on a 16-level progressive intelligence stack:

```
                  [ Floating Mira AI Companion ]
                                │
          ┌─────────────────────┴─────────────────────┐
          ▼                                           ▼
 [ Manual Query / Quick Action ]             [ Live View Streaming ]
          │                                           │
          ▼                                           ▼
[ Level 2 Screen Capture ]               [ Level 9 Visual Change Engine ]
  (320ms clean frame flush)                (1.8s frame differential)
          │                                           │
          └─────────────────────┬─────────────────────┘
                                ▼
             [ Level 10 Local Intelligence Engine ]
                                │
             ┌──────────────────┴──────────────────┐
             ▼                                     ▼
   (Local Regex/OCR Cache)               (Complex Screen Query)
  [ Zero-Latency Smart Reply ]          [ Level 8 Vision AI Service ]
   - Level 12 Conversation Context       - Gemini 1.5 / 2.5 Flash
   - Level 15 Request Coalescing         - Structured Vision Prompts
   - Level 16 Real-Device Diagnostics    - Ephemeral Frame Discard
             │                                     │
             └──────────────────┬──────────────────┘
                                ▼
                     [ AI Response Display ]
              - Grounded Screen Answer & Suggestions
              - One-Tap Clipboard Copy / Action Chips
              - Mira Reaction & Expressive Companion States
```

### Architecture Level Highlights

- **Level 1–3**: Core Android Navigation, MediaProjection Screen Capture, & Direct Gemini REST integration.
- **Level 4–7**: System Overlay (`TYPE_APPLICATION_OVERLAY`), Floating Assistant Service, & Level 7 Expressive Anime Companion (**Mira**).
- **Level 8–9**: Grounded Multimodal Vision AI & Level 9 Visual Change Engine for Live View streaming.
- **Level 10**: Local Intelligence & Smart Context Engine (determines when on-device context satisfies the query vs sending to Gemini).
- **Level 12**: Multi-Turn Short-Lived Conversation Context Model.
- **Level 15**: Latest-State Request Coalescing (suppresses duplicate in-flight requests during rapid screen switches).
- **Level 16**: Real-Device Smart Reply Diagnostics, Validation Engine, & Fallback Pipeline.

---

## Quick Actions & Features
- 💬 *What should I reply?* (Smart Reply with Tone selection: Friendly, Professional, Concise, Witty)
- 💡 *Explain this screen*
- 🔍 *Look at my screen*
- 📝 *Summarize this*
- 🌐 *Translate this*
- 🎥 *Live View* (Real-time visual monitoring & streaming guidance)

---

## Building and Running

### 1. Backend Server Setup (Optional proxy)
```bash
cd backend
npm install
cp .env.example .env
# Edit .env with your GEMINI_API_KEY
npm start
```

### 2. Android Client Setup
Set `GEMINI_API_KEY` (or `BACKEND_URL`) in the AI Studio Secrets panel / `.env`.

Build and install:
```bash
gradle assembleDebug
```

