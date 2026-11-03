package com.lenswise.app.features.product.presentation

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
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.features.product.agent.ProductLensAgent
import com.lenswise.app.features.product.domain.IProductLensAgent
import com.lenswise.app.features.product.service.ProductLensService
import kotlinx.coroutines.CoroutineScope

class ProductLensViewModel(
    private val agentFactory: () -> IProductLensAgent,
    service: ProductLensService,
    chatAgentFactory: (() -> UniversalChatAgent)? = null,
    capturedImageRepository: CapturedImageRepository? = null,
    scope: CoroutineScope? = null
) : BaseScannerViewModel<ProductLensRecord>(
    service = service,
    capturedImageRepository = capturedImageRepository,
    chatAgentFactory = chatAgentFactory,
    scope = scope
) {
    override val logTag: String = "ProductLensVM"
    override val featureName: String = "商品"

    override fun createEmptyRecord(): ProductLensRecord = ProductLensRecord.empty()

    override fun resolveAgent(): IScannerAgent<ProductLensRecord> = agentFactory()

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

    fun analyzeProduct(images: List<AppImageEntity>) {
        startAnalysis(images)
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
