#!/usr/bin/env python3
"""Synthesise the game's sound effects (original, generated from code) as 16-bit WAV.

Usage: gen_sounds.py OUTDIR
"""
import math
import os
import random
import struct
import sys
import wave

RATE = 22050


def write(path, samples):
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', max(-32767, min(32767, int(s * 32767)))) for s in samples))


def env(i, n, a=0.01, r=0.3):
    t = i / RATE
    d = n / RATE
    e = min(1.0, t / a) if a > 0 else 1.0
    return e * max(0.0, min(1.0, (d - t) / (r * d)))


def blip(f0, f1, dur, vol=0.5):
    n = int(dur * RATE)
    out, ph = [], 0.0
    for i in range(n):
        f = f0 + (f1 - f0) * i / n
        ph += 2 * math.pi * f / RATE
        out.append(math.sin(ph) * env(i, n) * vol)
    return out


def bubbles(dur, rnd):
    n = int(dur * RATE)
    out = [0.0] * n
    t = 0
    while t < n - 2000:
        f0 = rnd.uniform(300, 900)
        m = int(rnd.uniform(0.03, 0.08) * RATE)
        ph = 0.0
        for i in range(m):
            if t + i >= n:
                break
            ph += 2 * math.pi * f0 * (1 + 1.5 * i / m) / RATE
            out[t + i] += math.sin(ph) * (1 - i / m) * 0.35
        t += int(rnd.uniform(0.02, 0.12) * RATE)
    return out


def golpe(rnd):
    # a dull underwater thump: falling low tone plus a burst of filtered noise
    n = int(0.35 * RATE)
    out, ph, lp = [], 0.0, 0.0
    for i in range(n):
        t = i / n
        ph += 2 * math.pi * (140 - 90 * t) / RATE
        lp += (rnd.uniform(-1, 1) - lp) * 0.08
        out.append((math.sin(ph) * 0.7 + lp * 1.2) * (1 - t) ** 2)
    return out


def rugido(rnd):
    # a long, low, wavering growl for the leviathan
    n = int(2.4 * RATE)
    out, ph, ph2, lp = [], 0.0, 0.0, 0.0
    for i in range(n):
        t = i / n
        f = 48 + 10 * math.sin(t * 9) + 6 * math.sin(t * 23)
        ph += 2 * math.pi * f / RATE
        ph2 += 2 * math.pi * f * 1.51 / RATE
        lp += (rnd.uniform(-1, 1) - lp) * 0.02
        e = min(1.0, t * 5) * (1 - t) ** 1.5
        out.append((math.sin(ph) * 0.6 + math.sin(ph2) * 0.25 + lp * 2.0) * e * 0.9)
    return out


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    rnd = random.Random(7)
    write(os.path.join(out, 'recoger.wav'), blip(520, 1040, 0.12) + blip(780, 1560, 0.1))
    write(os.path.join(out, 'burbujas.wav'), bubbles(0.8, rnd))
    write(os.path.join(out, 'golpe.wav'), golpe(rnd))
    write(os.path.join(out, 'rugido.wav'), rugido(rnd))


if __name__ == '__main__':
    main()
