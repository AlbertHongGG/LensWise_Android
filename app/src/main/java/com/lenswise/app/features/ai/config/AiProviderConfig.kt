package com.lenswise.app.features.ai.config

import com.lenswise.app.features.ai.domain.AiAgentType
import kotlinx.serialization.Serializable

/**
 * 配置屬性描述器 (ConfigProperty)。
 * 作為配置欄位的單一真實來源 (Single Source of Truth)，
 * 統一封裝欄位鍵名、標準顯示名稱、輸入提示文字與機密屬性。
 * 徹底杜絕雙語括號與硬編碼字串。
 */
data class ConfigProperty(
    val key: String,
    val label: String,
    val placeholder: String,
    val isSecret: Boolean = false
) {
    companion object {
        val API_KEY = ConfigProperty(
            key = "apiKey",
            label = "API Key",
            placeholder = "請輸入 API Key",
            isSecret = true
        )
        val API_KEY_OR_TOKEN = ConfigProperty(
            key = "apiKey",
            label = "API Key / Token",
            placeholder = "請輸入 API Key / Token",
            isSecret = true
        )
        val BASE_URL = ConfigProperty(
            key = "baseUrl",
            label = "Base URL",
            placeholder = "請輸入 Base URL"
        )
        val ENDPOINT_URL = ConfigProperty(
            key = "baseUrl",
            label = "Endpoint URL",
            placeholder = "請輸入 Endpoint URL"
        )
        val PROJECT_ID = ConfigProperty(
            key = "projectId",
            label = "Project ID",
            placeholder = "請輸入 GCP 專案 ID"
        )
        val LOCATION = ConfigProperty(
            key = "location",
            label = "Location",
            placeholder = "請輸入 Location，例如 us-central1"
        )
        val MODEL = ConfigProperty(
            key = "modelName",
            label = "Model",
            placeholder = "請選擇 Model"
        )
        val MODEL_TAG = ConfigProperty(
            key = "modelName",
            label = "Model Tag",
            placeholder = "請輸入 Model Tag"
        )
        val THINKING_LEVEL = ConfigProperty(
            key = "thinkingLevel",
            label = "Thinking Level",
            placeholder = "請選擇 Thinking Level"
        )
    }
}

/**
 * AI 配置缺失領域異常。
 * 當 Agent 與 Global 皆未指定必要參數時拋出，包含 Agent 型別、具體缺失屬性與供應商描述。
 */
class MissingAiConfigException(
    val agentType: AiAgentType,
    val property: ConfigProperty,
    val providerType: AiProviderType? = null
) : IllegalStateException(
    if (providerType != null) {
        "「${agentType.displayName}」執行失敗：未指定 ${property.label}，且全域 ${providerType.displayName} 設定檔亦未提供，請至 AI 代理調度中心設定。"
    } else {
        "「${agentType.displayName}」執行失敗：未指定 ${property.label}，且全域設定檔亦未提供，請至 AI 代理調度中心設定。"
    }
) {
    val fieldKey: String get() = property.key
    val fieldLabel: String get() = property.label
}

/**
 * 設定欄位定義。
 */
sealed class ConfigField(
    val property: ConfigProperty,
    val value: String,
    val hintValue: String? = null
) {
    val key: String get() = property.key
    val label: String get() = property.label
    val placeholder: String get() = property.placeholder

    /**
     * 動態解析 Placeholder 提示文字。
     * 若為全域配置直接使用標準 Placeholder；
     * 若為 Agent 專屬配置且有全域繼承值，則顯示「跟隨全域」；
     * 若全域尚未設定且該欄位為機密憑證（如 API Key），則明確提示「全域尚未設定 ${property.label}」。
     */
    fun resolvePlaceholder(isGlobal: Boolean): String {
        return if (isGlobal) {
            placeholder
        } else {
            if (!hintValue.isNullOrBlank()) {
                "跟隨全域"
            } else if (property.isSecret) {
                "全域尚未設定 ${property.label}"
            } else {
                "跟隨全域"
            }
        }
    }

    class Text(
        property: ConfigProperty,
        value: String,
        hintValue: String? = null
    ) : ConfigField(property, value, hintValue) {
        val isSecret: Boolean get() = property.isSecret
    }

    class Select(
        property: ConfigProperty,
        value: String,
        hintValue: String? = null,
        val options: List<SelectOption>
    ) : ConfigField(property, value, hintValue) {
        val displayLabel: String
            get() {
                val matched = options.firstOrNull { it.value.equals(value, ignoreCase = true) }
                return matched?.label ?: value.ifEmpty { placeholder }
            }
    }
}

