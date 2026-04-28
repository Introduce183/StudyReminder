package com.study.reminder.helper

import android.content.Context
import org.json.JSONArray

object WhitelistManager {

    private const val PREF_KEY = "whitelist_packages"

    // 默认白名单（电话/短信/设置/学习类常见包名）
   private val DEFAULT_WHITELIST = setOf(
    "com.android.dialer",
    "com.android.phone",
    "com.android.mms",
    "com.android.settings",
    "com.study.reminder",
    "com.android.systemui",
    // 小米/澎湃OS 桌面
    "com.miui.home",
    "com.xiaomi.launcher",
    "com.miui.securitycenter",
    // 锁屏/通知
    "com.android.keyguard",
    "com.android.launcher",
    "com.android.launcher3",
    // 通用桌面
    "com.google.android.apps.nexuslauncher",
    "com.sec.android.app.launcher",
    "com.huawei.android.launcher",
    "com.oppo.launcher",
    "com.vivo.launcher",
)

    fun getWhitelist(context: Context): Set<String> {
        val prefs = context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_KEY, null)
        return if (json == null) {
            // 第一次使用，写入默认值
            saveWhitelist(context, DEFAULT_WHITELIST)
            DEFAULT_WHITELIST
        } else {
            try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }.toSet()
            } catch (e: Exception) { DEFAULT_WHITELIST }
        }
    }

    fun saveWhitelist(context: Context, packages: Set<String>) {
        val arr = JSONArray()
        packages.forEach { arr.put(it) }
        context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, arr.toString()).apply()
    }

    fun addPackage(context: Context, pkg: String) {
        val list = getWhitelist(context).toMutableSet()
        list.add(pkg)
        saveWhitelist(context, list)
    }

    fun removePackage(context: Context, pkg: String) {
        val list = getWhitelist(context).toMutableSet()
        list.remove(pkg)
        saveWhitelist(context, list)
    }

    fun isWhitelisted(context: Context, pkg: String): Boolean {
        return pkg in getWhitelist(context)
    }
}
