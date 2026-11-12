# -*- coding: utf-8 -*-
"""用小黄鸭头像生成 Android 启动图标（自适应 / 圆形 / 商店图标）"""
from PIL import Image, ImageDraw, ImageFilter, ImageEnhance
import numpy as np
import os

SRC = r"E:\StudyReminder\docs\icon_src\uploaded_0.gif"
RES = r"E:\StudyReminder\app\src\main\res"
DEBUG = r"E:\StudyReminder\docs\icon_src"
BG = (255, 247, 232)  # 奶油色

# 1) 取第一帧，裁到鸭鸭外接框
gif = Image.open(SRC)
gif.seek(0)
duck = gif.convert("RGBA")
arr = np.array(duck)
ys, xs = np.where(arr[:, :, 3] > 8)
pad = 3
box = (max(xs.min() - pad, 0), max(ys.min() - pad, 0),
       min(xs.max() + pad + 1, duck.width), min(ys.max() + pad + 1, duck.height))
duck = duck.crop(box)
# 放大并轻微锐化，减小 GIF 放大后的模糊
duck = duck.resize((1024, int(1024 * duck.height / duck.width)), Image.LANCZOS)
duck = duck.filter(ImageFilter.UnsharpMask(radius=1.2, percent=90, threshold=2))
duck.save(os.path.join(DEBUG, "duck_icon_source.png"))

def compose(size, ratio, circle=False, shadow=True):
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(canvas)
    if circle:
        d.ellipse([0, 0, size - 1, size - 1], fill=BG + (255,))
    else:
        canvas.paste(Image.new("RGBA", (size, size), BG + (255,)), (0, 0))

    target = int(size * ratio)
    b = duck.copy()
    b.thumbnail((target, target), Image.LANCZOS)
    px = (size - b.width) // 2
    py = (size - b.height) // 2

    if shadow:
        alpha = b.split()[3].point(lambda v: int(v * 0.22))
        sh = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        sh.paste(Image.new("RGBA", b.size, (0, 0, 0, 255)), (px, py + int(size * 0.015)), alpha)
        sh = sh.filter(ImageFilter.GaussianBlur(max(2, size // 90)))
        canvas.alpha_composite(sh)

    canvas.alpha_composite(b, (px, py))
    return canvas

# 2) 自适应图标前景：512，鸭鸭占 68%（安全区）
compose(512, 0.68, shadow=True).save(
    os.path.join(RES, "drawable-nodpi", "ic_launcher_foreground.png"), "PNG")

# 3) 传统 mipmap：方形 + 圆形
densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for name, size in densities.items():
    compose(size, 0.86, shadow=True).convert("RGB").save(
        os.path.join(RES, f"mipmap-{name}", "ic_launcher.webp"),
        "WEBP", lossless=True, quality=100)
    compose(size, 0.78, circle=True, shadow=True).save(
        os.path.join(RES, f"mipmap-{name}", "ic_launcher_round.webp"),
        "WEBP", lossless=True, quality=100)

# 4) 商店图标 512
compose(512, 0.86, shadow=True).convert("RGB").save(
    os.path.join(RES, "..", "ic_launcher-playstore.png"), "PNG")

# 预览
compose(512, 0.86, shadow=True).convert("RGB").save(os.path.join(DEBUG, "duck_icon_preview_square.png"))
compose(512, 0.78, circle=True, shadow=True).convert("RGB").save(os.path.join(DEBUG, "duck_icon_preview_round.png"))
print("done")
