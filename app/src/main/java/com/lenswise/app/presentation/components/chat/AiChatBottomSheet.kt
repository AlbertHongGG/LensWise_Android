package com.lenswise.app.presentation.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.core.ai.model.AiChatRequest
import com.lenswise.app.core.ai.model.ChatMessage
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.AppTypography
import com.lenswise.app.presentation.theme.OutfitFontFamily
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 通用 AI 專屬顧問對話抽屜 (AiChatBottomSheet)。
 * 1:1 復刻 LensWise Flutter 版 AiChatBottomSheet 規範：
 * - 支援多代理人（收據、商品、比對、菜單）動態注入上下文提示詞與流式產生器。
 * - 具備流式漸進展開氣泡 (Streaming Bubble) 與輸入列自動鍵盤避讓 (imePadding)。
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
    placeholderText: String = "輸入提問..."
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = initialMessage,
                isUser = false
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E),
        contentColor = Color.White,
        modifier = modifier,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // 標題列
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = AppTypography.dialogTitle.copy(color = Color.White)
                )
            }

            // 對話訊息列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            // 輸入欄位
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(Color(0xFF2C2C2C), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = OutfitFontFamily
                    ),
                    cursorBrush = SolidColor(AppColors.primary),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = placeholderText,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontFamily = OutfitFontFamily
                            )
                        }
                        innerTextField()
                    }
                )

                IconButton(
                    onClick = {
                        val query = inputText.trim()
                        if (query.isNotEmpty() && !isSending) {
                            inputText = ""
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
                    enabled = inputText.isNotBlank() && !isSending
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "發送",
                        tint = if (inputText.isNotBlank() && !isSending) AppColors.primary else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) AppColors.primary else Color(0xFF333333)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .background(
                    bgColor,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text.ifEmpty { "• • •" },
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = OutfitFontFamily,
                lineHeight = 20.sp
            )
        }
    }
}
