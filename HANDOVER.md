# StudyReminder 项目交接文档

> 最后更新: 2026-11-12 | 版本: v2.4.8 (versionCode 71)

---

## 一、项目概述

Android 学习提醒器，核心功能：
- 固定时间段学习提醒（闹钟调度）
- ICS 课表导入（支持双周课 INTERVAL=2）
- 学习计时统计（周/月柱状图）
- 多种提醒方式：通知、动态岛、小窗、全屏弹出、强制回桌面
- 白名单应用检测（AccessibilityService）
- AI 调侃功能（调用小米 MIMO API）

**技术栈**: Kotlin, Jetpack Compose, SharedPreferences (JSON), AlarmManager, Foreground Service, AccessibilityService

**仓库**: https://github.com/Introduce183/StudyReminder

---

## 二、版本历史关键节点

| 版本 | 日期 | 事件 |
|------|------|------|
| 1.0.x | 2026-04 | 初始版本，基础提醒功能 |
| 1.1.x | 2026-04~05 | 手势检测、悬浮窗、动态岛 |
| 1.2.x | 2026-05-12 | 学习统计页面、底部导航 |
| 1.3.0 | 2026-05-20 | 课表模式（ICS 导入） |
| 1.3.1~1.3.10 | 2026-05-21 | 课表解析修复（重写 3 次） |
| 1.ULTRA | 2026-05-27 | 深度审计修复（10 个 bug） |
| 1.ULTIMATE | 2026-05-27 | 技术评审修复（P0 安全+稳定性） |
| 1.DREAM | 2026-05-27 | 全量审计 + 架构清理 |
| 1.MIMO | 2026-06-11 | 修复交接记录中的 P0–P3 问题，并完成版本验证 |
| 2.x | 2026-08 | 深色主题、动效重构、课表与稳定性迭代（2.3.1） |
| 2.4 | 2026-11-10 | UI 全面重构：设计令牌清理、呼吸光环计时器、XML 界面统一 |
| 2.4.1 | 2026-11-11 | 修复无障碍检测：同一应用二次进入不提醒的问题 |
| 2.4.2 | 2026-11-12 | 修复关闭时间段后仍触发提醒并自动开始计时的问题 |
| 2.4.3 | 2026-11-12 | 修复手动开始学习（不在时间段内）不触发监督的问题 |
| 2.4.4 | 2026-11-12 | 保活前台服务 + 电池优化白名单；游戏触发检测；横屏提醒布局 |
| 2.4.5 | 2026-11-12 | 全屏提醒新增「再看 30 秒」延迟重弹按钮 |
| 2.4.6 | 2026-11-12 | 关闭课表监督时同步结束课表会话与计时 |
| 2.4.7 | 2026-11-12 | 修复「再看 30 秒」后监督立即重弹的冲突 |
| 2.4.8 | 2026-11-12 | 「再看」时长可配置（5/10/15/30/60 秒） |

**版本号规则**: 1.3.9 → 1.3.10，不自动进位到 1.4.0。特殊版本用名字后缀（ULTRA, ULTIMATE, DREAM）。

**Git push 策略**: 失败时自动重试最多 10 次。

---

## 三、目录结构

