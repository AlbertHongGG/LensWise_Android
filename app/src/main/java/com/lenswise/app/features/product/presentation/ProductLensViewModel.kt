package com.lenswise.app.features.product.presentation

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
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.features.product.agent.ProductLensAgent
import com.lenswise.app.features.product.domain.IProductLensAgent
import com.lenswise.app.features.product.service.ProductLensService
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

enum class ProductLensStatus {
    IDLE,
    CONNECTING,
    STREAMING,
    SUCCESS,
    ERROR
}

data class ProductLensUiState(
    val status: ProductLensStatus = ProductLensStatus.IDLE,
    val record: ProductLensRecord = ProductLensRecord.empty(),
    val capturedImages: List<AppImageEntity> = emptyList(),
    val isStreamingComplete: Boolean = false,
    val errorMessage: String? = null
) {
    val isAnalyzing: Boolean
        get() = status == ProductLensStatus.CONNECTING || status == ProductLensStatus.STREAMING
}

class ProductLensViewModel(
    private val agentFactory: () -> IProductLensAgent,
    private val service: ProductLensService,
    private val chatAgentFactory: (() -> UniversalChatAgent)? = null,
    private val capturedImageRepository: CapturedImageRepository? = null,
    scope: CoroutineScope? = null
) : ViewModel() {

    // Secondary constructor for unit testing
    constructor(
        agent: IProductLensAgent,
        service: ProductLensService,
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

    private val _uiState = MutableStateFlow(ProductLensUiState())
    val uiState: StateFlow<ProductLensUiState> = _uiState.asStateFlow()

    fun createChatAgent(): UniversalChatAgent? {
        return chatAgentFactory?.invoke()
    }

    private var streamJob: Job? = null

    fun reset() {
        streamJob?.cancel()
        streamJob = null
        _uiState.value = ProductLensUiState()
    }

    fun updateRecord(updated: ProductLensRecord) {
        _uiState.update { it.copy(record = updated) }
    }

    fun updateAndPersistRecord(updated: ProductLensRecord) {
        _uiState.update { it.copy(record = updated) }
        vmScope.launch {
            try {
                service.saveRecordOnly(updated)
            } catch (e: Exception) {
                Log.e("ProductLensVM", "Failed to persist updated record", e)
            }
        }
    }

    fun analyzeProduct(images: List<AppImageEntity>) {
        if (images.isEmpty()) return

        streamJob?.cancel()

        val activeAgent: IProductLensAgent
        try {
            activeAgent = agentFactory()
        } catch (e: Exception) {
            Log.e("ProductLensVM", "Failed to resolve AI Agent provider", e)
            _uiState.update {
                it.copy(
                    status = ProductLensStatus.ERROR,
                    errorMessage = e.localizedMessage ?: "AI 配置無效"
                )
            }
            return
        }

        _uiState.update {
            ProductLensUiState(
                capturedImages = images,
                status = ProductLensStatus.CONNECTING,
                record = ProductLensRecord.empty(),
                isStreamingComplete = false,
                errorMessage = null
            )
        }

        streamJob = vmScope.launch {
            activeAgent.executeStreamSnapshot(images)
                .catch { error ->
                    Log.e("ProductLensVM", "Product analysis failed", error)
                    val rawMsg = error.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: error.message?.takeIf { it.isNotBlank() }
                        ?: error.javaClass.simpleName
                    val displayError = "商品解析失敗：$rawMsg"
                    _uiState.update {
                        it.copy(
                            status = ProductLensStatus.ERROR,
                            errorMessage = displayError
                        )
                    }
                    NotificationController.showError("商品分析失敗: $rawMsg")
                }
                .collect { snapshot ->
                    val isDone = snapshot.isComplete
                    _uiState.update { current ->
                        current.copy(
                            status = if (isDone) ProductLensStatus.SUCCESS else ProductLensStatus.STREAMING,
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
                            Log.e("ProductLensVM", "Failed to save completed record", e)
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
                val service = ProductLensService(app, app.productHistoryRepository)
                ProductLensViewModel(
                    agentFactory = {
                        val provider = AiProviderFactory.createProviderForAgent(
                            agentType = com.lenswise.app.features.ai.domain.AiAgentType.PRODUCT_LENS,
                            configRepo = app.aiConfigRepository
                        )
                        val targetLang = settingsRepo.getTargetLanguage()
                        ProductLensAgent(
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
                            defaultSystemPrompt = "你是一個具備深厚專業知識的頂級商品分析顧問 AI，請根據提供的商品內容與情境，簡潔、專業、精確地回答問題。"
                        )
                    },
                    capturedImageRepository = app.capturedImageRepository
                )
            }
        }
    }
}

