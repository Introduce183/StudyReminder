# StudyReminder 审计交接（2026-08-04）

## 结论

- 审计基线：`0174577`（`v2.2`）；Git 工作区在审计开始时干净。
- 实际 Android 模块为 `app/`。`app/app/` 未被 `settings.gradle` 引入，是历史遗留源码，不能代表当前 APK。
- `:app:assembleDebug --offline` 和 `:app:lintDebug --offline --warning-mode all` 均成功；没有自动化测试源码。
- 请先处理 P0：多个新增学习时段目前共用 ID `0`，会彼此覆盖闹钟并使编辑、开关、删除作用于同一条/多条记录。

## 审计范围与限制

已审阅当前 `app/src/main` 的 Kotlin、Manifest、Gradle 与资源配置，重点覆盖学习时段、课程 ICS 导入、AlarmManager、前台服务、无障碍、悬浮窗、通知与本地存储。

未在真实 Android 设备/模拟器上执行交互测试；后台服务限制、通知权限、精确闹钟、悬浮窗和无障碍行为仍须在 Android 12、13、14+ 真机验证。

## 发现与建议修复顺序

### P0：新建时段 ID 恒为 0，导致闹钟和数据操作冲突

- 位置：`app/src/main/java/com/study/reminder/ui/AddSlotActivity.kt:216`
- 原因：调用 `StudySlot(...)` 时明确传入 `id = initialSlot?.id ?: 0L`，绕过了 `StudySlot` 构造参数的默认 ID 生成器。
- 影响：任意两个新时段共享同一持久化 ID 和同一个 `PendingIntent` 请求码。后创建的时段会覆盖前一个闹钟；切换、编辑、删除按 ID 匹配时会影响所有 ID 为 0 的记录。
- 修复：编辑时传入原 ID；新建时不要传入 `id`，或使用可靠的持久化唯一 ID（如 UUID/毫秒加随机数）。修复后需要迁移已有的重复/0 ID 数据，并调用 `SlotScheduler.rescheduleAll`。
- 验证：连续新增三个时段，确认 JSON 中 ID 唯一、三个闹钟请求码不同；分别开关、编辑、删除，确认互不影响；重启后再次验证。

### P1：ICS 的多星期规则会丢失除第一天以外的课程

- 位置：`helper/IcsParser.kt:219-224`
- 原因：`BYDAY` 虽被解析为列表，但只取 `firstOrNull()`；如 `BYDAY=MO,WE` 仅生成周一课程。
- 影响：常见的同一 VEVENT 多日重复课会漏提醒、漏显示。
- 修复：将一个 RawVEvent 展开为每个 `BYDAY` 的独立课程记录，或让数据模型支持多天；保留相同时间、地点、范围和 EXDATE。
- 验证：导入含 `RRULE:FREQ=WEEKLY;BYDAY=MO,WE` 的最小 ICS，必须产生两条可调度课程。

### P1：ICS 忽略 `COUNT`，无 `UNTIL` 时硬编码为 20 周

- 位置：`helper/IcsParser.kt:65`
- 原因：没有 `UNTIL` 的规则统一被延长至开始日后 20 周，且未解析 `COUNT`。
- 影响：短课程会多提醒；超过 20 周或无限重复的日历会被截断。
- 修复：解析 `COUNT` 并按频率/间隔生成精确次数；若规则无限重复，应在 UI 提示并采用明确产品策略，而非静默截断。
- 验证：覆盖 `COUNT=3`、无 `COUNT`/`UNTIL`、`INTERVAL=2` 三类 ICS 样本。

### P1：课程日期时间未正确处理 UTC/时区

- 位置：`helper/IcsParser.kt:270-288`
- 原因：`DTSTART...Z` 仅移除了 `Z`，并把 UTC 的钟点直接当作本地时间；`TZID` 参数也没有进行时区换算。
- 影响：跨时区日历、以 UTC 导出的校园日历会在本地显示/提醒错时。
- 修复：迁移到 `java.time`，按 `TZID` 或 UTC 解析为 `ZonedDateTime` 后转换到设备/课表时区；为无时区字段保留 floating-time 语义。

### P1：后台触发时悬浮窗服务可能被 Android 拒绝，异常被吞没

- 位置：`helper/ReminderDispatcher.kt:40,62,72`
- 原因：从 AlarmReceiver/无障碍后台路径用 `startService` 启动普通 `OverlayService`；Android 8+ 对后台启动服务有限制，`runCatching` 没有记录失败原因。
- 影响：通知可作为回退，但动态岛/小窗/全屏悬浮窗会在部分系统上静默失效，难以诊断。
- 修复：设计符合后台执行限制的提醒流程（优先通知和 FullScreenIntent）；若确需服务，确认前台服务类型与启动窗口合规，并记录受控错误。不要仅靠吞异常。
- 验证：锁屏、应用被划掉、Doze、后台受限四种状态下，在 Android 12/13/14+ 实测各提醒方式。

