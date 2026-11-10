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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.reminder.model.StudySlot

// ── Easing Curves (Emil Kowalski style) ───────────────────────

private val EaseOut = androidx.compose.animation.core.CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

// ── Section Label ─────────────────────────────────────────────

/** 分组小标题：左侧短竖条 + 次级文字，用于划分页面区块 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppColors.Accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSecondary,
            letterSpacing = 0.6.sp
        )
    }
}

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
        shadowElevation = 2.dp,
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
        animationSpec = tween(
            durationMillis = if (isPressed) 100 else 160,
            easing = EaseOut
        ),
        label = "btn_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(
            durationMillis = if (isPressed) 100 else 160,
            easing = EaseOut
        ),
        label = "btn_alpha"
    )

    val shape = RoundedCornerShape(CornerRadius.lg)

    val backgroundBrush = if (primary) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2DB5AB),
                Color(0xFF39C5BB),
                Color(0xFF45CFC5)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                AppColors.InputBg,
                AppColors.InputBg.copy(alpha = 0.95f)
            )
        )
    }

    val strokeColor = if (primary) Color(0x30FFFFFF) else AppColors.GlassStroke
    // 主色上的深色文字对比度更高，视觉更精致
    val textColor = if (primary) OnAccent else AppColors.TextPrimary

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .then(
                if (primary) {
                    Modifier.shadow(
                        elevation = if (isPressed) 2.dp else 8.dp,
                        shape = shape,
                        ambientColor = AppColors.Accent.copy(alpha = 0.4f),
                        spotColor = AppColors.Accent.copy(alpha = 0.4f)
                    )
                } else Modifier
            )
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

// ── Empty State ──────────────────────────────────────────────

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    hint: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 40.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        if (hint != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                fontSize = 12.sp,
                color = AppColors.TextTertiary,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
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
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, AppColors.LightDivider),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧状态条：启用时显示主色
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        if (slot.enabled) AppColors.Accent else Color.Transparent
                    )
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 13.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slot.label.ifEmpty { "学习时间" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (slot.enabled) AppColors.TextPrimary else AppColors.TextTertiary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${slot.startLabel}  →  ${slot.endLabel}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (slot.enabled) AppColors.Accent else AppColors.TextTertiary,
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
}
