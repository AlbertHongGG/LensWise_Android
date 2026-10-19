package com.lenswise.app.features.receipt.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.ai.provider.AiProviderFactory
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.storage.SettingsPreferencesRepository
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.ai.agents.receipt.ReceiptScannerAgent
import com.lenswise.app.features.receipt.data.ReceiptHistoryRepository
import com.lenswise.app.features.receipt.domain.IReceiptScannerAgent
import com.lenswise.app.features.receipt.service.ReceiptScannerService
import com.lenswise.app.presentation.notification.NotificationController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReceiptScannerStatus {
    IDLE,
    CONNECTING, // 等待首個 Token (TTFT)
    STREAMING,  // 首個 Token 抵達，進入 ResultPage 漸進展開
    SUCCESS,    // 結束且持久化完成
    ERROR
}

data class ReceiptScannerUiState(
    val status: ReceiptScannerStatus = ReceiptScannerStatus.IDLE,
    val receiptRecord: ReceiptScannerRecord = ReceiptScannerRecord.empty(),
    val capturedImages: List<AppImageEntity> = emptyList(),
    val isStreamingComplete: Boolean = false,
    val errorMessage: String? = null
) {
    val isAnalyzing: Boolean
        get() = status == ReceiptScannerStatus.CONNECTING || status == ReceiptScannerStatus.STREAMING
}

class ReceiptScannerViewModel(
    private val agentFactory: () -> IReceiptScannerAgent,
    private val service: ReceiptScannerService,
    private val chatAgentFactory: (() -> com.lenswise.app.features.ai.agents.receipt.ReceiptChatAgent)? = null
) : ViewModel() {

    // Secondary constructor for testing or static injection
    constructor(agent: IReceiptScannerAgent, service: ReceiptScannerService) : this(
        agentFactory = { agent },
        service = service,
        chatAgentFactory = null
    )

    private val _uiState = MutableStateFlow(ReceiptScannerUiState())
    val uiState: StateFlow<ReceiptScannerUiState> = _uiState.asStateFlow()

    fun createChatAgent(): com.lenswise.app.features.ai.agents.receipt.ReceiptChatAgent? {
        return chatAgentFactory?.invoke()
    }

    private var streamJob: Job? = null

    fun reset() {
        streamJob?.cancel()
        streamJob = null
        _uiState.value = ReceiptScannerUiState()
    }

    fun updateRecord(updated: ReceiptScannerRecord) {
        _uiState.update { it.copy(receiptRecord = updated) }
    }

    fun analyzeReceipt(images: List<AppImageEntity>) {
        if (images.isEmpty()) return

        streamJob?.cancel()

        val activeAgent: IReceiptScannerAgent
        try {
            activeAgent = agentFactory()
        } catch (e: Exception) {
            Log.e("ReceiptScannerVM", "Failed to resolve AI Agent provider", e)
            _uiState.update {
                it.copy(
                    status = ReceiptScannerStatus.ERROR,
                    errorMessage = e.localizedMessage ?: "AI 配置無效"
                )
            }
            return
        }

        _uiState.update {
            ReceiptScannerUiState(
                capturedImages = images,
                status = ReceiptScannerStatus.CONNECTING,
                receiptRecord = ReceiptScannerRecord.empty(),
                isStreamingComplete = false,
                errorMessage = null
            )
        }

        streamJob = viewModelScope.launch {
            activeAgent.executeStreamSnapshot(images)
                .catch { error ->
                    Log.e("ReceiptScannerVM", "Receipt analysis failed", error)
                    _uiState.update {
                        it.copy(
                            status = ReceiptScannerStatus.ERROR,
                            errorMessage = error.localizedMessage ?: "收據解析過程發生異常"
                        )
                    }
                    NotificationController.showError("收據分析失敗: ${error.localizedMessage}")
                }
                .collect { snapshot ->
                    val isDone = snapshot.isComplete
                    _uiState.update { current ->
                        current.copy(
                            status = if (isDone) ReceiptScannerStatus.SUCCESS else ReceiptScannerStatus.STREAMING,
                            receiptRecord = snapshot.data,
                            isStreamingComplete = isDone
                        )
                    }

                    if (isDone) {
                        try {
                            val saved = service.saveCompletedRecord(snapshot.data, images)
                            _uiState.update { it.copy(receiptRecord = saved) }
                        } catch (e: Exception) {
                            Log.e("ReceiptScannerVM", "Failed to save completed record", e)
                        }
                    }
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LensWiseApplication
                val settingsRepo = SettingsPreferencesRepository(app)
                val service = ReceiptScannerService(app, app.receiptHistoryRepository)
                ReceiptScannerViewModel(
                    agentFactory = {
                        val provider = AiProviderFactory.createProviderForAgent(
                            agentType = com.lenswise.app.features.ai.domain.AiAgentType.RECEIPT_SCANNER,
                            configRepo = app.aiConfigRepository
                        )
                        val targetLang = settingsRepo.getTargetLanguage()
                        ReceiptScannerAgent(
                            provider = provider,
                            logger = app.apiLogManager,
                            targetLanguage = targetLang
                        )
                    },
                    service = service,
                    chatAgentFactory = {
                        val provider = AiProviderFactory.createProviderForAgent(
                            agentType = com.lenswise.app.features.ai.domain.AiAgentType.RECEIPT_CHAT,
                            configRepo = app.aiConfigRepository
                        )
                        com.lenswise.app.features.ai.agents.receipt.ReceiptChatAgent(
                            provider = provider,
                            logger = app.apiLogManager
                        )
                    }
                )
            }
        }
    }
}
