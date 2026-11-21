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


def descarga(rnd):
    # electric discharge: crackling noise bursts over a falling buzz
    n = int(0.7 * RATE)
    out, ph = [], 0.0
    for i in range(n):
        t = i / n
        ph += 2 * math.pi * (220 - 150 * t) / RATE
        buzz = 1.0 if math.sin(ph) > 0 else -1.0
        crack = rnd.uniform(-1, 1) if rnd.random() < 0.35 * (1 - t) else 0.0
        out.append((buzz * 0.25 + crack * 0.8) * (1 - t) ** 1.5)
    return out


def tajo(rnd):
    # a knife swing through water: short swish of band-limited noise rising then falling
    n = int(0.22 * RATE)
    out, lp, hp = [], 0.0, 0.0
    for i in range(n):
        t = i / n
        x = rnd.uniform(-1, 1)
        lp += (x - lp) * (0.05 + 0.25 * math.sin(t * math.pi))
        out.append(lp * math.sin(t * math.pi) * 1.4)
    return out


def mordida(rnd):
    # being hurt: two quick crunches and a low thud
    out = []
    for k in range(2):
        n = int(0.07 * RATE)
        for i in range(n):
            out.append(rnd.uniform(-1, 1) * (1 - i / n) * 0.8)
        out += [0.0] * int(0.03 * RATE)
    n = int(0.25 * RATE)
    ph = 0.0
    for i in range(n):
        ph += 2 * math.pi * (90 - 40 * i / n) / RATE
        out.append(math.sin(ph) * (1 - i / n) * 0.7)
    return out


def alarma():
    # warning: two-tone beeps
    out = []
    for k in range(3):
        out += blip(880, 880, 0.12, 0.35) + [0.0] * int(0.05 * RATE)
        out += blip(660, 660, 0.12, 0.35) + [0.0] * int(0.05 * RATE)
    return out


def sonar():
    # the Remora's sonar ping with a soft echo
    out = blip(1400, 1350, 0.25, 0.3)
    out += [0.0] * int(0.15 * RATE)
    out += [x * 0.35 for x in blip(1400, 1350, 0.25, 0.3)]
    return out


def motor(rnd):
    # the sub starting up: an electric whine spinning up
    n = int(0.9 * RATE)
    out, ph, lp = [], 0.0, 0.0
    for i in range(n):
        t = i / n
        ph += 2 * math.pi * (80 + 260 * t * t) / RATE
        lp += (rnd.uniform(-1, 1) - lp) * 0.05
        e = min(1.0, t * 4) * (1 - max(0.0, t - 0.7) / 0.3)
        out.append((math.sin(ph) * 0.4 + math.sin(ph * 2) * 0.15 + lp * 0.6) * e)
    return out


def click():
    # lamp switch
    n = int(0.03 * RATE)
    return [math.sin(2 * math.pi * 2200 * i / RATE) * (1 - i / n) * 0.5 for i in range(n)]


def latido(rnd):
    # the Heartbeat: a deep double thump felt more than heard
    out = []
    for k, (amp, dur) in enumerate(((1.0, 0.32), (0.65, 0.28))):
        n = int(dur * RATE)
        ph, lp = 0.0, 0.0
        for i in range(n):
            t = i / n
            ph += 2 * math.pi * (55 - 25 * t) / RATE
            lp += (rnd.uniform(-1, 1) - lp) * 0.02
            out.append((math.sin(ph) * 0.9 + lp * 0.8) * amp * (1 - t) ** 2 * min(1.0, t * 30))
        out += [0.0] * int(0.12 * RATE)
    return out


def chillido(rnd):
    # the canyon screecher: a piercing, wavering shriek
    n = int(0.9 * RATE)
    out, ph = [], 0.0
    for i in range(n):
        t = i / n
        f = 1800 + 600 * math.sin(t * 40) - 700 * t
        ph += 2 * math.pi * f / RATE
        e = min(1.0, t * 20) * (1 - t) ** 0.8
        out.append((math.sin(ph) * 0.5 + math.sin(ph * 1.5) * 0.2 + rnd.uniform(-1, 1) * 0.15) * e * 0.8)
    return out


def escotilla(rnd):
    # the hatch: a metal clunk and a hiss of air
    out = []
    n = int(0.12 * RATE)
    ph = 0.0
    for i in range(n):
        t = i / n
        ph += 2 * math.pi * (180 - 80 * t) / RATE
        out.append((math.sin(ph) + math.sin(ph * 2.7) * 0.5) * (1 - t) ** 2 * 0.7)
    n = int(0.45 * RATE)
    lp = 0.0
    for i in range(n):
        t = i / n
        lp += (rnd.uniform(-1, 1) - lp) * 0.5
        out.append(lp * 0.35 * (1 - t))
    return out


