package com.study.reminder.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.reminder.model.StudySlot
import com.study.reminder.ui.compose.GlassCard
import com.study.reminder.ui.compose.LiquidGlassButton
import com.study.reminder.ui.compose.StudyReminderComposeTheme

class AddSlotActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "添加学习时间段"

        setContent {
            StudyReminderComposeTheme {
                AddSlotScreen(
                    onConfirm = { slot ->
                        setResult(Activity.RESULT_OK, Intent().putExtra("slot_json", slot.toJson().toString()))
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
private fun AddSlotScreen(
    onConfirm: (StudySlot) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var label by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf("") }
    var startMinute by remember { mutableStateOf("") }
    var endHour by remember { mutableStateOf("") }
    var endMinute by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }

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
            // 备注
            GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "备注（可选）",
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    BasicTextField(
                        value = label,
                        onValueChange = { label = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF10393F)),
                        cursorBrush = SolidColor(Color(0xFF39C5BB)),
                        decorationBox = { innerTextField ->
                            Box {
                                if (label.isEmpty()) {
                                    Text(
                                        text = "如：上午自习、晚间复习",
                                        fontSize = 15.sp,
                                        color = Color(0xFFBBBBBB)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            // 开始时间
            GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🕐 开始时间",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimeInputRow(
                        hour = startHour,
                        minute = startMinute,
                        onHourChange = { startHour = it },
                        onMinuteChange = { startMinute = it }
                    )
                }
            }

            // 结束时间
            GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🕑 结束时间",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimeInputRow(
                        hour = endHour,
                        minute = endMinute,
                        onHourChange = { endHour = it },
                        onMinuteChange = { endMinute = it }
                    )
                }
            }

            // 重复日期
            GlassCard(modifier = Modifier.padding(bottom = 24.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📅 重复日期",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    DaySelector(
                        selectedDays = selectedDays,
                        onToggle = { day ->
                            selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                        }
                    )
                }
            }

            // 按钮
            LiquidGlassButton(
                text = "确认添加",
                onClick = {
                    val sH = startHour.toIntOrNull()
                    val sM = startMinute.toIntOrNull()
                    val eH = endHour.toIntOrNull()
                    val eM = endMinute.toIntOrNull()

                    if (sH == null || sM == null || eH == null || eM == null) {
                        Toast.makeText(context, "请填写完整时间", Toast.LENGTH_SHORT).show()
                        return@LiquidGlassButton
                    }
                    if (sH !in 0..23 || eH !in 0..23) {
                        Toast.makeText(context, "小时请输入 0-23", Toast.LENGTH_SHORT).show()
                        return@LiquidGlassButton
                    }
                    if (sM !in 0..59 || eM !in 0..59) {
                        Toast.makeText(context, "分钟请输入 0-59", Toast.LENGTH_SHORT).show()
                        return@LiquidGlassButton
                    }
                    if (eH * 60 + eM <= sH * 60 + sM) {
                        Toast.makeText(context, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show()
                        return@LiquidGlassButton
                    }
                    if (selectedDays.isEmpty()) {
                        Toast.makeText(context, "请至少选择一天", Toast.LENGTH_SHORT).show()
                        return@LiquidGlassButton
                    }

                    onConfirm(
                        StudySlot(
                            label = label.trim().ifEmpty { "学习时间" },
                            startHour = sH, startMinute = sM,
                            endHour = eH, endMinute = eM,
                            enableDays = selectedDays
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 12.dp)
            )

            LiquidGlassButton(
                text = "取消",
                onClick = onCancel,
                primary = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }
    }
}

@Composable
private fun TimeInputRow(
    hour: String,
    minute: String,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeField(
            value = hour,
            onValueChange = { onHourChange(it.take(2)) },
            hint = "10",
            label = "小时 (0-23)",
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ":",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(40.dp)
                .padding(top = 16.dp),
            textAlign = TextAlign.Center
        )
        TimeField(
            value = minute,
            onValueChange = { onMinuteChange(it.take(2)) },
            hint = "00",
            label = "分钟 (0-59)",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimeField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
            modifier = Modifier
                .width(90.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xDDF9F4))
                .border(1.dp, Color(0x90FFFFFF), RoundedCornerShape(12.dp)),
            textStyle = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10393F),
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(Color(0xFF39C5BB)),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCCCCCC)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun DaySelector(
    selectedDays: Set<Int>,
    onToggle: (Int) -> Unit
) {
    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dayLabels.forEachIndexed { index, label ->
            val day = index + 1
            val isSelected = day in selectedDays
            val shape = RoundedCornerShape(12.dp)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape)
                    .background(
                        if (isSelected) Color(0xFF39C5BB) else Color(0xDDF9F4)
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF39C5BB) else Color(0x90FFFFFF),
                        shape
                    )
                    .clickable { onToggle(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color(0xFF4E7F84)
                )
            }
        }
    }
}
