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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.study.reminder.model.StudySlot

// ── Glass Card ────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(CornerRadius.xl)
    Surface(
        modifier = modifier,
        shape = shape,
        color = AppColors.CardBg,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, AppColors.GlassStroke)
    ) {
        Column(content = content)
    }
}

// ── Liquid Glass Button ───────────────────────────────────────

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
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 80 else 150),
        label = "btn_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 80 else 150),
        label = "btn_alpha"
    )

    val shape = RoundedCornerShape(CornerRadius.lg)

    val backgroundBrush = if (primary) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF30BDB4),
                Color(0xFF39C5BB),
                Color(0xFF45CFC0)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFF0F7F6),
                Color(0xFFEFF6F5)
            )
        )
    }

    val strokeColor = if (primary) Color(0x40FFFFFF) else Color(0xFFD8ECE9)
    val textColor = if (primary) Color.White else AppColors.TextPrimary

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
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 0.2.sp
        )
    }
}

// ── Study Slot Item ───────────────────────────────────────────

@Composable
fun StudySlotItem(
    slot: StudySlot,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    val daysText = if (slot.enableDays.size == 7) "每天" else
        slot.enableDays.sorted().joinToString(" ") { "周${dayLabels[it - 1]}" }

    Surface(
        shape = RoundedCornerShape(CornerRadius.lg),
        color = AppColors.CardSoft,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AppColors.LightDivider),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = CornerRadius.sm.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.label.ifEmpty { "学习时间" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${slot.startLabel}  →  ${slot.endLabel}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Accent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = daysText,
                    fontSize = 11.sp,
                    color = AppColors.TextSecondary,
                    letterSpacing = 0.3.sp
                )
            }
            Switch(
                checked = slot.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 2.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppColors.White,
                    checkedTrackColor = AppColors.Accent,
                    uncheckedThumbColor = AppColors.White,
                    uncheckedTrackColor = AppColors.TextTertiary,
                    uncheckedBorderColor = AppColors.TextTertiary
                )
            )
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = "✏️",
                    fontSize = 14.sp
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = AppColors.AccentPink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
