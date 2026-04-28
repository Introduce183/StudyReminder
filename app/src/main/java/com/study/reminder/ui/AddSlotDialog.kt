package com.study.reminder.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import com.study.reminder.R
import com.study.reminder.model.StudySlot

class AddSlotDialog(
    context: Context,
    private val onConfirm: (StudySlot) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_new_slot)
        setTitle("添加学习时间段")

        val etLabel = findViewById<EditText>(R.id.et_slot_label)
        val tpStart = findViewById<TimePicker>(R.id.tp_slot_start)
        val tpEnd = findViewById<TimePicker>(R.id.tp_slot_end)
        val cbDays = listOf(
            findViewById<CheckBox>(R.id.cb_mon),
            findViewById<CheckBox>(R.id.cb_tue),
            findViewById<CheckBox>(R.id.cb_wed),
            findViewById<CheckBox>(R.id.cb_thu),
            findViewById<CheckBox>(R.id.cb_fri),
            findViewById<CheckBox>(R.id.cb_sat),
            findViewById<CheckBox>(R.id.cb_sun),
        )
        tpStart.setIs24HourView(true)
        tpEnd.setIs24HourView(true)

        // 默认周一到周五
        cbDays.take(5).forEach { it.isChecked = true }

        findViewById<Button>(R.id.btn_slot_confirm).setOnClickListener {
            val startH = tpStart.hour
            val startM = tpStart.minute
            val endH = tpEnd.hour
            val endM = tpEnd.minute

            if (endH * 60 + endM <= startH * 60 + startM) {
                Toast.makeText(context, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val days = cbDays.mapIndexedNotNull { idx, cb ->
                if (cb.isChecked) idx + 1 else null  // 1=周一..7=周日
            }.toSet()

            if (days.isEmpty()) {
                Toast.makeText(context, "请至少选择一天", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val slot = StudySlot(
                label = etLabel.text.toString().trim().ifEmpty { "学习时间" },
                startHour = startH,
                startMinute = startM,
                endHour = endH,
                endMinute = endM,
                enableDays = days
            )
            onConfirm(slot)
            dismiss()
        }

        findViewById<Button>(R.id.btn_slot_cancel).setOnClickListener { dismiss() }
    }
}
