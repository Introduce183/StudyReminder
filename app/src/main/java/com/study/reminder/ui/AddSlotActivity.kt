package com.study.reminder.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.study.reminder.R
import com.study.reminder.model.StudySlot

class AddSlotActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_slot)
        title = "添加学习时间段"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val cbDays = listOf(
            findViewById<CheckBox>(R.id.cb_mon),
            findViewById<CheckBox>(R.id.cb_tue),
            findViewById<CheckBox>(R.id.cb_wed),
            findViewById<CheckBox>(R.id.cb_thu),
            findViewById<CheckBox>(R.id.cb_fri),
            findViewById<CheckBox>(R.id.cb_sat),
            findViewById<CheckBox>(R.id.cb_sun),
        )
        cbDays.take(5).forEach { it.isChecked = true }

        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            val label = findViewById<EditText>(R.id.et_slot_label).text.toString().trim()
            val startH = findViewById<EditText>(R.id.et_start_hour).text.toString().toIntOrNull()
            val startM = findViewById<EditText>(R.id.et_start_minute).text.toString().toIntOrNull()
            val endH = findViewById<EditText>(R.id.et_end_hour).text.toString().toIntOrNull()
            val endM = findViewById<EditText>(R.id.et_end_minute).text.toString().toIntOrNull()

            if (startH == null || startM == null || endH == null || endM == null) {
                Toast.makeText(this, "请填写完整时间", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (startH !in 0..23 || endH !in 0..23) {
                Toast.makeText(this, "小时请输入 0-23", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (startM !in 0..59 || endM !in 0..59) {
                Toast.makeText(this, "分钟请输入 0-59", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (endH * 60 + endM <= startH * 60 + startM) {
                Toast.makeText(this, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val days = cbDays.mapIndexedNotNull { i, cb -> if (cb.isChecked) i + 1 else null }.toSet()
            if (days.isEmpty()) {
                Toast.makeText(this, "请至少选择一天", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }

            val slot = StudySlot(
                label = label.ifEmpty { "学习时间" },
                startHour = startH, startMinute = startM,
                endHour = endH, endMinute = endM, enableDays = days
            )
            setResult(Activity.RESULT_OK, Intent().putExtra("slot_json", slot.toJson().toString()))
            finish()
        }

        findViewById<Button>(R.id.btn_cancel).setOnClickListener { finish() }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
