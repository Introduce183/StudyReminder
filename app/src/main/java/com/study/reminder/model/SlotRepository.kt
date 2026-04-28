package com.study.reminder.model

import android.content.Context
import org.json.JSONArray

object SlotRepository {

    private const val PREF_KEY = "study_slots"

    fun getSlots(context: Context): MutableList<StudySlot> {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { StudySlot.fromJson(arr.getJSONObject(it)) }.toMutableList()
        } catch (e: Exception) { mutableListOf() }
    }

    fun saveSlots(context: Context, slots: List<StudySlot>) {
        val arr = JSONArray()
        slots.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, arr.toString()).apply()
    }

    fun addSlot(context: Context, slot: StudySlot) {
        val slots = getSlots(context)
        slots.add(slot)
        saveSlots(context, slots)
    }

    fun removeSlot(context: Context, id: Long) {
        val slots = getSlots(context).filter { it.id != id }
        saveSlots(context, slots)
    }

    fun toggleSlot(context: Context, id: Long) {
        val slots = getSlots(context).map {
            if (it.id == id) it.copy(enabled = !it.enabled) else it
        }
        saveSlots(context, slots)
    }
}
