
package com.example.starroute

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

import java.util.concurrent.atomic.AtomicBoolean

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * 별길 전용 도착 효과음
 *
 * 변경 사항:
 * 1. 일반 미디어 오디오 경로 사용
 * 2. 44100Hz PCM 출력
 * 3. 스트리밍 재생 방식 사용
 * 4. 효과음 볼륨 개선
 * 5. 중복 재생 방지
 * 6. 재생 오류 Logcat 기록
 *
 * 별도의 음원 파일 없이
 * 세 개의 음을 직접 생성합니다.
 */
object StarArrivalChime {

    private const val TAG = "StarArrivalChime"

    private const val SAMPLE_RATE = 44100

    private val playing =
        AtomicBoolean(false)

    /**
     * 효과음 재생
     *
     * true:
     * 재생 작업을 시작했습니다.
     *
     * false:
     * 이미 재생 중이거나
     * 작업을 시작하지 못했습니다.
     *
     * 실제 출력 성공은
     * Logcat과 소리로 확인해야 합니다.
     */
    fun play(): Boolean {

        if (!playing.compareAndSet(false, true)) {

            Log.d(
                TAG,
                "효과음이 이미 재생 중입니다."
            )

            return false
        }

        return try {

            Thread(
                {
                    playInternal()
                },
                "StarRouteArrivalChime"
            ).apply {
                isDaemon = true
                start()
            }

            true

        } catch (e: Exception) {

            playing.set(false)

            Log.e(
                TAG,
                "효과음 스레드 시작 실패",
                e
            )

            false
        }
    }

    /**
     * 실제 오디오 출력 처리
     */
    private fun playInternal() {

        var audioTrack: AudioTrack? = null

        try {

            val samples = createSound()

            val minBufferSize =
                AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

            check(minBufferSize > 0) {
                "오디오 버퍼 크기를 가져오지 못했습니다."
            }

            val bufferSize = max(
                minBufferSize,
                SAMPLE_RATE / 4 * 2
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            AudioAttributes.USAGE_MEDIA
                        )
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_MUSIC
                        )
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(
                            AudioFormat.ENCODING_PCM_16BIT
                        )
                        .setSampleRate(
                            SAMPLE_RATE
                        )
                        .setChannelMask(
                            AudioFormat.CHANNEL_OUT_MONO
                        )
                        .build()
                )
                .setBufferSizeInBytes(
                    bufferSize
                )
                .setTransferMode(
                    AudioTrack.MODE_STREAM
                )
                .build()

            audioTrack = track

            check(
                track.state ==
                        AudioTrack.STATE_INITIALIZED
            ) {
                "AudioTrack 초기화에 실패했습니다."
            }

            track.setVolume(1.0f)

            Log.d(
                TAG,
                "효과음 오디오 초기화 성공"
            )

            // 스트리밍 재생 시작
            track.play()

            check(
                track.playState ==
                        AudioTrack.PLAYSTATE_PLAYING
            ) {
                "AudioTrack 재생 시작에 실패했습니다."
            }

            var offset = 0

            while (offset < samples.size) {

                val remaining =
                    samples.size - offset

                val written = track.write(
                    samples,
                    offset,
                    remaining,
                    AudioTrack.WRITE_BLOCKING
                )

                check(written > 0) {
                    "오디오 데이터 기록 실패: $written"
                }

                offset += written
            }

            Log.d(
                TAG,
                "효과음 데이터 전송 완료: " +
                        "${samples.size} samples"
            )

            // 출력 버퍼의 소리가 끝날 때까지
            // 재생 객체 유지
            val durationMillis =
                samples.size * 1000L / SAMPLE_RATE

            Thread.sleep(
                durationMillis + 250L
            )

            Log.d(
                TAG,
                "별빛 효과음 재생 처리 완료"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "별빛 효과음 재생 오류",
                e
            )

        } finally {

            try {
                audioTrack?.stop()
            } catch (_: Exception) {
            }

            try {
                audioTrack?.release()
            } catch (_: Exception) {
            }

            playing.set(false)
        }
    }

    /**
     * 별길 전용 3음 효과음
     *
     * 부드럽게 올라가는 세 음을
     * 차례대로 재생합니다.
     */
    private fun createSound(): ShortArray {

        val notes = doubleArrayOf(
            523.25,
            659.25,
            783.99
        )

        val noteDuration = 0.30

        val gapDuration = 0.04

        val noteSamples =
            (SAMPLE_RATE * noteDuration).toInt()

        val gapSamples =
            (SAMPLE_RATE * gapDuration).toInt()

        val totalSamples =
            notes.size *
                    (noteSamples + gapSamples)

        val result =
            ShortArray(totalSamples)

        var position = 0

        for (frequency in notes) {

            for (i in 0 until noteSamples) {

                val progress =
                    i.toDouble() / noteSamples

                // 음의 처음과 끝을
                // 부드럽게 조절
                val envelope =
                    sin(PI * progress).let {
                        it * it
                    }

                val fundamental =
                    sin(
                        2.0 * PI * frequency *
                                i / SAMPLE_RATE
                    )

                val harmonic =
                    sin(
                        2.0 * PI *
                                frequency * 2.0 *
                                i / SAMPLE_RATE
                    )

                val wave =
                    0.85 * fundamental +
                            0.15 * harmonic

                val amplitude =
                    0.45 * envelope * wave

                val sample =
                    (amplitude * Short.MAX_VALUE)
                        .toInt()
                        .coerceIn(
                            Short.MIN_VALUE.toInt(),
                            Short.MAX_VALUE.toInt()
                        )

                result[position++] =
                    sample.toShort()
            }

            position += gapSamples
        }

        return result
    }
}
