#!/usr/bin/env python3
"""Draw the XMB icon (ICON0.PNG, 144x80): the ABISAL escape pod at the surface, seen half
above and half below the water, with the title. Original artwork generated from code."""
import sys

from PIL import Image, ImageDraw


def main():
    out = sys.argv[1]
    w, h = 144, 80
    img = Image.new('RGB', (w, h))
    px = img.load()
    agua = 30
    for y in range(h):
        for x in range(w):
            if y < agua:
                t = y / agua
                px[x, y] = (int(120 + 60 * t), int(190 + 30 * t), int(230 + 10 * t))
            else:
                t = (y - agua) / (h - agua)
                px[x, y] = (int(30 * (1 - t) + 4 * t), int(140 * (1 - t) + 30 * t), int(170 * (1 - t) + 55 * t))
    d = ImageDraw.Draw(img, 'RGBA')
    # light shafts under water
    for x0 in (14, 50, 92, 126):
        d.polygon([(x0, agua), (x0 + 8, agua), (x0 + 24, h), (x0 + 10, h)], fill=(190, 240, 255, 26))
    # pod: white dome, teal band, grey keel, coral pontoons and mast light
    cx = 52
    d.ellipse((cx - 22, agua - 14, cx + 22, agua + 30), fill=(232, 236, 239, 255), outline=(60, 80, 90, 255))
    d.rectangle((cx - 22, agua + 2, cx + 22, agua + 12), fill=(31, 163, 168, 255))
    d.chord((cx - 22, agua - 14, cx + 22, agua + 30), 30, 150, fill=(138, 149, 156, 255))
    for wx in (cx - 12, cx + 4):
        d.rectangle((wx, agua + 4, wx + 8, agua + 9), fill=(29, 42, 51, 255))
    d.line((cx + 9, agua - 12, cx + 9, agua - 26), fill=(138, 149, 156, 255), width=2)
    d.ellipse((cx + 6, agua - 30, cx + 12, agua - 24), fill=(224, 83, 61, 255))
    for ox in (-32, 26):
        d.rounded_rectangle((cx + ox, agua - 3, cx + ox + 12, agua + 5), radius=4, fill=(224, 83, 61, 255))
    # water line over the pod
    d.rectangle((0, agua, w, agua + 1), fill=(200, 245, 255, 120))
    # bubbles and a glowing fish
    for bx, by, r in ((96, 60, 2), (100, 50, 1.5), (93, 44, 1)):
        d.ellipse((bx - r, by - r, bx + r, by + r), outline=(220, 250, 255, 200))
    d.ellipse((108, 56, 122, 64), fill=(43, 179, 166, 255))
    d.polygon([(108, 60), (102, 55), (102, 65)], fill=(242, 122, 46, 255))
    # title
    d.text((84, 8), "ABISAL", fill=(6, 20, 28, 255))
    d.text((83, 7), "ABISAL", fill=(255, 255, 255, 255))
    img.save(out)


if __name__ == '__main__':
    main()
