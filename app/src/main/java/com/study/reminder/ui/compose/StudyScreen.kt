package com.study.reminder.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.reminder.model.DayStats
import com.study.reminder.model.StudyStatsRepository
import com.study.reminder.service.TimerService
import kotlinx.coroutines.delay

@Composable
fun StudyScreen() {
    val context = LocalContext.current

    var weekStats by remember { mutableStateOf(StudyStatsRepository.getWeekStats(context)) }
    var monthStats by remember { mutableStateOf(StudyStatsRepository.getMonthStats(context)) }
    var selectedTab by remember { mutableStateOf(0) } // 0=周, 1=月
    var isStudying by remember { mutableStateOf(StudyStatsRepository.hasActiveSession(context)) }
    var isInSlot by remember { mutableStateOf(StudyStatsRepository.getActiveSlotRemainingMinutes(context) > 0) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    // 计算已学习秒数（自动刷新）
    LaunchedEffect(isStudying) {
        if (isStudying) {
            val prefs = context.getSharedPreferences("study_prefs", android.content.Context.MODE_PRIVATE)
            val startMs = prefs.getLong("active_session_start", 0L)
            while (true) {
                isStudying = StudyStatsRepository.hasActiveSession(context)
                if (!isStudying) break
                elapsedSeconds = ((System.currentTimeMillis() - startMs) / 1000).toInt().coerceAtLeast(0)
                delay(1000L)
            }
        } else {
            elapsedSeconds = 0
        }
    }

    // 刷新数据
    fun refresh() {
        weekStats = StudyStatsRepository.getWeekStats(context)
        monthStats = StudyStatsRepository.getMonthStats(context)
        isStudying = StudyStatsRepository.hasActiveSession(context)
        isInSlot = StudyStatsRepository.getActiveSlotRemainingMinutes(context) > 0
    }

    fun startManualSession() {
        val remaining = StudyStatsRepository.getActiveSlotRemainingMinutes(context)
        val duration = if (remaining > 0) remaining else 24 * 60 // 不在时间段内默认24小时
        StudyStatsRepository.startSession(context, -1L)
        val serviceIntent = android.content.Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DURATION_MINUTES, duration)
        }
        context.startForegroundService(serviceIntent)
        refresh()
    }

    fun endStudySession() {
        StudyStatsRepository.endSession(context)
        // 停止 TimerService
        val serviceIntent = android.content.Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startService(serviceIntent)
        refresh()
    }

    LaunchedEffect(Unit) { refresh() }

    val todayStats = remember(weekStats) { StudyStatsRepository.getTodayStats(context) }
    val weekTotal = remember(weekStats) { weekStats.sumOf { it.totalMinutes } }
    val weekDistraction = remember(weekStats) { weekStats.sumOf { it.distractionCount } }
    val monthTotal = remember(monthStats) { monthStats.sumOf { it.totalMinutes } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFEAFBF8)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // 标题
            Text(
                text = "📊 学习统计",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10393F),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // 学习状态卡片
            GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isStudying) {
                        val h = elapsedSeconds / 3600
                        val m = (elapsedSeconds % 3600) / 60
                        val s = elapsedSeconds % 60
                        Text(
                            text = "🟢 学习中",
                            fontSize = 14.sp,
                            color = Color(0xFF39C5BB),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format("%02d:%02d:%02d", h, m, s),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10393F)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidGlassButton(
                            text = "结束学习",
                            primary = false,
                            onClick = { endStudySession() }
                        )
                    } else {
                        Text(
                            text = if (isInSlot) "当前在学习时间段内" else "当前不在学习时间段",
                            fontSize = 14.sp,
                            color = Color(0xFF4E7F84)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidGlassButton(
                            text = "开始学习",
                            onClick = { startManualSession() }
                        )
                    }
                }
            }

            // 今日概览卡片
            GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "今日概览",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10393F),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            label = "学习时长",
                            value = formatMinutes(todayStats.totalMinutes),
                            color = Color(0xFF39C5BB),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        StatCard(
                            label = "摸鱼次数",
                            value = "${todayStats.distractionCount}",
                            color = Color(0xFFFF77B7),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 周/月切换
            GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TabButton("本周", selectedTab == 0) { selectedTab = 0 }
                        Spacer(modifier = Modifier.width(8.dp))
                        TabButton("本月", selectedTab == 1) { selectedTab = 1 }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // 周汇总
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCard(
                                label = "周总时长",
                                value = formatMinutes(weekTotal),
                                color = Color(0xFF39C5BB),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            StatCard(
                                label = "周摸鱼",
                                value = "${weekDistraction}次",
                                color = Color(0xFFFF77B7),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        BarChart(
                            data = weekStats.map { it.totalMinutes },
                            labels = weekStats.map { it.label },
                            maxValue = (weekStats.maxOfOrNull { it.totalMinutes } ?: 60).coerceAtLeast(30)
                        )
                    } else {
                        // 月汇总
                        StatCard(
                            label = "月总时长",
                            value = formatMinutes(monthTotal),
                            color = Color(0xFF39C5BB),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BarChart(
                            data = monthStats.map { it.totalMinutes },
                            labels = monthStats.map { it.label },
                            maxValue = (monthStats.maxOfOrNull { it.totalMinutes } ?: 60).coerceAtLeast(30),
                            showEveryNLabel = 5
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xDDF9F4),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF4E7F84)
            )
        }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = if (selected) Color(0xFF39C5BB) else Color(0xDDF9F4),
        onClick = onClick
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF4E7F84),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun BarChart(
    data: List<Int>,
    labels: List<String>,
    maxValue: Int,
    showEveryNLabel: Int = 1
) {
    val barColor = Color(0xFF39C5BB)
    val barBg = Color(0x2239C5BB)

    Column {
        // 柱形图
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, value ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 数值标签
                    if (value > 0) {
                        Text(
                            text = "${value}",
                            fontSize = 9.sp,
                            color = Color(0xFF4E7F84),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    // 柱子
                    val fraction = if (maxValue > 0) (value.toFloat() / maxValue).coerceIn(0f, 1f) else 0f
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (value > 0) barColor else barBg)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 标签行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEachIndexed { index, label ->
                if (showEveryNLabel > 1 && (index + 1) % showEveryNLabel != 0 && index != 0) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = Color(0xFF4E7F84),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    return if (minutes >= 60) {
        val h = minutes / 60
        val m = minutes % 60
        "${h}h${m}m"
    } else {
        "${minutes}m"
    }
}
