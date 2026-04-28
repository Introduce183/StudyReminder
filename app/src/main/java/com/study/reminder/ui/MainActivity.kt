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
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.study.reminder.R
import com.study.reminder.databinding.ActivityMainBinding
import com.study.reminder.helper.NotificationHelper
import com.study.reminder.helper.SlotScheduler
import com.study.reminder.model.SlotRepository
import com.study.reminder.model.StudySlot
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var slotAdapter: SlotAdapter
    private var isShowingStartupGuide = false
    private var activeGuideDialog: BottomSheetDialog? = null

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NotificationHelper.createChannels(this)
        setupSlotList()
        setupOptions()
        setupButtons()
        requestCorePermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshSlotList()
        SlotScheduler.rescheduleAll(this)
        maybeShowStartupGuide()
    }

    private fun setupSlotList() {
        slotAdapter = SlotAdapter(
            slots = SlotRepository.getSlots(this).toMutableList(),
            onToggle = { slot ->
                SlotRepository.toggleSlot(this, slot.id)
                SlotScheduler.rescheduleAll(this)
                refreshSlotList()
            },
            onDelete = { slot ->
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
            }
        )
        binding.rvSlots.layoutManager = LinearLayoutManager(this)
        binding.rvSlots.adapter = slotAdapter
    }

    private fun refreshSlotList() {
        val slots = SlotRepository.getSlots(this)
        slotAdapter.updateData(slots)
        binding.tvSlotHint.visibility = if (slots.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupOptions() {
        val prefs = getSharedPreferences("study_prefs", MODE_PRIVATE)
        binding.switchNotification.isChecked = prefs.getBoolean("opt_notification", true)
        binding.switchDynamicIsland.isChecked = prefs.getBoolean("opt_island", false)
        binding.switchSmallWindow.isChecked = prefs.getBoolean("opt_small_window", false)
        binding.switchFullScreen.isChecked = prefs.getBoolean("opt_fullscreen", false)
        binding.switchForceHome.isChecked = prefs.getBoolean("opt_force_home", false)

        fun save() = prefs.edit().apply {
            putBoolean("opt_notification", binding.switchNotification.isChecked)
            putBoolean("opt_island", binding.switchDynamicIsland.isChecked)
            putBoolean("opt_small_window", binding.switchSmallWindow.isChecked)
            putBoolean("opt_fullscreen", binding.switchFullScreen.isChecked)
            putBoolean("opt_force_home", binding.switchForceHome.isChecked)
        }.apply()

        binding.switchSmallWindow.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.switchFullScreen.isChecked = false
                binding.switchForceHome.isChecked = false
                maybeGuideOverlayPermission()
            }
            save()
        }

        binding.switchDynamicIsland.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                maybeGuideOverlayPermission()
            }
            save()
        }

        binding.switchFullScreen.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.switchSmallWindow.isChecked = false
                binding.switchForceHome.isChecked = false
                maybeGuideOverlayPermissionForFullscreen()
            }
            save()
        }

        binding.switchForceHome.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.switchSmallWindow.isChecked = false
                binding.switchFullScreen.isChecked = false
                maybeGuideOverlayPermissionForFullscreen()
            }
            save()
        }

        binding.switchNotification.setOnCheckedChangeListener { _, _ -> save() }
    }

    private fun setupButtons() {
        binding.btnAddSlot.setOnClickListener {
            addSlotLauncher.launch(Intent(this, AddSlotActivity::class.java))
        }
        binding.btnWhitelist.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
        binding.btnAccessibilityGuide.setOnClickListener {
            showGuideSheet(
                title = "关于无障碍权限",
                message = "无障碍权限只用于在学习时间内检测是否切到白名单之外的应用。\n\n不开启也不会影响固定时间段的到点提醒。",
                primaryText = "去开启",
                secondaryText = "暂不"
            ) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
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

        val prefs = getSharedPreferences("study_prefs", MODE_PRIVATE)
        val startupIntroShown = prefs.getBoolean("startup_intro_shown", false)
        val backgroundGuideShown = prefs.getBoolean("startup_background_guide_shown", false)
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
                onDismiss = { isShowingStartupGuide = false }
            ) {
                prefs.edit().putBoolean("startup_background_guide_shown", true).apply()
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
        if (Settings.canDrawOverlays(this)) {
            showGuideSheet(
                title = "还建议打开后台弹出和自启动",
                message = "全屏弹出和强制回桌面已经基于悬浮窗实现，但在部分机型上，仍建议额外允许后台弹出界面、自启动和电池白名单，以提高稳定性。",
                primaryText = "去设置",
                secondaryText = "暂不"
            ) {
                getSharedPreferences("study_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("startup_background_guide_shown", true)
                    .apply()
                openBackgroundPermissionSettings()
            }
            return
        }

        showGuideSheet(
            title = "需要悬浮窗和后台弹出权限",
            message = "全屏弹出和强制回桌面依赖悬浮窗权限，部分机型还需要手动允许后台弹出界面。点击继续后会带你去设置。",
            primaryText = "去设置",
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
        view.findViewById<Button>(R.id.btn_sheet_primary).text = primaryText
        view.findViewById<Button>(R.id.btn_sheet_secondary).text = secondaryText
        view.findViewById<Button>(R.id.btn_sheet_primary).setOnClickListener {
            dialog.dismiss()
            onPrimary()
        }
        view.findViewById<Button>(R.id.btn_sheet_secondary).setOnClickListener {
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
