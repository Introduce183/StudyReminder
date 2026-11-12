# -*- coding: utf-8 -*-
"""生成学习提醒器·星火杯路演答辩 PPT（16:9，深色青绿主题）"""
import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import qn

ASSETS = r"E:\StudyReminder\docs\ppt_assets"
OUT = r"C:\Users\Introduce\Desktop\学习提醒器_星火杯_路演答辩.pptx"

# ── 调色板 ──────────────────────────────────────────────
BG      = RGBColor(0x0B, 0x10, 0x13)
CARD    = RGBColor(0x15, 0x1E, 0x21)
CARD2   = RGBColor(0x11, 0x1A, 0x1D)
TEXT    = RGBColor(0xEA, 0xF3, 0xF2)
MUTED   = RGBColor(0x9D, 0xB5, 0xB2)
DIM     = RGBColor(0x64, 0x80, 0x7D)
ACCENT  = RGBColor(0x39, 0xC5, 0xBB)
PINK    = RGBColor(0xFF, 0x77, 0xB7)
BLUE    = RGBColor(0x5F, 0xD7, 0xFF)
ON_ACC  = RGBColor(0x06, 0x27, 0x2B)
LINE    = RGBColor(0x24, 0x32, 0x36)

EA = "Microsoft YaHei"
LATIN = "Segoe UI"
EMOJI = "Segoe UI Emoji"

prs = Presentation()
prs.slide_width = Inches(10)
prs.slide_height = Inches(5.625)

prs.core_properties.title = "学习提醒器 · 星火杯路演答辩"
prs.core_properties.author = "学习提醒器项目组"
prs.core_properties.subject = "第三十七届星火杯 · 科技发明制作 B 类"

# ── 基础工具 ────────────────────────────────────────────

def blank():
    return prs.slides.add_slide(prs.slide_layouts[6])

def bg(slide):
    f = slide.background.fill
    f.solid()
    f.fore_color.rgb = BG

def _no_line(sh):
    sh.line.fill.background()

def _no_shadow(sh):
    sh.shadow.inherit = False

def _alpha(shape, pct):
    """给纯色填充增加透明度（pct = 0~100 不透明度）"""
    spPr = shape._element.spPr
    sf = spPr.find(qn('a:solidFill'))
    if sf is None:
        return
    clr = sf.find(qn('a:srgbClr'))
    if clr is None:
        return
    a = clr.makeelement(qn('a:alpha'), {'val': str(int(pct * 1000))})
    clr.append(a)

GLOW_FILES = {
    "teal": os.path.join(ASSETS, "glow_teal.png"),
    "pink": os.path.join(ASSETS, "glow_pink.png"),
    "blue": os.path.join(ASSETS, "glow_blue.png"),
}

def glow(slide, cx, cy, d, color, alpha_pct=100):
    """柔光光晕：用预生成的径向渐变 PNG，避免 PowerPoint 渐变渲染差异"""
    key = "teal"
    if color == PINK:
        key = "pink"
    elif color == BLUE:
        key = "blue"
    path = GLOW_FILES[key]
    pic = slide.shapes.add_picture(path, Inches(cx - d / 2), Inches(cy - d / 2),
                                   Inches(d), Inches(d))
    return pic

def card(slide, x, y, w, h, fill=CARD, line=LINE, radius=0.07, line_w=0.75):
    sh = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,
                                Inches(x), Inches(y), Inches(w), Inches(h))
    sh.adjustments[0] = radius
    if fill is None:
        sh.fill.background()
    else:
        sh.fill.solid()
        sh.fill.fore_color.rgb = fill
    if line is None:
        _no_line(sh)
    else:
        sh.line.color.rgb = line
        sh.line.width = Pt(line_w)
    _no_shadow(sh)
    return sh

def pill(slide, x, y, w, h, text, color=ACCENT, fill_alpha=9, size=9, bold=True):
    card(slide, x, y, w, h, fill=color, line=None, radius=0.5)
    # 给填充加透明
    sh = slide.shapes[-1]
    _alpha(sh, fill_alpha)
    add_text(slide, x, y, w, h, text, size=size, color=color, bold=bold,
             align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    return sh

def set_run(run, size, bold, color, latin=LATIN, ea=EA, italic=False):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    run.font.color.rgb = color
    run.font.name = latin
    rPr = run._r.get_or_add_rPr()
    e = rPr.find(qn('a:ea'))
    if e is None:
        e = rPr.makeelement(qn('a:ea'), {})
        rPr.append(e)
    e.set('typeface', ea)

def add_text(slide, x, y, w, h, text, size=12, color=TEXT, bold=False,
             align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, spacing=None,
             latin=LATIN, ea=EA, italic=False):
    tb = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.margin_left = tf.margin_right = 0
    tf.margin_top = tf.margin_bottom = 0
    tf.vertical_anchor = anchor
    lines = text.split("\n")
    for i, line in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        if spacing is not None:
            p.line_spacing = spacing
        r = p.add_run()
        r.text = line
        set_run(r, size, bold, color, latin, ea, italic)
    return tb

def add_emoji(slide, x, y, w, h, emoji, size=24, align=PP_ALIGN.LEFT):
    return add_text(slide, x, y, w, h, emoji, size=size, color=TEXT,
                    align=align, latin=EMOJI, ea=EMOJI)

def header(slide, num, title, sub=None):
    add_text(slide, 0.5, 0.30, 0.6, 0.25, num, size=10.5, color=ACCENT, bold=True)
    add_text(slide, 0.5, 0.47, 5.2, 0.46, title, size=22, color=TEXT, bold=True)
    bar = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,
                                 Inches(0.52), Inches(1.02), Inches(0.52), Inches(0.032))
    bar.adjustments[0] = 0.5
    bar.fill.solid(); bar.fill.fore_color.rgb = ACCENT
    _no_line(bar); _no_shadow(bar)
    if sub:
        add_text(slide, 5.6, 0.64, 3.9, 0.28, sub, size=9.5, color=MUTED,
                 align=PP_ALIGN.RIGHT)

