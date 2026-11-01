# ScreenBuddy Vision AI Backend (Level 5)

A secure, zero-persistence proxy server for **ScreenBuddy** — the privacy-first AI screen assistant.

---

## 1. Overview & Privacy Principles

ScreenBuddy is designed around the core principle: **"ScreenBuddy only looks when you ask."**

- **Ephemeral In-Memory Processing**: Screen images and questions reside strictly in memory for the duration of the request.
- **Zero Data Storage**: No databases, no disk caches, no screenshot history, no analytics tracking, and no OCR logs.
- **Secret Isolation**: `GEMINI_API_KEY` stays strictly on the backend server environment. The Android client never handles or stores API keys.
- **No Unsolicited Monitoring**: The backend processes screenshots solely upon explicit user-initiated requests.

---

## 2. Technology Stack

- **Runtime**: Node.js (>= 18.0.0)
- **Framework**: Express.js with CORS and in-memory rate limiting
- **AI Model**: Google Gemini Vision (`gemini-3.5-flash` configurable via environment)
- **Security**: Strict JSON parsing limits (15MB), timeout abort controllers (45s), sanitized operational logging (zero screen data logged)

---

## 3. Configuration & Environment Variables

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

| Variable | Description | Default |
| :--- | :--- | :--- |
| `GEMINI_API_KEY` | **Required**. Your Google Gemini API key. Never expose in client code. | *(None)* |
| `GEMINI_MODEL` | Gemini Vision model identifier. | `gemini-3.5-flash` |
| `PORT` | Local server port. | `8080` |

> **Security Note**: Real `.env` files are ignored by Git via `.gitignore`. Never commit keys to source control.

---

## 4. API Endpoints

### `GET /health`
Verifies backend server availability without exposing secrets.

**Response**:
```json
{
  "status": "ok"
}
```

---

### `POST /analyze-screen`
Primary visual analysis endpoint.

**Request Headers**:
```http
Content-Type: application/json
```

**Request Body**:
```json
{
  "image": "<base64 encoded JPEG, PNG, or WebP screenshot>",
  "question": "What should I reply?"
}
```

**Successful Response** (`200 OK`):
```json
{
  "success": true,
  "answer": "Based on the conversation, they are asking whether you're free tomorrow. You could reply: 'Yeah, I'm free tomorrow evening. What did you have in mind?'"
}
```

**Error Response** (`400`, `429`, `500`, `503`, `504`):
```json
{
  "success": false,
  "error": "Please capture your screen first."
}
```

---

## 5. Android Integration

For the Android application developer integrating ScreenBuddy:

### Architecture Flow
```
Android App (ScreenBuddy)
   │
   ├─► User taps ScreenBuddy and asks question
   ├─► Capture screenshot into ephemeral RAM
   │
   ▼
POST {BACKEND_URL}/analyze-screen
   │
   ├─► Headers: Content-Type: application/json
   ├─► Body: { "image": "<base64>", "question": "<question>" }
   │
   ▼
ScreenBuddy Backend (Level 5)
   │
   ├─► In-Memory Validation & Gemini Vision Call
   ├─► Receives Gemini Vision advice
   │
   ▼
Returns { "success": true, "answer": "..." }
   │
   ▼
Android App
   │
   ├─► Displays answer to user in floating assistant
   └─► Immediately clears bitmap from memory
```

### Integration Details
1. **Endpoint**: `POST /analyze-screen` (alias: `POST /api/analyze-screen`)
2. **Content-Type**: `application/json`
3. **Image Encoding**: Base64 string (raw base64 or Data URI format `data:image/jpeg;base64,...`). Supported image formats: JPEG, PNG, WebP.
4. **Max Request Size**: Up to 15MB.
5. **Timeout Expectations**: Set client HTTP timeout to **45–60 seconds**.
6. **Backend URL Configuration**:
   - In development/emulator: `http://10.0.2.2:8080` (standard Android emulator loopback) or your local LAN IP (e.g. `http://192.168.1.50:8080`).
   - In production: Set `BACKEND_URL` in the Android `.env` or Secrets configuration to your deployed backend URL (e.g. `https://screenbuddy-backend.run.app`).

---

## 6. How to Run & Test Locally

### 1. Install Dependencies
```bash
cd backend
npm install
```

### 2. Run Test Suite
```bash
npm test
```
The test suite validates:
- `GET /health` functionality and security
- Error handling on missing/corrupt payloads
- Vision AI query generation
- Zero-persistence guarantees

### 3. Start the Server
```bash
npm start
```
Server will start listening on port `8080`.
