package com.lenswise.app.features.receipt.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.ai.agent.IScannerAgent
import com.lenswise.app.core.ai.agent.UniversalChatAgent
import com.lenswise.app.core.ai.provider.AiProviderFactory
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.CapturedImageRepository
import com.lenswise.app.core.scanner.BaseScannerViewModel
import com.lenswise.app.core.storage.SettingsPreferencesRepository
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.receipt.agent.ReceiptScannerAgent
import com.lenswise.app.features.receipt.domain.IReceiptScannerAgent
import com.lenswise.app.features.receipt.service.ReceiptScannerService
import kotlinx.coroutines.CoroutineScope

class ReceiptScannerViewModel(
    private val agentFactory: () -> IReceiptScannerAgent,
    service: ReceiptScannerService,
    chatAgentFactory: (() -> UniversalChatAgent)? = null,
    capturedImageRepository: CapturedImageRepository? = null,
    scope: CoroutineScope? = null
) : BaseScannerViewModel<ReceiptScannerRecord>(
    service = service,
    capturedImageRepository = capturedImageRepository,
    chatAgentFactory = chatAgentFactory,
    scope = scope
) {
    override val logTag: String = "ReceiptScannerVM"
    override val featureName: String = "收據"

    override fun createEmptyRecord(): ReceiptScannerRecord = ReceiptScannerRecord.empty()

    override fun resolveAgent(): IScannerAgent<ReceiptScannerRecord> = agentFactory()

    // Secondary constructor for testing
    constructor(
        agent: IReceiptScannerAgent,
        service: ReceiptScannerService,
        capturedImageRepository: CapturedImageRepository? = null,
        scope: CoroutineScope? = null
    ) : this(
        agentFactory = { agent },
        service = service,
        chatAgentFactory = null,
        capturedImageRepository = capturedImageRepository,
        scope = scope
    )

    fun analyzeReceipt(images: List<AppImageEntity>) {
        startAnalysis(images)
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
                            agentType = com.lenswise.app.features.ai.domain.AiAgentType.AI_CHAT,
                            configRepo = app.aiConfigRepository
                        )
                        UniversalChatAgent(
                            provider = provider,
                            logger = app.apiLogManager,
                            defaultSystemPrompt = "你是一個頂級消費與發票分析顧問 AI，請根據提供的發票內容，簡潔、專業、精確地回答問題。"
                        )
                    },
                    capturedImageRepository = app.capturedImageRepository
                )
            }
        }
    }
}