def footer(slide, page):
    add_text(slide, 0.5, 5.30, 4.0, 0.2, "学习提醒器 · 星火杯", size=7.5, color=DIM)
    add_text(slide, 8.9, 5.30, 0.6, 0.2, f"{page:02d}", size=7.5, color=DIM,
             align=PP_ALIGN.RIGHT)

def notes(slide, text):
    slide.notes_slide.notes_text_frame.text = text

def image(slide, path, x, y, w, h):
    return slide.shapes.add_picture(path, Inches(x), Inches(y), Inches(w), Inches(h))

# ============================================================
# 01 封面
# ============================================================
s = blank(); bg(s)
glow(s, 8.3, 1.2, 5.2, ACCENT, 7)
glow(s, 7.4, 4.6, 3.6, PINK, 4)
glow(s, 1.2, 5.6, 4.0, ACCENT, 3)

pill(s, 0.6, 0.52, 3.28, 0.34, "第三十七届星火杯 · 科技发明制作 B 类", size=9)
add_text(s, 0.6, 1.05, 6.0, 0.24, "STUDYREMINDER  /  ANDROID APP", size=9, color=ACCENT, bold=True)
add_text(s, 0.58, 1.42, 6.2, 0.8, "学习提醒器", size=44, color=TEXT, bold=True)
bar = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,
                         Inches(0.62), Inches(2.34), Inches(0.9), Inches(0.035))
bar.adjustments[0] = 0.5
bar.fill.solid(); bar.fill.fore_color.rgb = ACCENT
_no_line(bar); _no_shadow(bar)
add_text(s, 0.6, 2.56, 6.2, 0.42, "把「提醒」做成「约束」", size=21, color=TEXT, bold=True)
add_text(s, 0.6, 3.06, 6.0, 0.34, "面向大学生的固定时段学习监督应用", size=11.5, color=MUTED)

chips = [("固定时段", 1.02), ("分级提醒", 1.02), ("实时监督", 1.02), ("完全离线", 1.02)]
cx = 0.6
for label, w in chips:
    pill(s, cx, 3.62, w, 0.32, label, size=9)
    cx += w + 0.12

add_text(s, 0.6, 4.86, 5.6, 0.24, "西安电子科技大学  ·  第三十七届星火杯", size=9.5, color=DIM)
add_text(s, 0.6, 5.12, 5.6, 0.24, "答辩人：____________          版本：v2.4", size=9.5, color=DIM)

# 手机预览 + 边框
card(s, 6.86, 0.44, 2.36, 4.82, fill=CARD2, line=LINE, radius=0.06)
image(s, os.path.join(ASSETS, "mock_study_rounded.png"), 6.96, 0.54, 2.16, 4.62)

notes(s, "各位老师好，我是学习提醒器的开发者。今天我会按「为什么做、怎么做、怎么用、强在哪、怎么实现、给谁用、怎么走」的顺序汇报。我们的产品是一款面向大学生的固定时段学习监督 App——不只是提醒你去学习，而是用系统级约束，帮你真正坐下来。")

# ============================================================
# 02 问题背景
# ============================================================
s = blank(); bg(s); glow(s, 9.6, 0.4, 3.2, ACCENT, 5)
header(s, "01", "问题背景", "计划很丰满，执行很骨感")

problems = [
    ("😴", "拖延是常态", "定了学习计划，却总想「再刷五分钟」，一拖就是一小时。", "典型表现：计划反复重启"),
    ("📱", "手机是最大干扰", "学习时频繁切到短视频、社交与游戏，注意力被反复打断。", "典型表现：专注被持续打断"),
    ("🔔", "提醒太「软」", "普通通知随手就能划掉，无法形成真正的约束与监督。", "典型表现：提醒被无视"),
]
xs = [0.5, 3.55, 6.6]
for (emoji, title, desc, tag), x in zip(problems, xs):
    card(s, x, 1.30, 2.9, 2.78)
    add_emoji(s, x + 0.22, 1.48, 1.0, 0.5, emoji, size=26)
    add_text(s, x + 0.22, 2.10, 2.45, 0.34, title, size=14, color=TEXT, bold=True)
    add_text(s, x + 0.22, 2.52, 2.45, 0.9, desc, size=10, color=MUTED, spacing=1.45)
    ln = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x + 0.22), Inches(3.42), Inches(2.45), Inches(0.008))
    ln.fill.solid(); ln.fill.fore_color.rgb = LINE
    _no_line(ln); _no_shadow(ln)
    add_text(s, x + 0.22, 3.56, 2.45, 0.24, tag, size=8.6, color=ACCENT, bold=True)

