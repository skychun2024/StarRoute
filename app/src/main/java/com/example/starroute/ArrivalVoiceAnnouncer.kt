
package com.example.starroute

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ========================================
// 앱 전체 공유 음성 관리자
// ========================================

val LocalStarRouteVoiceManager =
    staticCompositionLocalOf<StarRouteVoiceManager?> {
        null
    }

val LocalStarRouteVoiceReady =
    staticCompositionLocalOf {
        false
    }

// ========================================
// 별빛 도착 인사 문구
// ========================================

private fun createStarArrivalGreeting(
    destinationName: String
): String {

    return """
        $destinationName 근처에 도착하고 있어요.

        오늘의 별빛 여행은 어떠셨나요?

        안전한 장소에 주차한 뒤
        밤하늘을 바라보세요.

        반짝이는 별들에는
        오래전부터 전해 내려오는
        특별한 이야기가 숨어 있답니다.

        별길과 함께
        오늘 밤의 별자리를 만나보세요.
    """.trimIndent()
}

// ========================================
// GPS 음성 안내 및 별빛 도착 체험
// ========================================

@Composable
fun ArrivalVoiceAnnouncer(
    destinationName: String,
    distanceKm: Double,
    testMode: Boolean = false
) {

    val voiceManager =
        LocalStarRouteVoiceManager.current

    val voiceReady =
        LocalStarRouteVoiceReady.current

    // MainActivity와 동일한 음성 관리자 공유
    val speechCoordinator = remember(voiceManager) {
        voiceManager?.let {
            createStarRouteSpeechCoordinator(it)
        }
    }

    val arrivalStage =
        getArrivalStage(distanceKm)

    var resetVersion by remember(
        destinationName,
        testMode
    ) {
        mutableIntStateOf(0)
    }

    val tracker = remember(
        destinationName,
        testMode,
        resetVersion
    ) {
        ArrivalAnnouncementTracker()
    }

    var autoVoiceEnabled by remember(
        destinationName,
        testMode
    ) {
        mutableStateOf(true)
    }

    var showGreeting by remember(
        destinationName,
        testMode
    ) {
        mutableStateOf(false)
    }

    var parkedAcknowledged by remember(
        destinationName,
        testMode
    ) {
        mutableStateOf(false)
    }

    var voiceStatus by remember(
        destinationName,
        testMode,
        resetVersion
    ) {
        mutableStateOf(
            "GPS 자동 음성 안내 준비 중..."
        )
    }

    var soundStatus by remember(
        destinationName,
        testMode
    ) {
        mutableStateOf("")
    }

    val nearDestination =
        arrivalStage == ArrivalStage.WITHIN_2_KM

    val arrivalMessage =
        getArrivalStageMessage(
            destinationName,
            distanceKm
        )

    val greeting = remember(destinationName) {
        createStarArrivalGreeting(destinationName)
    }

    // ========================================
    // 중요 수정 1
    //
    // 화면 진입 시에는 stopAll()을
    // 실행하지 않습니다.
    //
    // 이 카드가 사라지거나 목적지가
    // 바뀔 때 기존 음성을 정리합니다.
    // ========================================

    DisposableEffect(
        speechCoordinator,
        destinationName,
        testMode
    ) {

        onDispose {
            speechCoordinator?.stopAll()
        }
    }

    // ========================================
    // 중요 수정 2
    //
    // GPS 단계별 자동 음성 안내
    // 2km 안내가 10km 안내보다 우선
    // ========================================

    LaunchedEffect(
        tracker,
        arrivalStage,
        speechCoordinator,
        voiceReady,
        autoVoiceEnabled
    ) {

        if (!autoVoiceEnabled) {

            voiceStatus =
                "자동 음성 안내가 꺼져 있습니다."

            return@LaunchedEffect
        }

        val coordinator = speechCoordinator

        if (coordinator == null || !voiceReady) {

            voiceStatus =
                "Pleos 음성 서비스 연결 대기 중"

            return@LaunchedEffect
        }

        val nextStage = when {

            arrivalStage == ArrivalStage.WITHIN_2_KM &&
                    !tracker.hasAnnounced2Km() -> {

                ArrivalStage.WITHIN_2_KM
            }

            arrivalStage == ArrivalStage.WITHIN_10_KM &&
                    !tracker.hasAnnounced10Km() -> {

                ArrivalStage.WITHIN_10_KM
            }

            else -> null
        }

        if (nextStage == null) {

            voiceStatus = when (arrivalStage) {

                ArrivalStage.FAR ->
                    "목적지 10km 접근 안내 대기 중"

                ArrivalStage.WITHIN_10_KM ->
                    "10km 접근 안내 처리 완료"

                ArrivalStage.WITHIN_2_KM ->
                    "2km 도착 안내 처리 완료"

                ArrivalStage.INVALID ->
                    "GPS 거리 정보를 확인해 주세요."
            }

            return@LaunchedEffect
        }

        val speechType = when (nextStage) {

            ArrivalStage.WITHIN_10_KM ->
                StarRouteSpeechType.DISTANCE_10KM

            ArrivalStage.WITHIN_2_KM ->
                StarRouteSpeechType.DISTANCE_2KM

            else -> return@LaunchedEffect
        }

        val speechDistance = when (nextStage) {

            ArrivalStage.WITHIN_10_KM -> 10.0

            ArrivalStage.WITHIN_2_KM -> 2.0

            else -> distanceKm
        }

        val speechMessage =
            getArrivalStageMessage(
                destinationName,
                speechDistance
            )

        val result = coordinator.play(
            type = speechType,
            text = speechMessage,

            requestKey =
                "$destinationName:" +
                        "${nextStage.name}:" +
                        "$resetVersion"
        )

        when (result.status) {

            StarRouteSpeechPlayStatus.REQUESTED,
            StarRouteSpeechPlayStatus.REPLACED -> {

                if (
                    nextStage ==
                    ArrivalStage.WITHIN_2_KM
                ) {

                    // 10km 단계를 건너뛰고
                    // 2km에 들어온 경우도 처리
                    tracker.markRequested(
                        ArrivalStage.WITHIN_10_KM
                    )

                    tracker.markRequested(
                        ArrivalStage.WITHIN_2_KM
                    )

                    voiceStatus =
                        "2km 도착 음성을 요청했습니다."

                } else {

                    tracker.markRequested(
                        ArrivalStage.WITHIN_10_KM
                    )

                    voiceStatus =
                        "10km 접근 음성을 요청했습니다."
                }
            }

            else -> {
                voiceStatus = result.message
            }
        }
    }

    // ========================================
    // 별길 음성 안내 화면
    // ========================================

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF263C63),
            contentColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp),

            verticalArrangement =
                Arrangement.spacedBy(15.dp)
        ) {

            Text(
                text = "✦ 별길 음성 여행 안내",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD782)
            )

            // ----------------------------
            // 자동 안내 설정
            // ----------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text = "자동 음성 안내",
                    fontSize = 19.sp
                )

                Switch(
                    checked = autoVoiceEnabled,

                    onCheckedChange = { enabled ->

                        autoVoiceEnabled = enabled

                        if (!enabled) {
                            speechCoordinator?.stopAll()
                        }

                        voiceStatus = if (enabled) {
                            "자동 음성 안내를 켰습니다."
                        } else {
                            "자동 음성 안내를 껐습니다."
                        }
                    }
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f)
            )

            // ----------------------------
            // 현재 GPS 단계
            // ----------------------------

            Text(
                text = "현재 여행 단계",
                fontSize = 18.sp,
                color = Color.LightGray
            )

            Text(
                text = when (arrivalStage) {

                    ArrivalStage.FAR ->
                        "별빛 여행 중 · 10km 초과"

                    ArrivalStage.WITHIN_10_KM ->
                        "별빛 명소 접근 중 · 10km 이내"

                    ArrivalStage.WITHIN_2_KM ->
                        "별빛 명소 도착 구간 · 2km 이내"

                    ArrivalStage.INVALID ->
                        "위치 정보 확인 필요"
                },

                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD782)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),

                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF365981),
                    contentColor = Color.White
                )
            ) {

                Column(
                    modifier = Modifier.padding(17.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(9.dp)
                ) {

                    Text(
                        text = "접근 안내 문구",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD782)
                    )

                    Text(
                        text = arrivalMessage,
                        fontSize = 18.sp,
                        lineHeight = 28.sp
                    )
                }
            }

            Text(
                text = voiceStatus,
                fontSize = 17.sp,
                lineHeight = 26.sp
            )

            Text(
                text = if (voiceReady) {
                    "Pleos 음성 서비스: 준비 완료"
                } else {
                    "Pleos 음성 서비스: 연결 대기 중"
                },

                fontSize = 16.sp,

                color = if (voiceReady) {
                    Color(0xFF9FE3B2)
                } else {
                    Color.LightGray
                }
            )

            // ----------------------------
            // GPS 접근 안내 기록
            // ----------------------------

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f)
            )

            Text(
                text = "접근 안내 기록",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD782)
            )

            Text(
                text = if (tracker.hasAnnounced10Km()) {
                    "10km 접근 안내: 처리됨"
                } else {
                    "10km 접근 안내: 미요청"
                },
                fontSize = 17.sp
            )

            Text(
                text = if (tracker.hasAnnounced2Km()) {
                    "2km 도착 안내: 요청됨"
                } else {
                    "2km 도착 안내: 미요청"
                },
                fontSize = 17.sp
            )

            // ====================================
            // 목적지 2km 이내 도착 화면
            // ====================================

            if (nearDestination) {

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.2f)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(18.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3E365B),
                        contentColor = Color.White
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "✦ 별빛 여행의 마지막 장",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD782)
                        )

                        Text(
                            text =
                                "$destinationName 근처에 " +
                                        "도착하고 있어요.",
                            fontSize = 18.sp
                        )

                        Text(
                            text =
                                "안전한 장소에 주차한 뒤 " +
                                        "별빛 효과음과 " +
                                        "도착 인사를 들어보세요.",
                            fontSize = 18.sp,
                            lineHeight = 27.sp
                        )

                        Button(
                            onClick = {
                                showGreeting = !showGreeting
                            },

                            modifier = Modifier.fillMaxWidth(),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD782),
                                contentColor = Color(0xFF15213D)
                            )
                        ) {

                            Text(
                                text = if (showGreeting) {
                                    "별빛 도착 인사 접기"
                                } else {
                                    "별빛 도착 인사 보기"
                                },
                                fontSize = 18.sp
                            )
                        }

                        if (showGreeting) {

                            Text(
                                text = greeting,
                                fontSize = 19.sp,
                                lineHeight = 29.sp
                            )

                            // --------------------
                            // 수동 주차 확인
                            // --------------------

                            Card(
                                modifier = Modifier.fillMaxWidth(),

                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF263C63),
                                    contentColor = Color.White
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(14.dp),

                                    verticalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {

                                    Text(
                                        text = "안전한 주차 상태 확인",
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD782)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement =
                                            Arrangement.spacedBy(10.dp)
                                    ) {

                                        Checkbox(
                                            checked = parkedAcknowledged,

                                            onCheckedChange = { checked ->
                                                parkedAcknowledged = checked
                                            }
                                        )

                                        Text(
                                            text =
                                                "안전한 장소에 정차하고 " +
                                                        "주차를 완료했습니다.",

                                            fontSize = 17.sp,
                                            lineHeight = 25.sp,

                                            modifier =
                                                Modifier.weight(1f)
                                        )
                                    }

                                    Text(
                                        text =
                                            "개발용 수동 확인입니다. " +
                                                    "실제 차량 기어 상태와 " +
                                                    "연동되지 않습니다.",

                                        fontSize = 15.sp,
                                        color = Color.LightGray,
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            // --------------------
                            // 별빛 효과음 버튼
                            // --------------------

                            OutlinedButton(
                                onClick = {

                                    if (parkedAcknowledged) {

                                        speechCoordinator?.stopAll()

                                        val started =
                                            StarArrivalChime.play()

                                        soundStatus = if (started) {
                                            "별빛 효과음 재생을 요청했습니다."
                                        } else {
                                            "효과음이 재생 중이거나 " +
                                                    "시작하지 못했습니다."
                                        }
                                    }
                                },

                                enabled = parkedAcknowledged,

                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text = "✦ 별빛 도착 효과음 듣기",
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }

                            if (soundStatus.isNotBlank()) {

                                Text(
                                    text = soundStatus,
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFD782)
                                )
                            }

                            // --------------------
                            // 도착 TTS 인사
                            // --------------------

                            Button(
                                onClick = {

                                    if (parkedAcknowledged) {

                                        val coordinator =
                                            speechCoordinator

                                        if (
                                            coordinator == null ||
                                            !voiceReady
                                        ) {

                                            voiceStatus =
                                                "Pleos 음성 서비스가 " +
                                                        "아직 준비되지 않았습니다."

                                        } else {

                                            coordinator.stopAll()

                                            val result =
                                                coordinator.play(
                                                    type =
                                                        StarRouteSpeechType
                                                            .ARRIVAL_GREETING,

                                                    text = greeting,

                                                    requestKey =
                                                        "$destinationName:" +
                                                                "greeting:" +
                                                                System.nanoTime()
                                                )

                                            voiceStatus =
                                                result.message
                                        }
                                    }
                                },

                                enabled = parkedAcknowledged,

                                modifier = Modifier.fillMaxWidth(),

                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB8C7FF),
                                    contentColor = Color(0xFF102047)
                                )
                            ) {

                                Text(
                                    text = "별빛 도착 인사 듣기",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // ====================================
            // 음성 안내 중지
            // ====================================

            OutlinedButton(
                onClick = {

                    speechCoordinator?.stopAll()

                    voiceStatus =
                        "음성 안내 중지를 요청했습니다."
                },

                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "음성 안내 중지",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            // ====================================
            // 개발용 GPS 테스트 초기화
            // ====================================

            if (testMode) {

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.2f)
                )

                Text(
                    text = "개발용 GPS 음성 테스트",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD782)
                )

                OutlinedButton(
                    onClick = {

                        speechCoordinator?.stopAll()
                        resetVersion += 1
                    },

                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = "음성 안내 테스트 기록 초기화",
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }

                Text(
                    text =
                        "초기화하면 현재 거리에서 " +
                                "GPS 안내가 다시 요청될 수 있습니다.",
                    fontSize = 15.sp,
                    color = Color.LightGray
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f)
            )

            Text(
                text =
                    "현재 안내는 GPS 직선거리 기준입니다. " +
                            "도로 주행거리와 다를 수 있습니다. " +
                            "음성 및 효과음의 실제 출력 여부는 " +
                            "시뮬레이터에서 확인해야 합니다.",

                fontSize = 15.sp,
                color = Color.LightGray,
                lineHeight = 23.sp
            )
        }
    }
}
