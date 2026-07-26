# v1.MIMO Bugfix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all P0-P3 issues from HANDOVER.md and release as v1.MIMO

**Architecture:** Incremental fixes to existing codebase - no architectural rewrites. Focus on thread safety, performance, and code quality.

**Tech Stack:** Kotlin, Jetpack Compose, SharedPreferences, AlarmManager

---

## Task 1: P0 — ScreenAccessibilityService 主线程阻塞

**Covers:** HANDOVER.md §七 P0

**Problem:** `isInStudyTime()` 在主线程做 JSON 解析（SlotRepository.getSlots + CourseScheduleRepository.getCourses），窗口切换事件每秒 5-10 次，低端机 ANR 风险。

**Files:**
- Modify: `app/src/main/java/com/study/reminder/service/ScreenAccessibilityService.kt`

**Solution:** 将 `isInStudyTime()` 的 JSON 解析结果缓存，避免每次事件都重新解析。使用 `@Volatile` 缓存 + TTL 过期机制。

- [ ] Step 1: 添加缓存字段和 TTL

```kotlin
// 在 companion object 中添加
@Volatile private var cachedInStudyTime: Boolean? = null
@Volatile private var studyTimeCacheTime: Long = 0L
private const val STUDY_TIME_CACHE_TTL = 30_000L // 30秒
```

- [ ] Step 2: 修改 isInStudyTime() 使用缓存

```kotlin
private fun isInStudyTime(): Boolean {
    val now = System.currentTimeMillis()
    cachedInStudyTime?.let {
        if (now - studyTimeCacheTime < STUDY_TIME_CACHE_TTL) return it
    }
    val result = isInStudyTimeUncached()
    cachedInStudyTime = result
    studyTimeCacheTime = now
    return result
}

private fun isInStudyTimeUncached(): Boolean {
    // 原有的 isInStudyTime 逻辑移到这里
    val now = Calendar.getInstance()
    // ...
}
```

- [ ] Step 3: Run lint check
```bash
.\gradlew.bat lintDebug
```

---

## Task 2: P1 — SlotRepository/CourseScheduleRepository 性能优化

**Covers:** HANDOVER.md §七 P1

**Problem:** 每次读写都解析/序列化完整 JSON，session 数 > 500 时 UI 卡顿。

**Files:**
- Modify: `app/src/main/java/com/study/reminder/model/SlotRepository.kt`
- Modify: `app/src/main/java/com/study/reminder/model/CourseScheduleRepository.kt`

**Solution:** 添加内存缓存，避免重复 JSON 解析。

- [ ] Step 1: 在 SlotRepository 中添加缓存

```kotlin
@Volatile private var cachedSlots: MutableList<StudySlot>? = null

fun getSlots(context: Context): MutableList<StudySlot> {
    cachedSlots?.let { return it.toMutableList() }
    // 原有解析逻辑
    // ...
    .also { cachedSlots = it.toMutableList() }
}

fun saveSlots(context: Context, slots: List<StudySlot>) {
    // 原有保存逻辑
    cachedSlots = slots.toMutableList()
}
```

- [ ] Step 2: 在 CourseScheduleRepository 中添加缓存

```kotlin
@Volatile private var cachedCourses: List<CourseSchedule>? = null

fun getCourses(context: Context): List<CourseSchedule> {
    cachedCourses?.let { return it }
    // 原有解析逻辑
    // ...
    .also { cachedCourses = it }
}

fun saveCourses(context: Context, courses: List<CourseSchedule>) {
    // 原有保存逻辑
    cachedCourses = courses
}
```

- [ ] Step 3: Run lint check
```bash
.\gradlew.bat lintDebug
```

---

## Task 3: P1 — 删除未使用的 TimerViewModel

**Covers:** HANDOVER.md §七 P1

**Problem:** TimerViewModel 存在但未使用，StudyScreen 直接操作 Repository 和 Service。

**Files:**
- Delete: `app/src/main/java/com/study/reminder/viewmodel/TimerViewModel.kt` (if exists)

- [ ] Step 1: Check if TimerViewModel exists
```bash
dir app\src\main\java\com\study\reminder\viewmodel\
```

- [ ] Step 2: If exists, delete it and run lint

---

## Task 4: P1 — TimerService LiveData 改 StateFlow

**Covers:** HANDOVER.md §七 P1

**Problem:** TimerService.remainingTimeLive 和 timerRunningLive 是 companion object 中的 static MutableLiveData，是反模式。

**Files:**
- Modify: `app/src/main/java/com/study/reminder/service/TimerService.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/compose/StudyScreen.kt`

**Solution:** 改为伴生对象中的 StateFlow。

- [ ] Step 1: 修改 TimerService companion object

```kotlin
companion object {
    // 替换
    val remainingTimeLive = MutableStateFlow(0)
    val timerRunningLive = MutableStateFlow(false)
}
```

- [ ] Step 2: 修改所有 postValue 调用为 .value 赋值

```kotlin
// TimerService.kt 中
remainingTimeLive.value = remainingSeconds
timerRunningLive.value = true
```

- [ ] Step 3: 修改 StudyScreen.kt 中的观察方式

```kotlin
// 使用 collectAsState
val remainingTime by TimerService.remainingTimeLive.collectAsState()
val timerRunning by TimerService.timerRunningLive.collectAsState()
```

- [ ] Step 4: Run lint check
```bash
.\gradlew.bat lintDebug
```

---

## Task 5: P2 — 提取硬编码颜色到 ComposeTheme

**Covers:** HANDOVER.md §七 P2

**Problem:** 15+ 处硬编码颜色值，虽然有 ComposeTheme 但 90% UI 代码直接写死 hex。

