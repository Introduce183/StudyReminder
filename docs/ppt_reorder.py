# -*- coding: utf-8 -*-
"""按答辩叙事顺序重排 ppt_build.py 中的幻灯片代码块"""
import re, io

path = r"E:\StudyReminder\docs\ppt_build.py"
text = open(path, encoding="utf-8").read()

pattern = re.compile(r"(?m)^# ={20,}\n# (.+?)\n# ={20,}\n")
parts = pattern.split(text)
head = parts[0]
blocks = {}
order = []
for i in range(1, len(parts), 2):
    title = parts[i]
    body = parts[i + 1]
    blocks[title] = body
    order.append(title)

print("OLD ORDER:", order)

new_order = [
    "01 封面",
    "02 问题背景",
    "03 解决方案",
    "06 使用流程",
    "08.5 界面展示",
    "04 核心功能",
    "07 创新亮点",
    "08 竞品对比",
    "05 技术架构",
    "09 应用价值与市场",
    "10 推广与可持续",
    "11 路线图",
    "12 团队",
    "13 结束页",
]
assert set(new_order) == set(order), (set(new_order) ^ set(order))

# 调整编号 / 页码
def fix(body, title):
    if title == "06 使用流程":
        body = body.replace('header(s, "05", "使用流程"', 'header(s, "03", "使用流程"')
        body = body.replace("footer(s, 6)", "footer(s, 4)")
    elif title == "08.5 界面展示":
        body = body.replace('header(s, "08", "界面展示"', 'header(s, "04", "界面展示"')
        body = body.replace("footer(s, 9)", "footer(s, 5)")
    elif title == "04 核心功能":
        body = body.replace('header(s, "03", "核心功能"', 'header(s, "05", "核心功能"')
        body = body.replace("footer(s, 4)", "footer(s, 6)")
    elif title == "05 技术架构":
        body = body.replace('header(s, "04", "技术架构"', 'header(s, "08", "技术架构"')
        body = body.replace("footer(s, 5)", "footer(s, 9)")
    return body

out = [head]
for t in new_order:
    out.append("# " + "=" * 60 + "\n# " + t + "\n# " + "=" * 60 + "\n")
    out.append(fix(blocks[t], t))

open(path, "w", encoding="utf-8").write("".join(out))
print("NEW ORDER:", new_order)
