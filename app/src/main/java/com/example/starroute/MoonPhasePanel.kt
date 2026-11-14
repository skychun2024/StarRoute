
package com.example.starroute

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToLong

private val MoonGold = Color(0xFFFFD782)
private val MoonWhite = Color(0xFFF3F5FF)
private val MoonGray = Color(0xFFBCC7D8)
private val MoonCard = Color(0xFF1B2B48)
private val MoonInnerCard = Color(0xFF283C63)

/**
 * 달 위상 시연용 기준 시각
 *
 * 기존 MoonPhaseCalculator.kt의
 * 기준 신월에서 반 삭망월만큼 이동한 시각입니다.
 *
 * 실제 오늘의 달 상태가 아니라
 * 평균 주기에 따른 보름달 미리보기입니다.
 */
private fun demonstrationFullMoonTime(): Long {

    val referenceNewMoonJd = 2451550.1
    val unixEpochJd = 2440587.5
    val synodicMonthDays = 29.530588853

    val fullMoonJd =
        referenceNewMoonJd + synodicMonthDays / 2.0

    return (
            (fullMoonJd - unixEpochJd) * 86400000.0
            ).roundToLong()
}

@Composable
fun MoonPhasePanel() {

    // 실제 날짜 기준
    val now = System.currentTimeMillis()

    // 개발용 보름달 미리보기 상태
    var demoFullMoon by rememberSaveable {
        mutableStateOf(false)
    }

    // 시연 모드에서는 기준 보름달 시각 사용
    val calculationTime = if (demoFullMoon) {
        demonstrationFullMoonTime()
    } else {
        now
    }

    // 기존 달 위상 계산기 사용
    val moon = calculateStarRouteMoon(
        calculationTime
    )

    val todayText = SimpleDateFormat(
        "yyyy년 M월 d일 EEEE",
        Locale.KOREA
    ).format(Date(now))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MoonCard,
            contentColor = MoonWhite
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            // ----------------------------
            // 제목
            // ----------------------------

            Text(
                text = "✦ 오늘의 달 이야기",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = MoonGold
            )

            Text(
                text = "오늘 날짜: $todayText",
                fontSize = 18.sp,
                color = MoonGray
            )

            // ----------------------------
            // 개발용 시연 버튼
            // ----------------------------

            OutlinedButton(
                onClick = {
                    demoFullMoon = !demoFullMoon
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {

                Text(
                    text = if (demoFullMoon) {
                        "실제 날짜의 달 보기"
                    } else {
                        "개발용 보름달 미리보기"
                    },
                    fontSize = 18.sp,
                    color = MoonWhite
                )
            }

            if (demoFullMoon) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF55452D),
                        contentColor = MoonWhite
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(15.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {

                        Text(
                            text = "개발용 보름달 시연 중",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoonGold
                        )

                        Text(
                            text =
                                "지금 보이는 달은 오늘의 실제 " +
                                        "달 상태가 아닙니다. " +
                                        "보름달 소원 메시지를 보여주기 위한 " +
                                        "미리보기 화면입니다.",
                            fontSize = 16.sp,
                            lineHeight = 25.sp
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MoonGray.copy(alpha = 0.3f)
            )

            // ----------------------------
            // 달 위상 카드
            // ----------------------------

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MoonInnerCard,
                    contentColor = MoonWhite
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = moon.symbol,
                        fontSize = 64.sp
                    )

                    Text(
                        text = moon.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoonGold
                    )

                    Text(
                        text = moon.description,
                        fontSize = 19.sp,
                        lineHeight = 29.sp
                    )
                }
            }

            // ----------------------------
            // 달의 밝은 부분
            // ----------------------------

            Text(
                text = "달의 밝은 부분 · 추정치",
                fontSize = 19.sp,
                color = MoonGray
            )

            Text(
                text = "${moon.illuminationPercent}%",
                fontSize = 33.sp,
                fontWeight = FontWeight.Bold,
                color = MoonGold
            )

            LinearProgressIndicator(
                progress = {
                    moon.illuminationPercent / 100f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = MoonGold,
                trackColor = Color(0xFF40506B)
            )

            HorizontalDivider(
                color = MoonGray.copy(alpha = 0.3f)
            )

            // ----------------------------
            // 보름달 소원 메시지
            // ----------------------------

            if (moon.nearFullMoon) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF57432E),
                        contentColor = MoonWhite
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "🌕 보름달이 가까운 밤이에요",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoonGold
                        )

                        Text(
                            text =
                                "오늘 밤하늘을 바라보며 " +
                                        "마음속 소원을 하나 빌어보세요.",
                            fontSize = 20.sp,
                            lineHeight = 30.sp
                        )

                        Text(
                            text =
                                "별길이 당신의 소중한 소원이 " +
                                        "이루어지길 응원할게요.",
                            fontSize = 18.sp,
                            color = Color(0xFFFFE8B5)
                        )

                        if (demoFullMoon) {

                            Text(
                                text =
                                    "위 메시지는 개발용 보름달 " +
                                            "미리보기입니다.",
                                fontSize = 15.sp,
                                color = MoonGray
                            )
                        }
                    }
                }

            } else {

                // ----------------------------
                // 일반 날짜의 달 안내
                // ----------------------------

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF263C63),
                        contentColor = MoonWhite
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        Text(
                            text = "다음 보름달 이야기",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoonGold
                        )

                        Text(
                            text =
                                "다음 보름달까지 약 " +
                                        "${ceil(moon.daysUntilFullMoon).toInt()}일",
                            fontSize = 22.sp
                        )

                        Text(
                            text =
                                "달의 변화를 살펴보며 " +
                                        "다음 별빛 여행을 준비해 보세요.",
                            fontSize = 18.sp,
                            lineHeight = 28.sp
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MoonGray.copy(alpha = 0.3f)
            )

            Text(
                text =
                    "달 위상과 밝기는 평균 달 주기를 " +
                            "이용한 근사 계산입니다. " +
                            "실제 천문 현상과 차이가 있을 수 있습니다.",
                fontSize = 15.sp,
                color = MoonGray,
                lineHeight = 23.sp
            )
        }
    }
}
