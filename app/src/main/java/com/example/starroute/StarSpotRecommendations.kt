
package com.example.starroute

import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Handler
import android.os.Looper

import androidx.compose.foundation.layout.*
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

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// ========================================
// 별길 공통 색상
// ========================================

private val SpotCardColor = Color(0xFF1C2027)
private val SelectedCardColor = Color(0xFF252B34)
private val SpotGold = Color(0xFF8FC8FF)
private val SpotWhite = Color(0xFFF3F5FF)
private val SpotGray = Color(0xFFBEC8D9)
private val SpotGreen = Color(0xFF9FE3B2)

// ========================================
// 별 관측 명소 정보
// ========================================

private data class StarSpot(
    val name: String,
    val region: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val approximate: Boolean = false
)

private val skyTimeFormat = DateTimeFormatter.ofPattern("M/d HH:mm").withZone(KoreaZone)

private val starSpots = listOf(
    StarSpot(
        name = "거창 감악산 별바람언덕",
        region = "경상남도 거창군 신원면 연수사길 452",
        description = "산 위 전망 구역 · 야간 출입과 주차 여부 확인 필요",
        latitude = 35.589622,
        longitude = 127.917508,
        approximate = true
    ),

    StarSpot(
        name = "김해천문대",
        region = "경상남도 김해시",
        description = "천체 관측과 별자리 체험을 즐길 수 있는 천문대",
        latitude = 35.25306424,
        longitude = 128.887298
    ),

    StarSpot(
        name = "좌구산 별 관측 지역",
        region = "충청북도 증평군",
        description = "자연휴양림과 천문대가 있는 별 관측 지역",
        latitude = 36.70556821,
        longitude = 127.6511216,
        approximate = true
    ),


    StarSpot(
        name = "보현산천문대",
        region = "경상북도 영천시 화북면 정각길 475",
        description = "보현산 정상에 위치한 천문 관측 시설",
        latitude = 36.164775,
        longitude = 128.976578,
        approximate = false
    )
)


// ========================================
// GPS 직선거리 계산
// ========================================

private fun calculateDistanceKm(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double
): Double {

    val results = FloatArray(1)

    Location.distanceBetween(
        startLatitude,
        startLongitude,
        endLatitude,
        endLongitude,
        results
    )

    return results[0].toDouble() / 1000.0
}

// ========================================
// 지도 앱 열기 / 좌표 복사
// ========================================

