# 📚 StudyReminder - 学习提醒器 Android App

## 功能列表

| 功能 | 说明 |
|------|------|
| ⏰ 自定义计时 | 时间选择器设置学习时长 |
| 🔔 通知提醒 | 多次刷新通知，30秒后自动清除 |
| 💊 超级岛提醒 | 顶部动态胶囊，从小扩展弹出 |
| 🪟 后台小窗弹出 | 悬浮窗，可拖动，确认返桌面 |
| 📱 全屏弹出 | 全屏提醒界面，确认后自动返桌面 |
| 🏠 强制返回桌面 | 直接跳回桌面（与弹窗二选一） |
| 🤖 AI屏幕识别 | 无障碍服务读取屏幕内容，AI生成针对性挑衅语 |
| 🎭 角色选择 | 多种角色：傲娇学姐/元气学妹/冷酷学长/可爱小猫 |

---

## 项目结构

```
app/src/main/
├── java/com/study/reminder/
│   ├── ui/
│   │   ├── MainActivity.kt           # 主界面
│   │   └── FullScreenAlertActivity.kt # 全屏提醒
│   ├── service/
│   │   ├── TimerService.kt           # 前台计时服务（核心）
│   │   ├── OverlayService.kt         # 悬浮窗/超级岛服务
│   │   ├── ScreenAccessibilityService.kt # 屏幕内容读取
│   │   └── BootReceiver.kt           # 开机自启
│   ├── viewmodel/
│   │   └── TimerViewModel.kt         # 状态管理
│   └── helper/
│       ├── AiHelper.kt               # Claude API调用
│       ├── CharacterHelper.kt        # 角色资源映射
│       └── NotificationHelper.kt     # 通知管理
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   ├── activity_fullscreen_alert.xml
    │   ├── overlay_dynamic_island.xml
    │   └── overlay_small_window.xml
    └── xml/
        └── accessibility_service_config.xml
```

---

## 导入步骤

1. 用 **Android Studio** 打开 `StudyReminder` 文件夹
2. 等待 Gradle 同步完成
3. 在 `res/drawable/` 中添加以下图片资源：
   - `ic_timer.xml` — 计时图标
   - `ic_alert.xml` — 提醒图标
   - `ic_character_default.png` — 默认角色
   - `ic_character_tsundere.png` — 傲娇学姐
   - `ic_character_genki.png` — 元气学妹
   - `ic_character_cool.png` — 冷酷学长
   - `ic_character_cat.png` — 可爱小猫
4. 连接手机 → Run

---

## 权限说明

| 权限 | 用途 |
|------|------|
| SYSTEM_ALERT_WINDOW | 悬浮窗/小窗/超级岛 |
| POST_NOTIFICATIONS | 通知提醒 |
| FOREGROUND_SERVICE | 后台计时保活 |
| BIND_ACCESSIBILITY_SERVICE | AI屏幕内容识别 |
| USE_FULL_SCREEN_INTENT | 全屏提醒 |

---

## AI 功能配置

1. 点击主界面 **🔑 AI Key** 按钮
2. 输入你的 **Anthropic API Key**
3. 开启 **🤖 AI识别屏幕** 开关
4. 在手机设置 → 无障碍 → 学习提醒器 → 开启

AI 会读取当前屏幕内容（如：刷B站、打游戏），自动生成针对性挑衅话语！

---

## 小窗 & 全屏 互斥说明

- **小窗弹出** 和 **全屏弹出** 同时只能选一个
- **强制返桌面** 与以上两项也互斥
- 通知提醒和超级岛可以叠加使用
