package com.study.reminder.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.reminder.model.StudySlot

@Composable
fun SettingsScreen(
    slots: List<StudySlot>,
    notificationEnabled: Boolean,
    islandEnabled: Boolean,
    smallWindowEnabled: Boolean,
    fullScreenEnabled: Boolean,
    forceHomeEnabled: Boolean,
    onAddSlot: () -> Unit,
    onToggleSlot: (StudySlot) -> Unit,
    onDeleteSlot: (StudySlot) -> Unit,
    onEditSlot: (StudySlot) -> Unit,
    onNotificationChange: (Boolean) -> Unit,
    onIslandChange: (Boolean) -> Unit,
    onSmallWindowChange: (Boolean) -> Unit,
    onFullScreenChange: (Boolean) -> Unit,
    onForceHomeChange: (Boolean) -> Unit,
    onWhitelist: () -> Unit,
    onAccessibilityGuide: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFEAFBF8)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            item {
                Text(
                    text = "🎧 学习提醒器",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10393F),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            item {
                GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 学习时间段",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10393F)
                            )
                            LiquidGlassButton(
                                text = "＋ 添加",
                                onClick = onAddSlot
                            )
                        }

                        if (slots.isEmpty()) {
                            Text(
                                text = "还没有学习时间段\n点「添加」设置，如 10:00 → 11:30",
                                color = Color(0xFF4E7F84),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(slots, key = { it.id }) { slot ->
                StudySlotItem(
                    slot = slot,
                    onToggle = { onToggleSlot(slot) },
                    onEdit = { onEditSlot(slot) },
                    onDelete = { onDeleteSlot(slot) }
                )
            }

            item {
                GlassCard(modifier = Modifier.padding(bottom = 16.dp, top = if (slots.isNotEmpty()) 6.dp else 0.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "提醒方式",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10393F),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OptionSwitch("🔔 通知提醒（多次刷新）", notificationEnabled, onNotificationChange)
                        OptionSwitch("💊 超级岛提醒", islandEnabled, onIslandChange)
                        OptionSwitch("🪟 后台弹出小窗", smallWindowEnabled, onSmallWindowChange)
                        OptionSwitch("📱 全屏弹出（确认返桌面）", fullScreenEnabled, onFullScreenChange)
                        OptionSwitch("🏠 强制返回桌面", forceHomeEnabled, onForceHomeChange)
                    }
                }
            }

            item {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    color = Color(0xE8FFFA),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "💡 时间段到点提醒无需无障碍权限，直接生效！\n无障碍权限仅用于检测「学习中途开手机」功能，可选开启。",
                        fontSize = 13.sp,
                        color = Color(0xFF17646B),
                        modifier = Modifier.padding(14.dp),
                        lineHeight = 18.sp
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassButton(
                        text = "✅ 白名单",
                        onClick = onWhitelist,
                        primary = false,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    LiquidGlassButton(
                        text = "♿ 无障碍（可选）",
                        onClick = onAccessibilityGuide,
                        primary = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun OptionSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF10393F),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
