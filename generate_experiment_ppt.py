from __future__ import annotations

import math
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from PIL import Image
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_AUTO_SHAPE_TYPE
from pptx.enum.text import PP_ALIGN
from pptx.util import Cm, Pt


BASE_DIR = Path(__file__).resolve().parent
ASSET_DIR = BASE_DIR / "ppt_assets"
ASSET_DIR.mkdir(exist_ok=True)

REQ_1 = BASE_DIR / "req_1.jpg"
REQ_2 = BASE_DIR / "req_2.jpg"
COAXIAL_RAW = BASE_DIR / "coaxial_data.jpg"
PARALLEL_RAW = BASE_DIR / "parallel_data.jpg"
OUTPUT_PPT = BASE_DIR / "电流场模拟静电场实验报告.pptx"

plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei", "Noto Sans CJK SC"]
plt.rcParams["axes.unicode_minus"] = False


COAXIAL_VOLTAGES = np.array([7, 6, 5, 4, 3, 2, 1], dtype=float)
# 依据用户极坐标原始图按“每圈 2 mm”读数并取均值得到的半径
COAXIAL_RADII = np.array([2.48, 2.88, 3.26, 3.68, 4.22, 4.80, 5.58], dtype=float)
UA = 10.0
INNER_RADIUS = 1.0
OUTER_RADIUS = 6.5

# 从原始截图提取出的 60 个实验点中心
PARALLEL_POINTS_PX = np.array(
    [
        [781.47, 875.71],
        [216.00, 876.35],
        [604.13, 877.28],
        [994.55, 879.69],
        [451.74, 891.53],
        [299.77, 954.47],
        [612.34, 956.00],
        [765.49, 956.56],
        [933.25, 957.93],
        [479.79, 973.74],
        [368.55, 1034.90],
        [613.06, 1035.95],
        [742.97, 1037.02],
        [880.19, 1037.08],
        [140.72, 1047.66],
        [71.65, 1052.17],
        [218.44, 1059.16],
        [504.33, 1070.43],
        [843.01, 1114.88],
        [415.22, 1116.34],
        [732.73, 1117.67],
        [313.60, 1118.22],
        [621.07, 1118.78],
        [515.01, 1148.40],
        [723.27, 1211.48],
        [627.85, 1214.34],
        [830.19, 1215.37],
        [438.12, 1222.85],
        [372.13, 1225.26],
        [525.54, 1228.80],
        [731.65, 1274.59],
        [833.22, 1275.93],
        [626.71, 1277.01],
        [352.87, 1314.34],
        [740.00, 1357.59],
        [846.03, 1357.78],
        [628.94, 1358.25],
        [323.25, 1358.88],
        [418.41, 1359.27],
        [525.44, 1373.63],
        [269.08, 1388.25],
        [86.76, 1421.38],
        [161.16, 1430.70],
        [753.14, 1433.64],
        [627.84, 1435.18],
        [374.42, 1436.17],
        [884.77, 1441.55],
        [513.52, 1450.71],
        [309.44, 1516.01],
        [770.08, 1516.52],
        [626.47, 1518.59],
        [924.00, 1519.60],
        [496.02, 1527.82],
        [619.23, 1594.58],
        [218.07, 1596.48],
        [788.87, 1597.12],
        [475.36, 1597.30],
        [991.09, 1599.29],
        [455.70, 1659.35],
        [110.13, 1678.37],
    ],
    dtype=float,
)


def rgb(hex_color: str) -> RGBColor:
    hex_color = hex_color.lstrip("#")
    return RGBColor(int(hex_color[0:2], 16), int(hex_color[2:4], 16), int(hex_color[4:6], 16))


def set_cell_text(cell, text: str, size: int = 12, bold: bool = False, color: str = "1F2937") -> None:
    cell.text = text
    p = cell.text_frame.paragraphs[0]
    p.alignment = PP_ALIGN.CENTER
    run = p.runs[0]
    run.font.name = "Microsoft YaHei"
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = rgb(color)


