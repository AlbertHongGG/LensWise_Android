package com.lenswise.app.features.comparison.presentation

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
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.features.comparison.agent.ProductComparisonAgent
import com.lenswise.app.features.comparison.domain.IProductComparisonAgent
import com.lenswise.app.features.comparison.service.ProductComparisonService
import kotlinx.coroutines.CoroutineScope

class ProductComparisonViewModel(
    private val agentFactory: () -> IProductComparisonAgent,
    service: ProductComparisonService,
    chatAgentFactory: (() -> UniversalChatAgent)? = null,
    capturedImageRepository: CapturedImageRepository? = null,
    scope: CoroutineScope? = null
) : BaseScannerViewModel<ProductComparisonRecord>(
    service = service,
    capturedImageRepository = capturedImageRepository,
    chatAgentFactory = chatAgentFactory,
    scope = scope
) {
    override val logTag: String = "ComparisonVM"
    override val featureName: String = "商品比對"

    override fun createEmptyRecord(): ProductComparisonRecord = ProductComparisonRecord.empty()

    override fun resolveAgent(): IScannerAgent<ProductComparisonRecord> = agentFactory()

    // Secondary constructor for testing
    constructor(
        agent: IProductComparisonAgent,
        service: ProductComparisonService,
        capturedImageRepository: CapturedImageRepository? = null,
        scope: CoroutineScope? = null
    ) : this(
        agentFactory = { agent },
        service = service,
        chatAgentFactory = null,
        capturedImageRepository = capturedImageRepository,
        scope = scope
    )

    fun analyzeComparison(images: List<AppImageEntity>) {
        startAnalysis(images)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LensWiseApplication
                val settingsRepo = SettingsPreferencesRepository(app)
                val service = ProductComparisonService(app, app.comparisonHistoryRepository)
                ProductComparisonViewModel(
                    agentFactory = {
                        val provider = AiProviderFactory.createProviderForAgent(
                            agentType = com.lenswise.app.features.ai.domain.AiAgentType.PRODUCT_COMPARISON,
                            configRepo = app.aiConfigRepository
                        )
                        val targetLang = settingsRepo.getTargetLanguage()
                        ProductComparisonAgent(
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
                            defaultSystemPrompt = "你是一個具備深厚專業知識的頂級商品分析與選購顧問 AI，請根據提供的多商品比對內容，簡潔、專業、精確地回答問題。"
                        )
                    },
                    capturedImageRepository = app.capturedImageRepository
                )
            }
        }
    }
}
