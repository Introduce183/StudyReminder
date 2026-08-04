package com.study.reminder.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
    onThemeChange: (Boolean) -> Unit = {}
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
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // Study slots section
            item {
                GlassCard(modifier = Modifier.padding(bottom = 14.dp)) {
                    Column(modifier = Modifier.padding(18.dp)) {
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
                            Text(
                                text = stringResource(R.string.no_slot_hint),
                                color = AppColors.TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                textAlign = TextAlign.Center
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

            // Reminder methods section
            item {
                GlassCard(modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.remind_method),
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                        OptionSwitch(stringResource(R.string.notification_remind), notificationEnabled, onNotificationChange)
                        OptionSwitch(stringResource(R.string.island_remind), islandEnabled, onIslandChange)
                        OptionSwitch(stringResource(R.string.fullscreen_remind), fullScreenEnabled, onFullScreenChange)
                        OptionSwitch(stringResource(R.string.force_home_remind), forceHomeEnabled, onForceHomeChange)
                    }
                }
            }

            // Info hint
            item {
                Surface(
                    shape = RoundedCornerShape(CornerRadius.xl),
                    color = AppColors.InfoBg,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.slot_info_hint),
                        fontSize = 12.sp,
                        color = AppColors.InfoText,
                        modifier = Modifier.padding(14.dp),
                        lineHeight = 18.sp
                    )
                }
            }

            // Action buttons
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassButton(
                        text = stringResource(R.string.whitelist),
                        onClick = onWhitelist,
                        primary = false,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    LiquidGlassButton(
                        text = stringResource(R.string.accessibility_optional),
                        onClick = onAccessibilityGuide,
                        primary = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Theme switch
            item {
                GlassCard(modifier = Modifier.padding(top = 14.dp)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        OptionSwitch(
                            label = if (isDarkTheme) "深色模式" else "浅色模式",
                            checked = isDarkTheme,
                            onCheckedChange = onThemeChange
                        )
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
