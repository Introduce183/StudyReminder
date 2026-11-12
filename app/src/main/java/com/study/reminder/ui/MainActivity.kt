package com.study.reminder.ui

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.Secure
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.study.reminder.ui.compose.AppColors
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.study.reminder.R
import com.study.reminder.helper.CourseScheduler
import com.study.reminder.helper.IcsParser
import com.study.reminder.helper.NotificationHelper
import com.study.reminder.helper.SlotScheduler
import com.study.reminder.model.CourseSchedule
import com.study.reminder.model.CourseScheduleRepository
import com.study.reminder.model.SlotRepository
import com.study.reminder.model.StudySlot
import com.study.reminder.model.StudyStatsRepository
import com.study.reminder.service.KeepAliveService
import com.study.reminder.service.TimerService
import com.study.reminder.ui.compose.CourseScheduleScreen
import com.study.reminder.ui.compose.SettingsScreen
import com.study.reminder.ui.compose.StudyReminderComposeTheme
import com.study.reminder.ui.compose.StudyScreen
import org.json.JSONObject
import java.util.Calendar

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "study_prefs"
        private const val KEY_STARTUP_BACKGROUND_GUIDE_SHOWN = "startup_background_guide_shown"
        private const val KEY_FULLSCREEN_BACKGROUND_GUIDE_SHOWN = "fullscreen_background_guide_shown"
        private const val KEY_ACCESSIBILITY_ENABLED_ONCE = "accessibility_enabled_once"
        private const val KEY_ACCESSIBILITY_OFF_NOTICE_TIME = "accessibility_off_notice_time"
        private const val ACCESSIBILITY_OFF_NOTICE_INTERVAL = 12 * 60 * 60 * 1000L
    }

    private var isShowingStartupGuide = false
    private var activeGuideDialog: BottomSheetDialog? = null

    // Compose state holders
    private val slotsState = mutableStateListOf<StudySlot>()
    private val notificationState = mutableStateOf(true)
    private val islandState = mutableStateOf(false)
    private val fullScreenState = mutableStateOf(false)
    private val forceHomeState = mutableStateOf(false)
    private val isDarkThemeState = mutableStateOf(true)
    private val accessibilityEnabledState = mutableStateOf(false)
    private val keepAliveState = mutableStateOf(true)
    private val batteryOptimizationIgnoredState = mutableStateOf(false)

    // Course schedule state
    private val coursesState = mutableStateListOf<CourseSchedule>()
    private val semesterStartDateState = mutableStateOf("")

    private val icsPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        importIcsFile(uri)
    }

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

    private val editSlotLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val json = result.data?.getStringExtra("slot_json") ?: return@registerForActivityResult
            val slot = StudySlot.fromJson(JSONObject(json))
            SlotRepository.updateSlot(this, slot)
            SlotScheduler.rescheduleAll(this)
            refreshSlotList()
            Toast.makeText(this, "已更新「${slot.label}」", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannels(this)

        // Load initial data
        loadSlots()
        loadOptions()
        loadCourses()

        setContent {
            StudyReminderComposeTheme(darkTheme = isDarkThemeState.value) {
                var selectedTab by remember { mutableStateOf(0) }

                Scaffold(
                    bottomBar = {
                        Column {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = AppColors.LightDivider
                            )
                            NavigationBar(
                                containerColor = AppColors.NavBar,
                                tonalElevation = 0.dp
                            ) {
                            val navColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppColors.Accent,
                                selectedTextColor = AppColors.Accent,
                                indicatorColor = AppColors.Accent.copy(alpha = 0.12f),
                                unselectedIconColor = AppColors.TextTertiary,
                                unselectedTextColor = AppColors.TextTertiary
                            )
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "学习") },
                                label = { Text("学习") },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.List, contentDescription = "课表") },
                                label = { Text("课表") },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                                label = { Text("设置") },
                                colors = navColors
                            )
                            }
                        }
                    }
                ) { paddingValues ->
                    val activity = this@MainActivity
                    Box(modifier = Modifier.padding(paddingValues)) {
                        when (selectedTab) {
                            0 -> StudyScreen(
                                onSlotDisabled = { refreshSlotList() }
                            )
                            1 -> CourseScheduleScreen(
                                courses = coursesState,
                                semesterStartDate = semesterStartDateState.value,
                                onImportIcs = { icsPickerLauncher.launch(arrayOf("text/calendar")) },
                                onToggleAll = { enabled ->
                                    CourseScheduleRepository.toggleAllCourses(activity, enabled)
                                    CourseScheduler.rescheduleAll(activity)
                                    refreshCourseList()
                                },
                                onDeleteAll = {
                                    CourseScheduleRepository.replaceAllCourses(activity, emptyList())
                                    CourseScheduler.rescheduleAll(activity)
                                    refreshCourseList()
                                }
                            )
                            else -> SettingsScreen(
                    slots = slotsState,
                    notificationEnabled = notificationState.value,
                    islandEnabled = islandState.value,
                    fullScreenEnabled = fullScreenState.value,
                    forceHomeEnabled = forceHomeState.value,
                    onAddSlot = {
                        addSlotLauncher.launch(Intent(activity, AddSlotActivity::class.java))
                    },
                    onToggleSlot = { slot ->
                        SlotRepository.toggleSlot(activity, slot.id)
                        SlotScheduler.rescheduleAll(activity)
                        refreshSlotList()
                        // 关闭时间段时，自动结束当前学习会话
                        if (slot.enabled && StudyStatsRepository.hasActiveSession(activity)) {
                            StudyStatsRepository.endSession(activity)
                            val stopIntent = Intent(activity, TimerService::class.java).apply {
                                action = TimerService.ACTION_STOP
                            }
                            activity.startService(stopIntent)
                        }
                    },
                    onDeleteSlot = { slot ->
                        val slotName = slot.label.ifEmpty { "${slot.startLabel} - ${slot.endLabel}" }
                        AlertDialog.Builder(activity)
                            .setTitle("删除时间段")
                            .setMessage("确定删除「$slotName」吗？")
                            .setPositiveButton("删除") { _, _ ->
                                SlotRepository.removeSlot(activity, slot.id)
                                SlotScheduler.rescheduleAll(activity)
                                refreshSlotList()
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    },
                    onEditSlot = { slot ->
                        val intent = Intent(activity, AddSlotActivity::class.java)
                        intent.putExtra(AddSlotActivity.EXTRA_SLOT_JSON, slot.toJson().toString())
                        editSlotLauncher.launch(intent)
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
                    onFullScreenChange = { checked ->
                        fullScreenState.value = checked
                        if (checked) {
                            forceHomeState.value = false
                            maybeGuideOverlayPermissionForFullscreen()
                        }
                        saveOptions()
                    },
                    onForceHomeChange = { checked ->
                        forceHomeState.value = checked
                        if (checked) {
                            fullScreenState.value = false
                            maybeGuideOverlayPermissionForFullscreen()
                        }
                        saveOptions()
                    },
                    onWhitelist = {
                        startActivity(Intent(activity, AppPickerActivity::class.java))
                    },
                    onAccessibilityGuide = {
                        showGuideSheet(
                            title = "关于无障碍权限",
                            message = "无障碍权限只用于在学习时间内检测是否切到白名单之外的应用。\n\n不开启也不会影响固定时间段的到点提醒。\n\n注意：部分系统在后台清理或重启后会关闭无障碍服务，需要重新开启；建议开启自启动、省电策略设为「无限制」，并在最近任务中锁定本应用。",
                            primaryText = "去开启",
                            secondaryText = "暂不"
                        ) {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    },
                    isDarkTheme = isDarkThemeState.value,
                    accessibilityEnabled = accessibilityEnabledState.value,
                    keepAliveEnabled = keepAliveState.value,
                    batteryOptimizationIgnored = batteryOptimizationIgnoredState.value,
                    onThemeChange = { isDark ->
                        isDarkThemeState.value = isDark
                        saveThemePreference(isDark)
                    },
                    onKeepAliveChange = { enabled ->
                        keepAliveState.value = enabled
                        saveOptions()
                        if (enabled) KeepAliveService.start(activity)
                        else KeepAliveService.stop(activity)
                    },
                    onRequestBatteryOptimization = { requestIgnoreBatteryOptimizations() }
                )
                        }
                    }
                }
            }
        }

        requestCorePermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshSlotList()
        SlotScheduler.rescheduleAll(this)
        refreshCourseList()
        CourseScheduler.rescheduleAll(this)
        refreshAccessibilityState()
        refreshBatteryOptimizationState()
        applyKeepAliveService()
        maybeShowStartupGuide()
        maybeNoticeAccessibilityDisabled()
    }

    private fun applyKeepAliveService() {
        if (keepAliveState.value) {
            KeepAliveService.start(this)
        } else {
            KeepAliveService.stop(this)
        }
    }

    private fun refreshBatteryOptimizationState() {
        val powerManager = getSystemService(PowerManager::class.java)
        batteryOptimizationIgnoredState.value =
            powerManager?.isIgnoringBatteryOptimizations(packageName) == true
    }

    private fun requestIgnoreBatteryOptimizations() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (_: Exception) {
            runCatching {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun refreshAccessibilityState() {
        accessibilityEnabledState.value = isAccessibilityServiceEnabled()
    }

    /**
     * 无障碍服务被系统关闭后给出一次提醒（12 小时内最多一次）。
     * 系统/厂商后台清理无法由 App 阻止，只能引导用户重新开启。
     */
    private fun maybeNoticeAccessibilityDisabled() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (accessibilityEnabledState.value) {
            prefs.edit()
                .putBoolean(KEY_ACCESSIBILITY_ENABLED_ONCE, true)
                .remove(KEY_ACCESSIBILITY_OFF_NOTICE_TIME)
                .apply()
            return
        }
        if (isShowingStartupGuide || activeGuideDialog != null) return
        if (!prefs.getBoolean(KEY_ACCESSIBILITY_ENABLED_ONCE, false)) return

        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_ACCESSIBILITY_OFF_NOTICE_TIME, 0L)
        if (now - last < ACCESSIBILITY_OFF_NOTICE_INTERVAL) return

        prefs.edit().putLong(KEY_ACCESSIBILITY_OFF_NOTICE_TIME, now).apply()
        showGuideSheet(
            title = "无障碍监督已被系统关闭",
            message = "后台清理、重启或系统省电策略可能会关闭无障碍服务。重新开启后，学习中途切换应用的提醒才会恢复。\n\n建议同时开启自启动、把省电策略设为「无限制」，并在最近任务中锁定本应用。",
            primaryText = "去重新开启",
            secondaryText = "知道了"
        ) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
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

    private fun loadCourses() {
        coursesState.clear()
        coursesState.addAll(CourseScheduleRepository.getCourses(this))
        semesterStartDateState.value = CourseScheduleRepository.getSemesterStartDate(this)
    }

    private fun refreshCourseList() {
        val courses = CourseScheduleRepository.getCourses(this)
        coursesState.clear()
        coursesState.addAll(courses)
        semesterStartDateState.value = CourseScheduleRepository.getSemesterStartDate(this)
    }

    private fun importIcsFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val text = inputStream.bufferedReader().use { it.readText() }
            if (text.isBlank()) {
                Toast.makeText(this, "文件为空", Toast.LENGTH_SHORT).show()
                return
            }

            val result = IcsParser.parseIcs(text)
            if (result.courses.isEmpty()) {
                Toast.makeText(this, "未解析到课程，请检查 .ics 文件格式", Toast.LENGTH_LONG).show()
                return
            }

            CourseScheduleRepository.replaceAllCourses(this, result.courses)
            if (result.semesterStartDate.isNotEmpty()) {
                CourseScheduleRepository.setSemesterStartDate(this, result.semesterStartDate)
            }
            CourseScheduler.rescheduleAll(this)
            refreshCourseList()

            val msg = buildString {
                append("已导入 ${result.courses.size} 个课程")
                if (result.semesterStartDate.isNotEmpty()) {
                    append("，学期起始: ${result.semesterStartDate}")
                }
                if (result.warnings.isNotEmpty()) {
                    append("\n注意: ${result.warnings.joinToString("; ")}")
                }
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadOptions() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        notificationState.value = prefs.getBoolean("opt_notification", true)
        islandState.value = prefs.getBoolean("opt_island", false)
        fullScreenState.value = prefs.getBoolean("opt_fullscreen", false)
        forceHomeState.value = prefs.getBoolean("opt_force_home", false)
        isDarkThemeState.value = prefs.getBoolean("opt_dark_theme", true)
        keepAliveState.value = prefs.getBoolean("opt_keep_alive", true)
    }

    private fun saveOptions() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putBoolean("opt_notification", notificationState.value)
            putBoolean("opt_island", islandState.value)
            putBoolean("opt_fullscreen", fullScreenState.value)
            putBoolean("opt_force_home", forceHomeState.value)
            putBoolean("opt_keep_alive", keepAliveState.value)
        }.apply()
    }

    private fun saveThemePreference(isDark: Boolean) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putBoolean("opt_dark_theme", isDark)
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
                message = "为了让提醒更稳定，建议继续检查后台弹出界面、自启动和电池优化白名单，并在最近任务中锁定本应用，避免系统后台清理。不同机型入口不一样，应用会尽量帮你打开对应页面。",
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
                secondaryText = "跳过",
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
            secondaryText = "跳过"
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

        // 弹窗颜色跟随应用内主题，而不是系统主题
        val isDark = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean("opt_dark_theme", true)
        val sheetBg = if (isDark) 0xFF151E21.toInt() else 0xFFFFFFFF.toInt()
        val titleColor = if (isDark) 0xFFEAF3F2.toInt() else 0xFF0D2F33.toInt()
        val bodyColor = if (isDark) 0xFF9DB5B2.toInt() else 0xFF4A7A80.toInt()
        val accentColor = 0xFF39C5BB.toInt()
        val onAccentColor = 0xFF06272B.toInt()
        val radius = 24f * resources.displayMetrics.density

        view.background = GradientDrawable().apply {
            cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
            setColor(sheetBg)
        }

        view.findViewById<TextView>(R.id.tv_sheet_title).apply {
            text = title
            setTextColor(titleColor)
        }
        view.findViewById<TextView>(R.id.tv_sheet_message).apply {
            text = message
            setTextColor(bodyColor)
        }
        val primaryButton = view.findViewById<Button>(R.id.btn_sheet_primary)
        val secondaryButton = view.findViewById<Button>(R.id.btn_sheet_secondary)
        primaryButton.apply {
            text = primaryText
            setTextColor(onAccentColor)
        }
        secondaryButton.apply {
            text = secondaryText
            setTextColor(accentColor)
        }
        primaryButton.setOnClickListener {
            dialog.dismiss()
            onPrimary()
        }
        secondaryButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(AndroidColor.TRANSPARENT)
        }
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

        for (intent in intents) {
            try {
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }

        // 兜底：直接打开本应用的系统设置页
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {}
    }
}
