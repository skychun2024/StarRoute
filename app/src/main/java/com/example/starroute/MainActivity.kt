
package com.example.starroute

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.starroute.ui.theme.StarRouteTheme

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ========================================
// 별길 디자인 색상
// ========================================

private val NightBackground = Color(0xFF101216)
private val NightCard = Color(0xFF1C2027)
private val NightCardLight = Color(0xFF252B34)
private val StarGold = Color(0xFF8FC8FF)
private val StarWhite = Color(0xFFF3F5FF)
private val StarGray = Color(0xFFBCC5D0)
private val StarGreen = Color(0xFF9FE3B2)
private val StarRed = Color(0xFFFFB8AE)

// ========================================
// 별자리 정보
// ========================================

// ========================================
// 별빛도감 저장 및 불러오기
// ========================================

private const val RECORDS_KEY =
    "observation_records_v1"

private fun readRecords(
    preferences: SharedPreferences
): List<StarDiaryEntry> {

    val saved = try {
        preferences.getStringSet(
            RECORDS_KEY,
            emptySet()
        ).orEmpty().toSet()
    } catch (_: Exception) {
        emptySet()
    }

    return saved.mapNotNull { value ->
        runCatching {
            val json = JSONObject(value)

            StarDiaryEntry(
                starId = json.getString("starId"),
                spot = json.getString("spot"),
                day = json.getString("day"),
                time = json.getLong("time"),
                note = json.optString("note", "")
            )
        }.getOrNull()
    }.sortedByDescending { it.time }
}

private fun saveRecords(
    preferences: SharedPreferences,
    records: List<StarDiaryEntry>
): Boolean {

    return try {
        val values = records.map { record ->
            JSONObject()
                .put("starId", record.starId)
                .put("spot", record.spot)
                .put("day", record.day)
                .put("time", record.time)
                .put("note", record.note)
                .toString()
        }.toSet()

        preferences.edit()
            .putStringSet(RECORDS_KEY, values)
            .commit()

    } catch (_: Exception) {
        false
    }
}

// ========================================
// MainActivity
// ========================================

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            StarRouteTheme {
                StarRouteApp()
            }
        }
    }
}

// ========================================
// 별길 메인 화면
// ========================================