bar_card = card(s, 0.5, 4.38, 9.0, 0.62, fill=ACCENT, line=None, radius=0.18)
_alpha(bar_card, 7)
# 左侧强调线
acc_bar = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,
                             Inches(0.52), Inches(4.52), Inches(0.035), Inches(0.34))
acc_bar.adjustments[0] = 0.5
acc_bar.fill.solid(); acc_bar.fill.fore_color.rgb = ACCENT
_no_line(acc_bar); _no_shadow(acc_bar)
add_text(s, 0.78, 4.44, 8.55, 0.5,
         "真正缺的不是计划工具，而是一个能打断分心的「硬约束」。",
         size=12.5, color=TEXT, bold=True, anchor=MSO_ANCHOR.MIDDLE)

footer(s, 2)
notes(s, "先说为什么做。我们观察到的现实是：计划定得很满，执行却总被手机打断；普通提醒随手一划就没了，缺少真正的约束。市面上的计划工具很多，但能在分心那一刻把人拉回来的很少。这就是我们的切入点。")

# ============================================================
# 03 解决方案
# ============================================================
s = blank(); bg(s); glow(s, -0.6, 5.6, 3.4, ACCENT, 5)
header(s, "02", "解决方案", "把学习时段变成不可逃避的约定")

card(s, 0.5, 1.28, 4.12, 3.48)
add_text(s, 0.72, 1.46, 3.6, 0.22, "PRODUCT  /  产品定义", size=8.5, color=ACCENT, bold=True)
add_text(s, 0.72, 1.74, 3.6, 0.45, "学习提醒器", size=20, color=TEXT, bold=True)
add_text(s, 0.72, 2.22, 3.62, 1.05,
         "以固定学习时段为核心：到点用系统级提醒强制打断，学习中途切到非白名单应用立即再次提醒。",
         size=9.8, color=MUTED, spacing=1.4)

rows = [
    ("01", "固定时段", "像课表一样按周重复，精确到分钟"),
    ("02", "分级提醒", "通知 → 超级岛 → 全屏 → 返回桌面"),
    ("03", "实时监督", "切到非白名单应用，立即再次提醒"),
]
ry = 3.30
for n, t, d in rows:
    add_text(s, 0.72, ry, 0.36, 0.26, n, size=9.5, color=ACCENT, bold=True)
    add_text(s, 1.08, ry - 0.01, 3.3, 0.26, t, size=10.5, color=TEXT, bold=True)
    add_text(s, 1.60, ry, 2.9, 0.26, "—  " + d, size=8.6, color=MUTED)
    ry += 0.44

steps = [
    ("1", "设定学习时段", "选择开始 / 结束时间与重复日期，自动生成每周计划。"),
    ("2", "到点强制提醒", "通知多次刷新；可叠加超级岛、全屏弹窗与返回桌面。"),
    ("3", "学习中途监督", "无障碍检测应用切换，越界即提醒并计入摸鱼次数。"),
]
sy = 1.30
vline = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,
                           Inches(5.12), Inches(1.72), Inches(0.02), Inches(2.72))