```
app/src/main/java/com/study/reminder/
├── helper/                    # 工具类
│   ├── AiHelper.kt           # MIMO API 调用
│   ├── ButtonMotionHelper.kt # 按钮动画
│   ├── CharacterHelper.kt    # 角色资源（目前硬编码）
│   ├── CourseScheduler.kt    # 课表闹钟调度
│   ├── IcsParser.kt          # ICS 文件解析（支持双周）
│   ├── NotificationHelper.kt # 通知管理（前台+提醒）
│   ├── ReminderDispatcher.kt # 提醒分发（5 种方式组合）
│   ├── SlotScheduler.kt      # 时间段闹钟调度
│   ├── TauntHelper.kt        # 挑衅语句
│   └── WhitelistManager.kt   # 白名单管理
├── model/                     # 数据层
│   ├── CourseSchedule.kt     # 课表数据模型
│   ├── CourseScheduleRepository.kt # 课表存取
│   ├── SlotRepository.kt     # 时间段存取
│   ├── StudySlot.kt          # 时间段模型
│   └── StudyStats.kt         # 学习统计（会话+摸鱼）
├── service/                   # Android 服务
│   ├── BootReceiver.kt       # 开机恢复闹钟
│   ├── CourseAlarmReceiver.kt # 课表闹钟接收
│   ├── FullScreenAlertActivity.kt # 全屏提醒
│   ├── OverlayService.kt     # 悬浮窗服务
│   ├── ScreenAccessibilityService.kt # 无障碍服务
│   ├── SlotAlarmReceiver.kt  # 时间段闹钟接收
│   └── TimerService.kt       # 前台计时服务
├── ui/                        # 界面
│   ├── AddSlotActivity.kt    # 添加/编辑时间段
│   ├── AppPickerActivity.kt  # 白名单选择器
│   ├── MainActivity.kt       # 主 Activity（设置页入口）
│   └── compose/              # Compose UI
│       ├── Components.kt     # GlassCard, LiquidGlassButton, StudySlotItem
│       ├── ComposeTheme.kt   # Material3 主题
│       ├── CourseScheduleScreen.kt # 课表页面
│       ├── SettingsScreen.kt # 设置页面（从 MainActivity 提取）
│       └── StudyScreen.kt    # 学习统计页面
└── ...
```

---

## 四、架构图

```
┌─────────────────────────────────────────────────────────┐
│                    MainActivity                          │
│  权限引导 + 导航 + ICS 导入 + 闹钟恢复                    │
│  3 个 Tab: StudyScreen / CourseScheduleScreen / Settings │
└────────┬──────────┬──────────┬──────────────────────────┘
         │          │          │
    ┌────▼───┐ ┌───▼────┐ ┌──▼──────────┐
    │StudyScr│ │CourseSc│ │SettingsScr  │
    │  een   │ │  reen  │ │  een        │
    └───┬────┘ └───┬────┘ └─────────────┘
        │          │
   ┌────▼──────────▼────────────────────────┐
   │      StudyStatsRepository (object)      │
   │  SharedPreferences + JSON 序列化         │
   │  内存缓存 (@Volatile cachedSessions)    │
   └────────┬───────────────────────────────┘
            │
   ┌────────▼───────────────────────────────┐
   │      Service / Receiver 层              │
   │  TimerService (前台计时)                 │
   │  OverlayService (悬浮窗)                │
   │  ScreenAccessibilityService (白名单检测) │
   │  SlotAlarmReceiver / CourseAlarmReceiver │
   │  BootReceiver (开机恢复)                │
   └────────┬───────────────────────────────┘
            │
   ┌────────▼───────────────────────────────┐
   │      Helper / Repository 层             │
   │  SlotRepository / CourseScheduleRepository│
   │  NotificationHelper                     │
   │  ReminderDispatcher                     │
   │  SlotScheduler / CourseScheduler        │
   │  IcsParser / WhitelistManager           │
   └────────────────────────────────────────┘
```

---

## 五、数据存储方案

### 所有数据存在 SharedPreferences 中（无数据库）

| 文件名 | 存储内容 | 格式 |
|--------|---------|------|
| `study_prefs` | 选项、活跃会话、摸鱼事件、API key | 布尔/长整型/JSON 字符串 |
| `study_slots` | 学习时间段列表 | JSON 数组字符串 |
| `study_courses` | 课表列表 | JSON 数组字符串 |
| `study_whitelist` | 白名单包名 | JSON 数组字符串 |
| `study_prefs` | 学期起始日期 | 字符串 |

**关键问题**: 每次读写都是 JSON 全量序列化/反序列化。详见"已知问题"章节。

---

## 六、已修复的重要 Bug（按时间倒序）

### v1.DREAM 修复
1. **AddSlotActivity ID 绕过**: 创建新 StudySlot 时用 `System.currentTimeMillis()` 而非 AtomicLong 计数器
2. **StudyStatsRepository SimpleDateFormat 线程不安全**: 改为 ThreadLocal
3. **CourseScheduler SimpleDateFormat 线程不安全**: 改为 ThreadLocal
4. **CourseScheduleScreen ThreadLocal 冲突**: Compose runtime 有自己的 ThreadLocal，需用 `java.lang.ThreadLocal` 全限定名
5. **MainActivity God Class**: MainScreen/OptionSwitch 提取到 SettingsScreen.kt
6. **StudyStatsRepository 性能**: 添加内存缓存避免重复 JSON 解析
7. **NotificationHelper Handler 泄漏**: cancelAll 中清理缓存的 PendingIntent
8. **死代码清理**: AddSlotDialog、TimerViewModel、dialog_new_slot.xml