def add_textbox(slide, left, top, width, height, text, size=18, bold=False, color="1F2937", align=PP_ALIGN.LEFT):
    box = slide.shapes.add_textbox(left, top, width, height)
    tf = box.text_frame
    tf.word_wrap = True
    tf.clear()
    for idx, line in enumerate(text.split("\n")):
        p = tf.paragraphs[0] if idx == 0 else tf.add_paragraph()
        p.text = line
        p.alignment = align
        p.space_after = Pt(4)
        for run in p.runs:
            run.font.name = "Microsoft YaHei"
            run.font.size = Pt(size)
            run.font.bold = bold
            run.font.color.rgb = rgb(color)
    return box


def add_title(slide, title: str, subtitle: str | None = None) -> None:
    add_textbox(slide, Cm(0.9), Cm(0.55), Cm(16), Cm(1.2), title, size=25, bold=True, color="12344D")
    if subtitle:
        add_textbox(slide, Cm(0.95), Cm(1.55), Cm(17), Cm(0.7), subtitle, size=10, color="4B6476")


def add_background(slide, accent="DCEFF7", light="F8FBFD") -> None:
    slide.background.fill.solid()
    slide.background.fill.fore_color.rgb = rgb(light)
    band = slide.shapes.add_shape(MSO_AUTO_SHAPE_TYPE.RECTANGLE, 0, 0, prs.slide_width, Cm(1.0))
    band.fill.solid()
    band.fill.fore_color.rgb = rgb(accent)
    band.line.fill.background()
    deco = slide.shapes.add_shape(MSO_AUTO_SHAPE_TYPE.ROUNDED_RECTANGLE, prs.slide_width - Cm(4.2), Cm(0.3), Cm(3.2), Cm(0.55))
    deco.fill.solid()
    deco.fill.fore_color.rgb = rgb("BFE3F2")
    deco.line.fill.background()


def crop_images() -> dict[str, Path]:
    coaxial_img = Image.open(COAXIAL_RAW)
    parallel_img = Image.open(PARALLEL_RAW)

    coaxial_crop = coaxial_img.crop((260, 120, 2900, 2920))
    coaxial_crop_path = ASSET_DIR / "coaxial_raw_crop.png"
    coaxial_crop.save(coaxial_crop_path)

    parallel_crop = parallel_img.crop((0, 620, 1150, 2050))
    parallel_crop_path = ASSET_DIR / "parallel_raw_crop.png"
    parallel_crop.save(parallel_crop_path)

    req1_crop_path = ASSET_DIR / "req_1.png"
    req2_crop_path = ASSET_DIR / "req_2.png"
    Image.open(REQ_1).save(req1_crop_path)
    Image.open(REQ_2).save(req2_crop_path)

    return {
        "coaxial_raw": coaxial_crop_path,
        "parallel_raw": parallel_crop_path,
        "req1": req1_crop_path,
        "req2": req2_crop_path,
    }