data class SelectOption(
    val value: String,
    val label: String,
    val description: String? = null
) {
    companion object {
        fun empty(description: String) = SelectOption(
            value = "",
            label = "未指定",
            description = description
        )
    }
}

/**
 * 思考強度列舉 (GeminiThinkingLevel)。
 * 完全對標參考原型，支援低/中/高與專屬副標描述。
 */
enum class GeminiThinkingLevel(
    val value: String,
    val label: String,
    val description: String
) {
    LOW("low", "低", "極速回應"),
    MEDIUM("medium", "中", "平衡推理"),
    HIGH("high", "高", "極致思維鏈");

    companion object {
        fun fromValue(raw: String?): GeminiThinkingLevel {
            if (raw == null) return LOW
            val lower = raw.trim().lowercase()
            return entries.firstOrNull {
                it.value == lower || it.name.lowercase() == lower || it.label == raw.trim()
            } ?: LOW
        }
    }
}

/**
 * GeminiFlow 模型條目與型錄定義。
 */
data class GeminiFlowModelItem(
    val id: String,
    val displayName: String,
    val description: String? = null
)

object GeminiFlowModel {
    const val FLASH = "gemini-3.8-flash"
    const val PRO = "gemini-3-pro"
    const val ARGON = "gemini4Argon"

    val ITEMS = listOf(
        GeminiFlowModelItem(
            id = FLASH,
            displayName = "Gemini 3.8 Flash",
            description = "極速輕量"
        ),
        GeminiFlowModelItem(
            id = PRO,
            displayName = "Gemini 3 Pro",
            description = "旗艦推理"
        ),
        GeminiFlowModelItem(
            id = ARGON,
            displayName = "Gemini 4 Argon",
            description = "次世代旗艦"
        )
    )

    fun getDisplayName(id: String): String {
        return ITEMS.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }?.displayName ?: id
    }
}

/**
 * AI 供應商配置核心抽象類別。
 */
@Serializable
sealed class AiProviderConfig {
    abstract val modelName: String

    abstract fun getFormFields(fallback: AiProviderConfig? = null): List<ConfigField>
    abstract fun copyWithField(key: String, value: String): AiProviderConfig
    abstract fun fallbackTo(fallback: AiProviderConfig): AiProviderConfig
    abstract fun validateRequiredFields(agentType: AiAgentType, providerType: AiProviderType = AiProviderType.GOOGLE_AI_STUDIO)
}