### v1.ULTIMATE 修复
1. **allowBackup 安全漏洞**: `android:allowBackup="false"`
2. **TimerService 通知泄漏**: onTaskRemoved 中取消前台通知
3. **StudySlot ID 冲突**: 改用 AtomicLong(System.nanoTime())
4. **AiHelper 错误处理**: 读取错误响应体后再断开

### v1.ULTRA 修复（10 个 bug）
1. **SlotAlarmReceiver 未检查 enabled**: 添加 `if (!slot.enabled) return`
2. **CourseAlarmReceiver 未检查星期**: 添加 dayOfWeek 验证
3. **TimerService null intent**: START_STICKY 重启时 intent 为 null
4. **TimerService 停止方式**: STOP_FOREGROUND_DETACH → STOP_FOREGROUND_REMOVE
5. **IcsParser SimpleDateFormat**: 改为 ThreadLocal
6. **IcsParser 双周课**: 支持 INTERVAL=2 RRULE
7. **CourseScheduleRepository SimpleDateFormat**: 改为 ThreadLocal
8. **CourseSchedule ID 冲突**: 改用 AtomicLong
9. **NotificationHelper PendingIntent 缓存**: 避免每秒重建
10. **ScreenAccessibilityService 白名单缓存**: 60 秒 TTL

### 更早的修复
- **BarChart 基线对齐** (3 轮修复): fillMaxHeight → 固定高度容器 + BottomCenter，标签高度 14dp → 18dp
- **Handler 泄漏**: TimerService 中 cancelPendingIntent 在 onDestroy
- **LiveData 污染**: companion object 中的 static MutableLiveData
- **requestCode 冲突**: SlotScheduler XOR 0x3C9D vs CourseScheduler XOR 0x7A1B
- **OverlayService 竞态**: windowManager.addView 失败时降级到通知

---

## 七、已知未修复问题（按优先级）

### P0 — 线程安全
- **ScreenAccessibilityService 主线程阻塞**: isInStudyTime() 在主线程做 JSON 解析，窗口切换事件每秒 5-10 次，低端机 ANR 风险

### P1 — 架构问题
- **SharedPreferences JSON 全量序列化**: 每次读写都解析/序列化完整 JSON，session 数 > 500 时 UI 卡顿
- **10 个 object 单例无法测试**: StudyStatsRepository, SlotRepository, CourseScheduleRepository 等全部直接耦合 SharedPreferences
- **TimerViewModel 存在但未使用**: StudyScreen 直接操作 Repository 和 Service，绕过 ViewModel
- **MutableLiveData 在 Service companion object 中**: TimerService.remainingTimeLive 是反模式

### P2 — 代码质量
- **硬编码颜色值 15+ 处**: 虽然有 ComposeTheme 但 90% UI 代码直接写死 hex
- **硬编码中文字符串 30+ 处**: 未使用 strings.xml
- **SlotScheduler/CourseScheduler 80% 重复**: 应抽象为泛型 AlarmScheduler
- **ReminderDispatcher 5 个 boolean 参数**: 32 种组合，加第 6 种提醒方式需改所有调用方
- **厂商适配硬编码**: MainActivity 中小米 3 个 Activity 路径

### P3 — 安全
- **API Key 明文存储**: SharedPreferences 中的 api_key
- **无证书固定**: AiHelper 的 HTTPS 连接

---

## 八、关键设计决策与陷阱

### 8.1 AlarmManager requestCode 计算
```kotlin
// SlotScheduler: 用 XOR 0x3C9D
(slot.id.toInt() shl 4) xor 0x3C9D

// CourseScheduler: 用 XOR 0x7A1B
(course.id.toInt() shl 4) xor 0x7A1B
```
两个 Scheduler 的 requestCode 必须用不同的 XOR 值，否则会互相覆盖闹钟。

