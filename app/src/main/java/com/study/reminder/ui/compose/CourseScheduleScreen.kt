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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.reminder.model.CourseSchedule
import com.study.reminder.model.CourseScheduleRepository
import java.text.SimpleDateFormat
import java.util.*

private val courseColors = listOf(
    Color(0xFF39C5BB), Color(0xFFFF77B7), Color(0xFF6C9CFF),
    Color(0xFFFFB347), Color(0xFF9B59B6), Color(0xFF5DADE2),
    Color(0xFF58D68D), Color(0xFFEC7063), Color(0xFF45B7D1),
    Color(0xFFDDA0DD)
)

private fun courseColor(name: String): Color {
    val idx = (name.hashCode().and(0x7FFFFFFF)) % courseColors.size
    return courseColors[idx]
}

@Composable
fun CourseScheduleScreen(
    courses: List<CourseSchedule>,
    semesterStartDate: String,
    onImportIcs: () -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onDeleteAll: () -> Unit
) {
    val context = LocalContext.current
    val weekNum = remember(semesterStartDate) { CourseScheduleRepository.getSemesterWeekNumber(context) }
    val allEnabled = courses.isNotEmpty() && courses.all { it.enabled }

    // Calculate total weeks from semester start or course data
    val totalWeeks = remember(courses, semesterStartDate) {
        calculateTotalWeeks(courses, semesterStartDate)
    }

    // Current display week (default to current semester week, or 1)
    var displayWeek by remember { mutableIntStateOf(if (weekNum > 0) weekNum else 1) }

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
            // Title
            Text(
                text = "📅 课表模式",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10393F),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Semester info + master toggle
            if (courses.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (weekNum > 0) "当前第${weekNum}周" else "课表已导入",
                        fontSize = 14.sp,
                        color = Color(0xFF4E7F84)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "监督开关",
                            fontSize = 14.sp,
                            color = Color(0xFF4E7F84)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = allEnabled,
                            onCheckedChange = { onToggleAll(it) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action buttons
            Row(modifier = Modifier.fillMaxWidth()) {
                LiquidGlassButton(
                    text = "导入课表",
                    onClick = onImportIcs,
                    modifier = Modifier.weight(1f)
                )
                if (courses.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    LiquidGlassButton(
                        text = "清空课表",
                        primary = false,
                        onClick = onDeleteAll,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (courses.isEmpty()) {
                // Empty state
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📚", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "还没有导入课表",
                            fontSize = 16.sp,
                            color = Color(0xFF4E7F84)
                        )
                        Text(
                            text = "点击「导入课表」选择 .ics 文件",
                            fontSize = 13.sp,
                            color = Color(0xFF4E7F84),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                // Week selector
                if (totalWeeks > 1) {
                    WeekSelector(
                        totalWeeks = totalWeeks,
                        currentWeek = displayWeek,
                        onSelectWeek = { displayWeek = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Filter courses for selected week
                val weekCourses = remember(courses, displayWeek, semesterStartDate) {
                    filterCoursesForWeek(courses, displayWeek, semesterStartDate, dateFormatTL.get()!!)
                }

                // Timetable grid
                val weekDates = remember(displayWeek, semesterStartDate) {
                    getWeekDates(displayWeek, semesterStartDate)
                }
                GlassCard {
                    TimetableGrid(
                        courses = weekCourses,
                        weekDates = weekDates,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (weekCourses.isEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "本周没有课程",
                        fontSize = 14.sp,
                        color = Color(0xFF4E7F84),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekSelector(
    totalWeeks: Int,
    currentWeek: Int,
    onSelectWeek: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val buttonWidth = 40.dp
    val spacing = 6.dp
    val density = LocalDensity.current

    // Center the selected week in the scrollable area
    LaunchedEffect(currentWeek) {
        val buttonWidthPx = with(density) { buttonWidth.toPx() }
        val spacingPx = with(density) { spacing.toPx() }
        val containerWidthPx = with(density) { 300.dp.toPx() } // approximate visible width
        val targetPx = (currentWeek - 1) * (buttonWidthPx + spacingPx) - containerWidthPx / 2 + buttonWidthPx / 2
        scrollState.scrollTo(targetPx.toInt().coerceAtLeast(0))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (week in 1..totalWeeks) {
            val isSelected = week == currentWeek
            Box(
                modifier = Modifier
                    .size(width = buttonWidth, height = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Color(0xFF39C5BB) else Color(0x15000000)
                    )
                    .clickable { onSelectWeek(week) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$week",
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color(0xFF4E7F84)
                )
            }
        }
    }
}

@Composable
private fun TimetableGrid(
    courses: List<CourseSchedule>,
    weekDates: List<String>,
    modifier: Modifier = Modifier
) {
    if (courses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "本周无课程",
                fontSize = 14.sp,
                color = Color(0xFF4E7F84)
            )
        }
        return
    }

    val hourHeight = 56.dp
    val labelWidth = 40.dp
    val dayWidth = 48.dp

    val minHour = courses.minOf { it.startHour }.coerceAtMost(8)
    val maxHour = (courses.maxOf { it.endHour } + 1).coerceAtLeast(22)
    val hours = (minHour until maxHour).toList()

    // Day headers
    val dayHeaders = listOf("", "一", "二", "三", "四", "五", "六", "日")

    Column(modifier = modifier) {
        // Header row — day name + date
        Row {
            Box(modifier = Modifier.width(labelWidth))
            for (day in 1..7) {
                Column(
                    modifier = Modifier.width(dayWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayHeaders[day],
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10393F)
                    )
                    if (day <= weekDates.size) {
                        Text(
                            text = weekDates[day - 1],
                            fontSize = 9.sp,
                            color = Color(0xFF4E7F84)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Grid
        Row {
            // Time labels
            Column {
                for (hour in hours) {
                    Box(
                        modifier = Modifier
                            .width(labelWidth)
                            .height(hourHeight),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text(
                            text = String.format("%02d:00", hour),
                            fontSize = 10.sp,
                            color = Color(0xFF4E7F84),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }

            // 7 day columns
            for (day in 1..7) {
                Box(
                    modifier = Modifier
                        .width(dayWidth)
                        .height(hourHeight * hours.size)
                ) {
                    // Hour grid lines
                    for (hour in hours) {
                        val offsetY = ((hour - minHour) * hourHeight.value).dp
                        HorizontalDivider(
                            color = Color(0x15000000),
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = offsetY)
                        )
                    }

                    // Course blocks
                    for (course in courses.filter { it.dayOfWeek == day }) {
                        val startOffset = ((course.startHour - minHour) + course.startMinute / 60f) * hourHeight.value
                        val duration = ((course.endHour - course.startHour) + (course.endMinute - course.startMinute) / 60f) * hourHeight.value
                        val color = courseColor(course.courseName)

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = color.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 1.dp)
                                .offset(y = startOffset.dp)
                                .height(duration.dp.coerceAtLeast(20.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(2.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = course.courseName,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (course.location.isNotEmpty()) {
                                    Text(
                                        text = course.location,
                                        fontSize = 7.sp,
                                        color = color.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────

private val dateFormatTL = java.lang.ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

private fun getWeekDates(weekNum: Int, semesterStart: String): List<String> {
    val dateFormat = dateFormatTL.get()!!
    return try {
        val start = dateFormat.parse(semesterStart)!!
        val cal = Calendar.getInstance().apply {
            time = start
            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) add(Calendar.DAY_OF_YEAR, -1)
            add(Calendar.DAY_OF_YEAR, (weekNum - 1) * 7)
        }
        (0..6).map { offset ->
            val d = Calendar.getInstance().apply {
                time = cal.time
                add(Calendar.DAY_OF_YEAR, offset)
            }
            "${d.get(Calendar.MONTH) + 1}/${d.get(Calendar.DAY_OF_MONTH)}"
        }
    } catch (_: Exception) { emptyList() }
}

private fun calculateTotalWeeks(courses: List<CourseSchedule>, semesterStart: String): Int {
    val dateFormat = dateFormatTL.get()!!

    // Find the latest end date across all courses
    var maxDate = ""
    for (course in courses) {
        for (range in course.weekRanges) {
            if (range[1] > maxDate) maxDate = range[1]
        }
    }

    if (maxDate.isEmpty() || semesterStart.isEmpty()) return 16 // default

    return try {
        val start = dateFormat.parse(semesterStart)!!
        // Adjust to Monday
        val startCal = Calendar.getInstance().apply {
            time = start
            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) add(Calendar.DAY_OF_YEAR, -1)
        }
        val end = dateFormat.parse(maxDate)!!
        val diffMs = end.time - startCal.timeInMillis
        val weeks = (diffMs / (7 * 24 * 60 * 60 * 1000)).toInt() + 1
        weeks.coerceIn(1, 30)
    } catch (_: Exception) { 16 }
}

private fun filterCoursesForWeek(
    courses: List<CourseSchedule>,
    weekNum: Int,
    semesterStart: String,
    dateFormat: SimpleDateFormat
): List<CourseSchedule> {
    if (semesterStart.isEmpty()) return courses

    // Calculate Monday of the target week
    val weekMonday = try {
        val start = dateFormat.parse(semesterStart)!!
        val cal = Calendar.getInstance().apply {
            time = start
            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) add(Calendar.DAY_OF_YEAR, -1)
            add(Calendar.DAY_OF_YEAR, (weekNum - 1) * 7)
        }
        dateFormat.format(cal.time)
    } catch (_: Exception) { return courses }

    // Calculate Sunday of the target week
    val weekSunday = try {
        val cal = Calendar.getInstance().apply {
            time = dateFormat.parse(weekMonday)!!
            add(Calendar.DAY_OF_YEAR, 6)
        }
        dateFormat.format(cal.time)
    } catch (_: Exception) { return courses }

    return courses.filter { course ->
        if (course.weekRanges.isEmpty()) return@filter true // no limit = always active
        // Check if any week range overlaps with the target week
        course.weekRanges.any { range ->
            range[0] <= weekSunday && range[1] >= weekMonday
        }
    }
}
