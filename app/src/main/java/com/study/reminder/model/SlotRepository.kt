package com.study.reminder.model

import android.content.Context
import android.util.Log
import com.study.reminder.helper.StudyTimeCache
import org.json.JSONArray

object SlotRepository {

    private const val PREF_KEY = "study_slots"
    @Volatile private var cachedSlots: MutableList<StudySlot>? = null

    fun getSlots(context: Context): MutableList<StudySlot> {
        cachedSlots?.let { return it.toMutableList() }
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { StudySlot.fromJson(arr.getJSONObject(it)) }.toMutableList()
        } catch (e: Exception) { Log.w("SlotRepository", "Failed to parse slots", e); mutableListOf() }
            .also { cachedSlots = it.toMutableList() }
    }

    fun saveSlots(context: Context, slots: List<StudySlot>) {
        val arr = JSONArray()
        slots.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, arr.toString()).apply()
        cachedSlots = slots.toMutableList()
        // 时间段开关变化后立即让“是否在学习时段”缓存失效
        StudyTimeCache.invalidate()
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

    fun disableSlot(context: Context, id: Long) {
        val slots = getSlots(context).map {
            if (it.id == id) it.copy(enabled = false) else it
        }
        saveSlots(context, slots)
    }

    fun updateSlot(context: Context, slot: StudySlot) {
        val slots = getSlots(context).map {
            if (it.id == slot.id) slot else it
        }
        saveSlots(context, slots)
    }
}