@Serializable
data class GeminiFlowConfig(
    override val modelName: String = GeminiFlowModel.FLASH,
    val baseUrl: String = "http://127.0.0.1:5000",
    val thinkingLevel: String = GeminiThinkingLevel.LOW.value
) : AiProviderConfig() {

    override fun validateRequiredFields(agentType: AiAgentType, providerType: AiProviderType) {
        if (baseUrl.isBlank()) {
            throw MissingAiConfigException(agentType, ConfigProperty.BASE_URL, providerType)
        }
        if (modelName.isBlank()) {
            throw MissingAiConfigException(agentType, ConfigProperty.MODEL, providerType)
        }
    }

    override fun getFormFields(fallback: AiProviderConfig?): List<ConfigField> {
        val fb = fallback as? GeminiFlowConfig

        val modelOptions = if (fallback != null) {
            listOf(SelectOption(value = "", label = "跟隨全域設定檔", description = "繼承全域設定")) +
                GeminiFlowModel.ITEMS.map {
                    SelectOption(value = it.id, label = it.displayName, description = it.description)
                }
        } else {
            GeminiFlowModel.ITEMS.map {
                SelectOption(value = it.id, label = it.displayName, description = it.description)
            }
        }

        val thinkingOptions = if (fallback != null) {
            listOf(SelectOption(value = "", label = "跟隨全域設定檔", description = "繼承全域設定")) +
                GeminiThinkingLevel.entries.map {
                    SelectOption(value = it.value, label = it.label, description = it.description)
                }
        } else {
            GeminiThinkingLevel.entries.map {
                SelectOption(value = it.value, label = it.label, description = it.description)
            }
        }

        return listOf(
            ConfigField.Select(
                property = ConfigProperty.MODEL,
                value = modelName,
                hintValue = fb?.modelName?.let { GeminiFlowModel.getDisplayName(it) },
                options = modelOptions
            ),
            ConfigField.Text(
                property = ConfigProperty.BASE_URL,
                value = baseUrl,
                hintValue = fb?.baseUrl
            ),
            ConfigField.Select(
                property = ConfigProperty.THINKING_LEVEL,
                value = thinkingLevel,
                hintValue = fb?.thinkingLevel,
                options = thinkingOptions
            )
        )
    }

    override fun copyWithField(key: String, value: String): AiProviderConfig {
        return when (key) {
            "modelName" -> copy(modelName = value)
            "baseUrl" -> copy(baseUrl = value)
            "thinkingLevel" -> copy(thinkingLevel = value)
            else -> this
        }
    }

    override fun fallbackTo(fallback: AiProviderConfig): AiProviderConfig {
        if (fallback !is GeminiFlowConfig) return this
        return GeminiFlowConfig(
            modelName = modelName.ifEmpty { fallback.modelName },
            baseUrl = baseUrl.ifEmpty { fallback.baseUrl },
            thinkingLevel = thinkingLevel.ifEmpty { fallback.thinkingLevel }
        )
    }

    companion object {
        fun empty() = GeminiFlowConfig(modelName = "", baseUrl = "", thinkingLevel = "")
    }
}

@Serializable
data class OllamaConfig(
    override val modelName: String = "llama3.2-vision:latest",
    val baseUrl: String = "http://127.0.0.1:11434"
) : AiProviderConfig() {

    override fun validateRequiredFields(agentType: AiAgentType, providerType: AiProviderType) {
        if (baseUrl.isBlank()) {
            throw MissingAiConfigException(agentType, ConfigProperty.ENDPOINT_URL, providerType)
        }
        if (modelName.isBlank()) {
            throw MissingAiConfigException(agentType, ConfigProperty.MODEL_TAG, providerType)
        }
    }

    override fun getFormFields(fallback: AiProviderConfig?): List<ConfigField> {
        val fb = fallback as? OllamaConfig
        return listOf(
            ConfigField.Text(
                property = ConfigProperty.ENDPOINT_URL,
                value = baseUrl,
                hintValue = fb?.baseUrl
            ),
            ConfigField.Text(
                property = ConfigProperty.MODEL_TAG,
                value = modelName,
                hintValue = fb?.modelName
            )
        )
    }

    override fun copyWithField(key: String, value: String): AiProviderConfig {
        return when (key) {
            "baseUrl" -> copy(baseUrl = value)
            "modelName" -> copy(modelName = value)
            else -> this
        }
    }

    override fun fallbackTo(fallback: AiProviderConfig): AiProviderConfig {
        if (fallback !is OllamaConfig) return this
        return OllamaConfig(
            modelName = modelName.ifEmpty { fallback.modelName },
            baseUrl = baseUrl.ifEmpty { fallback.baseUrl }
        )
    }

    companion object {
        fun empty() = OllamaConfig(modelName = "", baseUrl = "")
    }
}