vline.adjustments[0] = 0.5
vline.fill.solid(); vline.fill.fore_color.rgb = LINE
_no_line(vline); _no_shadow(vline)
for n, t, d in steps:
    card(s, 4.92, sy, 4.58, 1.02, fill=CARD)
    circ = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(5.10), Inches(sy + 0.28), Inches(0.42), Inches(0.42))
    circ.fill.solid(); circ.fill.fore_color.rgb = ACCENT
    _no_line(circ); _no_shadow(circ)
    add_text(s, 5.10, sy + 0.28, 0.42, 0.42, n, size=12, color=ON_ACC, bold=True,
             align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    add_text(s, 5.68, sy + 0.16, 3.6, 0.26, t, size=12, color=TEXT, bold=True)
    add_text(s, 5.68, sy + 0.47, 3.66, 0.45, d, size=8.8, color=MUTED, spacing=1.2)
    sy += 1.19

pill(s, 0.5, 4.88, 1.18, 0.30, "无需账号", size=8.5)
pill(s, 1.80, 4.88, 1.18, 0.30, "无需服务器", size=8.5)
pill(s, 3.10, 4.88, 1.30, 0.30, "数据本地存储", size=8.5)
pill(s, 4.52, 4.88, 1.18, 0.30, "完全离线", size=8.5)

footer(s, 3)
notes(s, "针对这三个问题，我们的解法是：把学习时间做成像课表一样的固定时段，到点用分级提醒强制打断，学习中途继续监督。整个过程不需要账号和服务器，数据全部留在本地。具体用户怎么用？下一页用四步说明。")

# ============================================================
# 06 使用流程
# ============================================================
s = blank(); bg(s); glow(s, 0.0, 0.2, 3.4, ACCENT, 5)
header(s, "03", "使用流程", "四步完成一次完整督学")

flow = [
    ("01", "设定时段", "选择开始 / 结束时间\n与每周重复日期"),
    ("02", "精准触发", "AlarmManager 到点触发\n精确到分钟"),
    ("03", "分级提醒", "通知 / 超级岛 / 全屏\n/ 强制返回桌面"),
    ("04", "中途监督", "无障碍检测应用切换\n非白名单即提醒"),
]
xs = [0.5, 2.8, 5.1, 7.4]
for (n, t, d), x in zip(flow, xs):
    card(s, x, 1.40, 2.05, 2.35)
    add_text(s, x + 0.22, 1.58, 1.2, 0.5, n, size=23, color=ACCENT, bold=True)
    add_text(s, x + 0.22, 2.22, 1.7, 0.3, t, size=13.5, color=TEXT, bold=True)
    add_text(s, x + 0.22, 2.60, 1.68, 1.0, d, size=9.3, color=MUTED, spacing=1.35)

for ax in [2.60, 4.90, 7.20]:
    add_text(s, ax, 2.44, 0.22, 0.3, "→", size=16, color=ACCENT, bold=True,
             align=PP_ALIGN.CENTER)

add_text(s, 0.5, 4.28, 9.0, 0.3,
         "⏱   每个时间段结束后自动调度下一次，无需每天重复设置。",
         size=10.5, color=MUTED, align=PP_ALIGN.CENTER)
add_text(s, 0.5, 4.82, 9.0, 0.3,
         "设定一次，自动执行一整周。",
         size=13, color=ACCENT, bold=True, align=PP_ALIGN.CENTER)

footer(s, 4)
notes(s, "用户只需要设定一次：选时间、选重复日期，之后到点触发、分级提醒、中途监督都会自动完成，不需要每天重复配置。这个流程在界面上是什么样？我们看下一页。")

# ============================================================
# 08.5 界面展示
# ============================================================
s = blank(); bg(s); glow(s, 0.4, 5.4, 2.8, ACCENT, 4)
header(s, "04", "界面展示", "v2.4 深色主题 · 界面预览")

shots = [
    ("mock_study_rounded.png", "学习统计"),
    ("mock_course_rounded.png", "课表模式"),
    ("mock_settings_rounded.png", "设置"),
    ("mock_alert_rounded.png", "全屏提醒"),
]
pw, ph = 1.52, 3.29
gap = 0.30
total = len(shots) * pw + (len(shots) - 1) * gap
sx = (10 - total) / 2
sy = 1.26
for i, (fn, cap) in enumerate(shots):
    x = sx + i * (pw + gap)
    card(s, x - 0.045, sy - 0.045, pw + 0.09, ph + 0.09, fill=None, line=LINE, radius=0.05)
    image(s, os.path.join(ASSETS, fn), x, sy, pw, ph)
    add_text(s, x, sy + ph + 0.10, pw, 0.24, cap, size=8.6, color=MUTED,
             align=PP_ALIGN.CENTER)

add_text(s, 0.5, 4.98, 9.0, 0.22,
         "深色 / 浅色双主题   ·   学习统计 / 课表 / 设置 / 全屏提醒",
         size=8.2, color=DIM, align=PP_ALIGN.CENTER)

footer(s, 5)
notes(s, "这是 v2.4 的实际界面布局：学习统计页看计时和趋势，课表页按周管理课程，设置页管理时间段与提醒方式，最后是全屏提醒。界面背后的能力，下一页展开。")

# ============================================================
# 04 核心功能
# ============================================================
s = blank(); bg(s); glow(s, 10.2, 4.8, 3.4, PINK, 4)
header(s, "05", "核心功能", "从「提醒」到「约束」的完整闭环")

features = [
    ("📅", "固定学习时段", "按周重复、精确到分钟，结束后自动调度下一次。"),
    ("🔔", "多级通知提醒", "到点多次刷新，标题逐步升级，避免被随手忽略。"),
    ("💊", "超级岛提醒", "顶部胶囊轻提醒，自动收起，不打断当前心流。"),
    ("📱", "全屏强制弹出", "全屏覆盖手机，确认之后才允许返回桌面。"),
    ("🏠", "强制返回桌面", "直接打断当前操作，把注意力拉回学习本身。"),
    ("🛡️", "白名单监督", "学习中途切到非白名单应用，立即再次提醒。"),
]
xs = [0.5, 3.55, 6.6]
ys = [1.28, 3.10]
for i, (emoji, title, desc) in enumerate(features):
    x = xs[i % 3]; y = ys[i // 3]
    card(s, x, y, 2.9, 1.68)
    add_emoji(s, x + 0.20, y + 0.14, 0.8, 0.38, emoji, size=19)
    add_text(s, x + 0.20, y + 0.56, 2.5, 0.28, title, size=12.5, color=TEXT, bold=True)
    add_text(s, x + 0.20, y + 0.88, 2.52, 0.68, desc, size=9.2, color=MUTED, spacing=1.25)

add_text(s, 0.5, 4.98, 9.0, 0.24,
         "课表导入   ·   学习统计   ·   深浅主题   ·   专注计时",
         size=8.8, color=DIM, align=PP_ALIGN.CENTER)
footer(s, 6)
notes(s, "功能可以分成三层：最基础的是固定时段和多级通知；往上可以叠加超级岛、全屏和强制返回桌面；最外层是白名单与无障碍监督。这些能力里，哪些是别人没有的？")

# ============================================================
# 07 创新亮点
# ============================================================
s = blank(); bg(s); glow(s, 10.4, 2.6, 4.0, ACCENT, 4)
header(s, "06", "创新亮点", "四个关键差异点")

innov = [
    ("🎯", "时段模型", "不是倒计时，而是课程表式的固定时段，贴合真实作息。"),
    ("💪", "强制级提醒", "全屏 + 强制返回桌面，突破「通知可忽略」的天花板。"),
    ("🧠", "实时监督", "无障碍监听应用切换，白名单内自由使用，越界即提醒。"),
    ("📴", "完全离线", "无服务器、无账号、无网络依赖，隐私友好、成本为零。"),
]
xs = [0.5, 5.15]
ys = [1.28, 2.90]
for i, (emoji, title, desc) in enumerate(innov):
    x = xs[i % 2]; y = ys[i // 2]
    card(s, x, y, 4.35, 1.42)
    add_text(s, x + 3.72, y + 0.14, 0.45, 0.25, f"0{i+1}", size=8, color=DIM, bold=True,
             align=PP_ALIGN.RIGHT)
    add_emoji(s, x + 0.24, y + 0.14, 0.6, 0.4, emoji, size=19)
    add_text(s, x + 0.70, y + 0.20, 3.3, 0.3, title, size=13.5, color=TEXT, bold=True)
    add_text(s, x + 0.24, y + 0.68, 3.95, 0.6, desc, size=9.8, color=MUTED, spacing=1.3)

add_text(s, 0.5, 4.72, 9.0, 0.34,
         "把「软提醒」升级为「硬约束」。",
         size=13, color=ACCENT, bold=True, align=PP_ALIGN.CENTER)

footer(s, 7)
notes(s, "我们的差异化集中在四点：时段模型、强制级提醒、实时监督、完全离线。尤其是「强制返回桌面 + 无障碍监督」的组合，普通番茄钟做不到。放到竞品里看更清楚。")

# ============================================================
# 08 竞品对比
# ============================================================
s = blank(); bg(s); glow(s, 9.9, 3.6, 3.4, ACCENT, 4)
header(s, "07", "竞品对比", "在「强制力」上做差异化")

# 高亮列
card(s, 6.82, 1.22, 1.78, 3.62, fill=ACCENT, line=None, radius=0.06)
hl = s.shapes[-1]
_alpha(hl, 6)
hlb = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,
                         Inches(6.84), Inches(1.24), Inches(1.74), Inches(3.58))
hlb.fill.background(); hlb.line.color.rgb = ACCENT; hlb.line.width = Pt(0.75)
hlb.adjustments[0] = 0.06
_no_shadow(hlb)

add_text(s, 0.5, 1.36, 1.8, 0.3, "能力", size=10, color=DIM, bold=True)
cols = [("系统闹钟", 2.45), ("番茄钟 App", 3.93), ("锁机类 App", 5.41), ("学习提醒器", 6.89)]
for name, x in cols:
    color = ACCENT if name == "学习提醒器" else TEXT
    add_text(s, x, 1.36, 1.42, 0.3, name, size=9.6, color=color, bold=True, align=PP_ALIGN.CENTER)

rows = [
    ("固定学习时段", "✓", "✓", "△", "✓"),
    ("到点强制提醒", "×", "△", "✓", "✓"),
    ("学习中途监督", "×", "×", "△", "✓"),
    ("白名单放行", "×", "×", "×", "✓"),
    ("完全离线", "✓", "✓", "△", "✓"),
    ("免费无内购", "✓", "△", "×", "✓"),
]
ry = 1.84
for label, a, b, c, d in rows:
    add_text(s, 0.5, ry + 0.06, 1.85, 0.3, label, size=9.8, color=TEXT)
    for val, x in zip([a, b, c, d], [2.45, 3.93, 5.41, 6.89]):
        if val == "✓":
            add_text(s, x, ry, 1.42, 0.34, "✓", size=13, color=ACCENT, bold=True, align=PP_ALIGN.CENTER)
        elif val == "×":
            add_text(s, x, ry + 0.02, 1.42, 0.34, "×", size=12, color=DIM, align=PP_ALIGN.CENTER)
        else:
            add_text(s, x, ry + 0.05, 1.42, 0.34, "部分", size=8.6, color=MUTED, align=PP_ALIGN.CENTER)
    ln = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.5), Inches(ry + 0.42), Inches(8.4), Inches(0.008))
    ln.fill.solid(); ln.fill.fore_color.rgb = LINE
    _no_line(ln); _no_shadow(ln)
    ry += 0.47

