package com.study.reminder.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.study.reminder.model.StudySlot

// ── 液态玻璃按钮 ──────────────────────────────────────────────

@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 90 else 170),
        label = "btn_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 90 else 170),
        label = "btn_alpha"
    )

    val shape = RoundedCornerShape(28.dp)

    val backgroundBrush = if (primary) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xB95CCFD8),
                Color(0xC46CD8E7),
                Color(0xBC78C8EB)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xC86FE6F0),
                Color(0xD37CEAF8),
                Color(0xCC8FD9FF)
            )
        )
    }

    val strokeColor = if (primary) Color(0xD6F7FFFF) else Color(0xF2EDFFFF)
    val textColor = if (primary) Color.White else Color(0xFF10393F)

    // 高光层
    val highlightBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xCCFFFFFF),
            Color(0x18FFFFFF)
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
            .clip(shape)
            .background(backgroundBrush)
            .border(BorderStroke(1.dp, strokeColor), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // 高光层（上半部分）
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 3.dp, top = 2.dp, end = 3.dp, bottom = 28.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(highlightBrush)
        )
        // 更亮的小高光
        if (primary) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(start = 10.dp, top = 8.dp, end = 90.dp, bottom = 34.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x66FFFFFF))
            )
        }
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

// ── 玻璃卡片 ──────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color(0xCCF7FFFF),
        border = BorderStroke(1.dp, Color(0x90FFFFFF))
    ) {
        Column(content = content)
    }
}

// ── 时间段列表项 ──────────────────────────────────────────────

@Composable
fun StudySlotItem(
    slot: StudySlot,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    val daysText = if (slot.enableDays.size == 7) "每天" else
        slot.enableDays.sorted().joinToString(" ") { "周${dayLabels[it - 1]}" }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xDDF9F4),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.label.ifEmpty { "学习时间" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10393F)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${slot.startLabel}  →  ${slot.endLabel}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF39C5BB),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = daysText,
                    fontSize = 12.sp,
                    color = Color(0xFF4E7F84)
                )
            }
            Switch(
                checked = slot.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 8.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color(0xFFFF77B7)
                )
            }
        }
    }
}