@Serializable
data class VertexAiConfig(
    override val modelName: String = "gemini-2.5-flash",
    val projectId: String = "",
    val location: String = "us-central1",
    val apiKey: String = ""
) : AiProviderConfig() {

    override fun validateRequiredFields(agentType: AiAgentType, providerType: AiProviderType) {
        if (projectId.isBlank()) {
            throw MissingAiConfigException(agentType, ConfigProperty.PROJECT_ID, providerType)
        }
        if (location.isBlank()) {
            throw MissingAiConfigException(agentType, ConfigProperty.LOCATION, providerType)
        }
        if (apiKey.isBlank()) {
            throw MissingAiConfigException(agentType, ConfigProperty.API_KEY_OR_TOKEN, providerType)
        }
        if (modelName.isBlank()) {
            throw MissingAiConfigException(agentType, ConfigProperty.MODEL, providerType)
        }
    }

    override fun getFormFields(fallback: AiProviderConfig?): List<ConfigField> {
        val fb = fallback as? VertexAiConfig

        val defaultVertexModels = listOf(
            SelectOption("gemini-2.5-flash", "Gemini 2.5 Flash"),
            SelectOption("gemini-2.5-pro", "Gemini 2.5 Pro")
        )
        val modelOptions = if (fallback != null) {
            listOf(SelectOption(value = "", label = "跟隨全域設定檔", description = "繼承全域設定")) + defaultVertexModels
        } else {
            defaultVertexModels
        }

        val apiKeyHint = if (fallback != null) {
            if (!fb?.apiKey.isNullOrEmpty()) "••••••••" else null
        } else {
            null
        }

        return listOf(
            ConfigField.Text(
                property = ConfigProperty.PROJECT_ID,
                value = projectId,
                hintValue = fb?.projectId
            ),
            ConfigField.Text(
                property = ConfigProperty.LOCATION,
                value = location,
                hintValue = fb?.location
            ),
            ConfigField.Text(
                property = ConfigProperty.API_KEY_OR_TOKEN,
                value = apiKey,
                hintValue = apiKeyHint
            ),
            ConfigField.Select(
                property = ConfigProperty.MODEL,
                value = modelName,
                hintValue = fb?.modelName,
                options = modelOptions
            )
        )
    }

    override fun copyWithField(key: String, value: String): AiProviderConfig {
        return when (key) {
            "projectId" -> copy(projectId = value)
            "location" -> copy(location = value)
            "apiKey" -> copy(apiKey = value)
            "modelName" -> copy(modelName = value)
            else -> this
        }
    }

    override fun fallbackTo(fallback: AiProviderConfig): AiProviderConfig {
        if (fallback !is VertexAiConfig) return this
        return VertexAiConfig(
            modelName = modelName.ifEmpty { fallback.modelName },
            projectId = projectId.ifEmpty { fallback.projectId },
            location = location.ifEmpty { fallback.location },
            apiKey = apiKey.ifEmpty { fallback.apiKey }
        )
    }

    companion object {
        fun empty() = VertexAiConfig(modelName = "", projectId = "", location = "", apiKey = "")
    }
}

/**
 * Google AI Studio 模型條目與型錄定義。
 * 嚴格限定 3 款指定模型：
 * 1. Gemini 3.8 Flash (gemini-3.8-flash)
 * 2. Gemini 3.6 Flash (gemini-3.6-flash)
 * 3. Gemini 3.5 Flash Lite (gemini-3.5-flash-lite)
 */
data class GoogleAiStudioModelItem(
    val id: String,
    val displayName: String,
    val description: String? = null
)

object GoogleAiStudioModel {
    const val FLASH_3_8 = "gemini-3.8-flash"
    const val FLASH_3_6 = "gemini-3.6-flash"
    const val FLASH_LITE_3_5 = "gemini-3.5-flash-lite"

