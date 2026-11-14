package com.example.starroute

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val viewingTimeFormat = DateTimeFormatter.ofPattern("M/d HH:mm").withZone(KoreaZone)

@Composable
internal fun TonightSkyPanel(latitude: Double, longitude: Double) {
    var expanded by remember(latitude, longitude) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF171B21),
            contentColor = Color(0xFFF3F5FF)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("오늘 밤 이 장소에서 볼 만한 별자리", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "추천 접기" else "오늘 밤 추천 보기", color = Color(0xFF8FC8FF))
            }
            if (expanded) {
                var now by remember { mutableStateOf(Instant.now()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(60_000)
                        now = Instant.now()
                    }
                }
                val recommendations = remember(latitude, longitude, now) {
                    tonightRecommendations(latitude, longitude, now)
                }
                Text("한국시간 기준 · 어두운 시간대에 대표 별이 지평선 위 20° 이상일 때 추천합니다.")
                if (recommendations.isEmpty()) {
                    Text("오늘 밤 남은 시간에는 기준을 만족하는 별자리가 없어요.")
                }
                for (result in recommendations) {
                    HorizontalDivider(color = Color(0xFF363E49))
                    val window = result.window
                    Text(result.star.name, style = MaterialTheme.typography.titleSmall)
                    val start = viewingTimeFormat.format(window.start)
                    val end = viewingTimeFormat.format(window.end)
                    Text(if (start == end) "관측 시각 $start" else "관측 시간 $start ~ $end")
                    Text(
                        "추천 시각 ${viewingTimeFormat.format(window.best)} · " +
                                "${result.guide.name} · ${compassDirection(window.position.azimuth)}쪽 " +
                                "하늘 약 ${window.position.altitude.roundToInt()}°"
                    )
                }
                Text("별의 위치를 계산한 결과입니다. 구름, 달빛, 빛공해, 지형에 따라 실제로 보이는 정도가 달라집니다.")
            }
        }
    }
}
