#!/usr/bin/env python3
"""Draw the XMB icon (ICON0.PNG, 144x80): an original underwater scene with a submarine."""
import sys

from PIL import Image, ImageDraw, ImageFilter


def main():
    out = sys.argv[1]
    w, h = 144, 80
    img = Image.new('RGB', (w, h))
    px = img.load()
    for y in range(h):
        t = y / (h - 1)
        r, g, b = int(20 * (1 - t)), int(120 * (1 - t) + 20 * t), int(170 * (1 - t) + 40 * t)
        for x in range(w):
            px[x, y] = (r, g, b)
    d = ImageDraw.Draw(img, 'RGBA')
    # light shafts from the surface
    for x0 in (20, 55, 95, 125):
        d.polygon([(x0, 0), (x0 + 10, 0), (x0 + 30, h), (x0 + 14, h)], fill=(180, 230, 255, 28))
    # submarine hull, tower and propeller
    d.ellipse((34, 36, 112, 58), fill=(214, 168, 44, 255), outline=(90, 60, 10, 255))
    d.rounded_rectangle((62, 26, 82, 40), radius=4, fill=(200, 150, 36, 255), outline=(90, 60, 10, 255))
    d.line((72, 26, 72, 18), fill=(90, 60, 10, 255), width=2)
    d.polygon([(34, 47), (24, 39), (24, 55)], fill=(150, 110, 30, 255))
    for i, cx in enumerate((52, 70, 88)):
        d.ellipse((cx - 4, 43, cx + 4, 51), fill=(120, 220, 255, 255), outline=(40, 60, 70, 255))
    # headlight beam and bubbles
    d.polygon([(112, 47), (144, 36), (144, 60)], fill=(255, 255, 210, 50))
    for (bx, by, br) in ((20, 30, 3), (16, 22, 2), (24, 14, 2), (120, 20, 2), (128, 12, 3)):
        d.ellipse((bx - br, by - br, bx + br, by + br), outline=(220, 245, 255, 200))
    img = img.filter(ImageFilter.SMOOTH)
    img.save(out)


if __name__ == '__main__':
    main()
