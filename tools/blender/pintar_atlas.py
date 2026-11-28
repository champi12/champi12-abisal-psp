#!/usr/bin/env python3
"""Compone el atlas de las criaturas en estilo bloque (assets/modelos/atlas_criaturas.png,
512 x 256) a partir de las descripciones assets/modelos/atlas/*.json que guarda cada modelo:
cada rectángulo se pinta con su estilo (escamas, franjas, placas, ojo...) en pixel art, con
el borde oscurecido y la fila de arriba con brillo, como en Blockbench."""
import glob, json, os, random, sys
from PIL import Image

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "assets", "modelos")
TW, TH = 512, 256


def mez(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def hexc(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def pintar(px, r, rnd):
    x0, y0, w, h, est = r["x"], r["y"], r["w"], r["h"], r["estilo"]
    c1, c2 = hexc(r["c1"]), hexc(r["c2"])
    cara = r.get("cara", "z")
    # light from above: tops brighter, bellies darker, sides in between
    luz = {"y": 0.14, "x": 0.0, "z": 0.04}.get(cara, 0.0)
    for j in range(h):
        for i in range(w):
            t = j / max(1, h - 1)
            u = i / max(1, w - 1)
            if est == "escamas":
                # overlapping scales: rows offset by half a scale, each scale with a lit
                # upper rim and a dark lower edge
                c = mez(c1, c2, t * 0.9)
                sx, sy = 4, 3
                fila = j // sy
                col = (i + (fila % 2) * (sx // 2)) % sx
                fy = j % sy
                if fy == 0 and 0 < col < sx - 1:
                    c = mez(c, (255, 255, 255), 0.22)
                elif fy == sy - 1 or col == 0:
                    c = mez(c, (0, 0, 0), 0.28)
                # lateral line along the middle of the flank
                if cara == "x" and abs(t - 0.45) < 0.5 / max(1, h) * 1.5:
                    c = mez(c, (255, 255, 255), 0.3)
            elif est == "franjas":
                c = c1 if (i // 3) % 2 == 0 else c2
                if i % 3 == 0:
                    c = mez(c, (0, 0, 0), 0.25)
                c = mez(c, (0, 0, 0), t * 0.25)
            elif est == "aleta":
                # fin rays fanning out, darker between them, lighter translucent tips
                c = mez(c1, c2, u)
                if i % 2 == 1:
                    c = mez(c, (0, 0, 0), 0.25)
                if u > 0.8:
                    c = mez(c, (255, 255, 255), 0.25)
            elif est == "placas":
                c = c1 if ((i // 4) + (j // 3)) % 2 == 0 else c2
                if j % 3 == 0:
                    c = mez(c, (0, 0, 0), 0.3)
                elif j % 3 == 1:
                    c = mez(c, (255, 255, 255), 0.12)
            elif est == "hueso":
                c = mez(c1, c2, rnd.random() * 0.5 + t * 0.3)
                if rnd.random() < 0.08:
                    c = mez(c, (0, 0, 0), 0.4)
                if i % 5 == 0:
                    c = mez(c, (0, 0, 0), 0.12)
            elif est == "puntos":
                c = mez(c1, c2, t * 0.4)
                if (i * 7 + j * 13) % 9 == 0:
                    c = mez(c2, (0, 0, 0), 0.3)
            elif est == "ojo":
                cx, cy = (w - 1) / 2, (h - 1) / 2
                d = ((i - cx) ** 2 + (j - cy) ** 2) ** 0.5
                rr = max(w, h) / 2
                c = c1 if d < rr * 0.9 else mez(c1, (0, 0, 0), 0.5)
                if d < rr * 0.45:
                    c = c2
                # the glint only fits in eyes of 5+ pixels; tiny eyes are a dark pupil in a ring
                if min(w, h) >= 5 and abs(i - (cx - rr * 0.25)) < 0.8 and abs(j - (cy - rr * 0.25)) < 0.8:
                    c = (255, 255, 255)
                elif min(w, h) < 5:
                    c = c2 if d < rr * 0.7 else mez(c1, (255, 255, 255), 0.35)
            elif est == "brillo":
                cx, cy = (w - 1) / 2, (h - 1) / 2
                d = max(abs(i - cx) / max(1, cx), abs(j - cy) / max(1, cy))
                c = mez(c1, c2, d)
            else:
                # smooth skin: soft gradient with sparse freckles
                c = mez(c1, mez(c1, (0, 0, 0), 0.25), t)
                if rnd.random() < 0.05:
                    c = mez(c, (0, 0, 0), 0.25)
            if est not in ("ojo", "brillo"):
                c = mez(c, (255, 255, 255), luz) if luz > 0 else c
                if cara == "y" and r["c1"] == r["c2"]:
                    pass
                c = mez(c, (0, 0, 0), rnd.uniform(0, 0.08))
                if j == 0 and h > 2:
                    c = mez(c, (255, 255, 255), 0.2)
                if i == 0 or i == w - 1 or j == h - 1:
                    c = mez(c, (0, 0, 0), 0.35)
            px[x0 + i, y0 + j] = c


def main():
    img = Image.new("RGB", (TW, TH), (255, 0, 255))
    px = img.load()
    for f in sorted(glob.glob(os.path.join(RAIZ, "atlas", "*.json"))):
        d = json.load(open(f))
        rnd = random.Random(d["modelo"])
        for r in d["rects"]:
            pintar(px, r, rnd)
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(RAIZ, "atlas_criaturas.png")
    img.save(out)
    print("ATLAS", out)


if __name__ == "__main__":
    main()
