package com.study.reminder.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.reminder.R
import com.study.reminder.model.StudySlot

@Composable
fun SettingsScreen(
    slots: List<StudySlot>,
    notificationEnabled: Boolean,
    islandEnabled: Boolean,
    fullScreenEnabled: Boolean,
    forceHomeEnabled: Boolean,
    isDarkTheme: Boolean = true,
    accessibilityEnabled: Boolean = false,
    keepAliveEnabled: Boolean = true,
    batteryOptimizationIgnored: Boolean = false,
    onAddSlot: () -> Unit,
    onToggleSlot: (StudySlot) -> Unit,
    onDeleteSlot: (StudySlot) -> Unit,
    onEditSlot: (StudySlot) -> Unit,
    onNotificationChange: (Boolean) -> Unit,
    onIslandChange: (Boolean) -> Unit,
    onFullScreenChange: (Boolean) -> Unit,
    onForceHomeChange: (Boolean) -> Unit,
    onWhitelist: () -> Unit,
    onAccessibilityGuide: () -> Unit,
    onThemeChange: (Boolean) -> Unit = {},
    onKeepAliveChange: (Boolean) -> Unit = {},
    onRequestBatteryOptimization: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.Background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "提醒方式与学习监督",
                    fontSize = 12.sp,
                    color = AppColors.TextTertiary,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // ── 学习时间段 ──
            item {
                SectionLabel(text = "学习时间段")
                GlassCard(modifier = Modifier.padding(bottom = 18.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.study_slots),
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.TextPrimary
                            )
                            LiquidGlassButton(
                                text = stringResource(R.string.add_slot),
                                onClick = onAddSlot
                            )
                        }

                        if (slots.isEmpty()) {
                            EmptyState(
                                emoji = "🗓️",
                                title = stringResource(R.string.no_slot_hint).substringBefore("\n"),
                                hint = "点「添加」设置，如 10:00 → 11:30"
                            )
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            slots.forEach { slot ->
                                StudySlotItem(
                                    slot = slot,
                                    onToggle = { onToggleSlot(slot) },
                                    onEdit = { onEditSlot(slot) },
                                    onDelete = { onDeleteSlot(slot) }
                                )
                            }
                        }
                    }
                }
            }

            // ── 提醒方式 ──
            item {
                SectionLabel(text = stringResource(R.string.remind_method))
                GlassCard(modifier = Modifier.padding(bottom = 18.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OptionSwitch(stringResource(R.string.notification_remind), notificationEnabled, onNotificationChange)
                        OptionSwitch(stringResource(R.string.island_remind), islandEnabled, onIslandChange)
                        OptionSwitch(stringResource(R.string.fullscreen_remind), fullScreenEnabled, onFullScreenChange)
                        OptionSwitch(stringResource(R.string.force_home_remind), forceHomeEnabled, onForceHomeChange)

                        Surface(
                            shape = RoundedCornerShape(CornerRadius.md),
                            color = AppColors.InfoBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.slot_info_hint),
                                fontSize = 12.sp,
                                color = AppColors.InfoText,
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // ── 其他设置 ──
            item {
                SectionLabel(text = "其他")
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 主题切换
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isDarkTheme) "🌙" else "☀️",
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDarkTheme) "深色模式" else "浅色模式",
                                    color = AppColors.TextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = onThemeChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AppColors.White,
                                    checkedTrackColor = AppColors.Accent,
                                    uncheckedThumbColor = AppColors.White,
                                    uncheckedTrackColor = AppColors.TextTertiary,
                                    uncheckedBorderColor = AppColors.TextTertiary
                                )
                            )
                        }

                        // 后台保活
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🛡️", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "后台保活（常驻通知）",
                                        color = AppColors.TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (keepAliveEnabled) "已开启，降低被系统清理的概率" else "已关闭",
                                        color = AppColors.TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Switch(
                                checked = keepAliveEnabled,
                                onCheckedChange = onKeepAliveChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AppColors.White,
                                    checkedTrackColor = AppColors.Accent,
                                    uncheckedThumbColor = AppColors.White,
                                    uncheckedTrackColor = AppColors.TextTertiary,
                                    uncheckedBorderColor = AppColors.TextTertiary
                                )
                            )
                        }

                        // 电池优化白名单
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CornerRadius.sm))
                                .clickable { onRequestBatteryOptimization() }
                                .padding(vertical = 6.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔋", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "电池优化",
                                        color = AppColors.TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (batteryOptimizationIgnored) "已加入白名单" else "建议设为「无限制」，减少后台清理",
                                        color = AppColors.TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = if (batteryOptimizationIgnored) "已允许" else "去设置",
                                color = AppColors.Accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(
                            color = AppColors.LightDivider,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassButton(
                                text = stringResource(R.string.whitelist),
                                onClick = onWhitelist,
                                primary = false,
                                modifier = Modifier.weight(1f)
                            )
                            LiquidGlassButton(
                                text = if (accessibilityEnabled) "♿ 无障碍：已开启" else "♿ 无障碍：未开启",
                                onClick = onAccessibilityGuide,
                                primary = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
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
            color = AppColors.TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppColors.White,
                checkedTrackColor = AppColors.Accent,
                uncheckedThumbColor = AppColors.White,
                uncheckedTrackColor = AppColors.TextTertiary,
                uncheckedBorderColor = AppColors.TextTertiary
            )
        )
    }
}