### P2：统计将所有提醒都记为“分心”

- 位置：`helper/ReminderDispatcher.kt:11`
- 原因：`dispatchFromPreferences` 无条件调用 `recordDistraction`，而它同时被时段开始和课程开始提醒调用。
- 影响：用户没有切换到非白名单应用时，课程/时段到点通知也会增加“分心次数”，统计含义失真。
- 修复：把“提醒分发”和“记录分心”解耦；仅由无障碍服务确认的违规切换触发统计，或显式传入事件类型。

### P2：统计页面结束会话后不会刷新汇总数据

- 位置：`ui/compose/StudyScreen.kt:40-43,90`
- 原因：周/月/今日统计只以 `weekOffset` 或 `monthOffset` 为 `remember` 键；结束会话后仅刷新 `isStudying`，不会重新查询统计。
- 影响：同一页面中计时停止后，当日、周、月总时长与次数可能显示旧值，直到切换周期/重建页面。
- 修复：引入可变的 stats refresh token（结束、开始、服务回调后递增），或改用 Flow/ViewModel 单一状态源。
- 验证：开始至少 1 分钟的会话，结束后不切页，确认所有统计即时更新。

### P2：数据层缓存是进程全局静态状态，且没有并发保护

- 位置：`model/SlotRepository.kt`、`CourseScheduleRepository.kt`、`StudyStats.kt`
- 风险：repository 以 `@Volatile` 缓存 JSON 解析结果，但读-改-写并非原子操作。Activity、Receiver、Service 可并发修改同一 SharedPreferences，造成最后写入覆盖；进程存活期间也缺少外部变更失效策略。
- 修复：将数据操作收口到单一 repository 和 `Mutex`/单线程 dispatcher，或迁移 DataStore/Room；以 Flow 驱动 UI 状态，避免 UI 手动刷新。

### P3：可维护性与性能问题

- `app/app/` 是未构建的历史模块，且与当前源码同包名、内容分叉。删除或移至明确的 archive，并在 README 标明唯一构建入口。
- `AiHelper` 与 `SecurePreferencesManager` 在当前模块无调用点；AI API 代码、网络权限和加密依赖形成未使用维护面。明确接入或删除。
- `AppPickerAdapter.filter` 使用 `notifyDataSetChanged`（Lint），应用很多时会整表重绑；改用 `ListAdapter` + `DiffUtil`。
- 多个 UI 文本散落在 Kotlin/XML（Lint HardcodedText）；迁移到 `strings.xml`，同时消除源文件编码混乱风险。
- `FullScreenAlertActivity` 针对 API 27 的分支在 `minSdk 29` 下已过时；`mipmap-anydpi-v26` 也可合并。
- 根 `build.gradle` 的 Groovy 空格赋值写法触发 Gradle 10 弃用警告；改为 `namespace = ...`、`compileSdk = ...` 等赋值语法。
- Lint 的 `InflateParams`（MainActivity 与 OverlayService）、根布局 overdraw、Android 12 data extraction rules 也应在常规清理中处理。`allowBackup=false` 已降低备份风险，但建议显式声明数据提取规则以消除平台语义歧义。

## 建议实施计划

1. 修复 P0 并为 ID 生成、仓库 CRUD、`PendingIntent` 唯一性补充单元测试；先处理已有 ID=0/重复 ID 的迁移。
2. 为 ICS 解析建立 fixture 测试：单次、BYDAY 多天、COUNT、UNTIL、INTERVAL、EXDATE、TZID、UTC、折行。
3. 把 reminder 的事件类型拆开，校正统计口径；改用 ViewModel + Flow/StateFlow 管理计时和统计刷新。
4. 在真机矩阵上验证后台/权限行为，记录各系统的降级策略。
5. 清除旧模块和未使用代码，升级构建脚本语法，并将 Lint 设为 CI 必跑项。

## 最小测试清单

- 时段：新增 3 个、重启、开关/编辑/删除各一条；验证三个提醒仍独立。
- 调度：精确闹钟已授权与未授权、重启、修改系统时间、Doze。
- 课程：上述 ICS fixture 导入后，核对课表显示和实际触发日。
- 提醒：通知关闭/开启、悬浮窗权限开/关、无障碍开/关、锁屏、应用被划掉。
- 统计：手动开始/停止、无障碍违规、纯到点课程提醒，确认三种场景的时长和分心计数符合定义。

## 已执行验证

```text
./gradlew.bat :app:assembleDebug --offline
BUILD SUCCESSFUL

./gradlew.bat :app:lintDebug --offline --warning-mode all
BUILD SUCCESSFUL
```

Lint 报告：`app/build/reports/lint-results-debug.html`。本仓库当前没有 `app/src/test` 或 `app/src/androidTest` 测试文件。
