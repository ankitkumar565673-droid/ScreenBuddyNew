package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.model.SampleQuestion
import com.example.ui.components.ChatBubble
import com.example.ui.components.RobotAvatar
import com.example.ui.theme.BentoBlueBg
import com.example.ui.theme.BentoBlueBorder
import com.example.ui.theme.BentoBluePrimary
import com.example.ui.theme.BentoIndigo
import com.example.ui.theme.BentoIndigoBorder
import com.example.ui.theme.BentoIndigoDark
import com.example.ui.theme.BentoIndigoLight
import com.example.ui.theme.BentoSlate100
import com.example.ui.theme.BentoSlate200
import com.example.ui.theme.BentoSlate600
import com.example.ui.theme.BentoSlate900
import kotlinx.coroutines.launch

/**
 * Demo Assistant Conversation Screen (Level 1) in Bento Grid Theme.
 *
 * Requirements:
 * - Prominent notice banner: "Screen access will be connected in Level 2."
 * - Mock conversation UX:
 *   Robot: "Okay, I can see your screen. What would you like help with?"
 *   User: "What should I reply?"
 *   Robot: "Once screen understanding is connected, I'll be able to analyze the current screen and suggest a reply."
 * - Interactive question chips for the planned product prompts:
 *   - "What should I reply?"
 *   - "What does this mean?"
 *   - "What should I do here?"
 *   - "Explain this screen."
 *   - "Help me fill this form."
 *   - "Why am I getting this error?"
 *   - "Summarize this conversation."
 *   - "Translate this."
 *   - "What button should I press?"
 *   - "Teach me what's on this screen."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoConversationScreen(
    onBackToHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Interactive mock chat messages starting with the required initial conversation
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = "1",
                sender = MessageSender.ROBOT,
                text = "Okay, I can see your screen. What would you like help with?",
                timestamp = "10:00 AM"
            ),
            ChatMessage(
                id = "2",
                sender = MessageSender.USER,
                text = "What should I reply?",
                timestamp = "10:01 AM"
            ),
            ChatMessage(
                id = "3",
                sender = MessageSender.ROBOT,
                text = "Once screen understanding is connected, I'll be able to analyze the current screen and suggest a reply.",
                timestamp = "10:01 AM"
            )
        )
    }

    var userInputText by remember { mutableStateOf("") }

    // Comprehensive list of questions matching user prompt specification
    val sampleQuestions = listOf(
        SampleQuestion(
            title = "Reply",
            query = "What should I reply?",
            sampleResponse = "Once screen understanding is connected, I'll analyze the conversation context and suggest 2-3 natural, personalized replies.",
            emoji = "✍️"
        ),
        SampleQuestion(
            title = "Meaning",
            query = "What does this mean?",
            sampleResponse = "Once vision AI is connected, I'll highlight the confusing terminology or graphic and explain it in plain English.",
            emoji = "💡"
        ),
        SampleQuestion(
            title = "Next Action",
            query = "What should I do here?",
            sampleResponse = "Once connected in Level 3, I'll detect the current app state and recommend the next safe step to complete your task.",
            emoji = "🧭"
        ),
        SampleQuestion(
            title = "Explain Screen",
            query = "Explain this screen.",
            sampleResponse = "In upcoming levels, I will break down all visual components on your active screen and give you an easy-to-read summary.",
            emoji = "📱"
        ),
        SampleQuestion(
            title = "Fill Form",
            query = "Help me fill this form.",
            sampleResponse = "In Level 5 & 6, I'll guide you field by field through required inputs without storing your sensitive private details.",
            emoji = "📝"
        ),
        SampleQuestion(
            title = "Understand Error",
            query = "Why am I getting this error?",
            sampleResponse = "Once vision AI is connected, I'll read the error dialog or stack message and tell you how to resolve it quickly.",
            emoji = "🛠️"
        ),
        SampleQuestion(
            title = "Summarize",
            query = "Summarize this conversation.",
            sampleResponse = "In upcoming levels, I'll generate bullet-point takeaways from active chat logs or long threads on your screen.",
            emoji = "📋"
        ),
        SampleQuestion(
            title = "Translate",
            query = "Translate this.",
            sampleResponse = "Once vision understanding is connected, I'll detect foreign text on screen and overlay instant translations.",
            emoji = "🌐"
        ),
        SampleQuestion(
            title = "Button Choice",
            query = "What button should I press?",
            sampleResponse = "Once connected, I'll guide your eye to the exact primary action button needed to proceed safely.",
            emoji = "🔘"
        ),
        SampleQuestion(
            title = "Teach Screen",
            query = "Teach me what's on this screen.",
            sampleResponse = "In future levels, I will act as your interactive tutor, explaining features, icons, and menus step by step.",
            emoji = "🎓"
        )
    )

    fun sendMockMessage(userText: String, robotReply: String? = null) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        messages.add(
            ChatMessage(
                id = System.currentTimeMillis().toString(),
                sender = MessageSender.USER,
                text = trimmed,
                timestamp = "Just now"
            )
        )

        val response = robotReply ?: sampleQuestions.find {
            it.query.equals(trimmed, ignoreCase = true)
        }?.sampleResponse
        ?: "Once Level 2 screen capture and Level 3 Vision AI are connected, I'll be able to inspect your screen to answer: \"$trimmed\"."

        messages.add(
            ChatMessage(
                id = (System.currentTimeMillis() + 1).toString(),
                sender = MessageSender.ROBOT,
                text = response,
                timestamp = "Just now"
            )
        )

        userInputText = ""

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Navigation & Header in Bento style
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = CardDefaults.outlinedCardBorder(),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    IconButton(
                        onClick = onBackToHomeClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BentoSlate100)
                            .testTag("demo_back_to_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = BentoSlate600,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    RobotAvatar(
                        size = 38.dp,
                        showGlow = false,
                        showOnlineBadge = true
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ScreenBuddy Demo",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoSlate900
                            )
                        )
                        Text(
                            text = "Level 1 UX Simulation",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BentoIndigo,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = {
                            messages.clear()
                            messages.addAll(
                                listOf(
                                    ChatMessage(
                                        id = "1",
                                        sender = MessageSender.ROBOT,
                                        text = "Okay, I can see your screen. What would you like help with?",
                                        timestamp = "10:00 AM"
                                    ),
                                    ChatMessage(
                                        id = "2",
                                        sender = MessageSender.USER,
                                        text = "What should I reply?",
                                        timestamp = "10:01 AM"
                                    ),
                                    ChatMessage(
                                        id = "3",
                                        sender = MessageSender.ROBOT,
                                        text = "Once screen understanding is connected, I'll be able to analyze the current screen and suggest a reply.",
                                        timestamp = "10:01 AM"
                                    )
                                )
                            )
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BentoSlate100)
                            .testTag("reset_demo_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Demo",
                            tint = BentoSlate600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Notice Banner explicitly required by prompt:
            // "Screen access will be connected in Level 2."
            Surface(
                color = BentoBlueBg,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BentoBlueBorder)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("level_2_notice_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BentoBluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Screen access will be connected in Level 2.",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoSlate900
                        )
                    )
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Quick Question Suggestion Chips in Bento Style
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = BentoSlate200
                    )
                    .padding(top = 10.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "Tap a sample question to test:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoSlate600
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleQuestions.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BentoIndigoLight,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(BentoIndigoBorder)
                            ),
                            modifier = Modifier
                                .clickable {
                                    sendMockMessage(item.query, item.sampleResponse)
                                }
                                .testTag("chip_${item.title.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(text = item.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.query,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = BentoIndigoDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Interactive User Input Bar in Bento Style
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = userInputText,
                        onValueChange = { userInputText = it },
                        placeholder = {
                            Text(
                                "Ask a question about the screen...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BentoSlate600.copy(alpha = 0.7f)
                                )
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoIndigo,
                            unfocusedBorderColor = BentoSlate200,
                            focusedContainerColor = BentoSlate100.copy(alpha = 0.5f),
                            unfocusedContainerColor = BentoSlate100.copy(alpha = 0.5f)
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (userInputText.isNotBlank()) {
                                sendMockMessage(userInputText)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (userInputText.isNotBlank()) BentoSlate900 else BentoSlate200)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (userInputText.isNotBlank()) Color.White else BentoSlate600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

