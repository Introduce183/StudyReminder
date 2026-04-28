package com.study.reminder.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.study.reminder.R
import com.study.reminder.model.StudySlot

class SlotAdapter(
    private val slots: MutableList<StudySlot>,
    private val onToggle: (StudySlot) -> Unit,
    private val onDelete: (StudySlot) -> Unit
) : RecyclerView.Adapter<SlotAdapter.ViewHolder>() {

    private val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvLabel: TextView = v.findViewById(R.id.tv_slot_label)
        val tvTime: TextView = v.findViewById(R.id.tv_slot_time)
        val tvDays: TextView = v.findViewById(R.id.tv_slot_days)
        val switchEnabled: SwitchCompat = v.findViewById(R.id.switch_slot_enabled)
        val btnDelete: ImageButton = v.findViewById(R.id.btn_slot_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study_slot, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val slot = slots[position]
        holder.tvLabel.text = if (slot.label.isNotEmpty()) slot.label else "学习时间"
        holder.tvTime.text = "${slot.startLabel}  →  ${slot.endLabel}"
        holder.tvDays.text = if (slot.enableDays.size == 7) "每天" else
            slot.enableDays.sorted().joinToString(" ") { "周${dayLabels[it - 1]}" }

        holder.switchEnabled.setOnCheckedChangeListener(null)
        holder.switchEnabled.isChecked = slot.enabled
        holder.switchEnabled.setOnCheckedChangeListener { _, _ -> onToggle(slot) }
        holder.btnDelete.setOnClickListener { onDelete(slot) }
    }

    override fun getItemCount() = slots.size

    fun updateData(newSlots: List<StudySlot>) {
        slots.clear()
        slots.addAll(newSlots)
        notifyDataSetChanged()
    }
}
