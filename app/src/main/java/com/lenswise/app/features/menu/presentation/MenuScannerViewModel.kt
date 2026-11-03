package com.lenswise.app.features.menu.presentation

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
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.features.menu.agent.MenuScannerAgent
import com.lenswise.app.features.menu.domain.IMenuScannerAgent
import com.lenswise.app.features.menu.service.MenuScannerService
import kotlinx.coroutines.CoroutineScope

class MenuScannerViewModel(
    private val agentFactory: () -> IMenuScannerAgent,
    service: MenuScannerService,
    chatAgentFactory: (() -> UniversalChatAgent)? = null,
    capturedImageRepository: CapturedImageRepository? = null,
    scope: CoroutineScope? = null
) : BaseScannerViewModel<MenuScannerRecord>(
    service = service,
    capturedImageRepository = capturedImageRepository,
    chatAgentFactory = chatAgentFactory,
    scope = scope
) {
    override val logTag: String = "MenuScannerVM"
    override val featureName: String = "菜單"

    override fun createEmptyRecord(): MenuScannerRecord = MenuScannerRecord.empty()

    override fun resolveAgent(): IScannerAgent<MenuScannerRecord> = agentFactory()

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

    fun analyzeMenu(images: List<AppImageEntity>) {
        startAnalysis(images)
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