**Files:**
- Modify: `app/src/main/java/com/study/reminder/ui/compose/ComposeTheme.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/compose/StudyScreen.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/compose/SettingsScreen.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/compose/CourseScheduleScreen.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/compose/Components.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/AddSlotActivity.kt`

**Solution:** 在 ComposeTheme 中定义扩展颜色属性，替换硬编码值。

- [ ] Step 1: 在 ComposeTheme.kt 中添加扩展颜色

```kotlin
// 添加自定义颜色
object AppColors {
    val Background = Color(0xFFEAFBF8)
    val CardBg = Color(0xCCF7FFFF)
    val CardBgSoft = Color(0xDDF9F4)
    val TextPrimary = Color(0xFF10393F)
    val TextSecondary = Color(0xFF4E7F84)
    val Accent = Color(0xFF39C5BB)
    val AccentPink = Color(0xFFFF77B7)
    val GlassStroke = Color(0x90FFFFFF)
    val InfoBg = Color(0xE8FFFA)
    val InfoText = Color(0xFF17646B)
}
```

- [ ] Step 2-7: 替换各文件中的硬编码颜色 (逐个文件)

- [ ] Step 8: Run lint check
```bash
.\gradlew.bat lintDebug
```

---

## Task 6: P2 — 提取硬编码中文字符串到 strings.xml

**Covers:** HANDOVER.md §七 P2

**Problem:** 30+ 处硬编码中文字符串，未使用 strings.xml。

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/study/reminder/ui/MainActivity.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/AddSlotActivity.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/compose/StudyScreen.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/compose/SettingsScreen.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/compose/CourseScheduleScreen.kt`

**Solution:** 将硬编码字符串提取到 strings.xml，在 Compose 中用 `stringResource()`。

- [ ] Step 1: 在 strings.xml 中添加常用字符串

```xml
<string name="study_time">学习时间</string>
<string name="start_study">开始学习</string>
<string name="end_study">结束学习</string>
<string name="add_slot">添加</string>
<string name="confirm_add">确认添加</string>
<string name="confirm_edit">确认修改</string>
<string name="cancel">取消</string>
<string name="delete">删除</string>
<string name="notification_remind">🔔 通知提醒（多次刷新）</string>
<string name="island_remind">💊 超级岛提醒</string>
<string name="small_window_remind">🪟 后台弹出小窗</string>
<string name="fullscreen_remind">📱 全屏弹出（确认返桌面）</string>
<string name="force_home_remind">🏠 强制返回桌面</string>
<string name="whitelist">✅ 白名单</string>
<string name="accessibility_optional">♿ 无障碍（可选）</string>
<string name="study_statistics">📊 学习统计</string>
<string name="course_schedule">📅 课表模式</string>
<string name="settings">🎧 学习提醒器</string>
<string name="today_overview">今日概览</string>
<string name="study_duration">学习时长</string>
<string name="distraction_count">摸鱼次数</string>
<string name="week_total">周总时长</string>
<string name="week_distraction">周摸鱼</string>
<string name="month_total">月总时长</string>
<string name="import_course">导入课表</string>
<string name="clear_course">清空课表</string>
<string name="no_course">还没有导入课表</string>
<string name="import_hint">点击「导入课表」选择 .ics 文件</string>
<string name="this_week">本周</string>
<string name="this_month">本月</string>
<string name="confirm_delete_slot">确定删除「%s」吗？</string>
<string name="slot_added">已添加「%s」并重新安排提醒</string>
<string name="slot_updated">已更新「%s」</string>
<string name="time_required">请填写完整时间</string>
<string name="hour_range">小时请输入 0-23</string>
<string name="minute_range">分钟请输入 0-59</string>
<string name="end_after_start">结束时间必须晚于开始时间</string>
<string name="select_day">请至少选择一天</string>
```

- [ ] Step 2-7: 在各 Kotlin 文件中替换硬编码字符串为 `stringResource()`

- [ ] Step 8: Run lint check
```bash
.\gradlew.bat lintDebug
```

---

## Task 7: P3 — API Key 加密存储

**Covers:** HANDOVER.md §七 P3

**Problem:** API Key 明文存储在 SharedPreferences 中。

**Files:**
- Modify: `app/build.gradle` (添加 security-crypto 依赖)
- Modify: `app/src/main/java/com/study/reminder/helper/AiHelper.kt`
- Modify: `app/src/main/java/com/study/reminder/ui/compose/SettingsScreen.kt`

**Solution:** 使用 AndroidX Security 的 EncryptedSharedPreferences。

- [ ] Step 1: 在 build.gradle 添加依赖

```groovy
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
```

- [ ] Step 2: 创建 SecurePreferencesManager

```kotlin
object SecurePreferencesManager {
    private const val PREF_FILE = "secure_prefs"
    
    fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun saveApiKey(context: Context, key: String) {
        getEncryptedPrefs(context).edit().putString("api_key", key).apply()
    }
    
    fun getApiKey(context: Context): String {
        return getEncryptedPrefs(context).getString("api_key", "") ?: ""
    }
}
```

- [ ] Step 3: 修改 AiHelper 使用加密存储

- [ ] Step 4: Run lint check
```bash
.\gradlew.bat lintDebug
```

---

## Task 8: 版本号更新和构建

**Covers:** HANDOVER.md §十

**Files:**
- Modify: `app/build.gradle`

- [ ] Step 1: 更新版本号

```groovy
defaultConfig {
    versionCode 40
    versionName "1.MIMO"
}
```

- [ ] Step 2: 构建 APK
```bash
.\gradlew.bat assembleDebug
```

- [ ] Step 3: Git commit and push
```bash
git add -A
git commit -m "v1.MIMO: fix P0-P3 bugs from HANDOVER.md"
git push origin main
```
