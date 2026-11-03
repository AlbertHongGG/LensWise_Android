package com.lenswise.app.core.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenswise.app.core.ai.agent.IScannerAgent
import com.lenswise.app.core.ai.agent.UniversalChatAgent
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.CapturedImageRepository
import com.lenswise.app.domain.model.history.IHistoryRecord
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
import java.util.concurrent.atomic.AtomicLong

/**
 * 掃描分析核心視圖模型基礎抽象類別 (BaseScannerViewModel)。
 * 統一管理生命週期、狀態機流轉、串流快照收集、持久化落地以及相片消費清除。
 */
abstract class BaseScannerViewModel<TRecord : IHistoryRecord>(
    protected val service: BaseScannerService<TRecord>,
    protected val capturedImageRepository: CapturedImageRepository? = null,
    protected val chatAgentFactory: (() -> UniversalChatAgent)? = null,
    scope: CoroutineScope? = null
) : ViewModel() {

    abstract val logTag: String
    abstract val featureName: String
    abstract fun createEmptyRecord(): TRecord
    abstract fun resolveAgent(): IScannerAgent<TRecord>

    protected val vmScope = scope ?: runCatching {
        val s = viewModelScope
        Dispatchers.Main.immediate.isDispatchNeeded(kotlin.coroutines.EmptyCoroutineContext)
        s
    }.getOrElse { CoroutineScope(Dispatchers.Default) }

    private val sessionCounter = AtomicLong(0L)

    private val _uiState by lazy {
        MutableStateFlow(
            ScanSessionUiState(
                sessionId = 0L,
                status = ScanAnalysisStatus.IDLE,
                record = createEmptyRecord()
            )
        )
    }
    val uiState: StateFlow<ScanSessionUiState<TRecord>> by lazy { _uiState.asStateFlow() }

    private var streamJob: Job? = null

    fun createChatAgent(): UniversalChatAgent? {
        return chatAgentFactory?.invoke()
    }

    /**
     * 重置所有分析工作與狀態回到乾淨的 IDLE。
     */
    fun reset() {
        streamJob?.cancel()
        streamJob = null
        _uiState.value = ScanSessionUiState(
            sessionId = sessionCounter.incrementAndGet(),
            status = ScanAnalysisStatus.IDLE,
            record = createEmptyRecord()
        )
    }

    /**
     * 啟動全新一輪分析：
     * 1. 取消舊有的 streamJob。
     * 2. **同步立即** 更新狀態為 CONNECTING，生成新的 sessionId 與空的 record，徹底隔絕上一輪狀態殘留。
     * 3. 在協程非同步啟動串流快照收集。
     */
    fun startAnalysis(images: List<AppImageEntity>) {
        if (images.isEmpty()) return

        streamJob?.cancel()

        val newSessionId = sessionCounter.incrementAndGet()

        val activeAgent: IScannerAgent<TRecord>
        try {
            activeAgent = resolveAgent()
        } catch (e: Exception) {
            Log.e(logTag, "Failed to resolve AI Agent provider", e)
            _uiState.update {
                it.copy(
                    sessionId = newSessionId,
                    status = ScanAnalysisStatus.ERROR,
                    errorMessage = e.localizedMessage ?: "AI 配置無效"
                )
            }
            return
        }

        // 同步狀態重置，第一幀進入 ProcessingScreen 時保證為 CONNECTING 且 record 為空
        _uiState.value = ScanSessionUiState(
            sessionId = newSessionId,
            status = ScanAnalysisStatus.CONNECTING,
            record = createEmptyRecord(),
            capturedImages = images,
            isStreamingComplete = false,
            errorMessage = null
        )

        streamJob = vmScope.launch {
            activeAgent.executeStreamSnapshot(images)
                .catch { error ->
                    Log.e(logTag, "$featureName analysis failed", error)
                    val rawMsg = error.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: error.message?.takeIf { it.isNotBlank() }
                        ?: error.javaClass.simpleName
                    val displayError = "${featureName}解析失敗：$rawMsg"
                    _uiState.update {
                        if (it.sessionId == newSessionId) {
                            it.copy(
                                status = ScanAnalysisStatus.ERROR,
                                errorMessage = displayError
                            )
                        } else it
                    }
                    NotificationController.showError("${featureName}分析失敗: $rawMsg")
                }
                .collect { snapshot ->
                    val isDone = snapshot.isComplete
                    _uiState.update { current ->
                        if (current.sessionId == newSessionId) {
                            current.copy(
                                status = if (isDone) ScanAnalysisStatus.SUCCESS else ScanAnalysisStatus.STREAMING,
                                record = snapshot.data,
                                isStreamingComplete = isDone
                            )
                        } else current
                    }

                    if (isDone) {
                        try {
                            val saved = service.saveCompletedRecord(snapshot.data, images)
                            _uiState.update { current ->
                                if (current.sessionId == newSessionId) {
                                    current.copy(record = saved)
                                } else current
                            }
                            // 成功執行完成且持久化落地後，安全精確清除已消費之相片
                            capturedImageRepository?.clearConsumed(images)
                        } catch (e: Exception) {
                            Log.e(logTag, "Failed to save completed record", e)
                        }
                    }
                }
        }
    }

    /**
     * 重試當前相片分析流程。
     */
    fun retryAnalysis() {
        val currentImages = _uiState.value.capturedImages
        if (currentImages.isNotEmpty()) {
            startAnalysis(currentImages)
        }
    }

    fun updateRecord(updated: TRecord) {
        _uiState.update { it.copy(record = updated) }
    }

    fun updateAndPersistRecord(updated: TRecord) {
        _uiState.update { it.copy(record = updated) }
        vmScope.launch {
            try {
                service.saveRecordOnly(updated)
            } catch (e: Exception) {
                Log.e(logTag, "Failed to persist updated record", e)
            }
        }
    }
}