@Composable
fun StarRouteApp() {

    val context = LocalContext.current

    val preferences = remember(context) {
        context.applicationContext.getSharedPreferences(
            "starroute_collection",
            Context.MODE_PRIVATE
        )
    }

    var records by remember {
        mutableStateOf(readRecords(preferences))
    }

    var screen by rememberSaveable {
        mutableStateOf("home")
    }

    var selectedSpot by rememberSaveable {
        mutableStateOf("")
    }

    var selectedStarId by rememberSaveable {
        mutableStateOf("orion")
    }

    var message by rememberSaveable {
        mutableStateOf("")
    }

    var demoParked by rememberSaveable {
        mutableStateOf(false)
    }

    var observationNote by rememberSaveable(
        selectedSpot,
        selectedStarId
    ) {
        mutableStateOf("")
    }

    var pendingDeleteRecord by remember {
        mutableStateOf<StarDiaryEntry?>(null)
    }

    // ====================================
    // 관측실 안전 조건
    // ====================================

    val observationMode = ObservationMode.DEMO

    val accessResult = checkObservationAccess(
        mode = observationMode,
        demoParked = demoParked
    )

    // ====================================
    // Pleos 음성 관리자
    // ====================================

    val voiceManager = remember(context) {
        StarRouteVoiceManager(
            context.applicationContext
        )
    }

    val speechCoordinator = remember(voiceManager) {
        createStarRouteSpeechCoordinator(
            voiceManager
        )
    }

    var voiceReady by remember {
        mutableStateOf(false)
    }

    var voiceStatus by remember {
        mutableStateOf(
            "Pleos 음성 서비스 연결 준비 중..."
        )
    }

    DisposableEffect(
        voiceManager,
        speechCoordinator
    ) {

        voiceManager.onStatusChanged = { status ->
            voiceStatus = status
            voiceReady = voiceManager.isReady()
        }

        voiceManager.initialize()

        onDispose {
            speechCoordinator.stopAll()
            voiceManager.onStatusChanged = null
            voiceManager.release()
        }
    }

    val selectedStar =
        starCatalog.firstOrNull {
            it.id == selectedStarId
        } ?: starCatalog.first()

    val collectedCount = countCollectedStars(
        records.filter { record ->
            starCatalog.any {
                it.id == record.starId
            }
        }
    )

    // ====================================
    // 화면 전환 시 음성 중지
    //
    // 중요:
    // 첫 화면이 실행될 때는
    // stopAll()을 호출하지 않습니다.
    // ====================================

    var previousAudioScreen by remember {
        mutableStateOf<String?>(null)
    }

    var previousAudioStarId by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(
        screen,
        selectedStarId,
        accessResult.allowed
    ) {

        val screenChanged =
            previousAudioScreen != null &&
                    previousAudioScreen != screen

        val starChanged =
            previousAudioStarId != null &&
                    previousAudioStarId != selectedStarId

        if (screenChanged || starChanged) {
            speechCoordinator.stopAll()
        }

        previousAudioScreen = screen
        previousAudioStarId = selectedStarId

        if (
            screen == "observe" &&
            !accessResult.allowed
        ) {
            speechCoordinator.stopAll()

            screen = "trip"

            message =
                "관측실 이용 조건이 해제되어 " +
                        "관측 모드를 종료했습니다."
        }
    }

    // ====================================
    // 도감 기록 삭제 확인
    // ====================================

    val recordToDelete = pendingDeleteRecord

    if (recordToDelete != null) {

        val starName =
            starCatalog.firstOrNull {
                it.id == recordToDelete.starId
            }?.name ?: "별자리"

        AlertDialog(
            onDismissRequest = {
                pendingDeleteRecord = null
            },

            title = {
                Text("체험 기록을 삭제할까요?")
            },

            text = {
                Text(
                    "$starName\n" +
                            "${recordToDelete.day} · " +
                            "${recordToDelete.spot}\n\n" +
                            "삭제한 기록과 여행 메모는 " +
                            "복구할 수 없습니다."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        val result = deleteStarDiaryEntry(
                            existing = records,
                            target = recordToDelete,
                            confirmed = true
                        )

                        if (!result.deleted) {
                            message =
                                "삭제할 기록을 찾지 못했습니다."
                        } else {
                            val saved = saveRecords(
                                preferences,
                                result.records
                            )

                            if (saved) {
                                records = result.records
                                message =
                                    "선택한 체험 기록을 삭제했습니다."
                            } else {
                                message =
                                    "기록 삭제에 실패했습니다."
                            }
                        }

                        pendingDeleteRecord = null
                    }
                ) {
                    Text(
                        "삭제하기",
                        color = Color(0xFFB3261E)
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDeleteRecord = null
                    }
                ) {
                    Text("취소")
                }
            },

            containerColor = Color(0xFFF5F5FA),
            titleContentColor = Color(0xFF17213A),
            textContentColor = Color(0xFF29354B)
        )
    }

    // ====================================
    // 앱 전체 음성 관리자 공유
    // ====================================

    CompositionLocalProvider(
        LocalStarRouteVoiceManager provides voiceManager,
        LocalStarRouteVoiceReady provides voiceReady
    ) {

        Surface(
            modifier = Modifier.fillMaxSize(),

            color = if (screen == "observe") {
                Color(0xFF14171D)
            } else {
                NightBackground
            },

            contentColor = StarWhite
        ) {

            key(screen) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(20.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(18.dp)
                ) {

                    StarRouteHeader()

                    // ==========================
                    // 상단 메뉴
                    // ==========================

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = screen == "home",
                            onClick = { screen = "home"; message = "" },
                            label = { Text("오늘의 별길", color = StarWhite) }
                        )
                        FilterChip(
                            selected = screen == "collection",
                            onClick = { screen = "collection"; message = "" },
                            label = { Text("별빛도감 $collectedCount/${starCatalog.size}", color = StarWhite) }
                        )
                    }

                    HorizontalDivider(
                        color = StarGray.copy(
                            alpha = 0.3f
                        )
                    )

                    when (screen) {

                        // ======================
                        // 메인 화면
                        // ======================

                        "home" -> {

                            StarSectionTitle(
                                title =
                                    "오늘 밤, 별 보러 떠나요",

                                subtitle =
                                    "현재 위치에서 가까운 " +
                                            "별 관측 명소를 찾아보세요."
                            )

                            GpsPanel(
                                onStartExperience = { spotName ->
                                    selectedSpot = spotName
                                    demoParked = false
                                    message = ""
                                    screen = "trip"
                                }
                            )

                            MoonPhasePanel()

                            StarSectionTitle(
                                title = "별자리 이야기 체험",
                                subtitle =
                                    "신화 이야기를 듣고 " +
                                            "별빛도감을 채워보세요."
                            )

                            StarRouteCard {

                                Text(
                                    "✦ 차 안 별자리 관측실",
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StarGold
                                )

                                Text(
                                    "별자리 ${starCatalog.size}종의 음성 이야기",
                                    fontSize = 18.sp,
                                    color = StarGray
                                )

                                Text(
                                    "별자리 이야기를 듣고 " +
                                            "오늘의 여행 메모를 남길 수 있습니다.",
                                    fontSize = 19.sp,
                                    lineHeight = 29.sp
                                )

                                StarRouteButton(
                                    label =
                                        "별자리 이야기 체험하기",

                                    onClick = {
                                        selectedSpot =
                                            "별자리 이야기 시연"

                                        demoParked = false
                                        message = ""
                                        screen = "trip"
                                    }
                                )
                            }
                        }

                        // ======================
                        // 안전 확인 화면
                        // ======================

                        "trip" -> {

                            StarSectionTitle(
                                title = "별자리 이야기 여행",
                                subtitle =
                                    "안전 상태를 확인한 뒤 " +
                                            "별자리 관측실로 이동합니다."
                            )

                            StarRouteCard {

                                Text(
                                    "선택한 장소",
                                    color = StarGray,
                                    fontSize = 18.sp
                                )

                                Text(
                                    selectedSpot,
                                    color = StarGold,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            StarRouteCard {

                                Text(
                                    "관측실 안전 확인",
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "현재는 실제 차량 기어가 아닌 " +
                                            "개발용 시연 상태를 사용합니다.",
                                    fontSize = 18.sp,
                                    color = StarGray
                                )

                                Text(
                                    if (demoParked) {
                                        "시연용 기어 상태: P단"
                                    } else {
                                        "시연용 기어 상태: 주행 중"
                                    },

                                    fontSize = 21.sp,

                                    color = if (demoParked) {
                                        StarGreen
                                    } else {
                                        StarRed
                                    }
                                )

                                Text(
                                    accessResult.message,
                                    fontSize = 18.sp,
                                    lineHeight = 28.sp
                                )

                                StarRouteButton(
                                    label = if (demoParked) {
                                        "시연용 P단 해제"
                                    } else {
                                        "시연용 P단 전환"
                                    },

                                    onClick = {
                                        demoParked = !demoParked
                                        message = ""
                                    }
                                )

                                StarRouteButton(
                                    label = if (
                                        accessResult.allowed
                                    ) {
                                        "별자리 관측실 입장"
                                    } else {
                                        "주차 상태 확인 필요"
                                    },

                                    enabled =
                                        accessResult.allowed,

                                    onClick = {
                                        val latest =
                                            checkObservationAccess(
                                                mode = observationMode,
                                                demoParked = demoParked
                                            )

                                        if (latest.allowed) {
                                            screen = "observe"
                                            message = ""
                                        } else {
                                            message = latest.message
                                        }
                                    }
                                )
                            }

                            if (message.isNotEmpty()) {
                                StarMessage(message)
                            }

                            StarRouteButton(
                                label =
                                    "메인 화면으로 돌아가기",

                                onClick = {
                                    screen = "home"
                                    message = ""
                                }
                            )
                        }

                        // ======================
                        // 별자리 관측실
                        // ======================

                        "observe" -> {

                            StarSectionTitle(
                                title = "차 안 별자리 관측실",
                                subtitle =
                                    "별자리를 선택하고 " +
                                            "신화 이야기를 들어보세요."
                            )

                            Text(
                                "선택한 장소: $selectedSpot",
                                color = StarGold,
                                fontSize = 19.sp
                            )

                            Text(
                                "개발용 주차 시연 상태입니다. " +
                                        "실제 하늘에 보이는 별자리를 " +
                                        "자동 판별한 결과는 아닙니다.",
                                color = StarGray,
                                fontSize = 16.sp,
                                lineHeight = 25.sp
                            )

                            // ------------------
                            // 별자리 선택
                            // ------------------

                            StarRouteCard {

                                Text(
                                    "별자리 선택",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                starCatalog.forEach { star ->

                                    StarRouteButton(
                                        label = if (
                                            star.id == selectedStarId
                                        ) {
                                            "✦ ${star.name} · 선택됨"
                                        } else {
                                            star.name
                                        },

                                        onClick = {
                                            speechCoordinator.stopAll()
                                            selectedStarId = star.id
                                            message = ""
                                        }
                                    )
                                }
                            }

                            // ------------------
                            // 별자리 이야기
                            // ------------------

                            StarRouteCard(
                                color = Color(0xFF202630)
                            ) {

                                Text(
                                    "✦ ${selectedStar.name}",
                                    fontSize = 29.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StarGold
                                )

                                HorizontalDivider(
                                    color = StarGold.copy(
                                        alpha = 0.3f
                                    )
                                )

                                Text(
                                    selectedStar.story,
                                    fontSize = 20.sp,
                                    lineHeight = 33.sp
                                )
                            }

                            // ------------------
                            // 음성 이야기
                            // ------------------

                            StarRouteCard {

                                Text(
                                    "별자리 음성 이야기",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StarGold
                                )

                                Text(
                                    voiceStatus,
                                    fontSize = 17.sp,
                                    color = if (voiceReady) {
                                        StarGreen
                                    } else {
                                        StarGray
                                    }
                                )

                                StarRouteButton(
                                    label = "▶ 별자리 이야기 듣기",
                                    enabled = accessResult.allowed,

                                    onClick = {

                                        val latest =
                                            checkObservationAccess(
                                                mode = observationMode,
                                                demoParked = demoParked
                                            )

                                        if (!latest.allowed) {

                                            message = latest.message

                                        } else if (!voiceReady) {

                                            message =
                                                "Pleos 음성 서비스가 " +
                                                        "아직 준비되지 않았습니다."

                                        } else {

                                            speechCoordinator.stopAll()

                                            val result =
                                                speechCoordinator.play(
                                                    type =
                                                        StarRouteSpeechType
                                                            .STAR_STORY,

                                                    text =
                                                        selectedStar.story,

                                                    requestKey =
                                                        "story:" +
                                                                selectedStar.id +
                                                                ":" +
                                                                System.nanoTime()
                                                )

                                            message = when (
                                                result.status
                                            ) {
                                                StarRouteSpeechPlayStatus.REQUESTED,
                                                StarRouteSpeechPlayStatus.REPLACED ->
                                                    "${selectedStar.name} " +
                                                            "음성 재생을 요청했습니다."

                                                else -> result.message
                                            }
                                        }
                                    }
                                )

                                OutlinedButton(
                                    onClick = {
                                        speechCoordinator.stopAll()
                                        message =
                                            "음성 중지를 요청했습니다."
                                    },

                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "■ 음성 안내 중지",
                                        color = StarWhite,
                                        fontSize = 18.sp
                                    )
                                }
                            }

                            // ------------------
                            // 여행 메모
                            // ------------------

                            StarRouteCard {

                                Text(
                                    "✦ 오늘의 별빛 여행 기록",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StarGold
                                )

                                Text(
                                    "오늘의 기분이나 기억하고 " +
                                            "싶은 이야기를 적어보세요.",
                                    fontSize = 18.sp,
                                    lineHeight = 27.sp
                                )

                                OutlinedTextField(
                                    value = observationNote,

                                    onValueChange = { newValue ->
                                        if (newValue.length <= 300) {
                                            observationNote = newValue
                                        }
                                    },

                                    label = {
                                        Text("나의 별빛 여행 메모")
                                    },

                                    placeholder = {
                                        Text(
                                            "예: 오늘은 오리온자리 " +
                                                    "신화가 기억에 남았다."
                                        )
                                    },

                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6,

                                    shape =
                                        RoundedCornerShape(14.dp),

                                    colors =
                                        OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = StarWhite,
                                            unfocusedTextColor = StarWhite,
                                            focusedBorderColor = StarGold,
                                            unfocusedBorderColor = StarGray,
                                            focusedLabelColor = StarGold,
                                            unfocusedLabelColor = StarGray,
                                            cursorColor = StarGold
                                        )
                                )

                                Text(
                                    "${observationNote.length}/300자",
                                    color = StarGray,
                                    fontSize = 16.sp
                                )

                                StarRouteButton(
                                    label =
                                        "별빛도감에 체험 기록 저장",

                                    enabled = accessResult.allowed,

                                    onClick = {

                                        val latest =
                                            checkObservationAccess(
                                                mode = observationMode,
                                                demoParked = demoParked
                                            )

                                        if (!latest.allowed) {

                                            message = latest.message

                                        } else {

                                            val now =
                                                System.currentTimeMillis()

                                            val day =
                                                SimpleDateFormat(
                                                    "yyyy.MM.dd",
                                                    Locale.KOREA
                                                ).format(Date(now))

                                            val candidate =
                                                StarDiaryEntry(
                                                    starId = selectedStarId,
                                                    spot = selectedSpot,
                                                    day = day,
                                                    time = now,
                                                    note = observationNote
                                                )

                                            val result =
                                                addStarDiaryEntry(
                                                    existing = records,
                                                    candidate = candidate
                                                )

                                            when (result.status) {

                                                StarDiarySaveStatus.SUCCESS -> {

                                                    val saved =
                                                        saveRecords(
                                                            preferences,
                                                            result.records
                                                        )

                                                    if (saved) {
                                                        records =
                                                            result.records

                                                        observationNote = ""

                                                        message =
                                                            "별자리 체험 기록과 " +
                                                                    "메모를 저장했습니다!"
                                                    } else {
                                                        message =
                                                            "기록 저장에 실패했습니다."
                                                    }
                                                }

                                                StarDiarySaveStatus.DUPLICATE -> {
                                                    message =
                                                        "오늘 이 장소의 " +
                                                                "체험 기록은 이미 있습니다."
                                                }

                                                StarDiarySaveStatus.NOTE_TOO_LONG -> {
                                                    message =
                                                        "메모는 300자 이내로 " +
                                                                "작성해 주세요."
                                                }

                                                StarDiarySaveStatus.INVALID -> {
                                                    message =
                                                        "별자리와 장소 정보를 " +
                                                                "확인해 주세요."
                                                }
                                            }
                                        }
                                    }
                                )

                                Text(
                                    "사용자가 직접 저장하는 " +
                                            "체험 기록입니다. 실제 관측 여부를 " +
                                            "자동으로 인증하지는 않습니다.",
                                    color = StarGray,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp
                                )
                            }

                            if (message.isNotEmpty()) {
                                StarMessage(message)
                            }

                            StarRouteButton(
                                label = "관측실 나가기",
                                onClick = {
                                    speechCoordinator.stopAll()
                                    screen = "trip"
                                    message = ""
                                }
                            )
                        }

                        // ======================
                        // 별빛도감
                        // ======================

                        "collection" -> {

                            StarSectionTitle(
                                title = "나의 별빛도감",
                                subtitle =
                                    "별자리 이야기와 함께 쌓아가는 " +
                                            "밤하늘 여행 기록"
                            )

                            StarRouteCard(
                                color = NightCardLight
                            ) {

                                Text(
                                    "✦ $collectedCount / " +
                                            "${starCatalog.size}",

                                    fontSize = 37.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StarGold
                                )

                                Text(
                                    "수집한 별자리",
                                    fontSize = 20.sp
                                )

                                Text(
                                    "체험 기록 ${records.size}개",
                                    color = StarGray,
                                    fontSize = 18.sp
                                )
                            }

                            if (records.isEmpty()) {

                                StarRouteCard {

                                    Text(
                                        "아직 별자리 기록이 없어요.",
                                        fontSize = 22.sp
                                    )

                                    Text(
                                        "별자리 이야기를 체험하고 " +
                                                "첫 번째 별을 기록해 보세요.",
                                        color = StarGray,
                                        fontSize = 18.sp
                                    )
                                }
                            }

                            starCatalog.forEach { star ->

                                val visits = records.filter {
                                    it.starId == star.id
                                }

                                val collected =
                                    visits.isNotEmpty()

                                StarRouteCard(
                                    color = if (collected) {
                                        NightCardLight
                                    } else {
                                        NightCard
                                    }
                                ) {

                                    Text(
                                        if (collected) {
                                            "★ ${star.name}"
                                        } else {
                                            "☆ ${star.name}"
                                        },

                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,

                                        color = if (collected) {
                                            StarGold
                                        } else {
                                            StarGray
                                        }
                                    )

                                    Text(
                                        if (collected) {
                                            "체험 기록 완료"
                                        } else {
                                            "아직 기록하지 않은 별자리"
                                        },

                                        color = if (collected) {
                                            StarGreen
                                        } else {
                                            StarGray
                                        },

                                        fontSize = 18.sp
                                    )

                                    visits.forEach { visit ->

                                        HorizontalDivider(
                                            color = StarGray.copy(
                                                alpha = 0.25f
                                            )
                                        )

                                        Text(
                                            "✦ ${visit.day}",
                                            color = StarGold,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            visit.spot,
                                            fontSize = 18.sp
                                        )

                                        if (visit.note.isNotBlank()) {

                                            Text(
                                                "나의 여행 메모",
                                                color = StarGray,
                                                fontSize = 17.sp
                                            )

                                            Text(
                                                visit.note,
                                                fontSize = 19.sp,
                                                lineHeight = 29.sp
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                pendingDeleteRecord = visit
                                            },

                                            modifier =
                                                Modifier.fillMaxWidth()
                                        ) {

                                            Text(
                                                "이 기록 삭제",
                                                color = StarRed,
                                                fontSize = 17.sp
                                            )
                                        }
                                    }

                                    if (!collected) {

                                        Text(
                                            "별자리 이야기를 체험하고 " +
                                                    "기록을 채워보세요.",
                                            color = StarGray,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }

                            if (message.isNotEmpty()) {
                                StarMessage(message)
                            }

                            Text(
                                "별빛도감에는 개발용 시연에서 " +
                                        "저장한 기록도 포함됩니다.",
                                color = StarGray,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )

                            StarRouteButton(
                                label =
                                    "오늘의 별길로 돌아가기",

                                onClick = {
                                    screen = "home"
                                    message = ""
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========================================
// 공통 UI
// ========================================


@Composable
private fun StarRouteHeader() {

    Text(
        text = "별길  StarRoute",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = StarWhite
    )
}

// ========================================
// 공통 제목 UI
// ========================================

@Composable
private fun StarSectionTitle(
    title: String,
    subtitle: String
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(9.dp)
    ) {

        Text(
            text = title,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            color = StarGold,
            lineHeight = 37.sp
        )

        Text(
            text = subtitle,
            fontSize = 19.sp,
            color = StarGray,
            lineHeight = 28.sp
        )
    }
}

// ========================================
// 공통 버튼 UI
// ========================================

@Composable
private fun StarRouteButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        enabled = enabled,

        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),

        shape = RoundedCornerShape(16.dp),

        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF8FC8FF),
            contentColor = Color(0xFF102235),
            disabledContainerColor = Color(0xFF414B60),
            disabledContentColor = Color(0xFFBAC2D2)
        )
    ) {

        Text(
            text = label,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 27.sp
        )
    }
}

// ========================================
// 공통 카드 UI
// ========================================

@Composable
private fun StarRouteCard(
    color: Color = NightCard,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = color,
            contentColor = StarWhite
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(15.dp),
            content = content
        )
    }
}

// ========================================
// 공통 안내 메시지 UI
// ========================================

@Composable
private fun StarMessage(
    message: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF47363A),
            contentColor = StarWhite
        )
    ) {

        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            fontSize = 19.sp,
            lineHeight = 27.sp
        )
    }
}
