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


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    rnd = random.Random(7)
    write(os.path.join(out, 'recoger.wav'), blip(520, 1040, 0.12) + blip(780, 1560, 0.1))
    write(os.path.join(out, 'burbujas.wav'), bubbles(0.8, rnd))


if __name__ == '__main__':
    main()