def generate_coaxial_field_figure(path: Path) -> None:
    fig, ax = plt.subplots(figsize=(6, 6), dpi=180)
    ax.set_aspect("equal")
    ax.set_facecolor("#FCFEFF")

    experimental = COAXIAL_RADII[::-1]
    colors = ["#0EA5E9", "#38BDF8", "#60A5FA", "#93C5FD", "#F59E0B", "#FB7185", "#EF4444"]
    for idx, (radius, color) in enumerate(zip(experimental, colors), start=1):
        circle = plt.Circle((0, 0), radius, fill=False, linewidth=2.0, color=color, alpha=0.9)
        ax.add_patch(circle)
        ax.text(radius + 0.08, 0.06, f"{idx}V", color=color, fontsize=8)

    inner = plt.Circle((0, 0), INNER_RADIUS, color="#D6EAF8", ec="#1F5F8B", linewidth=1.6)
    ax.add_patch(inner)
    ax.text(0, 0, "内导体", ha="center", va="center", fontsize=9, color="#174A6B", weight="bold")

    for ang in np.linspace(0, 2 * np.pi, 8, endpoint=False):
        ax.annotate(
            "",
            xy=(5.7 * math.cos(ang), 5.7 * math.sin(ang)),
            xytext=(1.25 * math.cos(ang), 1.25 * math.sin(ang)),
            arrowprops=dict(arrowstyle="->", lw=1.2, color="#F97316", alpha=0.9),
        )

    ax.set_xlim(-6.2, 6.2)
    ax.set_ylim(-6.2, 6.2)
    ax.grid(True, color="#D6E4EC", linestyle="--", linewidth=0.6)
    ax.set_xlabel("x / cm")
    ax.set_ylabel("y / cm")
    ax.set_title("同轴电缆模型等势线与电场线示意", fontsize=12)
    fig.tight_layout()
    fig.savefig(path, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def generate_coaxial_compare_figure(path: Path) -> None:
    u_rel = COAXIAL_VOLTAGES / UA
    ln_r_exp = np.log(COAXIAL_RADII)
    r_theory = OUTER_RADIUS * (INNER_RADIUS / OUTER_RADIUS) ** u_rel
    ln_r_theory = np.log(r_theory)

    fig, ax = plt.subplots(figsize=(6.3, 4.1), dpi=180)
    ax.plot(u_rel, ln_r_exp, "o-", color="#2563EB", linewidth=2, label="实验读数")
    ax.plot(u_rel, ln_r_theory, "s--", color="#EF4444", linewidth=1.8, label="理论曲线")
    ax.set_xlabel("相对电位 U'/Ua")
    ax.set_ylabel("ln r")
    ax.set_title("同轴电缆模型相对电位与 ln r 的关系", fontsize=12)
    ax.grid(True, color="#D7E3EA", linewidth=0.7, alpha=0.9)
    ax.legend(frameon=False, fontsize=9)
    fig.tight_layout()
    fig.savefig(path, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def generate_parallel_clean_figure(path: Path) -> None:
    x_step = 76.94
    y_step = 79.60
    x0 = 37.70
    y_max = 1678.37

    xs = (PARALLEL_POINTS_PX[:, 0] - x0) / x_step
    ys = (y_max - PARALLEL_POINTS_PX[:, 1]) / y_step

    fig, ax = plt.subplots(figsize=(7.2, 5.2), dpi=180)
    ax.set_facecolor("#FCFCFD")

    for x in np.arange(0, 14.51, 0.5):
        lw = 0.6 if abs(x - round(x)) < 1e-6 else 0.3
        color = "#BFC8D2" if lw > 0.4 else "#E5E7EB"
        ax.axvline(x, color=color, linewidth=lw, zorder=0)
    for y in np.arange(0, 10.51, 0.5):
        lw = 0.6 if abs(y - round(y)) < 1e-6 else 0.3
        color = "#BFC8D2" if lw > 0.4 else "#E5E7EB"
        ax.axhline(y, color=color, linewidth=lw, zorder=0)

    ax.scatter(xs, ys, s=34, color="#1D9BF0", edgecolors="white", linewidths=0.7, zorder=4, label="实验点")
    ax.scatter([3.5], [5.8], s=280, color="#FDE68A", edgecolors="#CA8A04", linewidths=1.8, zorder=5)
    ax.scatter([12.4], [5.8], s=280, color="#E5E7EB", edgecolors="#6B7280", linewidths=1.8, zorder=5)
    ax.text(3.5, 6.2, "+", ha="center", va="bottom", fontsize=15, color="#EF4444", weight="bold")
    ax.text(12.4, 6.2, "-", ha="center", va="bottom", fontsize=15, color="#111827", weight="bold")

    arrow_y = [4.2, 5.0, 5.8, 6.6, 7.4]
    for yy in arrow_y:
        ax.annotate("", xy=(11.4, yy), xytext=(4.5, yy), arrowprops=dict(arrowstyle="->", color="#F97316", lw=1.0, alpha=0.7))

    ax.set_xlim(0, 14)
    ax.set_ylim(0, 10.2)
    ax.set_xlabel("x / 绘图板坐标")
    ax.set_ylabel("y / 绘图板坐标")
    ax.set_title("平行线电极模型实验点清洗结果（60 点）", fontsize=12)
    ax.legend(frameon=False, fontsize=9, loc="upper right")
    fig.tight_layout()
    fig.savefig(path, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def add_bullet_block(slide, left, top, width, height, title, bullets, title_color="12344D") -> None:
    shape = slide.shapes.add_shape(MSO_AUTO_SHAPE_TYPE.ROUNDED_RECTANGLE, left, top, width, height)
    shape.fill.solid()
    shape.fill.fore_color.rgb = rgb("FFFFFF")
    shape.line.color.rgb = rgb("D5E4EC")
    add_textbox(slide, left + Cm(0.35), top + Cm(0.2), width - Cm(0.7), Cm(0.65), title, size=14, bold=True, color=title_color)

    box = slide.shapes.add_textbox(left + Cm(0.35), top + Cm(0.95), width - Cm(0.7), height - Cm(1.2))
    tf = box.text_frame
    tf.word_wrap = True
    tf.clear()
    for idx, bullet in enumerate(bullets):
        p = tf.paragraphs[0] if idx == 0 else tf.add_paragraph()
        p.text = f"• {bullet}"
        p.alignment = PP_ALIGN.LEFT
        p.space_after = Pt(4)
        for run in p.runs:
            run.font.name = "Microsoft YaHei"
            run.font.size = Pt(11)
            run.font.color.rgb = rgb("31475A")


def build_presentation(image_paths: dict[str, Path]) -> None:
    global prs
    prs = Presentation()
    prs.slide_width = Cm(33.867)
    prs.slide_height = Cm(19.05)

    coaxial_field_path = ASSET_DIR / "coaxial_field.png"
    coaxial_compare_path = ASSET_DIR / "coaxial_compare.png"
    parallel_clean_path = ASSET_DIR / "parallel_clean.png"
    generate_coaxial_field_figure(coaxial_field_path)
    generate_coaxial_compare_figure(coaxial_compare_path)
    generate_parallel_clean_figure(parallel_clean_path)

    u_rel = COAXIAL_VOLTAGES / UA
    ln_r = np.log(COAXIAL_RADII)
    r_theory = OUTER_RADIUS * (INNER_RADIUS / OUTER_RADIUS) ** u_rel

    # Slide 1
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_background(slide, accent="D8EDF6", light="F7FBFD")
    hero = slide.shapes.add_shape(MSO_AUTO_SHAPE_TYPE.ROUNDED_RECTANGLE, Cm(1.1), Cm(2.0), Cm(31.2), Cm(14.9))
    hero.fill.solid()
    hero.fill.fore_color.rgb = rgb("FFFFFF")
    hero.line.color.rgb = rgb("D5E8F0")
    add_textbox(slide, Cm(2.0), Cm(3.2), Cm(19), Cm(1.6), "电流场模拟静电场实验", size=28, bold=True, color="0F3554")
    add_textbox(slide, Cm(2.0), Cm(4.8), Cm(20), Cm(1.1), "实验报告展示稿", size=20, bold=True, color="1C6D8C")
    add_textbox(
        slide,
        Cm(2.0),
        Cm(6.2),
        Cm(17.5),
        Cm(3.6),
        "内容覆盖：实验要求、数据处理、同轴电缆模型结果、平行线电极模型结果、误差分析与问题讨论",
        size=14,
        color="35586E",
    )
    slide.shapes.add_picture(str(coaxial_field_path), Cm(20.2), Cm(3.2), height=Cm(6.4))
    slide.shapes.add_picture(str(parallel_clean_path), Cm(18.5), Cm(9.8), height=Cm(5.7))
    add_textbox(slide, Cm(2.0), Cm(14.2), Cm(15), Cm(0.7), "依据用户原始实验数据生成", size=11, color="5B7487")

    # Slide 2
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_background(slide)
    add_title(slide, "实验要求与任务拆解", "依据教材要求整理")
    add_bullet_block(
        slide,
        Cm(1.0),
        Cm(2.4),
        Cm(12.3),
        Cm(6.0),
        "同轴电缆模型",
        [
            "电源调至 10 V，测量 1 V 到 7 V 的等势点。",
            "每条等势线按相邻电位差 1 V 作图，并以平均半径表示。",
            "依据电场线与等势线正交原则绘制电场线，给出场强方向。",
            "作出 U'/Ua 与 ln r 的关系曲线，并与理论结果比较。",
        ],
    )
    add_bullet_block(
        slide,
        Cm(1.0),
        Cm(9.0),
        Cm(12.3),
        Cm(6.0),
        "平行线电极模型",
        [
            "从 3 V 开始每隔 1 V 测一条等势线，共 3 V 到 8 V 六条。",
            "每条等势线记录 10 个以上的点，再据此绘制场线。",
            "分析电场线方向、疏密变化以及边缘效应。",
        ],
    )
    slide.shapes.add_picture(str(image_paths["req1"]), Cm(14.1), Cm(2.5), width=Cm(8.8))
    slide.shapes.add_picture(str(image_paths["req2"]), Cm(23.2), Cm(2.5), width=Cm(9.2))

    # Slide 3
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_background(slide, accent="DDEFF2")
    add_title(slide, "实验原理与处理方法", "稳恒电流场与静电场满足同型的拉普拉斯方程")
    add_bullet_block(
        slide,
        Cm(1.0),
        Cm(2.4),
        Cm(10.2),
        Cm(5.9),
        "核心原理",
        [
            "均匀导电介质中的稳恒电流场与静电场满足相同边界条件时，可用来模拟静电场。",
            "等势线与电场线处处正交，电场线总是从高电位指向低电位。",
            "同轴结构理论上应得到一族同心圆等势线，平行线电极附近则呈弯曲分布。",
        ],
    )
    add_bullet_block(
        slide,
        Cm(1.0),
        Cm(9.0),
        Cm(10.2),
        Cm(5.8),
        "本次 PPT 的数据处理",
        [
            "同轴电缆：由原始极坐标纸读出七组平均半径，换算 ln r 并与理论值对比。",
            "平行线电极：从绘图板截图中识别 60 个实验点，做清洗后的坐标展示。",
            "最终保留原始数据图，同时给出便于汇报展示的整理图。",
        ],
    )
    slide.shapes.add_picture(str(image_paths["coaxial_raw"]), Cm(12.1), Cm(2.5), width=Cm(9.3))
    slide.shapes.add_picture(str(image_paths["parallel_raw"]), Cm(21.9), Cm(2.5), width=Cm(10.2))

    # Slide 4
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_background(slide, accent="DBEEF8")
    add_title(slide, "同轴电缆模型数据处理", "由原始极坐标图读出平均半径后进行理论比较")
    slide.shapes.add_picture(str(image_paths["coaxial_raw"]), Cm(1.0), Cm(2.4), width=Cm(11.2))

    table = slide.shapes.add_table(4, 8, Cm(12.8), Cm(2.7), Cm(19.2), Cm(5.0)).table
    headers = ["U'/V", "7.0", "6.0", "5.0", "4.0", "3.0", "2.0", "1.0"]
    for j, text in enumerate(headers):
        set_cell_text(table.cell(0, j), text, size=11, bold=True, color="0F3554")
        table.cell(0, j).fill.solid()
        table.cell(0, j).fill.fore_color.rgb = rgb("E8F4FA")
    set_cell_text(table.cell(1, 0), "r / cm", size=11, bold=True)
    set_cell_text(table.cell(2, 0), "U'/Ua", size=11, bold=True)
    set_cell_text(table.cell(3, 0), "ln r", size=11, bold=True)

    for idx, radius in enumerate(COAXIAL_RADII, start=1):
        set_cell_text(table.cell(1, idx), f"{radius:.2f}", size=11)
        set_cell_text(table.cell(2, idx), f"{u_rel[idx-1]:.2f}", size=11)
        set_cell_text(table.cell(3, idx), f"{ln_r[idx-1]:.3f}", size=11)

    add_bullet_block(
        slide,
        Cm(12.8),
        Cm(8.4),
        Cm(19.2),
        Cm(6.1),
        "读数结论",
        [
            "半径随电位降低而增大，说明等势线由内向外逐渐展开。",
            "实验结果与理论规律一致：ln r 与相对电位近似线性相关。",
            "偏差主要来自人工读点、极坐标纸视差、边缘区域电场畸变等因素。",
        ],
    )

    # Slide 5
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_background(slide, accent="E5F4FB")
    add_title(slide, "同轴电缆模型结果展示", "实验等势线、电场线示意以及理论对比")
    slide.shapes.add_picture(str(coaxial_field_path), Cm(1.0), Cm(2.5), width=Cm(14.5))
    slide.shapes.add_picture(str(coaxial_compare_path), Cm(16.2), Cm(2.8), width=Cm(16.0))
    add_bullet_block(
        slide,
        Cm(16.4),
        Cm(11.4),
        Cm(15.6),
        Cm(3.8),
        "理论值参考",
        [
            "理论半径 r_th = R·(a/R)^(U'/Ua)，取 a = 1 cm，R = 6.5 cm。",
            "本组实验读数整体略高于理论值，但线性趋势保持良好。",
        ],
    )

    # Slide 6
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_background(slide, accent="DDF2EC", light="F7FCFA")
    add_title(slide, "平行线电极模型结果展示", "原始截图与清洗后的 60 个实验点")
    slide.shapes.add_picture(str(image_paths["parallel_raw"]), Cm(1.0), Cm(2.6), width=Cm(10.0))
    slide.shapes.add_picture(str(parallel_clean_path), Cm(11.7), Cm(2.5), width=Cm(20.0))
    add_bullet_block(
        slide,
        Cm(1.0),
        Cm(12.6),
        Cm(31.0),
        Cm(2.8),
        "结果分析",
        [
            "实验点在两电极之间分布更密集，说明该区域电场更强；远离电极后点列逐渐展开，体现边缘效应。",
            "点集关于中轴近似对称，符合平行线电极模型的预期分布特征。",
        ],
        title_color="175D49",
    )

    # Slide 7
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_background(slide, accent="F3E9D7", light="FCFAF6")
    add_title(slide, "问题讨论与结论", "结合实验要求给出简要回答")
    add_bullet_block(
        slide,
        Cm(1.0),
        Cm(2.5),
        Cm(14.9),
        Cm(5.0),
        "问题 1",
        [
            "若电源电压 Ua 增加一倍，等势线与电力线的形状基本不变，因为边界几何形状没有改变。",
            "各点电位与电场强度会按比例增大，归一化后的电位分布 U/Ua 基本不变。",
        ],
        title_color="8B5E10",
    )
    add_bullet_block(
        slide,
        Cm(16.0),
        Cm(2.5),
        Cm(15.0),
        Cm(5.0),
        "问题 2",
        [
            "导电介质电导率越均匀，测量结果越稳定。",
            "若导电率偏低或分布不均，会导致电压分布畸变、探针读数波动增大，从而影响等势线精度。",
        ],
        title_color="8B5E10",
    )
    add_bullet_block(
        slide,
        Cm(1.0),
        Cm(8.1),
        Cm(14.9),
        Cm(5.2),
        "问题 3",
        [
            "导电介质应均匀、各向同性并满足稳恒条件。",
            "模型的边界形状与电极配置需与待模拟静电场一致。",
            "只有在满足相同边界条件并服从拉普拉斯方程时，电流场才能正确模拟静电场。",
        ],
        title_color="8B5E10",
    )
    add_bullet_block(
        slide,
        Cm(16.0),
        Cm(8.1),
        Cm(15.0),
        Cm(5.2),
        "总结",
        [
            "同轴电缆模型得到近似同心圆等势线，实验趋势与理论模型一致。",
            "平行线电极模型表现出明显的中部强场和边缘弯曲特征。",
            "本次 PPT 已将用户原始数据替换进展示内容，可直接继续微调封面信息后提交。",
        ],
        title_color="8B5E10",
    )

    prs.save(OUTPUT_PPT)


if __name__ == "__main__":
    image_paths = crop_images()
    build_presentation(image_paths)
    print(f"Generated: {OUTPUT_PPT}")