def atrapar(rnd):
    # catching a fish: a quick splash and a wet slap
    out = bubbles(0.25, rnd)
    n = int(0.08 * RATE)
    for i in range(n):
        out.append(rnd.uniform(-1, 1) * (1 - i / n) * 0.6)
    return out


def cortar(rnd):
    # cutting a plant: a fibrous rip
    n = int(0.25 * RATE)
    out, lp = [], 0.0
    for i in range(n):
        t = i / n
        x = rnd.uniform(-1, 1) if rnd.random() < 0.6 else 0.0
        lp += (x - lp) * 0.35
        out.append(lp * 1.2 * math.sin(t * math.pi))
    return out


def mineral(rnd):
    # chipping a mineral: a bright clink with a short ring
    out = []
    for k, f in enumerate((2100, 3150)):
        n = int(0.18 * RATE)
        ph = 0.0
        for i in range(n):
            t = i / n
            ph += 2 * math.pi * f / RATE
            v = math.sin(ph) * (1 - t) ** 3 * 0.5
            if k == 0:
                out.append(v)
            else:
                out[i] += v * 0.6
    n = int(0.05 * RATE)
    for i in range(n):
        out[i] += rnd.uniform(-1, 1) * (1 - i / n) * 0.5
    return out


def comer(rnd):
    # eating: two soft crunches and a gulp
    out = []
    for k in range(2):
        n = int(0.07 * RATE)
        lp = 0.0
        for i in range(n):
            lp += (rnd.uniform(-1, 1) - lp) * 0.3
            out.append(lp * (1 - i / n) * 0.9)
        out += [0.0] * int(0.06 * RATE)
    out += blip(300, 160, 0.15, 0.4)
    return out


def veneno(rnd):
    # poisoned: a sickly falling warble
    n = int(0.6 * RATE)
    out, ph = [], 0.0
    for i in range(n):
        t = i / n
        ph += 2 * math.pi * (500 - 300 * t + 40 * math.sin(t * 50)) / RATE
        out.append(math.sin(ph) * 0.4 * (1 - t))
    return out


def escupir(rnd):
    # the cave spitter: a wet, sucking 'pthoo' followed by a hiss of acid
    out, ph = [], 0.0
    n = int(0.14 * RATE)
    for i in range(n):
        t = i / n
        ph += 2 * math.pi * (240 + 500 * t) / RATE
        out.append((math.sin(ph) * 0.6 + rnd.uniform(-1, 1) * 0.4) * math.sin(t * math.pi))
    n = int(0.3 * RATE)
    lp = 0.0
    for i in range(n):
        t = i / n
        lp += (rnd.uniform(-1, 1) - lp) * 0.7
        out.append(lp * 0.3 * (1 - t))
    return out


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    rnd = random.Random(7)
    write(os.path.join(out, 'recoger.wav'), blip(520, 1040, 0.12) + blip(780, 1560, 0.1))
    write(os.path.join(out, 'burbujas.wav'), bubbles(0.8, rnd))
    write(os.path.join(out, 'golpe.wav'), golpe(rnd))
    write(os.path.join(out, 'rugido.wav'), rugido(rnd))
    write(os.path.join(out, 'descarga.wav'), descarga(rnd))
    write(os.path.join(out, 'tajo.wav'), tajo(rnd))
    write(os.path.join(out, 'mordida.wav'), mordida(rnd))
    write(os.path.join(out, 'alarma.wav'), alarma())
    write(os.path.join(out, 'fabricar.wav'), blip(400, 600, 0.08) + blip(600, 900, 0.08) + blip(900, 1300, 0.14))
    write(os.path.join(out, 'pda.wav'), blip(1200, 1600, 0.06, 0.3))
    write(os.path.join(out, 'click.wav'), click())
    write(os.path.join(out, 'motor.wav'), motor(rnd))
    write(os.path.join(out, 'sonar.wav'), sonar())
    write(os.path.join(out, 'latido.wav'), latido(rnd))
    write(os.path.join(out, 'chillido.wav'), chillido(rnd))
    write(os.path.join(out, 'escotilla.wav'), escotilla(rnd))
    write(os.path.join(out, 'atrapar.wav'), atrapar(rnd))
    write(os.path.join(out, 'cortar.wav'), cortar(rnd))
    write(os.path.join(out, 'mineral.wav'), mineral(rnd))
    write(os.path.join(out, 'comer.wav'), comer(rnd))
    write(os.path.join(out, 'veneno.wav'), veneno(rnd))
    write(os.path.join(out, 'escupir.wav'), escupir(rnd))


if __name__ == '__main__':
    main()