    val ITEMS = listOf(
        GoogleAiStudioModelItem(
            id = FLASH_3_8,
            displayName = "Gemini 3.8 Flash",
            description = "最新旗艦極速"
        ),
        GoogleAiStudioModelItem(
            id = FLASH_3_6,
            displayName = "Gemini 3.6 Flash",
            description = "平衡推理"
        ),
        GoogleAiStudioModelItem(
            id = FLASH_LITE_3_5,
            displayName = "Gemini 3.5 Flash Lite",
            description = "超輕量低延遲"
        )
    )

    fun getDisplayName(id: String): String {
        return ITEMS.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }?.displayName ?: id
    }
}

/**
 * Google AI Studio 供應商專屬配置。
 * 透過 API Key 直接連線 Google Generative Language API，無需 GCP 專案 ID 與區域配置。
 */
@Serializable
data class GoogleAiStudioConfig(
    override val modelName: String = GoogleAiStudioModel.FLASH_3_8,
    val apiKey: String = "",
    val thinkingLevel: String = GeminiThinkingLevel.LOW.value
) : AiProviderConfig() {

    override fun validateRequiredFields(agentType: AiAgentType, providerType: AiProviderType) {
        if (apiKey.isBlank()) {
            throw MissingAiConfigException(agentType, ConfigProperty.API_KEY, providerType)
        }
        if (modelName.isBlank()) {
            throw MissingAiConfigException(agentType, ConfigProperty.MODEL, providerType)
        }
    }

    override fun getFormFields(fallback: AiProviderConfig?): List<ConfigField> {
        val fb = fallback as? GoogleAiStudioConfig

        val modelOptions = if (fallback != null) {
            listOf(SelectOption(value = "", label = "跟隨全域設定檔", description = "繼承全域設定")) +
                GoogleAiStudioModel.ITEMS.map {
                    SelectOption(value = it.id, label = it.displayName, description = it.description)
                }
        } else {
            GoogleAiStudioModel.ITEMS.map {
                SelectOption(value = it.id, label = it.displayName, description = it.description)
            }
        }

        val thinkingOptions = if (fallback != null) {
            listOf(SelectOption(value = "", label = "跟隨全域設定檔", description = "繼承全域設定")) +
                GeminiThinkingLevel.entries.map {
                    SelectOption(value = it.value, label = it.label, description = it.description)
                }
        } else {
            GeminiThinkingLevel.entries.map {
                SelectOption(value = it.value, label = it.label, description = it.description)
            }
        }

        val apiKeyHint = if (fallback != null) {
            if (!fb?.apiKey.isNullOrEmpty()) "••••••••" else null
        } else {
            null
        }

        return listOf(
            ConfigField.Text(
                property = ConfigProperty.API_KEY,
                value = apiKey,
                hintValue = apiKeyHint
            ),
            ConfigField.Select(
                property = ConfigProperty.MODEL,
                value = modelName,
                hintValue = fb?.modelName?.let { GoogleAiStudioModel.getDisplayName(it) },
                options = modelOptions
            ),
            ConfigField.Select(
                property = ConfigProperty.THINKING_LEVEL,
                value = thinkingLevel,
                hintValue = fb?.thinkingLevel,
                options = thinkingOptions
            )
        )
    }

    override fun copyWithField(key: String, value: String): AiProviderConfig {
        return when (key) {
            "apiKey" -> copy(apiKey = value)
            "modelName" -> copy(modelName = value)
            "thinkingLevel" -> copy(thinkingLevel = value)
            else -> this
        }
    }

    override fun fallbackTo(fallback: AiProviderConfig): AiProviderConfig {
        if (fallback !is GoogleAiStudioConfig) return this
        return GoogleAiStudioConfig(
            modelName = modelName.ifEmpty { fallback.modelName },
            apiKey = apiKey.ifEmpty { fallback.apiKey },
            thinkingLevel = thinkingLevel.ifEmpty { fallback.thinkingLevel }
        )
    }

    companion object {
        fun empty() = GoogleAiStudioConfig(modelName = "", apiKey = "", thinkingLevel = "")
    }
}
