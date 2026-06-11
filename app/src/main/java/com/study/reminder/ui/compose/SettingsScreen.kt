package com.study.reminder.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                    text = stringResource(R.string.settings_title),
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
                                text = stringResource(R.string.study_slots),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10393F)
                            )
                            LiquidGlassButton(
                                text = stringResource(R.string.add_slot),
                                onClick = onAddSlot
                            )
                        }

                        if (slots.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_slot_hint),
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
                GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.remind_method),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10393F),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OptionSwitch(stringResource(R.string.notification_remind), notificationEnabled, onNotificationChange)
                        OptionSwitch(stringResource(R.string.island_remind), islandEnabled, onIslandChange)
                        OptionSwitch(stringResource(R.string.small_window_remind), smallWindowEnabled, onSmallWindowChange)
                        OptionSwitch(stringResource(R.string.fullscreen_remind), fullScreenEnabled, onFullScreenChange)
                        OptionSwitch(stringResource(R.string.force_home_remind), forceHomeEnabled, onForceHomeChange)
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
                        text = stringResource(R.string.slot_info_hint),
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
