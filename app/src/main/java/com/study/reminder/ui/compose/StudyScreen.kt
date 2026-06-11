package com.study.reminder.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.res.stringResource
import com.study.reminder.R
import com.study.reminder.model.DayStats
import com.study.reminder.model.StudyStatsRepository
import com.study.reminder.service.TimerService
import kotlinx.coroutines.delay

@Composable
fun StudyScreen() {
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0=周, 1=月
    var weekOffset by remember { mutableIntStateOf(0) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var isStudying by remember { mutableStateOf(StudyStatsRepository.hasActiveSession(context)) }
    var isInSlot by remember { mutableStateOf(StudyStatsRepository.getActiveSlotRemainingMinutes(context) > 0) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    val weekStats = remember(weekOffset) { StudyStatsRepository.getWeekStats(context, weekOffset) }
    val prevWeekStats = remember(weekOffset) { StudyStatsRepository.getWeekStats(context, weekOffset - 1) }
    val monthStats = remember(monthOffset) { StudyStatsRepository.getMonthStats(context, monthOffset) }
    val prevMonthStats = remember(monthOffset) { StudyStatsRepository.getMonthStats(context, monthOffset - 1) }
    val weekDateRange = remember(weekOffset) { StudyStatsRepository.getWeekDateRange(weekOffset) }
    val monthLabel = remember(monthOffset) { StudyStatsRepository.getMonthLabel(monthOffset) }

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

    fun refresh() {
        isStudying = StudyStatsRepository.hasActiveSession(context)
        isInSlot = StudyStatsRepository.getActiveSlotRemainingMinutes(context) > 0
    }

    fun startManualSession() {
        val remaining = StudyStatsRepository.getActiveSlotRemainingMinutes(context)
        val duration = if (remaining > 0) remaining else 24 * 60
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
        val serviceIntent = android.content.Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startService(serviceIntent)
        refresh()
    }

    LaunchedEffect(Unit) { refresh() }

    val todayStats = remember(weekStats) { StudyStatsRepository.getTodayStats(context) }
    val weekTotal = remember(weekStats) { weekStats.sumOf { it.totalMinutes } }
    val prevWeekTotal = remember(prevWeekStats) { prevWeekStats.sumOf { it.totalMinutes } }
    val weekDistraction = remember(weekStats) { weekStats.sumOf { it.distractionCount } }
    val monthTotal = remember(monthStats) { monthStats.sumOf { it.totalMinutes } }
    val prevMonthTotal = remember(prevMonthStats) { prevMonthStats.sumOf { it.totalMinutes } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // 标题
            Text(
                text = stringResource(R.string.study_statistics),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
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
                            text = stringResource(R.string.studying),
                            fontSize = 14.sp,
                            color = AppColors.Accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format("%02d:%02d:%02d", h, m, s),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidGlassButton(
                            text = stringResource(R.string.end_study),
                            primary = false,
                            onClick = { endStudySession() }
                        )
                    } else {
                        Text(
                            text = stringResource(if (isInSlot) R.string.in_study_slot else R.string.not_in_study_slot),
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidGlassButton(
                            text = stringResource(R.string.start_study),
                            onClick = { startManualSession() }
                        )
                    }
                }
            }

            // 今日概览卡片
            GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.today_overview),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            label = stringResource(R.string.study_duration),
                            value = formatMinutes(todayStats.totalMinutes),
                            color = AppColors.Accent,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        StatCard(
                            label = stringResource(R.string.distraction_count),
                            value = "${todayStats.distractionCount}",
                            color = AppColors.AccentPink,
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
                        TabButton(stringResource(R.string.this_week), selectedTab == 0) { selectedTab = 0 }
                        Spacer(modifier = Modifier.width(8.dp))
                        TabButton(stringResource(R.string.this_month), selectedTab == 1) { selectedTab = 1 }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // 周导航
                        PeriodNavigator(
                            label = "${weekDateRange.first} ~ ${weekDateRange.second}",
                            canGoNext = weekOffset < 0,
                            onPrev = { weekOffset-- },
                            onNext = { if (weekOffset < 0) weekOffset++ }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 周汇总 + 对比
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCard(
                                label = stringResource(R.string.week_total),
                                value = formatMinutes(weekTotal),
                                color = AppColors.Accent,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            StatCard(
                                label = stringResource(R.string.week_distraction),
                                value = "${weekDistraction}次",
                                color = AppColors.AccentPink,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (weekOffset < 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ComparisonText(current = weekTotal, previous = prevWeekTotal, period = "周")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        BarChart(
                            data = weekStats.map { it.totalMinutes },
                            labels = weekStats.map { it.label },
                            maxValue = (weekStats.maxOfOrNull { it.totalMinutes } ?: 60).coerceAtLeast(30)
                        )
                    } else {
                        // 月导航
                        PeriodNavigator(
                            label = monthLabel,
                            canGoNext = monthOffset < 0,
                            onPrev = { monthOffset-- },
                            onNext = { if (monthOffset < 0) monthOffset++ }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 月汇总 + 对比
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCard(
                                label = stringResource(R.string.month_total),
                                value = formatMinutes(monthTotal),
                                color = AppColors.Accent,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            ComparisonCard(
                                current = monthTotal,
                                previous = prevMonthTotal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ScrollableBarChart(
                            data = monthStats.map { it.totalMinutes },
                            labels = monthStats.map { it.label },
                            maxValue = (monthStats.maxOfOrNull { it.totalMinutes } ?: 60).coerceAtLeast(30)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodNavigator(
    label: String,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "◀",
            fontSize = 16.sp,
            color = Color(0xFF39C5BB),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onPrev() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF10393F),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Text(
            text = "▶",
            fontSize = 16.sp,
            color = if (canGoNext) Color(0xFF39C5BB) else Color(0x44000000),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .then(if (canGoNext) Modifier.clickable { onNext() } else Modifier)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ComparisonText(current: Int, previous: Int, period: String) {
    val diff = current - previous
    if (previous == 0 && current == 0) return
    val text = if (diff > 0) {
        "比上${period}多学了 ${formatMinutes(diff)}"
    } else if (diff < 0) {
        "比上${period}少学了 ${formatMinutes(-diff)}"
    } else {
        "与上${period}持平"
    }
    val color = if (diff > 0) AppColors.Accent else if (diff < 0) AppColors.AccentPink else AppColors.TextSecondary
    Text(
        text = text,
        fontSize = 12.sp,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ComparisonCard(current: Int, previous: Int, modifier: Modifier = Modifier) {
    val diff = current - previous
    val text = if (diff > 0) "+${formatMinutes(diff)}" else if (diff < 0) formatMinutes(diff) else "0"
    val label = if (previous > 0) "较上月" else "较上月"
    val color = if (diff > 0) AppColors.Accent else if (diff < 0) AppColors.AccentPink else AppColors.TextSecondary

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppColors.CardBgSoft,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
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
        color = AppColors.CardBgSoft,
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
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = if (selected) AppColors.Accent else AppColors.CardBgSoft,
        onClick = onClick
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) AppColors.White else AppColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun BarChart(
    data: List<Int>,
    labels: List<String>,
    maxValue: Int
) {
    val barColor = AppColors.Accent
    val barBg = Color(0x2239C5BB)
    val barHeight = 100.dp
    val labelHeight = 18.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, value ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Value label — fixed height so all columns are same total height
                Box(
                    modifier = Modifier.height(labelHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (value > 0) {
                        Text(
                            text = "$value",
                            fontSize = 9.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }
                // Bar
                val fraction = if (maxValue > 0) (value.toFloat() / maxValue).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(barHeight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(barBg)
                    )
                    if (value > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight * fraction)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                    }
                }
                // Day label — fixed height
                Box(
                    modifier = Modifier.height(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labels.getOrElse(index) { "" },
                        fontSize = 10.sp,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollableBarChart(
    data: List<Int>,
    labels: List<String>,
    maxValue: Int
) {
    val barColor = AppColors.Accent
    val barBg = Color(0x2239C5BB)
    val barWidth = 24.dp
    val barHeight = 100.dp
    val valueLabelHeight = 18.dp
    val dayLabelHeight = 16.dp
    val scrollState = rememberScrollState()

    val today = remember { java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(data.size) {
        if (today > 2) {
            val targetPx = with(density) { (barWidth * (today - 2)).toPx() }
            scrollState.scrollTo(targetPx.toInt().coerceIn(0, scrollState.maxValue))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, value ->
            Column(
                modifier = Modifier.width(barWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Value label — fixed height
                Box(
                    modifier = Modifier.height(valueLabelHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (value > 0) {
                        Text(
                            text = "$value",
                            fontSize = 9.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }
                // Bar
                val fraction = if (maxValue > 0) (value.toFloat() / maxValue).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(barHeight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(barBg)
                    )
                    if (value > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight * fraction)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                    }
                }
                // Day label — fixed height
                val dayNum = index + 1
                Box(
                    modifier = Modifier.height(dayLabelHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$dayNum",
                        fontSize = 8.sp,
                        color = if (dayNum == today) AppColors.Accent else AppColors.TextSecondary,
                        fontWeight = if (dayNum == today) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
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
