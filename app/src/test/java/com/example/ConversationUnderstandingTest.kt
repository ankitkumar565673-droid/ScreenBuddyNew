package com.example

import com.example.core.ai.CompanionReplyOption
import com.example.core.ai.CompanionSuggestedAction
import com.example.core.ai.GeminiVisionAiService
import com.example.core.ai.ScreenBuddyAiResponse
import com.example.core.ai.ScreenType
import com.example.core.diagnostics.ScreenBuddyDiagnostics
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConversationUnderstandingTest {

    private fun parseWithService(jsonText: String): ScreenBuddyAiResponse? {
        val service = GeminiVisionAiService()
        val method = GeminiVisionAiService::class.java.getDeclaredMethod("parseStructuredJson", String::class.java)
        method.isAccessible = true
        return method.invoke(service, jsonText) as? ScreenBuddyAiResponse
    }

    @Test
    fun testStructuredConversationJsonParsing() {
        val sampleJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "Instagram DM with Sarah",
          "summary": "Sarah is asking if you are free this Saturday for coffee at 3 PM.",
          "mainInsight": "Sarah proposed meeting on Saturday afternoon. A quick confirmation or alternative time is recommended.",
          "suggestedReplies": [
            { "tone": "Friendly", "text": "Yeah, I'd love to! 3 PM works perfectly." },
            { "tone": "Casual", "text": "Sounds good! See you then 😄" },
            { "tone": "Short", "text": "Yes, 3 PM is good." },
            { "tone": "Funny", "text": "Only if there's good espresso involved ☕" }
          ],
          "suggestedActions": [
            { "label": "Make it more casual", "actionQuery": "Make the suggested replies more casual." },
            { "label": "Make it shorter", "actionQuery": "Make the suggested replies shorter." }
          ],
          "confidence": 0.98
        }
        """.trimIndent()

        val parsed = parseWithService(sampleJson)

        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals("Instagram DM with Sarah", parsed?.screenTitle)
        assertEquals(4, parsed?.suggestedReplies?.size)
        assertEquals("Friendly", parsed?.suggestedReplies?.get(0)?.toneLabel)
        assertEquals("Casual", parsed?.suggestedReplies?.get(1)?.toneLabel)
        assertEquals("Short", parsed?.suggestedReplies?.get(2)?.toneLabel)
        assertEquals("Funny", parsed?.suggestedReplies?.get(3)?.toneLabel)
        assertEquals(2, parsed?.suggestedActions?.size)
    }

    // Level 11: 4 distinct candidate replies in recommended styles (Natural, Friendly, Confident, Playful)
    @Test
    fun testNaturalFriendlyConfidentPlayfulToneReplies() {
        val chatJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "WhatsApp - Elena",
          "summary": "Elena asks: 'Are we still on for the design review meeting this afternoon?'",
          "mainInsight": "Elena is confirming attendance for today's review. The latest incoming message is a direct question.",
          "suggestedReplies": [
            { "tone": "Natural", "text": "Yes, still on! See you in room 3." },
            { "tone": "Friendly", "text": "Definitely! Looking forward to reviewing the new slides 😊" },
            { "tone": "Confident", "text": "Yes, I'm prepared and will see you this afternoon." },
            { "tone": "Playful", "text": "Only if you brought the good coffee! See you then." },
            { "tone": "Extra", "text": "This fifth option should be dropped by the 4-candidate cap." }
          ],
          "confidence": 0.96
        }
        """.trimIndent()

        val parsed = parseWithService(chatJson)

        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        // Strictly capped at up to 4 replies as specified in Level 11
        assertEquals(4, parsed?.suggestedReplies?.size)
        assertEquals("Natural", parsed?.suggestedReplies?.get(0)?.toneLabel)
        assertEquals("Friendly", parsed?.suggestedReplies?.get(1)?.toneLabel)
        assertEquals("Confident", parsed?.suggestedReplies?.get(2)?.toneLabel)
        assertEquals("Playful", parsed?.suggestedReplies?.get(3)?.toneLabel)
        assertTrue(parsed?.suggestedReplies?.none { it.toneLabel == "Extra" } == true)
    }

    // Level 11: Sensitive or serious conversation prioritizes empathy and respect without forced humor
    @Test
    fun testSensitiveOrSeriousMessageDoesNotHavePlayfulReply() {
        val sensitiveJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "Messages - Marcus",
          "summary": "Marcus shared that his grandmother passed away this morning.",
          "mainInsight": "Marcus is sharing a deeply personal loss. Sincere condolences and support are needed.",
          "suggestedReplies": [
            { "tone": "Empathetic", "text": "I'm so sorry for your loss, Marcus. Thinking of you and your family." },
            { "tone": "Supportive", "text": "Sending you so much love. Please let me know if there's anything at all I can do." },
            { "tone": "Respectful", "text": "I am so sorry to hear that. Take all the time you need, here for you whenever." }
          ],
          "suggestedActions": [
            { "label": "Offer support", "actionQuery": "Suggest gentle messages offering help." }
          ],
          "confidence": 0.99
        }
        """.trimIndent()

        val parsed = parseWithService(sensitiveJson)

        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals(3, parsed?.suggestedReplies?.size)
        assertTrue(parsed?.suggestedReplies?.none { it.toneLabel.contains("playful", ignoreCase = true) || it.toneLabel.contains("funny", ignoreCase = true) } == true)
        assertEquals("Empathetic", parsed?.suggestedReplies?.get(0)?.toneLabel)
    }

    // Level 11: Professional work messages with appropriate business tone
    @Test
    fun testProfessionalConversationReplies() {
        val workJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "Slack - Project Lead",
          "summary": "Project Lead asks: 'Can you deliver the API schema documentation by end of day?'",
          "mainInsight": "Direct work inquiry regarding timeline for API schema documentation.",
          "suggestedReplies": [
            { "tone": "Natural", "text": "Yes, I'm finalizing it now and will share the link by 4 PM." },
            { "tone": "Professional", "text": "Confirmed. I will complete the API schema review and send it over before EOD." },
            { "tone": "Confident", "text": "On track. You will have the full documentation by end of day." }
          ],
          "confidence": 0.95
        }
        """.trimIndent()

        val parsed = parseWithService(workJson)

        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals(3, parsed?.suggestedReplies?.size)
        assertTrue(parsed?.suggestedReplies?.any { it.toneLabel.equals("Professional", ignoreCase = true) } == true)
        assertTrue(parsed?.suggestedReplies?.all { !it.text.contains("😂") } == true)
    }

    // Level 11: Browser, settings, forms, and code error screens strictly have 0 suggested replies
    @Test
    fun testNonConversationScreenHasNoReplies() {
        val nonChatScreens = listOf(
            """{"screenType": "SETTINGS", "summary": "Display settings", "mainInsight": "Dark theme toggle"}""",
            """{"screenType": "WEBPAGE", "summary": "Article about Android", "mainInsight": "Summary of article"}""",
            """{"screenType": "FORM", "summary": "Registration form", "mainInsight": "Fill email and password"}""",
            """{"screenType": "CODE_OR_ERROR", "summary": "NullPointerException", "mainInsight": "Check variable initialization"}"""
        )

        for (json in nonChatScreens) {
            val parsed = parseWithService(json)
            assertNotNull(parsed)
            assertTrue("Screen type ${parsed?.screenType} must have empty suggestedReplies", parsed?.suggestedReplies?.isEmpty() == true)
        }
    }

    // Level 11: Non-conversation screen with fake suggested replies in JSON is sanitized to empty list
    @Test
    fun testNonConversationScreenSanitizesFabricatedReplies() {
        val webJsonWithFakeReplies = """
        {
          "screenType": "WEBPAGE",
          "screenTitle": "Wikipedia: Quantum Computing",
          "summary": "Article describing quantum bits and superposition.",
          "mainInsight": "Quantum computers use qubits to represent superpositions of 0 and 1.",
          "suggestedReplies": [
            { "tone": "Friendly", "text": "That's so interesting!" }
          ],
          "suggestedActions": [
            { "label": "Explain qubits", "actionQuery": "Explain what qubits are in simple terms." }
          ]
        }
        """.trimIndent()

        val parsed = parseWithService(webJsonWithFakeReplies)

        assertNotNull(parsed)
        assertEquals(ScreenType.WEBPAGE, parsed?.screenType)
        // Must reject or drop suggestedReplies for WEBPAGE
        assertTrue("WEBPAGE must never contain suggested conversation replies", parsed?.suggestedReplies?.isEmpty() == true)
        assertEquals(1, parsed?.suggestedActions?.size)
    }

    // Level 11: Ambiguous or cropped screen acknowledges uncertainty
    @Test
    fun testAmbiguousOrCroppedContextAcknowledgement() {
        val croppedJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": null,
          "summary": "Visible text is partially cut off at the bottom of the screen.",
          "mainInsight": "The latest incoming message is partially obscured by the keyboard. I cannot confirm the full request.",
          "suggestedReplies": [
            { "tone": "Clarifying", "text": "Sorry, what did you mean by that?" },
            { "tone": "Natural", "text": "Could you repeat that? I didn't catch the full message." }
          ],
          "confidence": 0.55
        }
        """.trimIndent()

        val parsed = parseWithService(croppedJson)

        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertTrue(parsed?.summary?.contains("cut off") == true)
        assertTrue(parsed?.mainInsight?.contains("obscured") == true)
        assertEquals(0.55f, parsed?.confidence ?: 1f, 0.01f)
    }

    // Level 11: Plain string list or candidateReplies parsed robustly
    @Test
    fun testCandidateRepliesOrSimpleStringListParsing() {
        val simpleArrayJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "Alex",
          "summary": "Alex asking about lunch.",
          "mainInsight": "Alex wants to know if lunch at noon works.",
          "candidateReplies": [
            "Yes, noon works great!",
            "Can we push it to 12:30?",
            "Sure thing, see you at noon."
          ]
        }
        """.trimIndent()

        val parsed = parseWithService(simpleArrayJson)

        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals(3, parsed?.suggestedReplies?.size)
        assertEquals("Natural", parsed?.suggestedReplies?.get(0)?.toneLabel)
        assertEquals("Yes, noon works great!", parsed?.suggestedReplies?.get(0)?.text)
    }

    // Level 12: ScreenType aliases mapping to CONVERSATION
    @Test
    fun testScreenTypeAliasesMapToConversation() {
        val chatAliases = listOf("CHAT", "messaging", "Messages", "direct_message", "DM", "SMS", "text", "thread", "social_chat", "chat_thread")
        for (alias in chatAliases) {
            val mapped = ScreenType.fromString(alias)
            assertEquals("Alias '$alias' must map to CONVERSATION", ScreenType.CONVERSATION, mapped)
        }
    }

    // Level 12: Screen classified as "CHAT" retains suggested replies
    @Test
    fun testChatScreenTypeAliasPreservesReplies() {
        val chatAliasJson = """
        {
          "screenType": "CHAT",
          "screenTitle": "Telegram with David",
          "summary": "David says: 'Can we move our call to 4 PM?'",
          "mainInsight": "David requests rescheduling the meeting to 4 PM.",
          "suggestedReplies": [
            { "tone": "Natural", "text": "Sure, 4 PM works for me." },
            { "tone": "Friendly", "text": "No problem at all! See you at 4." },
            { "tone": "Confident", "text": "4 PM is confirmed on my calendar." }
          ]
        }
        """.trimIndent()

        val parsed = parseWithService(chatAliasJson)
        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals(3, parsed?.suggestedReplies?.size)
        assertEquals("Sure, 4 PM works for me.", parsed?.suggestedReplies?.get(0)?.text)
    }

    // Level 12: Screen classified as FEED or UNKNOWN with valid replies upgrades to CONVERSATION
    @Test
    fun testAmbiguousFeedOrUnknownWithRepliesUpgradesToConversation() {
        val feedWithRepliesJson = """
        {
          "screenType": "FEED",
          "screenTitle": "Instagram Direct Thread",
          "summary": "Incoming message in chat stream.",
          "mainInsight": "Latest message invites to dinner.",
          "suggestedReplies": [
            { "tone": "Friendly", "text": "I'd love to! Where are we going?" },
            { "tone": "Short", "text": "Count me in!" }
          ]
        }
        """.trimIndent()

        val parsed = parseWithService(feedWithRepliesJson)
        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals(2, parsed?.suggestedReplies?.size)
    }

    // Level 12: Object-formatted replies are parsed correctly
    @Test
    fun testJsonObjectFormattedRepliesParsing() {
        val objectRepliesJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "Signal",
          "summary": "Friend asking about weekend plans.",
          "mainInsight": "Friend asked: 'Are you free Sunday?'",
          "suggestedReplies": {
            "friendly": "Yes! Let's do something fun.",
            "natural": "Yeah, free all day Sunday.",
            "short": "Free Sunday!"
          }
        }
        """.trimIndent()

        val parsed = parseWithService(objectRepliesJson)
        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals(3, parsed?.suggestedReplies?.size)
        assertTrue(parsed?.suggestedReplies?.any { it.toneLabel.equals("Friendly", ignoreCase = true) } == true)
    }

    // Level 12: Intent inference correctly detects reply requests
    @Test
    fun testInferIntentFromQuestion() {
        val service = GeminiVisionAiService()
        val method = GeminiVisionAiService::class.java.getDeclaredMethod("inferIntentFromQuestion", String::class.java)
        method.isAccessible = true

        val replyIntent1 = method.invoke(service, "What should I reply?") as com.example.core.ai.UserIntent
        assertEquals(com.example.core.ai.UserIntent.REPLY, replyIntent1)

        val replyIntent2 = method.invoke(service, "What should I say to this message?") as com.example.core.ai.UserIntent
        assertEquals(com.example.core.ai.UserIntent.REPLY, replyIntent2)

        val summarizeIntent = method.invoke(service, "Summarize this page in bullet points") as com.example.core.ai.UserIntent
        assertEquals(com.example.core.ai.UserIntent.SUMMARIZE, summarizeIntent)
    }

    // Level 12: Text-only follow-up with recentContext does not fail with MISSING_INPUT
    @Test
    fun testTextOnlyFollowUpWithRecentContextDoesNotThrowMissingInput() = kotlinx.coroutines.runBlocking {
        val service = GeminiVisionAiService()
        val result = service.analyzeScreenshot(
            bitmap = null,
            question = "Can you make the replies shorter?",
            intent = com.example.core.ai.UserIntent.REPLY,
            recentContext = "Screen Category: Conversation\nPrevious Insight: Lunch invitation"
        )
        // Even without network/API key in local unit test, it should NOT fail with MISSING_INPUT
        if (result is com.example.core.ai.VisionAiResult.Error) {
            assertTrue("Should not fail with MISSING_INPUT when recentContext is present", result.errorType != "MISSING_INPUT")
        }
    }

    // Level 13: Prompt includes reference resolution, context separation, topic shift, and clarification directives
    @Test
    fun testBuildDirectPromptContainsLevel13Directives() {
        val service = GeminiVisionAiService()
        val method = GeminiVisionAiService::class.java.getDeclaredMethod(
            "buildDirectPrompt",
            String::class.java,
            com.example.core.ai.UserIntent::class.java,
            String::class.java,
            String::class.java
        )
        method.isAccessible = true

        val prompt = method.invoke(
            service,
            "Make that shorter",
            com.example.core.ai.UserIntent.REPLY,
            "=== [SCREEN-DERIVED VISUAL CONTEXT] ===\nScreen Category: Conversation\n=== [PREVIOUSLY SUGGESTED REPLIES DRAFTED BY SCREENBUDDY] ===\nOption 1 [Natural]: \"Hey, 4 PM works!\"",
            null
        ) as String

        assertTrue("Prompt must include Level 13 reference resolution", prompt.contains("REFERENCE RESOLUTION"))
        assertTrue("Prompt must include strict context separation", prompt.contains("STRICT CONTEXT SEPARATION"))
        assertTrue("Prompt must include topic shift handling", prompt.contains("TOPIC SHIFT HANDLING"))
        assertTrue("Prompt must include missing context clarification", prompt.contains("MISSING CONTEXT CLARIFICATION"))
        assertTrue("Prompt must include consistent companion personality", prompt.contains("CONSISTENT COMPANION PERSONALITY"))
    }

    // Level 13: Follow-up requesting shorter replies parses correctly
    @Test
    fun testFollowUpShortReplyRefinementParsing() {
        val shorterJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "WhatsApp - Alex",
          "summary": "Alex asked: 'Can we move our meeting to 4 PM?'",
          "mainInsight": "Here are punchy, shorter versions of the reply.",
          "suggestedReplies": [
            { "tone": "Short", "text": "4 PM works." },
            { "tone": "Natural", "text": "Sure, see you at 4!" },
            { "tone": "Friendly", "text": "Sounds good 👍" }
          ],
          "suggestedActions": [
            { "label": "More confident", "actionQuery": "Use a more confident tone." },
            { "label": "Explain why", "actionQuery": "Explain why you suggested that." }
          ],
          "confidence": 0.97
        }
        """.trimIndent()

        val parsed = parseWithService(shorterJson)
        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals(3, parsed?.suggestedReplies?.size)
        assertEquals("4 PM works.", parsed?.suggestedReplies?.get(0)?.text)
        assertTrue(parsed?.suggestedReplies?.all { it.text.length < 30 } == true)
    }

    // Level 13: Follow-up requesting confident tone parses correctly
    @Test
    fun testToneRefinementToConfidentParsing() {
        val confidentJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "Slack - Project Lead",
          "summary": "Lead asked if the report will be ready on time.",
          "mainInsight": "Adjusted replies to use a direct, self-assured tone.",
          "suggestedReplies": [
            { "tone": "Confident", "text": "Yes, on track. You will have it before 5 PM." },
            { "tone": "Professional", "text": "Confirmed, finalizing the data now." }
          ],
          "confidence": 0.98
        }
        """.trimIndent()

        val parsed = parseWithService(confidentJson)
        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals("Confident", parsed?.suggestedReplies?.get(0)?.toneLabel)
        assertTrue(parsed?.suggestedReplies?.any { it.toneLabel.equals("Confident", ignoreCase = true) } == true)
    }

    // Level 13: Topic shift to general question does not retain stale chat replies
    @Test
    fun testTopicShiftGeneralQuestionHasNoReplies() {
        val topicShiftJson = """
        {
          "screenType": "UNKNOWN",
          "screenTitle": null,
          "summary": "Calculation request",
          "mainInsight": "15% tip on a $60 bill is $9.00, making the total $69.00.",
          "suggestedReplies": [],
          "suggestedActions": [
            { "label": "Round to nearest dollar", "actionQuery": "Round the total up to the nearest dollar." }
          ],
          "confidence": 0.99
        }
        """.trimIndent()

        val parsed = parseWithService(topicShiftJson)
        assertNotNull(parsed)
        assertTrue("Suggested replies must be empty on topic shift", parsed?.suggestedReplies?.isEmpty() == true)
        assertTrue("Main insight must provide direct answer", parsed?.mainInsight?.contains("$9.00") == true)
    }

    // Level 13: Missing context clarification question
    @Test
    fun testMissingContextClarifyingQuestion() {
        val clarificationJson = """
        {
          "screenType": "UNKNOWN",
          "screenTitle": null,
          "summary": "Ambiguous reference without visible conversation",
          "mainInsight": "I don't see a message from Mark on the screen right now. Could you bring up your chat with him, or tell me what he said?",
          "suggestedReplies": [],
          "confidence": 0.85
        }
        """.trimIndent()

        val parsed = parseWithService(clarificationJson)
        assertNotNull(parsed)
        assertTrue(parsed?.suggestedReplies?.isEmpty() == true)
        assertTrue("Should ask a polite clarifying question", parsed?.mainInsight?.contains("Could you") == true)
    }

    // Level 13 Regression: WhatsApp reply suggestions still function properly
    @Test
    fun testWhatsAppReplySuggestionsRegression() {
        val whatsAppJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "WhatsApp - David",
          "summary": "David asks: 'Are you still free for dinner tonight at 7?'",
          "mainInsight": "David is asking to confirm dinner plans for 7 PM tonight.",
          "suggestedReplies": [
            { "tone": "Natural", "text": "Yes, 7 PM works! Where are we going?" },
            { "tone": "Friendly", "text": "Still free! Looking forward to it 😊" },
            { "tone": "Short", "text": "Yes, see you at 7!" },
            { "tone": "Playful", "text": "Only if tacos are involved! See you at 7." }
          ],
          "confidence": 0.99
        }
        """.trimIndent()

        val parsed = parseWithService(whatsAppJson)
        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals(4, parsed?.suggestedReplies?.size)
        assertEquals("WhatsApp - David", parsed?.screenTitle)
    }

    // Level 13 Regression: Chrome summarization still functions properly
    @Test
    fun testChromeSummarizationRegression() {
        val chromeJson = """
        {
          "screenType": "WEBPAGE",
          "screenTitle": "Wikipedia - Quantum Computing",
          "summary": "An encyclopedia entry defining qubits and superposition.",
          "mainInsight": "Quantum computing uses qubits to perform calculations exponentially faster than classical computers for specific problem classes.",
          "suggestedReplies": [],
          "suggestedActions": [
            { "label": "Key takeaways", "actionQuery": "Summarize key takeaways in 3 bullet points." }
          ],
          "confidence": 0.96
        }
        """.trimIndent()

        val parsed = parseWithService(chromeJson)
        assertNotNull(parsed)
        assertEquals(ScreenType.WEBPAGE, parsed?.screenType)
        assertTrue(parsed?.suggestedReplies?.isEmpty() == true)
        assertTrue(parsed?.mainInsight?.contains("Quantum computing") == true)
    }

    // Level 14: Ambiguous screens or plain social feeds must NOT produce chat reply suggestions
    @Test
    fun testLevel14AmbiguousScreenOrPlainFeedDoesNotProduceChatReplies() {
        // Plain social feed without 1-on-1 direct message conversation evidence
        val plainFeedJson = """
        {
          "screenType": "FEED",
          "screenTitle": "Instagram Feed",
          "summary": "User scrolling through general photo posts from various creators.",
          "mainInsight": "Browsing public feed updates with no direct conversation visible.",
          "suggestedReplies": [
            { "tone": "Casual", "text": "Nice pic!" }
          ],
          "confidence": 0.88
        }
        """.trimIndent()

        val parsed = parseWithService(plainFeedJson)
        assertNotNull(parsed)
        assertEquals(ScreenType.FEED, parsed?.screenType)
        // Level 14 mandate: Plain feeds must have EMPTY suggestedReplies even if model hallucinated one
        assertTrue("Suggested replies must be strictly empty for plain feeds without DM evidence",
            parsed?.suggestedReplies?.isEmpty() == true)

        // Ambiguous/unknown screen
        val unknownScreenJson = """
        {
          "screenType": "UNKNOWN",
          "screenTitle": null,
          "summary": "Partially occluded camera preview",
          "mainInsight": "I cannot clearly see an active conversation on the screen right now.",
          "suggestedReplies": [
            { "tone": "Friendly", "text": "Hey there!" }
          ],
          "confidence": 0.50
        }
        """.trimIndent()

        val parsedUnknown = parseWithService(unknownScreenJson)
        assertNotNull(parsedUnknown)
        assertEquals(ScreenType.UNKNOWN, parsedUnknown?.screenType)
        assertTrue("Suggested replies must be strictly empty for unknown/ambiguous screens",
            parsedUnknown?.suggestedReplies?.isEmpty() == true)
    }

    // Level 14: Genuine chat screens with clear DM evidence still reliably produce suggested replies
    @Test
    fun testLevel14GenuineChatWithDirectEvidenceProducesReplies() {
        val genuineChatJson = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "WhatsApp - Michael",
          "summary": "Michael sent: 'Hey, did you finish the project proposal yet?'",
          "mainInsight": "Michael is asking for project status. An immediate update or ETA is appropriate.",
          "suggestedReplies": [
            { "tone": "Natural", "text": "Almost done! Sending it over in about 20 minutes." },
            { "tone": "Friendly", "text": "Just putting the finishing touches on it now 😊" },
            { "tone": "Confident", "text": "Yes, it's complete and ready for your review." },
            { "tone": "Short", "text": "Working on it, will send shortly." }
          ],
          "confidence": 0.98
        }
        """.trimIndent()

        val parsed = parseWithService(genuineChatJson)
        assertNotNull(parsed)
        assertEquals(ScreenType.CONVERSATION, parsed?.screenType)
        assertEquals(4, parsed?.suggestedReplies?.size)
        assertEquals("WhatsApp - Michael", parsed?.screenTitle)
    }

    // Level 14: Missing or incomplete model JSON fields fall back gracefully without crash
    @Test
    fun testLevel14MalformedOrIncompleteJsonFallsBackGracefully() {
        val minimalJson = """
        {
          "screenType": "SETTINGS"
        }
        """.trimIndent()

        val parsed = parseWithService(minimalJson)
        assertNotNull(parsed)
        assertEquals(ScreenType.SETTINGS, parsed?.screenType)
        assertFalse("Summary must have safe fallback", parsed?.summary?.isBlank() == true)
        assertFalse("Main insight must have safe fallback", parsed?.mainInsight?.isBlank() == true)
        assertTrue(parsed?.suggestedReplies?.isEmpty() == true)
    }

    // Level 14: ScreenBuddyDiagnostics tracks token usage and error categories accurately
    @Test
    fun testLevel14DiagnosticsTracksTokenUsageAndErrorCategories() {
        ScreenBuddyDiagnostics.reset()

        // Verify initial state
        assertEquals(0, ScreenBuddyDiagnostics.promptTokensTotal)
        assertEquals(0, ScreenBuddyDiagnostics.candidateTokensTotal)
        assertNull(ScreenBuddyDiagnostics.lastErrorCategory)

        // Record usage
        ScreenBuddyDiagnostics.recordTokenUsage(promptTokens = 250, candidateTokens = 85)
        assertEquals(250, ScreenBuddyDiagnostics.promptTokensTotal)
        assertEquals(85, ScreenBuddyDiagnostics.candidateTokensTotal)

        // Record error category
        ScreenBuddyDiagnostics.recordErrorCategory("RATE_LIMITED")
        assertEquals("RATE_LIMITED", ScreenBuddyDiagnostics.lastErrorCategory)

        // Reset clears all counters safely
        ScreenBuddyDiagnostics.reset()
        assertEquals(0, ScreenBuddyDiagnostics.promptTokensTotal)
        assertEquals(0, ScreenBuddyDiagnostics.candidateTokensTotal)
        assertNull(ScreenBuddyDiagnostics.lastErrorCategory)
    }
}