add_text(s, 0.5, 4.92, 9.0, 0.2, "基于公开功能整理，仅作定性对比。", size=7.8, color=DIM)
footer(s, 8)
notes(s, "和系统闹钟、番茄钟、锁机类 App 相比，我们的优势不是功能更多，而是把「固定时段 + 强制提醒 + 过程监督」做成了完整闭环，同时保持免费和离线。能做到这些，靠的是什么？")

# ============================================================
# 05 技术架构
# ============================================================
s = blank(); bg(s); glow(s, 9.8, 0.2, 3.0, ACCENT, 4)
header(s, "08", "技术架构", "三层架构，全部运行在本地")

cols = [
    ("交互层  UI", ACCENT, [
        "Jetpack Compose 主界面",
        "学习 / 课表 / 设置 三页",
        "XML 悬浮窗与全屏提醒",
        "深浅双主题设计系统",
    ]),
    ("服务层  SERVICE", BLUE, [
        "AlarmManager 精确调度",
        "Foreground Service 计时保活",
        "AccessibilityService 应用监督",
        "WindowManager 悬浮窗",
    ]),
    ("数据层  DATA", PINK, [
        "SharedPreferences + JSON",
        "时间段 · 课表 · 学习统计",
        "白名单 · 主题偏好",
        "全部本地，无云端依赖",
    ]),
]
xs = [0.5, 3.55, 6.6]
for (title, color, items), x in zip(cols, xs):
    card(s, x, 1.28, 2.9, 3.30)
    add_text(s, x + 0.22, 1.46, 2.4, 0.24, title, size=10.5, color=color, bold=True)
    ln = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x + 0.22), Inches(1.76), Inches(2.42), Inches(0.012))
    ln.fill.solid(); ln.fill.fore_color.rgb = LINE
    _no_line(ln); _no_shadow(ln)
    iy = 1.94
    for it in items:
        dot = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x + 0.24), Inches(iy + 0.075), Inches(0.07), Inches(0.07))
        dot.fill.solid(); dot.fill.fore_color.rgb = color
        _no_line(dot); _no_shadow(dot)
        add_text(s, x + 0.42, iy, 2.28, 0.36, it, size=9.2, color=MUTED, spacing=1.15)
        iy += 0.58

