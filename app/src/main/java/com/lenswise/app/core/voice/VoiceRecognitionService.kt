package com.lenswise.app.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * 語音辨識狀態。
 */
data class VoiceState(
    val isListening: Boolean = false,
    val rmsDb: Float = 0f,
    val error: String? = null
)

/**
 * 語音辨識介面 (IVoiceRecognitionService)。
 * 1:1 對齊 LensWise Flutter 版 i_voice_recognition_service.dart 抽象規範。
 */
interface IVoiceRecognitionService {
    val voiceState: StateFlow<VoiceState>
    fun startListening(onResult: (text: String, isFinal: Boolean) -> Unit)
    fun stopListening()
    fun cancel()
    fun destroy()
}

/**
 * Android 原生語音辨識實作 (AndroidVoiceRecognitionService)。
 * 基於 Android 內建 SpeechRecognizer 引擎，支援繁體中文 (zh-TW) 與即時打字流 (Partial Results)。
 */
class AndroidVoiceRecognitionService(
    private val context: Context
) : IVoiceRecognitionService {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var resultCallback: ((text: String, isFinal: Boolean) -> Unit)? = null

    private val _voiceState = MutableStateFlow(VoiceState())
    override val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    init {
        mainHandler.post {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                initRecognizer()
            } else {
                _voiceState.update { it.copy(error = "此裝置未提供原生語音辨識服務") }
            }
        }
    }

    private fun initRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.update { it.copy(isListening = true, error = null) }
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceState.update { it.copy(isListening = true) }
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _voiceState.update { it.copy(rmsDb = rmsdB) }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _voiceState.update { it.copy(isListening = false) }
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "音訊錄製錯誤"
                            SpeechRecognizer.ERROR_CLIENT -> "客戶端呼叫錯誤"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少麥克風錄音權限"
                            SpeechRecognizer.ERROR_NETWORK -> "網路連線異常"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "網路逾時"
                            SpeechRecognizer.ERROR_NO_MATCH -> "未辨識出清晰語音"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "語音引擎忙碌中"
                            SpeechRecognizer.ERROR_SERVER -> "語音伺服器錯誤"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "說話逾時未收到聲音"
                            else -> "語音辨識異常 ($error)"
                        }
                        _voiceState.update { it.copy(isListening = false, error = message) }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            resultCallback?.invoke(text, true)
                        }
                        _voiceState.update { it.copy(isListening = false) }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            resultCallback?.invoke(text, false)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            _voiceState.update { it.copy(error = "無法初始化語音辨識器: ${e.localizedMessage}") }
        }
    }

    override fun startListening(onResult: (text: String, isFinal: Boolean) -> Unit) {
        resultCallback = onResult
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-TW")
                }

                speechRecognizer?.startListening(intent)
                _voiceState.update { it.copy(isListening = true, error = null) }
            } catch (e: Exception) {
                _voiceState.update { it.copy(isListening = false, error = "啟動錄音失敗: ${e.localizedMessage}") }
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore safe teardown
            } finally {
                _voiceState.update { it.copy(isListening = false) }
            }
        }
    }

    override fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                // Ignore safe teardown
            } finally {
                _voiceState.update { it.copy(isListening = false) }
            }
        }
    }

    override fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

/**
 * Compose 專用生命週期感知之語音服務記憶體註冊器。
 */
@Composable
fun rememberVoiceRecognitionService(): IVoiceRecognitionService {
    val context = LocalContext.current
    val service = remember(context) { AndroidVoiceRecognitionService(context) }

    DisposableEffect(service) {
        onDispose {
            service.destroy()
        }
    }

    return service
}
