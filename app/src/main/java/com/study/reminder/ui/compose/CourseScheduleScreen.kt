package com.study.reminder.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.reminder.model.CourseSchedule
import com.study.reminder.model.CourseScheduleRepository
import com.study.reminder.model.WeekType

// 课程颜色池
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

private val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@Composable
fun CourseScheduleScreen(
    courses: List<CourseSchedule>,
    semesterStartDate: String,
    onImportIcs: () -> Unit,
    onToggleCourse: (CourseSchedule) -> Unit,
    onDeleteCourse: (CourseSchedule) -> Unit,
    onDeleteAll: () -> Unit
) {
    val context = LocalContext.current
    val weekNum = remember(semesterStartDate) { CourseScheduleRepository.getSemesterWeekNumber(context) }
    val isOdd = remember(semesterStartDate) { CourseScheduleRepository.isOddWeek(context) }

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
                text = "📅 课表模式",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10393F),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 学期信息
            if (semesterStartDate.isNotEmpty() && weekNum > 0) {
                Text(
                    text = "第${weekNum}周 · ${if (isOdd) "单周" else "双周"}",
                    fontSize = 14.sp,
                    color = Color(0xFF4E7F84),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 操作按钮
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
                // 空状态
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
                // 课表网格
                GlassCard {
                    TimetableGrid(
                        courses = courses,
                        onCourseClick = { },
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 课程列表
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "课程列表",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10393F),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val grouped = courses.groupBy { it.courseName }
                        for ((name, sessions) in grouped) {
                            CourseGroupItem(
                                name = name,
                                sessions = sessions,
                                onToggle = onToggleCourse,
                                onDelete = onDeleteCourse
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseGroupItem(
    name: String,
    sessions: List<CourseSchedule>,
    onToggle: (CourseSchedule) -> Unit,
    onDelete: (CourseSchedule) -> Unit
) {
    val color = courseColor(name)

    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10393F)
            )
        }

        for (session in sessions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${session.dayLabel} ${session.startLabel}-${session.endLabel}" +
                                (if (session.location.isNotEmpty()) " · ${session.location}" else "") +
                                (if (session.weekTypeLabel.isNotEmpty()) " (${session.weekTypeLabel})" else ""),
                        fontSize = 13.sp,
                        color = Color(0xFF4E7F84)
                    )
                }
                Switch(
                    checked = session.enabled,
                    onCheckedChange = { onToggle(session) },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "×",
                    fontSize = 18.sp,
                    color = Color(0xFFCCCCCC),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onDelete(session) }
                        .padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color(0x20000000))
    }
}

@Composable
private fun TimetableGrid(
    courses: List<CourseSchedule>,
    onCourseClick: (CourseSchedule) -> Unit,
    modifier: Modifier = Modifier
) {
    if (courses.isEmpty()) return

    val hourHeight = 56.dp
    val labelWidth = 40.dp
    val dayWidth = 48.dp

    // 计算时间范围
    val minHour = courses.minOf { it.startHour }.coerceAtMost(8)
    val maxHour = (courses.maxOf { it.endHour } + 1).coerceAtLeast(22)
    val hours = (minHour until maxHour).toList()

    Row(modifier = modifier) {
        // 时间标签列
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

        // 7天列
        for (day in 1..7) {
            Box(
                modifier = Modifier
                    .width(dayWidth)
                    .height(hourHeight * hours.size)
            ) {
                // 小时分割线
                for (hour in hours) {
                    val offsetY = ((hour - minHour) * hourHeight.value).dp
                    HorizontalDivider(
                        color = Color(0x15000000),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = offsetY)
                    )
                }

                // 课程块
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
                            .height(duration.dp.coerceAtLeast(20.dp)),
                        onClick = { onCourseClick(course) }
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
                            if (course.weekType != WeekType.ALL) {
                                Text(
                                    text = course.weekTypeLabel,
                                    fontSize = 7.sp,
                                    color = color.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
