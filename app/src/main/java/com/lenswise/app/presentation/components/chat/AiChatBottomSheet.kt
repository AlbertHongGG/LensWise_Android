package com.lenswise.app.presentation.components.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lenswise.app.core.ai.model.AiChatRequest
import com.lenswise.app.core.ai.model.ChatMessage
import com.lenswise.app.core.voice.rememberVoiceRecognitionService
import com.lenswise.app.presentation.components.markdown.LensWiseMarkdown
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 通用 AI 專屬顧問對話抽屜 (AiChatBottomSheet)。
 * 1:1 對齊 LensWise Flutter 版 ai_chat_bottom_sheet.dart 規格：
 * 1. 預設高質感 Light Mode（白底/極淺灰，頂部 32dp 圓弧，細緻卡片陰影）。
 * 2. 整合 Android 原生語音輸入（Speech-to-Text），具備麥克風動態光圈與實時語音打字串流。
 * 3. 整合獨立模組化 Markdown 渲染器 (LensWiseMarkdown)，支援標題、粗體、條列點與引用區塊。
 * 4. 具備流式漸進展開氣泡 (Streaming Bubble) 與輸入列自動鍵盤避讓 (imePadding) 與導航列避讓 (navigationBarsPadding)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatBottomSheet(
    title: String,
    initialMessage: String,
    systemContextPrompt: String,
    onSendMessage: (AiChatRequest) -> Flow<String>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "輸入想進一步探討的問題..."
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // 語音辨識服務整合
    val voiceService = rememberVoiceRecognitionService()
    val voiceState by voiceService.voiceState.collectAsState()
    var baseTypedText by remember { mutableStateOf("") }

    // 麥克風權限請求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceService.startListening { text, isFinal ->
                // 自動將辨識字串注入輸入列
                val newText = if (baseTypedText.isBlank()) text else "$baseTypedText $text"
                inputText = newText
            }
        }
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = initialMessage,
                isUser = false
            )
        )
    }

    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val contentColor = if (isDark) Color.White else Color(0xFF111827)
    val chatAreaBg = if (isDark) Color(0xFF121212) else Color(0xFFF8F9FA)

    ModalBottomSheet(
        onDismissRequest = {
            voiceService.stopListening()
            onDismissRequest()
        },
        sheetState = sheetState,
        containerColor = bgColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = modifier,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.12f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .imePadding()
        ) {
            // 1. 頂部標題列 (1:1 對齊 Flutter 的 _buildHeader)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppColors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 對話訊息滾動區塊
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(chatAreaBg)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    )
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubbleRow(
                            message = msg,
                            isDark = isDark
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 思考中提示氣泡
                    if (isSending && (messages.lastOrNull()?.text?.isEmpty() == true)) {
                        item {
                            ThinkingBubble(isDark = isDark)
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }

            LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            // 3. 底部輸入控制列 (具備 Light Mode 白底、麥克風語音輸入與鍵盤/導航避讓)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                val inputContainerBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF1F5F9)
                val inputBorderColor = if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.08f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(inputContainerBg, RoundedCornerShape(24.dp))
                        .border(1.dp, inputBorderColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 輸入文字方塊
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        textStyle = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontFamily = OutfitFontFamily
                        ),
                        cursorBrush = SolidColor(AppColors.primary),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    text = if (voiceState.isListening) "聆聽中..." else placeholderText,
                                    color = if (voiceState.isListening) AppColors.primary else if (isDark) Color.White.copy(0.35f) else Color.Black.copy(0.35f),
                                    fontSize = 14.5.sp,
                                    fontFamily = OutfitFontFamily
                                )
                            }
                            innerTextField()
                        }
                    )

                    // 語音輸入按鈕 (麥克風)
                    val isListening = voiceState.isListening
                    IconButton(
                        onClick = {
                            if (isListening) {
                                voiceService.stopListening()
                            } else {
                                val hasAudioPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasAudioPermission) {
                                    baseTypedText = inputText.trim()
                                    voiceService.startListening { text, isFinal ->
                                        val combined = if (baseTypedText.isBlank()) text else "$baseTypedText $text"
                                        inputText = combined
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isListening) AppColors.primary.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Outlined.Mic,
                            contentDescription = "語音輸入",
                            tint = if (isListening) AppColors.primary else if (isDark) Color.White.copy(0.6f) else Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 發送按鈕
                    IconButton(
                        onClick = {
                            val query = inputText.trim()
                            if (query.isNotEmpty() && !isSending) {
                                if (voiceState.isListening) {
                                    voiceService.stopListening()
                                }
                                inputText = ""
                                baseTypedText = ""
                                isSending = true

                                val userMsg = ChatMessage(text = query, isUser = true)
                                messages.add(userMsg)

                                val aiMsgId = UUID.randomUUID().toString()
                                val aiMsg = ChatMessage(id = aiMsgId, text = "", isUser = false)
                                messages.add(aiMsg)

                                scope.launch {
                                    val chatRequest = AiChatRequest(
                                        systemContextPrompt = systemContextPrompt,
                                        history = messages.dropLast(2),
                                        newQuery = query
                                    )

                                    val buffer = StringBuilder()
                                    onSendMessage(chatRequest)
                                        .catch { err ->
                                            val idx = messages.indexOfFirst { it.id == aiMsgId }
                                            if (idx != -1) {
                                                messages[idx] = messages[idx].copy(text = "回應失敗: ${err.localizedMessage}")
                                            }
                                            isSending = false
                                        }
                                        .collect { chunk ->
                                            buffer.append(chunk)
                                            val idx = messages.indexOfFirst { it.id == aiMsgId }
                                            if (idx != -1) {
                                                messages[idx] = messages[idx].copy(text = buffer.toString())
                                            }
                                        }
                                    isSending = false
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isSending,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "發送",
                            tint = if (inputText.isNotBlank() && !isSending) AppColors.primary else Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 訊息氣泡列 (包含使用者氣泡與 AI Markdown 氣泡)。
 */
@Composable
private fun ChatBubbleRow(
    message: ChatMessage,
    isDark: Boolean
) {
    val isUser = message.isUser

    if (isUser) {
        // 使用者訊息氣泡 (靠右、品牌紫色、白色字體)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(AppColors.primary)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    fontFamily = OutfitFontFamily,
                    fontSize = 15.sp,
                    color = Color.White,
                    lineHeight = 22.sp
                )
            }
        }
    } else {
        // AI 訊息氣泡 (靠左、機器人圓形頭像、純白陰影卡片、結構化 Markdown 渲染)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(AppColors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val aiBubbleBg = if (isDark) Color(0xFF262626) else Color.White
            val aiBubbleBorder = if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.06f)

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .shadow(
                        elevation = if (isDark) 0.dp else 2.dp,
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 20.dp
                        ),
                        spotColor = Color.Black.copy(0.08f)
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 20.dp
                        )
                    )
                    .background(aiBubbleBg)
                    .border(
                        width = 1.dp,
                        color = aiBubbleBorder,
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 20.dp
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (message.text.isEmpty()) {
                    Text(
                        text = "• • •",
                        fontFamily = OutfitFontFamily,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White.copy(0.5f) else Color.Black.copy(0.4f)
                    )
                } else {
                    LensWiseMarkdown(
                        content = message.text,
                        isDark = isDark
                    )
                }
            }
        }
    }
}

/**
 * AI 思考中狀態氣泡。
 */
@Composable
private fun ThinkingBubble(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(AppColors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0xFF262626) else Color.White)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .alpha(alpha)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AppColors.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 深度思考中...",
                    fontFamily = OutfitFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.primary
                )
            }
        }
    }
}
