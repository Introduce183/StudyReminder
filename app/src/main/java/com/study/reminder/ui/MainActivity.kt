package com.study.reminder.ui

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.Secure
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.study.reminder.R
import com.study.reminder.helper.NotificationHelper
import com.study.reminder.helper.SlotScheduler
import com.study.reminder.model.SlotRepository
import com.study.reminder.model.StudySlot
import com.study.reminder.ui.compose.GlassCard
import com.study.reminder.ui.compose.LiquidGlassButton
import com.study.reminder.ui.compose.StudySlotItem
import com.study.reminder.ui.compose.StudyReminderComposeTheme
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "study_prefs"
        private const val KEY_STARTUP_BACKGROUND_GUIDE_SHOWN = "startup_background_guide_shown"
        private const val KEY_FULLSCREEN_BACKGROUND_GUIDE_SHOWN = "fullscreen_background_guide_shown"
    }

    private var isShowingStartupGuide = false
    private var activeGuideDialog: BottomSheetDialog? = null

    // Compose state holders
    private val slotsState = mutableStateListOf<StudySlot>()
    private val notificationState = mutableStateOf(true)
    private val islandState = mutableStateOf(false)
    private val smallWindowState = mutableStateOf(false)
    private val fullScreenState = mutableStateOf(false)
    private val forceHomeState = mutableStateOf(false)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    private val addSlotLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val json = result.data?.getStringExtra("slot_json") ?: return@registerForActivityResult
            val slot = StudySlot.fromJson(JSONObject(json))
            SlotRepository.addSlot(this, slot)
            SlotScheduler.rescheduleAll(this)
            refreshSlotList()
            Toast.makeText(this, "已添加「${slot.label}」并重新安排提醒", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannels(this)

        // Load initial data
        loadSlots()
        loadOptions()

        setContent {
            StudyReminderComposeTheme {
                MainScreen(
                    slots = slotsState,
                    notificationEnabled = notificationState.value,
                    islandEnabled = islandState.value,
                    smallWindowEnabled = smallWindowState.value,
                    fullScreenEnabled = fullScreenState.value,
                    forceHomeEnabled = forceHomeState.value,
                    onAddSlot = {
                        addSlotLauncher.launch(Intent(this, AddSlotActivity::class.java))
                    },
                    onToggleSlot = { slot ->
                        SlotRepository.toggleSlot(this, slot.id)
                        SlotScheduler.rescheduleAll(this)
                        refreshSlotList()
                    },
                    onDeleteSlot = { slot ->
                        val slotName = slot.label.ifEmpty { "${slot.startLabel} - ${slot.endLabel}" }
                        AlertDialog.Builder(this)
                            .setTitle("删除时间段")
                            .setMessage("确定删除「$slotName」吗？")
                            .setPositiveButton("删除") { _, _ ->
                                SlotRepository.removeSlot(this, slot.id)
                                SlotScheduler.rescheduleAll(this)
                                refreshSlotList()
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    },
                    onNotificationChange = { checked ->
                        notificationState.value = checked
                        saveOptions()
                    },
                    onIslandChange = { checked ->
                        islandState.value = checked
                        if (checked) maybeGuideOverlayPermission()
                        saveOptions()
                    },
                    onSmallWindowChange = { checked ->
                        smallWindowState.value = checked
                        if (checked) {
                            fullScreenState.value = false
                            forceHomeState.value = false
                            maybeGuideOverlayPermission()
                        }
                        saveOptions()
                    },
                    onFullScreenChange = { checked ->
                        fullScreenState.value = checked
                        if (checked) {
                            smallWindowState.value = false
                            forceHomeState.value = false
                            maybeGuideOverlayPermissionForFullscreen()
                        }
                        saveOptions()
                    },
                    onForceHomeChange = { checked ->
                        forceHomeState.value = checked
                        if (checked) {
                            smallWindowState.value = false
                            fullScreenState.value = false
                            maybeGuideOverlayPermissionForFullscreen()
                        }
                        saveOptions()
                    },
                    onWhitelist = {
                        startActivity(Intent(this, AppPickerActivity::class.java))
                    },
                    onAccessibilityGuide = {
                        showGuideSheet(
                            title = "关于无障碍权限",
                            message = "无障碍权限只用于在学习时间内检测是否切到白名单之外的应用。\n\n不开启也不会影响固定时间段的到点提醒。",
                            primaryText = "去开启",
                            secondaryText = "暂不"
                        ) {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    }
                )
            }
        }

        requestCorePermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshSlotList()
        SlotScheduler.rescheduleAll(this)
        maybeShowStartupGuide()
    }

    private fun loadSlots() {
        slotsState.clear()
        slotsState.addAll(SlotRepository.getSlots(this))
    }

    private fun refreshSlotList() {
        val slots = SlotRepository.getSlots(this)
        slotsState.clear()
        slotsState.addAll(slots)
    }

    private fun loadOptions() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        notificationState.value = prefs.getBoolean("opt_notification", true)
        islandState.value = prefs.getBoolean("opt_island", false)
        smallWindowState.value = prefs.getBoolean("opt_small_window", false)
        fullScreenState.value = prefs.getBoolean("opt_fullscreen", false)
        forceHomeState.value = prefs.getBoolean("opt_force_home", false)
    }

    private fun saveOptions() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putBoolean("opt_notification", notificationState.value)
            putBoolean("opt_island", islandState.value)
            putBoolean("opt_small_window", smallWindowState.value)
            putBoolean("opt_fullscreen", fullScreenState.value)
            putBoolean("opt_force_home", forceHomeState.value)
        }.apply()
    }

    private fun requestCorePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(this).areNotificationsEnabled()
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        maybeGuideExactAlarmPermission()
    }

    private fun maybeShowStartupGuide() {
        if (isShowingStartupGuide) return

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val startupIntroShown = prefs.getBoolean("startup_intro_shown", false)
        val backgroundGuideShown = prefs.getBoolean(KEY_STARTUP_BACKGROUND_GUIDE_SHOWN, false)
        val accessibilityReminded = prefs.getBoolean("startup_accessibility_reminded", false)

        if (!startupIntroShown) {
            isShowingStartupGuide = true
            showGuideSheet(
                title = "首次使用建议先完成这些设置",
                message = "为了让提醒尽量稳定，建议先检查以下权限：\n\n1. 允许显示悬浮窗\n2. 允许后台弹出界面\n3. 建议加入自启动和电池优化白名单\n\n其中悬浮窗可以直接跳转授权；后台弹出界面和自启动通常是厂商设置，应用会尽量帮你打开对应页面。",
                primaryText = "去设置",
                secondaryText = "稍后",
                onDismiss = { isShowingStartupGuide = false }
            ) {
                prefs.edit().putBoolean("startup_intro_shown", true).apply()
                isShowingStartupGuide = false
                maybeShowStartupGuide()
            }
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            isShowingStartupGuide = true
            showGuideSheet(
                title = "需要悬浮窗权限",
                message = "动态岛、小窗、全屏悬浮提醒都依赖悬浮窗权限。点击继续后会跳到系统设置页，请手动打开。",
                primaryText = "去授权",
                secondaryText = "暂不",
                onDismiss = { isShowingStartupGuide = false }
            ) {
                isShowingStartupGuide = false
                overlayPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            return
        }

        if (!backgroundGuideShown) {
            isShowingStartupGuide = true
            showGuideSheet(
                title = "建议打开后台弹出和自启动",
                message = "为了让提醒更稳定，建议继续检查后台弹出界面、自启动和电池优化白名单。不同机型入口不一样，应用会尽量帮你打开对应页面。",
                primaryText = "去设置",
                secondaryText = "先跳过",
                onDismiss = {
                    prefs.edit().putBoolean(KEY_STARTUP_BACKGROUND_GUIDE_SHOWN, true).apply()
                    isShowingStartupGuide = false
                }
            ) {
                prefs.edit().putBoolean(KEY_STARTUP_BACKGROUND_GUIDE_SHOWN, true).apply()
                isShowingStartupGuide = false
                openBackgroundPermissionSettings()
            }
            return
        }

        if (!accessibilityReminded && !isAccessibilityServiceEnabled()) {
            isShowingStartupGuide = true
            showGuideSheet(
                title = "建议开启无障碍权限",
                message = "无障碍权限不是固定时间段到点提醒的必需项。\n\n它的作用是：在学习时间内，如果你切到白名单之外的应用，软件可以及时检测并再次提醒你。\n\n如果你只需要普通到点提醒，也可以暂时不开。",
                primaryText = "去开启",
                secondaryText = "先不",
                onDismiss = { isShowingStartupGuide = false }
            ) {
                prefs.edit().putBoolean("startup_accessibility_reminded", true).apply()
                isShowingStartupGuide = false
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    private fun maybeGuideOverlayPermission() {
        if (Settings.canDrawOverlays(this)) return

        showGuideSheet(
            title = "需要悬浮窗权限",
            message = "动态岛、小窗、全屏悬浮提醒都依赖悬浮窗权限。点击继续后会跳到系统设置页，请手动打开。",
            primaryText = "去授权",
            secondaryText = "暂不"
        ) {
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun maybeGuideOverlayPermissionForFullscreen() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        if (Settings.canDrawOverlays(this)) {
            if (prefs.getBoolean(KEY_FULLSCREEN_BACKGROUND_GUIDE_SHOWN, false)) return

            showGuideSheet(
                title = "还建议打开后台弹出和自启动",
                message = "全屏弹出和强制回桌面已经基于悬浮窗实现，但在部分机型上，仍建议额外允许后台弹出界面、自启动和电池白名单，以提高稳定性。",
                primaryText = "去设置",
                secondaryText = "??",
                onDismiss = {
                    prefs.edit().putBoolean(KEY_FULLSCREEN_BACKGROUND_GUIDE_SHOWN, true).apply()
                }
            ) {
                prefs.edit()
                    .putBoolean(KEY_STARTUP_BACKGROUND_GUIDE_SHOWN, true)
                    .putBoolean(KEY_FULLSCREEN_BACKGROUND_GUIDE_SHOWN, true)
                    .apply()
                openBackgroundPermissionSettings()
            }
            return
        }

        showGuideSheet(
            title = "需要悬浮窗和后台弹出权限",
            message = "全屏弹出和强制回桌面依赖悬浮窗权限，部分机型还需要手动允许后台弹出界面。点击继续后会带你去设置。",
            primaryText = "去设置",
            secondaryText = "??"
        ) {
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun maybeGuideExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val alarmManager = getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) return

        showGuideSheet(
            title = "需要精确闹钟权限",
            message = "学习时间段依赖精确闹钟，到点提醒会更稳定。点击继续后会跳到系统设置页，请手动允许。",
            primaryText = "去开启",
            secondaryText = "暂不"
        ) {
            exactAlarmPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun showGuideSheet(
        title: String,
        message: String,
        primaryText: String,
        secondaryText: String,
        onDismiss: (() -> Unit)? = null,
        onPrimary: () -> Unit
    ) {
        activeGuideDialog?.dismiss()
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_permission_guide, null)
        view.findViewById<TextView>(R.id.tv_sheet_title).text = title
        view.findViewById<TextView>(R.id.tv_sheet_message).text = message
        val primaryButton = view.findViewById<Button>(R.id.btn_sheet_primary)
        val secondaryButton = view.findViewById<Button>(R.id.btn_sheet_secondary)
        primaryButton.text = primaryText
        secondaryButton.text = secondaryText
        primaryButton.setOnClickListener {
            dialog.dismiss()
            onPrimary()
        }
        secondaryButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.setOnDismissListener {
            activeGuideDialog = null
            onDismiss?.invoke()
        }
        activeGuideDialog = dialog
        dialog.show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Secure.getString(contentResolver, Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val expected = ComponentName(this, com.study.reminder.service.ScreenAccessibilityService::class.java)
            .flattenToString()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun openBackgroundPermissionSettings() {
        val intents = listOf(
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", packageName)
            },
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
                )
                putExtra("extra_pkgname", packageName)
            },
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        )

        val intent = intents.firstOrNull { candidate ->
            candidate.resolveActivity(packageManager) != null
        } ?: return

        startActivity(intent)
    }
}

// ── Compose UI ────────────────────────────────────────────────

@Composable
private fun MainScreen(
    slots: List<StudySlot>,
    notificationEnabled: Boolean,
    islandEnabled: Boolean,
    smallWindowEnabled: Boolean,
    fullScreenEnabled: Boolean,
    forceHomeEnabled: Boolean,
    onAddSlot: () -> Unit,
    onToggleSlot: (StudySlot) -> Unit,
    onDeleteSlot: (StudySlot) -> Unit,
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
            // 标题
            item {
                Text(
                    text = "🎧 学习提醒器",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10393F),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // 时间段列表卡片
            item {
                GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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

            // 时间段列表项
            items(slots, key = { it.id }) { slot ->
                StudySlotItem(
                    slot = slot,
                    onToggle = { onToggleSlot(slot) },
                    onDelete = { onDeleteSlot(slot) }
                )
            }

            // 提醒方式卡片
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

            // 说明卡片
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

            // 工具按钮
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
private fun OptionSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
