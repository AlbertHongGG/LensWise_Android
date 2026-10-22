package com.lenswise.app.features.menu.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.ai.agent.UniversalChatAgent
import com.lenswise.app.core.ai.provider.AiProviderFactory
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.CapturedImageRepository
import com.lenswise.app.core.storage.SettingsPreferencesRepository
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.features.menu.agent.MenuScannerAgent
import com.lenswise.app.features.menu.domain.IMenuScannerAgent
import com.lenswise.app.features.menu.service.MenuScannerService
import com.lenswise.app.presentation.notification.NotificationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MenuScannerStatus {
    IDLE,
    CONNECTING,
    STREAMING,
    SUCCESS,
    ERROR
}

data class MenuScannerUiState(
    val status: MenuScannerStatus = MenuScannerStatus.IDLE,
    val record: MenuScannerRecord = MenuScannerRecord.empty(),
    val capturedImages: List<AppImageEntity> = emptyList(),
    val isStreamingComplete: Boolean = false,
    val errorMessage: String? = null
) {
    val isAnalyzing: Boolean
        get() = status == MenuScannerStatus.CONNECTING || status == MenuScannerStatus.STREAMING
}

class MenuScannerViewModel(
    private val agentFactory: () -> IMenuScannerAgent,
    private val service: MenuScannerService,
    private val chatAgentFactory: (() -> UniversalChatAgent)? = null,
    private val capturedImageRepository: CapturedImageRepository? = null,
    scope: CoroutineScope? = null
) : ViewModel() {

    // Secondary constructor for testing
    constructor(
        agent: IMenuScannerAgent,
        service: MenuScannerService,
        capturedImageRepository: CapturedImageRepository? = null,
        scope: CoroutineScope? = null
    ) : this(
        agentFactory = { agent },
        service = service,
        chatAgentFactory = null,
        capturedImageRepository = capturedImageRepository,
        scope = scope
    )

    private val vmScope = scope ?: runCatching {
        val s = viewModelScope
        Dispatchers.Main.immediate.isDispatchNeeded(kotlin.coroutines.EmptyCoroutineContext)
        s
    }.getOrElse { CoroutineScope(Dispatchers.Default) }

    private val _uiState = MutableStateFlow(MenuScannerUiState())
    val uiState: StateFlow<MenuScannerUiState> = _uiState.asStateFlow()

    fun createChatAgent(): UniversalChatAgent? {
        return chatAgentFactory?.invoke()
    }

    private var streamJob: Job? = null

    fun reset() {
        streamJob?.cancel()
        streamJob = null
        _uiState.value = MenuScannerUiState()
    }

    fun updateRecord(updated: MenuScannerRecord) {
        _uiState.update { it.copy(record = updated) }
    }

    fun updateAndPersistRecord(updated: MenuScannerRecord) {
        _uiState.update { it.copy(record = updated) }
        vmScope.launch {
            try {
                service.saveRecordOnly(updated)
            } catch (e: Exception) {
                Log.e("MenuScannerVM", "Failed to persist updated record", e)
            }
        }
    }

    fun analyzeMenu(images: List<AppImageEntity>) {
        if (images.isEmpty()) return

        streamJob?.cancel()

        val activeAgent: IMenuScannerAgent
        try {
            activeAgent = agentFactory()
        } catch (e: Exception) {
            Log.e("MenuScannerVM", "Failed to resolve AI Agent provider", e)
            _uiState.update {
                it.copy(
                    status = MenuScannerStatus.ERROR,
                    errorMessage = e.localizedMessage ?: "AI 配置無效"
                )
            }
            return
        }

        _uiState.update {
            MenuScannerUiState(
                capturedImages = images,
                status = MenuScannerStatus.CONNECTING,
                record = MenuScannerRecord.empty(),
                isStreamingComplete = false,
                errorMessage = null
            )
        }

        streamJob = vmScope.launch {
            activeAgent.executeStreamSnapshot(images)
                .catch { error ->
                    Log.e("MenuScannerVM", "Menu analysis failed", error)
                    val rawMsg = error.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: error.message?.takeIf { it.isNotBlank() }
                        ?: error.javaClass.simpleName
                    val displayError = "菜單解析失敗：$rawMsg"
                    _uiState.update {
                        it.copy(
                            status = MenuScannerStatus.ERROR,
                            errorMessage = displayError
                        )
                    }
                    NotificationController.showError("菜單分析失敗: $rawMsg")
                }
                .collect { snapshot ->
                    val isDone = snapshot.isComplete
                    _uiState.update { current ->
                        current.copy(
                            status = if (isDone) MenuScannerStatus.SUCCESS else MenuScannerStatus.STREAMING,
                            record = snapshot.data,
                            isStreamingComplete = isDone
                        )
                    }

                    if (isDone) {
                        try {
                            val saved = service.saveCompletedRecord(snapshot.data, images)
                            _uiState.update { it.copy(record = saved) }
                            capturedImageRepository?.clearConsumed(images)
                        } catch (e: Exception) {
                            Log.e("MenuScannerVM", "Failed to save completed record", e)
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
                val service = MenuScannerService(app, app.menuHistoryRepository)
                MenuScannerViewModel(
                    agentFactory = {
                        val provider = AiProviderFactory.createProviderForAgent(
                            agentType = com.lenswise.app.features.ai.domain.AiAgentType.MENU_SCANNER,
                            configRepo = app.aiConfigRepository
                        )
                        val targetLang = settingsRepo.getTargetLanguage()
                        MenuScannerAgent(
                            provider = provider,
                            logger = app.apiLogManager,
                            targetLanguage = targetLang
                        )
                    },
                    service = service,
                    chatAgentFactory = {
                        val provider = AiProviderFactory.createProviderForAgent(
                            agentType = com.lenswise.app.features.ai.domain.AiAgentType.AI_CHAT,
                            configRepo = app.aiConfigRepository
                        )
                        UniversalChatAgent(
                            provider = provider,
                            logger = app.apiLogManager,
                            defaultSystemPrompt = "你是一個具備深厚專業知識的頂級菜單顧問 AI，請根據提供的菜單內容，簡潔、專業、精確地回答問題。"
                        )
                    },
                    capturedImageRepository = app.capturedImageRepository
                )
            }
        }
    }
}

