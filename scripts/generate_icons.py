#!/usr/bin/env python3
"""
Generate launcher icon PNGs for VideoHub Pro.
Creates ic_launcher.png and ic_launcher_round.png for all 5 densities.
"""
import os
from PIL import Image, ImageDraw

DENSITIES = {
    'mipmap-mdpi':    48,
    'mipmap-hdpi':    72,
    'mipmap-xhdpi':   96,
    'mipmap-xxhdpi':  144,
    'mipmap-xxxhdpi': 192,
}

BASE_DIR = '/home/z/my-project/android/app/src/main/res'

BG_COLOR = (255, 179, 0, 255)
FG_COLOR = (255, 255, 255, 255)

def create_icon(size, rounded=False):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    if rounded:
        draw.ellipse([0, 0, size-1, size-1], fill=BG_COLOR)
    else:
        radius = size // 6
        draw.rounded_rectangle([0, 0, size-1, size-1], radius=radius, fill=BG_COLOR)

    cx, cy = size // 2, size // 2
    s = size // 3

    bolt = [
        (cx + s//4, cy - s),
        (cx - s//2, cy + s//8),
        (cx, cy + s//8),
        (cx - s//4, cy + s),
        (cx + s//2, cy - s//8),
        (cx, cy - s//8),
    ]

    draw.polygon(bolt, fill=FG_COLOR)
    return img


def create_foreground(size):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    cx, cy = size // 2, size // 2
    s = size // 3

    bolt = [
        (cx + s//4, cy - s),
        (cx - s//2, cy + s//8),
        (cx, cy + s//8),
        (cx - s//4, cy + s),
        (cx + s//2, cy - s//8),
        (cx, cy - s//8),
    ]

    draw.polygon(bolt, fill=BG_COLOR)
    return img


for density, size in DENSITIES.items():
    dir_path = os.path.join(BASE_DIR, density)
    os.makedirs(dir_path, exist_ok=True)

    icon = create_icon(size, rounded=False)
    icon.save(os.path.join(dir_path, 'ic_launcher.png'))

    icon_round = create_icon(size, rounded=True)
    icon_round.save(os.path.join(dir_path, 'ic_launcher_round.png'))

    fg = create_foreground(size * 2)
    fg.save(os.path.join(dir_path, 'ic_launcher_foreground.png'))

    print(f'Generated {density}: {size}x{size}')

print('Done! Generated 15 PNG icons.')
