
package com.example.starroute

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

import java.util.Locale

// 개발용 가상 GPS 좌표
private data class TestGpsPoint(
    val title: String,
    val latitude: Double,
    val longitude: Double
)

// 김해천문대 접근 상황 시연
// 김해천문대 좌표: 35.25306424, 128.887298
private val testGpsPoints = listOf(
    TestGpsPoint(
        title = "1단계 · 약 16.6km 전",
        latitude = 35.4023,
        longitude = 128.887298
    ),
    TestGpsPoint(
        title = "2단계 · 약 5.5km 전",
        latitude = 35.3026,
        longitude = 128.887298
    ),
    TestGpsPoint(
        title = "3단계 · 약 0.5km 전",
        latitude = 35.2576,
        longitude = 128.887298
    )
)

@Composable
fun GpsPanel(
    onStartExperience: (String) -> Unit = {}
) {

    val context = LocalContext.current

    val locationManager = remember(context) {
        context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager
    }

    fun hasLocationPermission(): Boolean {
        val fineGranted =
            context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            context.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    // 실제 GPS 위치
    var permissionGranted by remember {
        mutableStateOf(hasLocationPermission())
    }

    var currentLocation by remember {
        mutableStateOf<Location?>(null)
    }

    var gpsStatus by remember {
        mutableStateOf(
            "현재 위치를 확인해 주세요."
        )
    }

    // 개발용 도구 펼침 여부
    var showDeveloperTools by rememberSaveable {
        mutableStateOf(false)
    }

    // 개발용 가상 GPS 사용 여부
    var testMode by rememberSaveable {
        mutableStateOf(false)
    }

    // 가상 GPS 단계
    var testStep by rememberSaveable {
        mutableIntStateOf(0)
    }

    // 위치 권한 요청
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            permissionGranted =
                permissions.values.any { it } ||
                        hasLocationPermission()

            gpsStatus = if (permissionGranted) {
                "GPS 위치를 확인하고 있습니다."
            } else {
                "위치 권한이 필요합니다."
            }
        }

    // 실제 GPS 업데이트 연결
    DisposableEffect(
        permissionGranted,
        locationManager
    ) {

        val listener = LocationListener { location ->
            currentLocation = location
            gpsStatus = "현재 GPS 위치를 확인했습니다."
        }

        if (permissionGranted) {

            try {

                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER
                )

                var activeProvider = false

                providers.forEach { provider ->

                    if (
                        locationManager.isProviderEnabled(
                            provider
                        )
                    ) {

                        activeProvider = true

                        val lastLocation =
                            locationManager.getLastKnownLocation(
                                provider
                            )

                        if (
                            lastLocation != null &&
                            (
                                    currentLocation == null ||
                                            lastLocation.time >
                                            (currentLocation?.time ?: 0L)
                                    )
                        ) {
                            currentLocation = lastLocation
                        }

                        locationManager.requestLocationUpdates(
                            provider,
                            3000L,
                            5f,
                            listener
                        )
                    }
                }

                gpsStatus = when {

                    !activeProvider ->
                        "GPS 또는 위치 서비스가 꺼져 있습니다."

                    currentLocation != null ->
                        "현재 GPS 위치를 확인했습니다."

                    else ->
                        "GPS 위치 신호를 확인하고 있습니다."
                }

            } catch (_: SecurityException) {

                gpsStatus =
                    "위치 권한을 확인해 주세요."

            } catch (_: Exception) {

                gpsStatus =
                    "위치 서비스를 사용할 수 없습니다."
            }
        }

        onDispose {
            try {
                locationManager.removeUpdates(listener)
            } catch (_: Exception) {
            }
        }
    }

    // 화면에서 사용할 좌표 결정
    // 테스트 모드일 때는 실제 GPS 대신
    // 개발용 좌표를 거리 계산에 사용
    val testPoint = testGpsPoints[testStep]

    val displayLatitude =
        if (testMode) {
            testPoint.latitude
        } else {
            currentLocation?.latitude
        }

    val displayLongitude =
        if (testMode) {
            testPoint.longitude
        } else {
            currentLocation?.longitude
        }

    // ---------------------------------------
    // GPS 정보 카드
    // ---------------------------------------
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B2B48),
            contentColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "✦ 나의 현재 위치",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD782)
            )

            // 현재 GPS 상태
            Text(
                text = if (testMode) {
                    "개발용 GPS 테스트 위치 적용 중"
                } else {
                    gpsStatus
                },
                fontSize = 19.sp,
                color = if (testMode) {
                    Color(0xFFFFD782)
                } else {
                    Color.LightGray
                }
            )

            // 실제 위치 확인 버튼
            Button(
                onClick = {

                    if (!hasLocationPermission()) {

                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )

                    } else {

                        permissionGranted = true

                        gpsStatus =
                            "GPS 위치 신호를 확인하고 있습니다."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB8C7FF),
                    contentColor = Color(0xFF102047)
                )
            ) {

                Text(
                    text = "GPS 현재 위치 확인",
                    fontSize = 20.sp
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f)
            )

            // 현재 적용 위치
            Text(
                text = if (testMode) {
                    "현재 적용 위치 · 테스트 좌표"
                } else {
                    "현재 적용 위치 · GPS"
                },
                fontSize = 20.sp,
                color = Color(0xFFFFD782)
            )

            if (
                displayLatitude != null &&
                displayLongitude != null
            ) {

                Text(
                    text = String.format(
                        Locale.US,
                        "위도: %.6f",
                        displayLatitude
                    ),
                    fontSize = 21.sp
                )

                Text(
                    text = String.format(
                        Locale.US,
                        "경도: %.6f",
                        displayLongitude
                    ),
                    fontSize = 21.sp
                )

                Text(
                    text = if (testMode) {
                        "위치 제공: 별길 개발용 테스트"
                    } else {
                        "위치 제공: " +
                                (currentLocation?.provider ?: "확인 중")
                    },
                    fontSize = 16.sp,
                    color = Color.LightGray
                )

            } else {

                Text(
                    text =
                        "GPS 위치를 확인하면 " +
                                "가까운 별 관측 명소가 표시됩니다.",
                    fontSize = 18.sp
                )
            }

            // ---------------------------------------
            // 개발용 테스트 도구 접기 / 펼치기
            // ---------------------------------------

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f)
            )

            OutlinedButton(
                onClick = {
                    showDeveloperTools = !showDeveloperTools
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {

                Text(
                    text = if (showDeveloperTools) {
                        "개발용 GPS 테스트 설정 닫기"
                    } else {
                        "개발용 GPS 테스트 설정 열기"
                    },
                    fontSize = 17.sp,
                    color = Color.White
                )
            }

            if (showDeveloperTools) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF304061),
                        contentColor = Color.White
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "개발용 GPS 테스트",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD782)
                        )

                        Text(
                            text =
                                "실제 차량이나 시뮬레이터의 " +
                                        "GPS 위치를 변경하지 않고 " +
                                        "거리 계산을 시연합니다.",
                            fontSize = 17.sp,
                            color = Color.LightGray
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "테스트 모드 사용",
                                fontSize = 18.sp
                            )

                            Switch(
                                checked = testMode,
                                onCheckedChange = { enabled ->

                                    testMode = enabled

                                    if (enabled) {
                                        testStep = 0
                                    }
                                }
                            )
                        }

                        if (testMode) {

                            Text(
                                text =
                                    "김해천문대를 목적지로 " +
                                            "선택하고 아래 단계를 " +
                                            "차례대로 눌러주세요.",
                                fontSize = 17.sp
                            )

                            testGpsPoints.forEachIndexed {
                                    index, point ->

                                Button(
                                    onClick = {
                                        testStep = index
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                if (testStep == index) {
                                                    Color(0xFFFFD782)
                                                } else {
                                                    Color(0xFF45638D)
                                                },
                                            contentColor =
                                                if (testStep == index) {
                                                    Color(0xFF102047)
                                                } else {
                                                    Color.White
                                                }
                                        )
                                ) {

                                    Text(
                                        text =
                                            if (testStep == index) {
                                                "선택됨 · ${point.title}"
                                            } else {
                                                point.title
                                            },
                                        fontSize = 17.sp
                                    )
                                }
                            }

                            Text(
                                text =
                                    "1단계 → 2단계 → 3단계 순서로 " +
                                            "이동하면 접근 거리별 " +
                                            "안내를 시험할 수 있습니다.",
                                fontSize = 16.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            // 테스트 중임을 항상 표시
            if (testMode) {

                Text(
                    text =
                        "현재 테스트 모드입니다. " +
                                "실제 GPS로 돌아가려면 " +
                                "개발용 설정에서 테스트를 꺼주세요.",
                    fontSize = 17.sp,
                    color = Color(0xFFFFD782)
                )
            }
        }
    }

    // ---------------------------------------
    // 실제 GPS / 테스트 좌표 기반 명소 추천
    // ---------------------------------------
    if (
        displayLatitude != null &&
        displayLongitude != null
    ) {

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        StarSpotRecommendations(
            latitude = displayLatitude,
            longitude = displayLongitude,
            testMode = testMode,
            onStartExperience = onStartExperience
        )
    }
}