techs = [("Kotlin", 0.82), ("Jetpack Compose", 1.45), ("Coroutines", 1.10),
         ("AlarmManager", 1.30), ("Foreground Service", 1.55), ("WindowManager", 1.28)]
cx = 0.5
for t, w in techs:
    pill(s, cx, 4.98, w, 0.30, t, color=TEXT, fill_alpha=7, size=8)
    cx += w + 0.12

footer(s, 9)
notes(s, "技术上采用三层结构：Compose 负责界面，Service 层承载闹钟、计时、悬浮窗和无障碍监督，数据层用 SharedPreferences 加 JSON 本地持久化。所有能力都在手机本地完成。技术之外，谁需要它？")

# ============================================================
# 09 应用价值与市场
# ============================================================
s = blank(); bg(s); glow(s, 10.2, 0.4, 3.2, PINK, 4)
header(s, "09", "应用价值", "真实场景里的刚需")

add_text(s, 0.5, 1.28, 2.0, 0.24, "目标用户", size=10, color=ACCENT, bold=True)
users = [
    ("🎓", "在校大学生", "课余自习与日常备考"),
    ("📚", "考研 / 考公", "长周期、高强度学习"),
    ("💼", "职场自学", "碎片时间提升技能"),
    ("👨‍👩‍👧", "家长督促", "为孩子设定学习时段"),
]
ux = [0.5, 2.72]
uy = [1.58, 2.56]
for i, (emoji, title, desc) in enumerate(users):
    x = ux[i % 2]; y = uy[i // 2]
    card(s, x, y, 2.08, 0.88)
    add_emoji(s, x + 0.14, y + 0.12, 0.4, 0.3, emoji, size=14)
    add_text(s, x + 0.54, y + 0.14, 1.45, 0.24, title, size=10, color=TEXT, bold=True)
    add_text(s, x + 0.54, y + 0.42, 1.45, 0.42, desc, size=8.2, color=MUTED, spacing=1.15)

stat1 = card(s, 5.20, 1.54, 2.05, 1.28)
add_text(s, 5.38, 1.68, 1.7, 0.5, "4763万", size=23, color=ACCENT, bold=True)
add_text(s, 5.38, 2.18, 1.80, 0.22, "全国高等教育在学总规模", size=8.6, color=MUTED)
add_text(s, 5.38, 2.44, 1.75, 0.2, "教育部 2023 统计", size=7.2, color=DIM)

stat2 = card(s, 7.45, 1.54, 2.05, 1.28)
add_text(s, 7.63, 1.68, 1.7, 0.5, "0", size=23, color=ACCENT, bold=True)
add_text(s, 7.63, 2.18, 1.75, 0.22, "服务器 / 账号 / 广告", size=9, color=MUTED)
add_text(s, 7.63, 2.44, 1.75, 0.2, "完全本地运行", size=7.2, color=DIM)

add_text(s, 5.20, 2.90, 4.3, 0.24, "数据来源：《2023 年全国教育事业发展统计公报》", size=6.8, color=DIM)

card(s, 0.5, 3.72, 9.0, 1.15)
add_text(s, 0.72, 3.86, 1.2, 0.22, "典型场景", size=9.5, color=ACCENT, bold=True)
scenes = [("晚自习", 1.02), ("考研备考", 1.18), ("网课学习", 1.18), ("论文写作", 1.18), ("周末自习", 1.18)]
scx = 1.72
for name, w in scenes:
    pill(s, scx, 3.84, w, 0.30, name, size=8.5)
    scx += w + 0.12
add_text(s, 0.72, 4.32, 8.5, 0.3,
         "不需要改变用户的设备与习惯——只需要在设定好的时间里，帮他把注意力留在书桌上。",
         size=9.5, color=MUTED)

footer(s, 10)
notes(s, "目标用户从在校大学生延伸到考研、职场自学和家长场景。全国高等教育在学总规模超过 4760 万，而我们不需要服务器和账号，意味着可以用极低的成本服务这部分人群。怎么触达他们？")

# ============================================================
# 10 推广与可持续
# ============================================================
s = blank(); bg(s); glow(s, -0.4, 0.4, 3.0, BLUE, 4)
header(s, "10", "推广与可持续", "从校园口碑到自我造血")

add_text(s, 0.5, 1.28, 3.0, 0.24, "校园推广三步走", size=10, color=ACCENT, bold=True)
promo = [
    ("01", "社团合作", "与学习类社团共建「自习打卡」活动"),
    ("02", "班级试点", "班委牵头，先在一个班跑通口碑"),
    ("03", "校园传播", "新生群、校园墙、B 站 / 小红书真实体验"),
]
py = 1.60
for n, t, d in promo:
    card(s, 0.5, py, 4.4, 0.74)
    add_text(s, 0.68, py + 0.22, 0.4, 0.24, n, size=9.5, color=ACCENT, bold=True)
    add_text(s, 1.10, py + 0.12, 1.3, 0.26, t, size=11, color=TEXT, bold=True)
    add_text(s, 1.10, py + 0.39, 3.6, 0.26, d, size=8.4, color=MUTED)
    py += 0.90

card(s, 5.15, 1.28, 4.35, 2.95)
add_text(s, 5.37, 1.46, 3.6, 0.26, "可持续模式", size=11, color=TEXT, bold=True)
model = [
    ("核心功能永久免费", "先用起来，再谈其他"),
    ("推广零成本", "依靠校园口碑与真实体验扩散"),
    ("未来增值", "主题皮肤 · 学习数据报告 · 家长端"),
]
my = 1.86
for t, d in model:
    dot = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(5.39), Inches(my + 0.07), Inches(0.08), Inches(0.08))
    dot.fill.solid(); dot.fill.fore_color.rgb = ACCENT
    _no_line(dot); _no_shadow(dot)
    add_text(s, 5.58, my, 3.6, 0.24, t, size=10.2, color=TEXT, bold=True)
    add_text(s, 5.58, my + 0.25, 3.6, 0.24, d, size=8.4, color=MUTED)
    my += 0.73