private fun openStarSpotMap(
    context: Context,
    spot: StarSpot
): String {

    // 목적지의 실제 좌표를 지도 앱에 전달합니다.
    val uri = Uri.parse(
        "geo:${spot.latitude},${spot.longitude}" +
                "?q=${spot.latitude},${spot.longitude}" +
                "(${Uri.encode(spot.name)})"
    )

    val intent = Intent(
        Intent.ACTION_VIEW,
        uri
    ).apply {

        // Activity가 아닌 Context에서도 실행 가능
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {

        context.startActivity(intent)

        "지도 앱 열기를 요청했습니다."

    } catch (_: ActivityNotFoundException) {

        copySpotCoordinates(context, spot)

    } catch (_: SecurityException) {

        copySpotCoordinates(context, spot)

    } catch (_: Exception) {

        copySpotCoordinates(context, spot)
    }
}

private fun copySpotCoordinates(
    context: Context,
    spot: StarSpot
): String {

    return try {

        val clipboard = context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

        val coordinates = String.format(
            Locale.US,
            "%.6f, %.6f",
            spot.latitude,
            spot.longitude
        )

        val clip = ClipData.newPlainText(
            "${spot.name} GPS 좌표",
            coordinates
        )

        clipboard.setPrimaryClip(clip)

        "연결 가능한 지도 앱이 없어 목적지 좌표를 복사했습니다."

    } catch (_: Exception) {

        "지도 앱을 열거나 좌표를 복사하지 못했습니다."
    }
}

// ========================================
// GPS 거리 안내 문구
// ========================================

private fun createSpotArrivalMessage(
    destinationName: String,
    distanceKm: Double
): String {

    return when {

        distanceKm <= 2.0 -> {

            "$destinationName 근처에 도착하고 있어요. " +
                    "안전한 곳에 주차한 뒤 " +
                    "별자리 이야기를 만나보세요."
        }

        distanceKm <= 10.0 -> {

            "${destinationName}까지 직선거리 " +
                    "10km 이내입니다. " +
                    "별빛 여행의 목적지가 가까워지고 있어요."
        }

        else -> {

            "목적지를 향해 이동하면 GPS 거리에 따라 " +
                    "별길의 접근 안내가 표시됩니다."
        }
    }
}

// ========================================
// 별 관측 명소 추천 화면
// ========================================

@Composable
fun StarSpotRecommendations(
    latitude: Double,
    longitude: Double,
    testMode: Boolean = false,
    onStartExperience: (String) -> Unit = {}
) {

    val context = LocalContext.current

    // Pleos 내비게이션 SDK
    val naviHelper = remember(context) {
        NaviHelper(context.applicationContext)
    }

    val mainHandler = remember {
        Handler(Looper.getMainLooper())
    }

    // 기존 내비게이션 요청 중복 방지 관리자
    val requestController = remember {
        NavigationRequestController()
    }

    // 선택한 목적지
    var selectedDestination by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // 지도 열기 결과 메시지
    var mapMessage by remember {
        mutableStateOf("")
    }

    // Pleos 서비스 연결 상태
    var naviConnected by remember {
        mutableStateOf(false)
    }

    var naviMessage by remember {
        mutableStateOf(
            "Pleos 내비게이션 서비스 연결 준비 중..."
        )
    }

    // 내비게이션 요청 상태
    var requestInfo by remember {
        mutableStateOf(
            requestController.getInfo()
        )
    }

    // ========================================
    // Pleos 내비게이션 연결
    // ========================================

    DisposableEffect(naviHelper) {

        var isActive = true

        try {

            naviHelper.initialize(

                onConnected = {

                    mainHandler.post {

                        if (isActive) {

                            naviConnected = true

                            naviMessage =
                                "Pleos 내비게이션 서비스가 연결되었습니다."
                        }
                    }
                },

                onFailed = { error ->

                    mainHandler.post {

                        if (isActive) {

                            naviConnected = false

                            naviMessage =
                                "내비게이션 연결 실패: " +
                                        (
                                                error.message
                                                    ?: "서비스 상태를 확인해 주세요."
                                                )
                        }
                    }
                }
            )

        } catch (e: Exception) {

            naviConnected = false

            naviMessage =
                "내비게이션 초기화 오류: " +
                        (e.message ?: "알 수 없는 오류")
        }

        onDispose {

            isActive = false

            try {
                naviHelper.release()
            } catch (_: Exception) {
            }
        }
    }

    // ========================================
    // GPS 기준 가까운 명소 정렬
    // ========================================

    val sortedSpots = remember(
        latitude,
        longitude
    ) {

        starSpots.map { spot ->

            val distance = calculateDistanceKm(
                latitude,
                longitude,
                spot.latitude,
                spot.longitude
            )

            spot to distance

        }.sortedBy { it.second }
    }

    var selectedConstellation by rememberSaveable { mutableStateOf<String?>(null) }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = Instant.now()
        }
    }
    val tonight = remember(latitude, longitude, now) {
        tonightRecommendations(latitude, longitude, now)
    }
    val matchingSpots = remember(selectedConstellation, now) {
        val starId = selectedConstellation
        if (starId == null) emptyMap()
        else starSpots.mapNotNull { spot ->
            tonightRecommendations(spot.latitude, spot.longitude, now)
                .firstOrNull { it.star.id == starId }
                ?.let { spot.name to it }
        }.toMap()
    }
    val visibleSpots = if (selectedConstellation == null) sortedSpots else sortedSpots
        .filter { it.first.name in matchingSpots }
        .sortedByDescending { matchingSpots.getValue(it.first.name).window.position.altitude.roundToInt() }

    // 선택한 목적지 찾기
    val selectedSpot = visibleSpots.find {
        it.first.name == selectedDestination
    }

    // ========================================
    // GPS 테스트 모드 전환
    // ========================================

    LaunchedEffect(testMode) {

        if (testMode) {

            requestController.reset()

            requestInfo =
                requestController.getInfo()

            naviMessage =
                "개발용 GPS 테스트 모드에서는 " +
                        "실제 Pleos 경로 요청을 실행하지 않습니다."

        } else if (naviConnected) {

            naviMessage =
                "Pleos 내비게이션 서비스가 연결되었습니다."
        }
    }

    // ========================================
    // 전체 화면
    //
    // 스크롤은 MainActivity에서 처리
    // ========================================

    Column(
        modifier = Modifier.fillMaxWidth(),

        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        Text("오늘 밤 볼 만한 별자리", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = SpotGold)
        Text("현재 위치 기준 · 보고 싶은 별자리를 고르면 장소 후보가 나와요.", color = SpotGray)
        if (tonight.isEmpty()) Text("오늘 밤 남은 시간에는 관측 기준을 만족하는 별자리가 없어요.")
        val selectedTonight = tonight.firstOrNull { it.star.id == selectedConstellation }
        if (selectedConstellation != null) {
            Text(
                selectedTonight?.let { "${it.star.name} · 추천 ${skyTimeFormat.format(it.window.best)}" }
                    ?: "현재 위치의 추천 시간이 지났어요.",
                color = SpotGold
            )
            TextButton(onClick = {
                selectedConstellation = null
                selectedDestination = null
                mapMessage = ""
            }) { Text("다른 별자리 보기", color = SpotGold) }
        } else {
            for (result in tonight) {
                OutlinedButton(
                    onClick = {
                        selectedConstellation = result.star.id
                        selectedDestination = null
                        mapMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("${result.star.name} · ${skyTimeFormat.format(result.window.best)}", color = SpotWhite) }
            }
        }

        Text(
            if (selectedConstellation == null) "가까운 관측 명소" else "선택한 별자리의 관측 장소 후보",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = SpotGold
        )
        Text(
            if (selectedConstellation == null) "GPS 직선거리순"
            else "대표 별의 추천 고도순 · 같은 고도는 가까운 순 · 구름, 빛공해, 시야 및 야간 접근 여부는 방문 전 확인",
            color = SpotGray
        )
        if (selectedConstellation != null && visibleSpots.isEmpty()) {
            Text("오늘 밤 남은 시간에 관측 기준을 만족하는 등록 장소가 없어요.")
        }

        // ====================================
        // 내비게이션 연결 정보
        // ====================================

        if (testMode || !naviConnected) {
            Text(
                text = if (testMode) "GPS 테스트 중 · Pleos 경로 요청 비활성화"
                else naviMessage,
                fontSize = 15.sp,
                color = SpotGray
            )
        }

        // ====================================
        // 선택한 목적지 상세 화면
        // ====================================

        if (selectedSpot != null) {

            val destination = selectedSpot.first

            val distance = selectedSpot.second

            val destinationKey = String.format(
                Locale.US,
                "%.6f,%.6f",
                destination.latitude,
                destination.longitude
            )

            val sameDestinationRequested =
                requestInfo.state ==
                        NavigationRequestState.REQUEST_SENT &&
                        requestInfo.destinationKey ==
                        destinationKey

            val requestInProgress =
                requestInfo.state ==
                        NavigationRequestState.REQUESTING

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(20.dp),

                colors = CardDefaults.cardColors(
                    containerColor = SelectedCardColor,
                    contentColor = SpotWhite
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(13.dp)
                ) {

                    // ------------------------
                    // 목적지 정보
                    // ------------------------

                    Text(
                        text = destination.name,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = destination.region,
                        fontSize = 20.sp,
                        color = SpotGray
                    )

                    Text(
                        text = destination.description,
                        fontSize = 18.sp,
                        lineHeight = 27.sp
                    )

                    Text(
                        text = String.format(
                            Locale.KOREA,
                            "현재 위치에서 직선거리: 약 %.1f km",
                            distance
                        ),

                        fontSize = 23.sp,
                        color = SpotGold
                    )

                    if (destination.approximate) {

                        Text(
                            text =
                                "대표 좌표로 표시된 지역입니다. " +
                                        "실제 방문 장소와 출입 가능 여부를 " +
                                        "미리 확인해 주세요.",

                            fontSize = 16.sp,
                            color = SpotGold
                        )
                    }

                    HorizontalDivider(
                        color = SpotGray.copy(
                            alpha = 0.3f
                        )
                    )

                    // ====================================
                    // 목적지 접근 안내 문구
                    // ====================================

                    Card(
                        modifier = Modifier.fillMaxWidth(),

                        colors = CardDefaults.cardColors(

                            containerColor =
                                if (distance <= 2.0) {

                                    Color(0xFF245C52)

                                } else {

                                    SpotCardColor
                                },

                            contentColor = SpotWhite
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(17.dp),

                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            Text(
                                text = "별길 목적지 접근 안내",
                                fontSize = 20.sp,
                                color = SpotGold
                            )

                            Text(
                                text = createSpotArrivalMessage(
                                    destination.name,
                                    distance
                                ),

                                fontSize = 18.sp,
                                lineHeight = 28.sp
                            )

                            Text(
                                text =
                                    "GPS 직선거리 기준이며 실제 도로 " +
                                            "주행거리와 다를 수 있습니다.",

                                fontSize = 15.sp,
                                color = SpotGray
                            )
                        }
                    }

                    // ====================================
                    // 10km / 2km 자동 음성 안내
                    // ====================================

                    ArrivalVoiceAnnouncer(
                        destinationName = destination.name,
                        distanceKm = distance,
                        testMode = testMode
                    )

                    HorizontalDivider(
                        color = SpotGray.copy(
                            alpha = 0.3f
                        )
                    )

                    // ====================================
                    // Pleos 내비게이션 경로 요청
                    // ====================================

                    Text(
                        text = "목적지 길 안내",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotGold
                    )

                    Button(
                        onClick = {

                            if (
                                !testMode &&
                                naviConnected
                            ) {

                                val result =
                                    requestController.requestRoute(
                                        destinationKey =
                                            destinationKey
                                    ) {

                                        val routeInfo = RouteInfo(
                                            latitude =
                                                destination.latitude,

                                            longitude =
                                                destination.longitude,

                                            poiName =
                                                destination.name,

                                            poiId = "",

                                            address =
                                                destination.region,

                                            poiSubId = ""
                                        )

                                        naviHelper.requestRoute(
                                            routeInfo
                                        )
                                    }

                                requestInfo =
                                    requestController.getInfo()

                                naviMessage =
                                    navigationResultMessage(
                                        result
                                    )
                            }
                        },

                        enabled =
                            !testMode &&
                                    naviConnected &&
                                    !requestInProgress &&
                                    !sameDestinationRequested,

                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8FC8FF),
                            contentColor = Color(0xFF102235)
                        )
                    ) {

                        Text(
                            text = when {

                                testMode ->

                                    "GPS 테스트 중 · 경로 요청 불가"

                                sameDestinationRequested ->

                                    "이 목적지로 경로 요청 완료"

                                requestInProgress ->

                                    "경로 요청 처리 중"

                                else ->

                                    "Pleos 내비게이션 경로 요청"
                            },

                            fontSize = 18.sp
                        )
                    }

                    Text(
                        text = requestInfo.message,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = SpotGray
                    )

                    // ------------------------
                    // 경로 요청 재시도
                    // ------------------------

                    if (
                        sameDestinationRequested &&
                        !testMode
                    ) {

                        OutlinedButton(
                            onClick = {

                                val resetSucceeded =
                                    requestController.reset()

                                requestInfo =
                                    requestController.getInfo()

                                naviMessage =
                                    if (resetSucceeded) {

                                        "경로 요청을 다시 시도할 수 있습니다."

                                    } else {

                                        "이전 경로 요청이 처리 중입니다."
                                    }
                            },

                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text = "경로 요청 재시도 준비",
                                fontSize = 18.sp,
                                color = SpotWhite
                            )
                        }
                    }

                    if (sameDestinationRequested && destination.name == "보현산천문대") {
                        Text(
                            text = "Pleos에서 경로 탐색에 실패하면 내비게이션 검색창에서 " +
                                    "‘보현산천문대’ 또는 ‘영천시 화북면 정각길 475’를 검색해 보세요.",
                            fontSize = 15.sp,
                            color = SpotGray
                        )
                    }

                    TonightSkyPanel(destination.latitude, destination.longitude)

                    HorizontalDivider(
                        color = SpotGray.copy(
                            alpha = 0.3f
                        )
                    )

                    // ====================================
                    // 별자리 이야기 체험
                    // ====================================

                    Button(
                        onClick = {

                            onStartExperience(
                                destination.name
                            )
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotGold,
                            contentColor = Color(0xFF16243D)
                        )
                    ) {

                        Text(
                            text =
                                "이 장소에서 별자리 이야기 체험",

                            fontSize = 18.sp
                        )
                    }

                    Text(
                        text =
                            "별자리 이야기는 개발용 주차 상태를 " +
                                    "확인한 후 체험할 수 있습니다.",

                        fontSize = 15.sp,
                        color = SpotGray
                    )

                    // ====================================
                    // 지도에서 위치 확인
                    // ====================================

                    OutlinedButton(
                        onClick = {

                            mapMessage = openStarSpotMap(
                                context = context,
                                spot = destination
                            )
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = "지도에서 위치 확인",
                            fontSize = 18.sp,
                            color = SpotWhite
                        )
                    }

                    if (mapMessage.isNotEmpty()) {

                        Text(
                            text = mapMessage,
                            fontSize = 17.sp,
                            lineHeight = 25.sp,
                            color = SpotGold
                        )
                    }

                    // ====================================
                    // 목적지 선택 취소
                    // ====================================

                    OutlinedButton(
                        onClick = {

                            selectedDestination = null
                            mapMessage = ""
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = "목적지 선택 취소",
                            fontSize = 18.sp,
                            color = SpotWhite
                        )
                    }
                }
            }
        }

        // ====================================
        // 가까운 별 관측 명소 목록
        // ====================================

        visibleSpots.forEachIndexed { index, item ->

            val spot = item.first

            val distance = item.second

            val isSelected =
                selectedDestination == spot.name

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(18.dp),

                colors = CardDefaults.cardColors(

                    containerColor =
                        if (isSelected) {

                            SelectedCardColor

                        } else {

                            SpotCardColor
                        },

                    contentColor = SpotWhite
                )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(11.dp)
                ) {

                    Text(
                        text =
                            "${index + 1}. ${spot.name}",

                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,

                        color = if (isSelected) {
                            SpotGold
                        } else {
                            SpotWhite
                        }
                    )

                    Text(
                        text = spot.region,
                        fontSize = 18.sp,
                        color = SpotGray
                    )

                    Text(
                        text = spot.description,
                        fontSize = 18.sp,
                        lineHeight = 27.sp
                    )

                    Text(
                        text = String.format(
                            Locale.KOREA,
                            "직선거리: 약 %.1f km",
                            distance
                        ),

                        fontSize = 21.sp,
                        color = SpotGold
                    )

                    if (spot.approximate) {

                        Text(
                            text =
                                "대표 좌표입니다. 실제 방문 지점을 " +
                                        "확인해 주세요.",

                            fontSize = 15.sp,
                            color = SpotGray
                        )
                    }

                    // ------------------------
                    // 목적지 선택 버튼
                    // ------------------------

                    matchingSpots[spot.name]?.let { result ->
                        val window = result.window
                        Text(
                            "${skyTimeFormat.format(window.best)} · ${result.guide.name} · " +
                                    "${compassDirection(window.position.azimuth)}쪽 하늘 약 ${window.position.altitude.roundToInt()}°",
                            fontSize = 18.sp,
                            color = SpotGold
                        )
                    }

                    Button(
                        onClick = {

                            selectedDestination = spot.name
                            mapMessage = ""

                        },

                        modifier = Modifier.fillMaxWidth(),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8FC8FF),
                            contentColor = Color(0xFF102235)
                        )
                    ) {

                        Text(
                            text = if (isSelected) {

                                "선택된 목적지"

                            } else {

                                "이 장소를 목적지로 선택"
                            },

                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // ====================================
        // 주의 사항
        // ====================================

        Text(
            text =
                "※ 표시된 거리는 GPS 직선거리입니다. " +
                        "방문 전 명소 운영시간, 접근 가능 여부, " +
                        "야간 출입 제한을 확인해 주세요.",

            fontSize = 16.sp,
            color = SpotGray,
            lineHeight = 24.sp
        )
    }
}