### 8.2 日期格式化
- **必须用 ThreadLocal**: IcsParser、CourseScheduleRepository、StudyStatsRepository、CourseScheduler 已修复
- **CourseScheduleScreen 用 `java.lang.ThreadLocal`**: 因为 Compose runtime 有自己的 `ThreadLocal` 类
- **未修复**: CourseScheduleScreen 的 `getWeekDates()` 和 `calculateTotalWeeks()` 顶层函数中仍有局部 SimpleDateFormat

### 8.3 TimerService 生命周期
```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent == null) {
        stopSelf()  // START_STICKY 重启时 intent 为 null
        return START_NOT_STICKY
    }
    // ...
}
```
- `stopForeground(STOP_FOREGROUND_REMOVE)` 而非 `STOP_FOREGROUND_DETACH`
- `onTaskRemoved` 中必须取消前台通知

### 8.4 ICS 双周课处理
IcsParser 解析 INTERVAL=2 时，不是生成一个跨越多周的范围，而是为每个活跃周生成独立的单周范围：
```
2026-02-23 ~ 2026-03-01  (第1周)
2026-03-09 ~ 2026-03-15  (第3周，跳过第2周)
```

### 8.5 BarChart 基线对齐
绝对不能用 `fillMaxHeight(fraction)` — 父 Column 中的 Text 会改变可用高度。
必须用固定高度容器 + `Alignment.BottomCenter`：
```kotlin
Box(modifier = Modifier.height(barHeight)) {
    Box(modifier = Modifier.fillMaxSize().background(barBg)) // 背景轨道
    Box(modifier = Modifier.fillMaxWidth().height(barHeight * fraction)
        .align(Alignment.BottomCenter).background(barColor)) // 填充
}
```
标签高度必须 18dp（9.sp 文字实际高度 > 14dp）。

### 8.6 白名单默认值
```kotlin
// WhitelistManager.kt
val DEFAULT_WHITELIST = setOf(
    "com.android.systemui",
    "com.miui.home",
    "com.huawei.android.launcher",
    // ... 13 个包名
)
```
包含小米、华为、OPPO、vivo 的桌面和系统 UI。修改默认值会影响所有新用户。

### 8.7 学习统计保留策略
- Session 数据保留 90 天（`endSession` 中 cutoff 过滤）
- Distraction 数据保留 30 天
- 每次 `endSession` 都全量读写 JSON

---

## 九、调用链路图

### 9.1 闹钟触发链
```
AlarmManager 触发
  → SlotAlarmReceiver.onReceive()
    → SlotRepository.getSlots() [JSON 解析]
    → 检查 slot.enabled
    → ReminderDispatcher.dispatchFromPreferences()
      → SharedPreferences 读取 5 个 boolean
      → NotificationHelper.showRepeatingAlertNotification()
      → OverlayService (如果开启)
    → SlotScheduler.scheduleSlot() [重新调度下次触发]

AlarmManager 触发
  → CourseAlarmReceiver.onReceive()
    → CourseScheduleRepository.getCourses() [JSON 解析]
    → 检查 exDates、dayOfWeek
    → CourseScheduler.scheduleCourse() [重新调度]
```

### 9.2 学习会话链
```
用户点击"开始学习"
  → StudyScreen.startManualSession()
    → StudyStatsRepository.startSession()
      → SharedPreferences 写入 active_session_start
    → context.startForegroundService(TimerService)
      → TimerService 创建前台通知
      → TimerService 每秒更新 LiveData

用户点击"结束学习"
  → StudyScreen.endStudySession()
    → StudyStatsRepository.endSession()
      → JSON 全量读取 sessions
      → 添加新 session
      → JSON 全量写入 sessions
    → TimerService.onTimerFinished()
      → stopForeground(STOP_FOREGROUND_REMOVE)
```

### 9.3 无障碍服务链
```
用户切换应用
  → ScreenAccessibilityService.onAccessibilityEvent()
    → isInStudyTime()
      → SlotRepository.getSlots() [JSON 解析]
      → CourseScheduleRepository.getCourses() [JSON 解析]
    → 检查应用是否在白名单
    → 如果不在白名单且在学习时间
      → showInterventionOverlay() 或 showInterventionNotification()
```

---

## 十、构建与发布

### 构建命令
```bash
cd /c/Users/Introduce/Downloads/StudyReminder
./gradlew assembleDebug
```
APK 输出: `app/build/outputs/apk/debug/app-debug.apk`