add_text(s, 0.5, 4.62, 9.0, 0.34,
         "先把「有用」做成口碑，再把「好用」做成产品。",
         size=13, color=TEXT, bold=True, align=PP_ALIGN.CENTER)

footer(s, 11)
notes(s, "推广上我们选择最轻的校园路径：社团、班级、校园传播。商业上不急着变现，先把核心功能免费做出口碑，未来再考虑主题、数据报告和家长端。接下来做什么？")

# ============================================================
# 11 路线图
# ============================================================
s = blank(); bg(s); glow(s, 0.2, 5.4, 3.2, ACCENT, 4)
header(s, "11", "路线图", "从一个想法到可用产品")

road = [
    ("DONE", ACCENT, "已完成 · v2.4", [
        "固定时段与闹钟调度",
        "课表导入与学习统计",
        "深浅主题与全新 UI",
        "无障碍监督与白名单",
    ]),
    ("NOW", BLUE, "进行中", [
        "更多机型适配与稳定性",
        "功耗与内存占用优化",
        "新手引导与快捷设置",
        "异常场景兜底处理",
    ]),
    ("NEXT", PINK, "规划中", [
        "iOS / 鸿蒙版本",
        "AI 督学语句",
        "学习周报与数据洞察",
        "家长端与多设备同步",
    ]),
]
xs = [0.5, 3.55, 6.6]
for (badge, color, title, items), x in zip(road, xs):
    card(s, x, 1.30, 2.9, 3.15)
    pill(s, x + 0.20, 1.46, 0.70, 0.26, badge, color=color, size=7.5)
    add_text(s, x + 0.20, 1.82, 2.5, 0.26, title, size=11.5, color=TEXT, bold=True)
    iy = 2.20
    for it in items:
        dot = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x + 0.22), Inches(iy + 0.07), Inches(0.07), Inches(0.07))
        dot.fill.solid(); dot.fill.fore_color.rgb = color
        _no_line(dot); _no_shadow(dot)
        add_text(s, x + 0.38, iy, 2.35, 0.24, it, size=8.8, color=MUTED, spacing=1.1)
        iy += 0.44

