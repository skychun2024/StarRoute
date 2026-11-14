
package com.example.starroute

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.tts.listener.EventListener

import android.content.Context
import android.os.Handler
import android.os.Looper

import java.util.Locale

/**
 * 별길 Pleos 음성 서비스 관리자
 *
 * 역할:
 * 1. Pleos TTS 초기화
 * 2. 한국어 음성 설정
 * 3. 음성 재생 및 중지
 * 4. 음성 완료 이벤트 전달
 * 5. 서비스 해제
 *
 * 음성 우선순위와 중복 요청은
 * StarRouteSpeechCoordinator가 관리합니다.
 */
class StarRouteVoiceManager(
    context: Context
) {

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val voice = TextToSpeech(
        context.applicationContext,
        Mode.ON_DEVICE
    )

    @Volatile
    private var ready = false

    @Volatile
    private var released = false

    private var initialized = false
    private var listenerRegistered = false

    // 화면에 서비스 상태 전달
    var onStatusChanged: ((String) -> Unit)? = null

    // 음성 완료 또는 오류 이벤트 전달
    var onPlaybackFinished: ((Boolean) -> Unit)? = null

    private fun onMain(action: () -> Unit) {

        mainHandler.post {

            if (!released) {
                action()
            }
        }
    }

    private fun updateStatus(text: String) {

        onMain {
            onStatusChanged?.invoke(text)
        }
    }

    // ========================================
    // Pleos 음성 이벤트
    // ========================================

    private val eventListener =
        object : EventListener {

            override fun onReady() {

                updateStatus(
                    "Pleos 음성 재생 준비 완료"
                )
            }

            override fun onStart() {

                updateStatus(
                    "별길 음성 안내 재생 중..."
                )
            }

            override fun onUpdatedRms(
                rms: Double
            ) = Unit

            override fun onDone() {

                onMain {

                    onStatusChanged?.invoke(
                        "음성 안내 재생이 완료됐습니다."
                    )

                    onPlaybackFinished?.invoke(true)
                }
            }

            override fun onStop() {

                // 수동 중지와 우선순위 교체는
                // Coordinator에서 관리합니다.
            }

            override fun onError(
                errorType: ErrorType
            ) {

                onMain {

                    onStatusChanged?.invoke(
                        "Pleos 음성 재생 오류: $errorType"
                    )

                    onPlaybackFinished?.invoke(false)
                }
            }
        }

    // ========================================
    // TTS 초기화
    // ========================================

    fun initialize() {

        if (released || initialized) return

        initialized = true

        updateStatus(
            "Pleos 음성 서비스 연결 중..."
        )

        try {

            voice.initialize(
                object : OnInitializeListener {

                    override fun onSuccess() {

                        onMain {

                            try {

                                if (!listenerRegistered) {

                                    voice.addEventListener(
                                        eventListener
                                    )

                                    listenerRegistered = true
                                }

                                voice.setLocale(
                                    Locale.KOREA
                                )

                                ready = true

                                updateStatus(
                                    "한국어 음성 서비스 준비 완료"
                                )

                            } catch (e: Exception) {

                                ready = false

                                updateStatus(
                                    "한국어 음성 설정 오류: " +
                                            (
                                                    e.message
                                                        ?: "알 수 없는 오류"
                                                    )
                                )
                            }
                        }
                    }

                    override fun onFailure(
                        errorType: ErrorType
                    ) {

                        ready = false

                        updateStatus(
                            "음성 서비스 연결 실패: $errorType"
                        )
                    }
                }
            )

        } catch (e: Exception) {

            ready = false
            initialized = false

            updateStatus(
                "음성 초기화 오류: " +
                        (e.message ?: "알 수 없는 오류")
            )
        }
    }

    // ========================================
    // 음성 재생
    // ========================================

    /**
     * 새로운 음성을 재생하도록 요청합니다.
     *
     * 중요:
     * 여기서는 voice.stop()을 호출하지 않습니다.
     *
     * 기존 음성 중지 여부는
     * StarRouteSpeechCoordinator에서
     * 우선순위에 따라 결정합니다.
     */
    fun speak(text: String): Boolean {

        if (text.isBlank()) {

            updateStatus(
                "읽을 음성 내용이 없습니다."
            )

            return false
        }

        if (!ready || released) {

            updateStatus(
                "음성 서비스가 아직 준비되지 않았습니다."
            )

            return false
        }

        return try {

            // 불필요한 중복 stop() 호출 제거
            voice.speak(text)

            updateStatus(
                "음성 재생을 요청했습니다."
            )

            true

        } catch (e: Exception) {

            updateStatus(
                "음성 재생 오류: " +
                        (e.message ?: "알 수 없는 오류")
            )

            false
        }
    }

    // ========================================
    // 음성 중지
    // ========================================

    fun stop() {

        if (released) return

        try {

            voice.stop()

            updateStatus(
                "음성 안내를 중지했습니다."
            )

        } catch (e: Exception) {

            updateStatus(
                "음성 중지 오류: " +
                        (e.message ?: "알 수 없는 오류")
            )
        }
    }

    // ========================================
    // 음성 서비스 준비 상태
    // ========================================

    fun isReady(): Boolean {

        return ready && !released
    }

    // ========================================
    // 음성 서비스 연결 해제
    // ========================================

    fun release() {

        if (released) return

        released = true
        ready = false

        onStatusChanged = null
        onPlaybackFinished = null

        try {
            voice.stop()
        } catch (_: Exception) {
        }

        if (listenerRegistered) {

            try {

                voice.removeEventListener(
                    eventListener
                )

            } catch (_: Exception) {
            }

            listenerRegistered = false
        }

        try {
            voice.release()
        } catch (_: Exception) {
        }
    }
}
