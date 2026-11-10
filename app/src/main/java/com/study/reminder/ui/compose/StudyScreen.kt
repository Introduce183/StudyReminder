package com.study.reminder.ui.compose

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.reminder.R
import com.study.reminder.helper.SlotScheduler
import com.study.reminder.model.SlotRepository
import com.study.reminder.model.StudyStatsRepository
import com.study.reminder.service.TimerService
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudyScreen(
    onSlotDisabled: () -> Unit = {}
) {
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var isStudying by remember { mutableStateOf(StudyStatsRepository.hasActiveSession(context)) }
    var isInSlot by remember { mutableStateOf(StudyStatsRepository.getActiveSlotRemainingMinutes(context) > 0) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var statsRefreshToken by remember { mutableIntStateOf(0) }
    var slotRemainingMinutes by remember { mutableIntStateOf(0) }

    val weekStats = remember(weekOffset, statsRefreshToken) { StudyStatsRepository.getWeekStats(context, weekOffset) }
    val prevWeekStats = remember(weekOffset, statsRefreshToken) { StudyStatsRepository.getWeekStats(context, weekOffset - 1) }
    val monthStats = remember(monthOffset, statsRefreshToken) { StudyStatsRepository.getMonthStats(context, monthOffset) }
    val prevMonthStats = remember(monthOffset, statsRefreshToken) { StudyStatsRepository.getMonthStats(context, monthOffset - 1) }
    val weekDateRange = remember(weekOffset) { StudyStatsRepository.getWeekDateRange(weekOffset) }
    val monthLabel = remember(monthOffset) { StudyStatsRepository.getMonthLabel(monthOffset) }

    LaunchedEffect(isStudying) {
        if (isStudying) {
            val prefs = context.getSharedPreferences("study_prefs", android.content.Context.MODE_PRIVATE)
            val startMs = prefs.getLong("active_session_start", 0L)
            var updateCounter = 0
            while (true) {
                isStudying = StudyStatsRepository.hasActiveSession(context)
                if (!isStudying) break
                elapsedSeconds = ((System.currentTimeMillis() - startMs) / 1000).toInt().coerceAtLeast(0)
                updateCounter++
                if (updateCounter % 60 == 0) {
                    statsRefreshToken++
                    slotRemainingMinutes = StudyStatsRepository.getActiveSlotRemainingMinutes(context)
                }
                delay(1000L)
            }
        } else {
            elapsedSeconds = 0
        }
    }

    fun refresh() {
        isStudying = StudyStatsRepository.hasActiveSession(context)
        isInSlot = StudyStatsRepository.getActiveSlotRemainingMinutes(context) > 0
        slotRemainingMinutes = StudyStatsRepository.getActiveSlotRemainingMinutes(context)
        statsRefreshToken++
    }

    fun startManualSession() {
        val remaining = StudyStatsRepository.getActiveSlotRemainingMinutes(context)
        val duration = if (remaining > 0) remaining else 24 * 60
        val activeSlotId = if (remaining > 0) StudyStatsRepository.getActiveSlotId(context) else -1L
        StudyStatsRepository.startSession(context, activeSlotId)
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

        val activeSlotIds = StudyStatsRepository.getActiveSlotIds(context)
        if (activeSlotIds.isNotEmpty()) {
            for (id in activeSlotIds) {
                SlotRepository.disableSlot(context, id)
            }
            SlotScheduler.rescheduleAll(context)
            onSlotDisabled()
        }

        refresh()
    }

    LaunchedEffect(Unit) { refresh() }

    val todayStats = remember(weekStats, statsRefreshToken) { StudyStatsRepository.getTodayStats(context) }
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 标题区
            Text(
                text = stringResource(R.string.study_statistics),
                style = MaterialTheme.typography.headlineLarge,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date()) },
                fontSize = 12.sp,
                color = AppColors.TextTertiary,
                letterSpacing = 0.4.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // 学习状态卡片（呼吸光环计时器）
            GlassCard(modifier = Modifier.padding(bottom = 14.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isStudying) {
                        val h = elapsedSeconds / 3600
                        val m = (elapsedSeconds % 3600) / 60
                        val s = elapsedSeconds % 60

                        // 光环进度：在时间段内显示真实进度，否则呼吸提示
                        val progress = if (slotRemainingMinutes > 0) {
                            val total = elapsedSeconds + slotRemainingMinutes * 60
                            (elapsedSeconds.toFloat() / total).coerceIn(0.02f, 1f)
                        } else 1f

                        Box(
                            modifier = Modifier.size(196.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            StudyRing(progress = progress, breathing = slotRemainingMinutes <= 0)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AppColors.Accent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.studying).replace("🟢 ", ""),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.Accent,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = String.format("%02d:%02d:%02d", h, m, s),
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary,
                                    letterSpacing = 1.sp
                                )
                                if (slotRemainingMinutes > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "剩余 ${slotRemainingMinutes} 分钟",
                                        fontSize = 11.sp,
                                        color = AppColors.TextTertiary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LiquidGlassButton(
                            text = stringResource(R.string.end_study),
                            primary = false,
                            onClick = { endStudySession() }
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isInSlot) "🟢" else "💤",
                            fontSize = 34.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(if (isInSlot) R.string.in_study_slot else R.string.not_in_study_slot),
                            fontSize = 13.sp,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LiquidGlassButton(
                            text = stringResource(R.string.start_study),
                            onClick = { startManualSession() }
                        )
                    }
                }
            }

            // 今日概览
            SectionLabel(text = stringResource(R.string.today_overview))
            GlassCard(modifier = Modifier.padding(bottom = 18.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
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

            // 周/月统计
            SectionLabel(text = "学习趋势")
            GlassCard(modifier = Modifier.padding(bottom = 14.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SegmentedTabs(
                        options = listOf(stringResource(R.string.this_week), stringResource(R.string.this_month)),
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (selectedTab == 0) {
                        PeriodNavigator(
                            label = "${weekDateRange.first} ~ ${weekDateRange.second}",
                            canGoNext = weekOffset < 0,
                            onPrev = { weekOffset-- },
                            onNext = { if (weekOffset < 0) weekOffset++ }
                        )
                        Spacer(modifier = Modifier.height(14.dp))

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
                        Spacer(modifier = Modifier.height(18.dp))
                        BarChart(
                            data = weekStats.map { it.totalMinutes },
                            labels = weekStats.map { it.label },
                            maxValue = (weekStats.maxOfOrNull { it.totalMinutes } ?: 60).coerceAtLeast(30)
                        )
                    } else {
                        PeriodNavigator(
                            label = monthLabel,
                            canGoNext = monthOffset < 0,
                            onPrev = { monthOffset-- },
                            onNext = { if (monthOffset < 0) monthOffset++ }
                        )
                        Spacer(modifier = Modifier.height(14.dp))

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
                        Spacer(modifier = Modifier.height(18.dp))
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

// ── 呼吸光环 ─────────────────────────────────────────────────

@Composable
private fun StudyRing(progress: Float, breathing: Boolean) {
    val infinite = rememberInfiniteTransition(label = "ring_breath")
    val breathAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )

    val trackColor = AppColors.LightDivider
    val accentColor = AppColors.Accent

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 10.dp.toPx()
        val inset = stroke / 2 + 2.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)

        // 轨道
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // 进度
        drawArc(
            color = accentColor.copy(alpha = if (breathing) breathAlpha else 1f),
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // 端点光晕
        if (progress > 0.02f) {
            val angle = Math.toRadians((-90f + 360f * progress).toDouble())
            val radius = arcSize.width / 2
            val cx = topLeft.x + radius + radius * Math.cos(angle).toFloat()
            val cy = topLeft.y + radius + radius * Math.sin(angle).toFloat()
            drawCircle(
                color = accentColor.copy(alpha = 0.25f),
                radius = stroke * 1.4f,
                center = Offset(cx, cy)
            )
        }
    }
}

// ── 分段式标签 ───────────────────────────────────────────────

@Composable
private fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(CornerRadius.md),
        color = AppColors.CardBgSoft,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Surface(
                    shape = RoundedCornerShape(CornerRadius.sm),
                    color = if (selected) AppColors.Accent else Color.Transparent,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(index) }
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) OnAccent else AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
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
            fontSize = 14.sp,
            color = AppColors.Accent,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onPrev() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Text(
            text = "▶",
            fontSize = 14.sp,
            color = if (canGoNext) AppColors.Accent else AppColors.TextTertiary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .then(if (canGoNext) Modifier.clickable { onNext() } else Modifier)
                .padding(horizontal = 10.dp, vertical = 6.dp)
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
    val label = "较上月"
    val color = if (diff > 0) AppColors.Accent else if (diff < 0) AppColors.AccentPink else AppColors.TextSecondary

    Surface(
        shape = RoundedCornerShape(CornerRadius.md),
        color = AppColors.CardBgSoft,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AppColors.LightDivider),
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
                fontSize = 11.sp,
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
        shape = RoundedCornerShape(CornerRadius.md),
        color = AppColors.CardBgSoft,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AppColors.LightDivider),
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
                fontSize = 11.sp,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun BarChart(
    data: List<Int>,
    labels: List<String>,
    maxValue: Int
) {
    val barColor = AppColors.Accent
    val barBg = AppColors.Accent.copy(alpha = 0.09f)
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
    val barBg = AppColors.Accent.copy(alpha = 0.09f)
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