add_text(s, 0.5, 4.86, 9.0, 0.26,
         "最新版本 v2.4 已发布：github.com/Introduce183/StudyReminder",
         size=9.5, color=ACCENT, align=PP_ALIGN.CENTER)

footer(s, 12)
notes(s, "目前 v2.4 已经完成核心闭环并发布，接下来主要是机型适配和体验打磨；更长远的规划包括 iOS / 鸿蒙、AI 督学语句和数据报告。这些工作是谁做的？")

# ============================================================
# 12 团队
# ============================================================
s = blank(); bg(s); glow(s, 10.4, 4.8, 3.4, ACCENT, 4)
header(s, "12", "团队", "一个人，也是一支队伍")

team = [
    ("🧑‍💻", "负责人", "产品设计 / Android 开发", "需求、交互、架构与实现"),
    ("🎨", "成员", "视觉设计 / 测试", "界面、图标与体验验证"),
    ("🧑‍🏫", "指导教师", "项目指导", "方向把关与答辩指导"),
]
xs = [0.5, 3.55, 6.6]
for (emoji, role, name, desc), x in zip(team, xs):
    card(s, x, 1.40, 2.9, 2.06)
    circ = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x + 1.10), Inches(1.60), Inches(0.70), Inches(0.70))
    circ.fill.solid(); circ.fill.fore_color.rgb = CARD2
    circ.line.color.rgb = LINE; circ.line.width = Pt(1)
    _no_shadow(circ)
    add_emoji(s, x + 1.10, 1.68, 0.70, 0.5, emoji, size=22, align=PP_ALIGN.CENTER)
    add_text(s, x + 0.2, 2.42, 2.5, 0.24, role, size=11, color=TEXT, bold=True, align=PP_ALIGN.CENTER)
    add_text(s, x + 0.2, 2.70, 2.5, 0.24, "（请填写姓名）", size=8.5, color=DIM, align=PP_ALIGN.CENTER)
    add_text(s, x + 0.2, 2.98, 2.5, 0.4, desc, size=8.2, color=MUTED, align=PP_ALIGN.CENTER, spacing=1.2)

card(s, 0.5, 3.82, 9.0, 0.80, fill=ACCENT, line=None, radius=0.16)
q = s.shapes[-1]; _alpha(q, 7)
add_text(s, 0.7, 3.94, 8.6, 0.56,
         "从需求、设计、开发到发布，独立完成一个真正可用的 Android 产品。",
         size=11.5, color=TEXT, bold=True, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)

add_text(s, 0.5, 4.90, 9.0, 0.24,
         "GitHub  ·  github.com/Introduce183/StudyReminder",
         size=9, color=ACCENT, align=PP_ALIGN.CENTER)

footer(s, 13)
notes(s, "这是一个由学生独立完成的项目：从需求、设计、开发到发布，全部自己推进，也感谢指导老师在方向上的把关。最后，谢谢各位老师。")

# ============================================================
# 13 结束页
# ============================================================
s = blank(); bg(s)
glow(s, 5.0, 0.4, 5.2, ACCENT, 6)
glow(s, 8.6, 4.8, 3.6, PINK, 4)

add_emoji(s, 4.55, 0.95, 0.9, 0.8, "📚", size=34, align=PP_ALIGN.CENTER)
add_text(s, 1.0, 1.78, 8.0, 0.66, "感谢评审老师", size=34, color=TEXT, bold=True, align=PP_ALIGN.CENTER)
add_text(s, 1.0, 2.62, 8.0, 0.4, "让自律，成为习惯。", size=17, color=ACCENT, bold=True, align=PP_ALIGN.CENTER)

bar = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,
                         Inches(4.35), Inches(3.30), Inches(1.30), Inches(0.035))
bar.adjustments[0] = 0.5
bar.fill.solid(); bar.fill.fore_color.rgb = ACCENT
_no_line(bar); _no_shadow(bar)

add_text(s, 1.0, 3.55, 8.0, 0.26, "西安电子科技大学  ·  第三十七届星火杯", size=10, color=MUTED, align=PP_ALIGN.CENTER)
add_text(s, 1.0, 3.85, 8.0, 0.26, "学习提醒器  v2.4  ·  Android  ·  完全离线运行", size=9, color=DIM, align=PP_ALIGN.CENTER)
add_text(s, 1.0, 4.35, 8.0, 0.26, "github.com/Introduce183/StudyReminder", size=9, color=ACCENT, align=PP_ALIGN.CENTER)

notes(s, "以上就是我们的项目。学习提醒器想做的事情很简单：让自律不再只靠意志力。谢谢各位老师。")

# ── 保存 ────────────────────────────────────────────────
prs.save(OUT)
print("SAVED:", OUT)
print("SLIDES:", len(prs.slides))