### 发布流程
1. 修改 `app/build.gradle` 中的 `versionCode` 和 `versionName`
2. `./gradlew assembleDebug`
3. `git add -A && git commit -m "vX.X: description"`
4. `git push origin main`（失败自动重试 10 次）
5. 通过 GitHub API 创建 Release 并上传 APK（`gh` CLI 未安装，用 `curl` + `$GITHUB_TOKEN`）

### GitHub Release 创建脚本
```bash
# 创建 Release
curl -s -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Content-Type: application/json" \
  "https://api.github.com/repos/Introduce183/StudyReminder/releases" \
  -d @/tmp/release_body.json

# 上传 APK
curl -s -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Content-Type: application/vnd.android.package-archive" \
  "https://uploads.github.com/repos/Introduce183/StudyReminder/releases/{ID}/assets?name=StudyReminder-vX.X.apk" \
  --data-binary @app/build/outputs/apk/debug/app-debug.apk
```

**注意**: `gh` CLI 未安装，必须用 curl + API。`$GITHUB_TOKEN` 环境变量已配置。

---

## 十一、统计与度量

| 指标 | 值 |
|------|-----|
| Kotlin 源文件数 | 31 |
| 最大文件 | MainActivity.kt (~460 行，已从 638 行缩减) |
| object 单例数 | 10 |
| SharedPreferences 读写点 | 20+ |
| 硬编码颜色值 | 15+ |
| 硬编码中文字符串 | 30+ |
| SimpleDateFormat 实例 | 6 (全部已修复为 ThreadLocal) |
| 死代码文件 | 0 (已清理) |
| 测试覆盖 | 0% |
| 总 commit 数 | ~47 |
| fix/feat 比 | 18:8 (69% fix) |

---

## 十二、给下一任维护者的建议

### 立即要做的
1. **AccessibilityService 主线程 JSON 解析** — 将 isInStudyTime 中的 Repository 调用移到后台线程或添加缓存
2. **补充单元测试** — 至少覆盖 IcsParser、StudyStatsRepository、SlotScheduler

### 中期要做的
3. **迁移到 Room 数据库** — 替代 SharedPreferences JSON，解决性能瓶颈
4. **引入依赖注入** — 替代 10 个 object 单例，使代码可测试
5. **统一闹钟调度器** — SlotScheduler 和 CourseScheduler 合并为泛型 AlarmScheduler
6. **外部化字符串和颜色** — 所有中文移入 strings.xml，所有颜色使用 Theme

### 长期要做的
7. **MainActivity 继续拆分** — 权限引导逻辑提取为独立组件
8. **提醒系统重构为策略模式** — 替代 ReminderDispatcher 的 5 个 boolean 参数
9. **迁移到 Kotlin Flow** — 替代 TimerService companion object 中的 static MutableLiveData
10. **API Key 加密存储** — 使用 EncryptedSharedPreferences

### 绝对不要做的事
- 不要把 SimpleDateFormat 作为类的实例字段（必须用 ThreadLocal）
- 不要在 Compose 中用 `ThreadLocal` 不带全限定名（会和 Compose runtime 冲突）
- 不要用 `fillMaxHeight(fraction)` 做柱状图（基线对齐会崩）
- 不要给 StudySlot 或 CourseSchedule 用 `System.currentTimeMillis()` 做 ID
- 不要在 TimerService.onStartCommand 中不检查 intent 是否为 null
- 不要用 `STOP_FOREGROUND_DETACH`（用 `STOP_FOREGROUND_REMOVE`）
- 不要自动把版本号从 1.3.9 进位到 1.4.0（应为 1.3.10）

---

## 十三、环境信息

- **OS**: Windows 11 Home China
- **Shell**: bash (Git Bash)
- **JDK**: 内置于 Android Studio
- **Gradle**: 9.0.0 (有 deprecation warning)
- **Kotlin**: 由 AGP 管理
- **Compose BOM**: 2024.09.00
- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 35 (Android 15)
- **Git remote**: `https://github.com/Introduce183/StudyReminder.git`
- **GitHub Token**: 环境变量 `$GITHUB_TOKEN` (ghp_ 开头)
- **gh CLI**: 未安装，用 curl + API 替代
