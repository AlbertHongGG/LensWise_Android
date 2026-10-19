package com.lenswise.app.features.ai.config

import com.lenswise.app.features.ai.domain.AiAgentType
import kotlinx.serialization.Serializable

/**
 * AI 配置缺失領域異常。
 * 當 Agent 與 Global 皆未指定必要參數時拋出，包含 Agent 型別與具體缺失欄位名稱。
 */
class MissingAiConfigException(
    val agentType: AiAgentType,
    val fieldKey: String,
    val fieldLabel: String
) : IllegalStateException("「${agentType.displayName}」執行失敗：未指定${fieldLabel}，請至 AI 代理調度中心設定全域或個別參數。")

/**
 * 設定欄位定義。
 */
sealed class ConfigField(
    val key: String,
    val label: String,
    val value: String,
    val hintValue: String? = null
) {
    class Text(
        key: String,
        label: String,
        value: String,
        hintValue: String? = null,
        val isSecret: Boolean = false
    ) : ConfigField(key, label, value, hintValue)

    class Select(
        key: String,
        label: String,
        value: String,
        hintValue: String? = null,
        val options: List<SelectOption>
    ) : ConfigField(key, label, value, hintValue) {
        val displayLabel: String
            get() {
                val matched = options.firstOrNull { it.value.equals(value, ignoreCase = true) }
                return matched?.label ?: value
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
    abstract fun validateRequiredFields(agentType: AiAgentType)
}

@Serializable
data class GeminiFlowConfig(
    override val modelName: String = GeminiFlowModel.FLASH,
    val baseUrl: String = "http://127.0.0.1:5000",
    val thinkingLevel: String = GeminiThinkingLevel.LOW.value
) : AiProviderConfig() {

    override fun validateRequiredFields(agentType: AiAgentType) {
        if (baseUrl.isBlank()) {
            throw MissingAiConfigException(agentType, "baseUrl", "伺服器位址 (Base URL)")
        }
        if (modelName.isBlank()) {
            throw MissingAiConfigException(agentType, "modelName", "模型 (Model)")
        }
    }

    override fun getFormFields(fallback: AiProviderConfig?): List<ConfigField> {
        val fb = fallback as? GeminiFlowConfig

        val modelEmptyDesc = if (fallback != null) "跟隨全域設定" else "未指定"

        val modelOptions = listOf(SelectOption.empty(modelEmptyDesc)) + GeminiFlowModel.ITEMS.map {
            SelectOption(
                value = it.id,
                label = it.displayName,
                description = it.description
            )
        }

        val thinkingEmptyDesc = if (fallback != null) "跟隨全域設定" else "未指定"

        val thinkingOptions = listOf(SelectOption.empty(thinkingEmptyDesc)) + GeminiThinkingLevel.entries.map {
            SelectOption(
                value = it.value,
                label = it.label,
                description = it.description
            )
        }

        return listOf(
            ConfigField.Select(
                key = "modelName",
                label = "模型",
                value = modelName,
                hintValue = fb?.modelName,
                options = modelOptions
            ),
            ConfigField.Text(
                key = "baseUrl",
                label = "Base URL",
                value = baseUrl,
                hintValue = fb?.baseUrl ?: "http://127.0.0.1:5000"
            ),
            ConfigField.Select(
                key = "thinkingLevel",
                label = "思考強度",
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

    override fun validateRequiredFields(agentType: AiAgentType) {
        if (baseUrl.isBlank()) {
            throw MissingAiConfigException(agentType, "baseUrl", "伺服器位址 (Base URL)")
        }
        if (modelName.isBlank()) {
            throw MissingAiConfigException(agentType, "modelName", "模型標籤 (Model Tag)")
        }
    }

    override fun getFormFields(fallback: AiProviderConfig?): List<ConfigField> {
        val fb = fallback as? OllamaConfig
        return listOf(
            ConfigField.Text(
                key = "baseUrl",
                label = "ENDPOINT URL",
                value = baseUrl,
                hintValue = fb?.baseUrl ?: "http://127.0.0.1:11434"
            ),
            ConfigField.Text(
                key = "modelName",
                label = "MODEL TAG",
                value = modelName,
                hintValue = fb?.modelName ?: "llama3.2-vision:latest"
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

    override fun validateRequiredFields(agentType: AiAgentType) {
        if (projectId.isBlank()) {
            throw MissingAiConfigException(agentType, "projectId", "專案 ID (Project ID)")
        }
        if (location.isBlank()) {
            throw MissingAiConfigException(agentType, "location", "區域位置 (Location)")
        }
        if (apiKey.isBlank()) {
            throw MissingAiConfigException(agentType, "apiKey", "API 金鑰 / 權杖 (API Key / Token)")
        }
        if (modelName.isBlank()) {
            throw MissingAiConfigException(agentType, "modelName", "模型 (Model Name)")
        }
    }

    override fun getFormFields(fallback: AiProviderConfig?): List<ConfigField> {
        val fb = fallback as? VertexAiConfig

        val modelEmptyDesc = if (fallback != null) "跟隨全域設定" else "未指定"

        val modelOptions = listOf(SelectOption.empty(modelEmptyDesc)) + listOf(
            SelectOption("gemini-2.5-flash", "Gemini 2.5 Flash"),
            SelectOption("gemini-2.5-pro", "Gemini 2.5 Pro")
        )

        return listOf(
            ConfigField.Text(
                key = "projectId",
                label = "PROJECT ID",
                value = projectId,
                hintValue = fb?.projectId ?: "my-gcp-project"
            ),
            ConfigField.Text(
                key = "location",
                label = "LOCATION",
                value = location,
                hintValue = fb?.location ?: "us-central1"
            ),
            ConfigField.Text(
                key = "apiKey",
                label = "API KEY / TOKEN",
                value = apiKey,
                hintValue = if (!fb?.apiKey.isNullOrEmpty()) "••••••••" else "AIzaSy...",
                isSecret = true
            ),
            ConfigField.Select(
                key = "modelName",
                label = "MODEL NAME",
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
