# -*- coding: utf-8 -*-
"""用西电校徽生成 Android 启动图标（自适应 / 圆形 / 商店图标）"""
from PIL import Image, ImageDraw
import numpy as np
from collections import deque
import os

SRC = r"E:\StudyReminder\docs\icon_src\badge1.png"
RES = r"E:\StudyReminder\app\src\main\res"
OUT_DEBUG = r"E:\StudyReminder\docs\icon_src"

img = Image.open(SRC).convert("RGB")
w, h = img.size

# 1) 去掉左下角的水印
d = ImageDraw.Draw(img)
d.rectangle([0, int(h * 0.895), int(w * 0.30), h], fill=(255, 255, 255))

# 2) 找到徽标外接框并裁成正方形
a = np.array(img)
mask = (a.min(axis=2) < 238) | ((a.max(axis=2).astype(int) - a.min(axis=2).astype(int)) > 14)
ys, xs = np.where(mask)
top, bottom, left, right = ys.min(), ys.max(), xs.min(), xs.max()
cw, ch = right - left + 1, bottom - top + 1
side = int(max(cw, ch) * 1.04)
cx, cy = (left + right) // 2, (top + bottom) // 2
box = (cx - side // 2, cy - side // 2, cx + side // 2, cy + side // 2)
crop = img.crop(box).resize((1024, 1024), Image.LANCZOS)
crop.save(os.path.join(OUT_DEBUG, "badge_square.png"))

# 3) 把外围白底变透明（保留徽标内部的白色）
rgba = crop.convert("RGBA")
arr = np.array(rgba)
rgb = arr[:, :, :3].astype(np.float32)
dist = np.sqrt(((255.0 - rgb) ** 2).sum(axis=2))
light = dist < 30
visited = np.zeros(light.shape, dtype=bool)
q = deque()
for x in range(1024):
    for y in (0, 1023):
        if light[y, x]:
            visited[y, x] = True; q.append((y, x))
for y in range(1024):
    for x in (0, 1023):
        if light[y, x] and not visited[y, x]:
            visited[y, x] = True; q.append((y, x))
while q:
    y, x = q.popleft()
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        ny, nx = y + dy, x + dx
        if 0 <= ny < 1024 and 0 <= nx < 1024 and light[ny, nx] and not visited[ny, nx]:
            visited[ny, nx] = True; q.append((ny, nx))
arr[:, :, 3] = np.where(visited, 0, arr[:, :, 3])
badge = Image.fromarray(arr)
badge.save(os.path.join(OUT_DEBUG, "badge_transparent.png"))

def compose(size, ratio, bg=(255, 255, 255), circle=False):
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(canvas)
    if circle:
        d.ellipse([0, 0, size - 1, size - 1], fill=bg + (255,))
    elif bg is not None:
        canvas.paste(Image.new("RGBA", (size, size), bg + (255,)), (0, 0))
    target = int(size * ratio)
    b = badge.copy()
    b.thumbnail((target, target), Image.LANCZOS)
    canvas.alpha_composite(b, ((size - b.width) // 2, (size - b.height) // 2))
    return canvas

# 4) 自适应图标前景：512，徽标占 64%（安全区）
fg = compose(512, 0.64, bg=None)
fg.save(os.path.join(RES, "drawable-nodpi", "ic_launcher_foreground.png"), "PNG")

# 5) 传统 mipmap：方形 + 圆形
densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for name, size in densities.items():
    compose(size, 0.84).convert("RGB").save(
        os.path.join(RES, f"mipmap-{name}", "ic_launcher.webp"), "WEBP", lossless=True, quality=100)
    compose(size, 0.76, circle=True).save(
        os.path.join(RES, f"mipmap-{name}", "ic_launcher_round.webp"), "WEBP", lossless=True, quality=100)

# 6) 商店图标 512
compose(512, 0.84).convert("RGB").save(
    os.path.join(RES, "..", "ic_launcher-playstore.png"), "PNG")

# 预览
compose(512, 0.84).convert("RGB").save(os.path.join(OUT_DEBUG, "icon_preview_square.png"))
compose(512, 0.76, circle=True).convert("RGB").save(os.path.join(OUT_DEBUG, "icon_preview_round.png"))
print("done")
