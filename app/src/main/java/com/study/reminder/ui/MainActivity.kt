package com.study.reminder.ui

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.study.reminder.databinding.ActivityMainBinding
import com.study.reminder.helper.NotificationHelper
import com.study.reminder.helper.SlotScheduler
import com.study.reminder.model.SlotRepository
import com.study.reminder.model.StudySlot
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var slotAdapter: SlotAdapter

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    private val fullScreenPermissionLauncher = registerForActivityResult(
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
                maybeGuideFullScreenIntentPermission()
            }
            save()
        }

        binding.switchForceHome.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.switchSmallWindow.isChecked = false
                binding.switchFullScreen.isChecked = false
                maybeGuideFullScreenIntentPermission()
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
            AlertDialog.Builder(this)
                .setTitle("关于无障碍权限")
                .setMessage(
                    "无障碍权限只用于在学习时间内检测是否切到白名单之外的应用。\n\n" +
                        "不开启也不会影响固定时间段的到点提醒。\n\n是否前往开启？"
                )
                .setPositiveButton("去开启") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("暂不", null)
                .show()
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

    private fun maybeGuideOverlayPermission() {
        if (Settings.canDrawOverlays(this)) return

        AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage("动态岛和小窗提醒依赖悬浮窗权限，是否前往授权？")
            .setPositiveButton("去授权") { _, _ ->
                overlayPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            .setNegativeButton("暂不", null)
            .show()
    }

    private fun maybeGuideExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val alarmManager = getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) return

        AlertDialog.Builder(this)
            .setTitle("需要精确闹钟权限")
            .setMessage("学习时间段依赖精确闹钟，到点提醒更稳定。是否前往开启？")
            .setPositiveButton("去开启") { _, _ ->
                exactAlarmPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            .setNegativeButton("暂不", null)
            .show()
    }

    private fun maybeGuideFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        if (NotificationManagerCompat.from(this).canUseFullScreenIntent()) return

        AlertDialog.Builder(this)
            .setTitle("需要全屏提醒权限")
            .setMessage("全屏提醒和强制回桌面模式依赖全屏意图权限，是否前往开启？")
            .setPositiveButton("去开启") { _, _ ->
                fullScreenPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            .setNegativeButton("暂不", null)
            .show()
    }
}
