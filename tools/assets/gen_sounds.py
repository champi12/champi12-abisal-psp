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
    samples = echo(samples)
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', max(-32767, min(32767, int(s * 32767)))) for s in samples))


def write_dry(path, samples):
    """Write a WAV with no echo: for loops, whose tail must not leak past the end."""
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', max(-32767, min(32767, int(s * 32767)))) for s in samples))


def echo(samples, delay=0.02, feedback=0.3, mix=0.25):
    """A short feedback echo: makes the effects sit in the water instead of a dry room."""
    d = max(1, int(delay * RATE))
    wet = [0.0] * len(samples)
    for i in range(len(samples)):
        wet[i] = samples[i] + (wet[i - d] * feedback if i >= d else 0.0)
    return [max(-1.0, min(1.0, samples[i] * (1 - mix) + wet[i] * mix)) for i in range(len(samples))]


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
        # fundamental + 2nd/3rd harmonics for body, with a short soft attack
        s = math.sin(ph) + 0.28 * math.sin(2 * ph) + 0.1 * math.sin(3 * ph)
        att = min(1.0, i / (0.004 * RATE))
        out.append(s * att * env(i, n) * vol)
    return out


def chime(f, dur, vol=0.5):
    """A soft bell: fundamental plus a couple of harmonics, gentle attack, long decay."""
    n = int(dur * RATE)
    out, ph = [], 0.0
    for i in range(n):
        t = i / n
        ph += 2 * math.pi * f / RATE
        env = min(1.0, i / (0.012 * RATE)) * (1 - t) ** 2
        out.append((math.sin(ph) + 0.3 * math.sin(2 * ph) + 0.1 * math.sin(3 * ph)) * env * vol)
    return out


def bubbles(dur, rnd):
    n = int(dur * RATE)
    out = [0.0] * n
    t = 0
    while t < n - 3000:
        # a "bloop": a short tone rising in pitch, smooth in and out
        f0 = rnd.uniform(170, 480)
        m = int(rnd.uniform(0.045, 0.1) * RATE)
        ph = 0.0
        for i in range(m):
            if t + i >= n:
                break
            f = f0 * (1 + 1.1 * i / m)
            ph += 2 * math.pi * f / RATE
            e = math.sin(math.pi * i / m)
            out[t + i] += (math.sin(ph) + 0.3 * math.sin(2 * ph)) * e * 0.4
        t += int(rnd.uniform(0.055, 0.15) * RATE)
    return out


def golpe(rnd):
    # a dull underwater thump: falling low tone, a burst of filtered noise and a sharp transient
    n = int(0.35 * RATE)
    out, ph, lp = [], 0.0, 0.0
    for i in range(n):
        t = i / n
        ph += 2 * math.pi * (150 - 110 * t) / RATE
        lp += (rnd.uniform(-1, 1) - lp) * 0.08
        thump = rnd.uniform(-1, 1) * max(0.0, 1 - i / (0.008 * RATE)) * 0.5
        out.append((math.sin(ph) * 0.7 + lp * 1.1 + thump) * (1 - t) ** 2)
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
    # a knife swing through water: a swish of noise with a bright transient at the start
    n = int(0.22 * RATE)
    out, lp = [], 0.0
    for i in range(n):
        t = i / n
        x = rnd.uniform(-1, 1)
        lp += (x - lp) * (0.05 + 0.3 * math.sin(t * math.pi))
        click = x * 0.5 * max(0.0, 1 - i / (0.01 * RATE))
        out.append((lp * 1.2 + click) * math.sin(t * math.pi))
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
    # a clean sonar ping: a pure tone with a fast attack and a long smooth decay, then
    # two quieter echoes that trail off
    n = int(0.4 * RATE)
    base = []
    ph = 0.0
    for i in range(n):
        t = i / n
        f = 1400.0 * (1 - 0.12 * t)
        ph += 2 * math.pi * f / RATE
        env = min(1.0, i / (0.006 * RATE)) * (1 - t) ** 2.2
        base.append((math.sin(ph) * 0.55 + math.sin(2 * ph) * 0.12) * env)
    out = list(base)
    for k, amp in ((1, 0.3), (2, 0.15)):
        out += [0.0] * int(0.16 * RATE)
        out += [x * amp for x in base]
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
    # the canyon screecher: a wavering shriek, but lower and softer than before (it was a
    # piercing 1800 Hz whine that hurt the ears)
    n = int(0.9 * RATE)
    out, ph = [], 0.0
    for i in range(n):
        t = i / n
        f = 850 + 260 * math.sin(t * 20) - 260 * t
        ph += 2 * math.pi * f / RATE
        e = min(1.0, t * 12) * (1 - t) ** 1.2
        out.append((math.sin(ph) * 0.45 + math.sin(ph * 1.5) * 0.12 + rnd.uniform(-1, 1) * 0.07) * e * 0.7)
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


def musica():
    """A slow, seamless ambient loop: a deep drone with drifting pads. Every frequency is an
    integer number of cycles over the loop, so the end meets the start with no click and no
    fade (a fade was what made it sound like 'it ends and starts again')."""
    dur = 16.0
    n = int(dur * RATE)
    out = []
    f = [880.0 / dur, 1320.0 / dur, 1760.0 / dur, 2636.0 / dur]  # 55, 82.5, 110, 164.75 Hz
    amp = [0.26, 0.14, 0.1, 0.05]
    lfo = [2.0 / dur, 2.0 / dur, 1.0 / dur, 1.0 / dur]
    phs = [0.0, 0.0, 0.0, 0.0]
    for i in range(n):
        t = i / RATE
        s = 0.0
        for k in range(4):
            phs[k] += 2 * math.pi * f[k] / RATE
            a = 0.5 + 0.5 * math.sin(2 * math.pi * lfo[k] * t + k * 1.3)
            s += math.sin(phs[k]) * a * amp[k]
        # a soft high shimmer, also an integer number of cycles over the loop
        s += math.sin(2 * math.pi * (7040.0 / dur) * t) * 0.02 * (0.5 + 0.5 * math.sin(2 * math.pi * (3.0 / dur) * t))
        out.append(s)
    return out


def salpicon(rnd):
    """A wet surface splash: a low whoosh of water, a plop and a few gurgles (no harsh hiss)."""
    n = int(0.6 * RATE)
    out = [0.0] * n
    lp = 0.0
    lp2 = 0.0
    for i in range(n):
        t = i / n
        x = rnd.uniform(-1, 1)
        lp += (x - lp) * 0.08
        lp2 += (lp - lp2) * 0.2
        env = min(1.0, i / (0.02 * RATE)) * (1 - t) ** 1.6
        out[i] = lp2 * env * 1.4
    # the plop: a low tone at the start
    m = int(0.18 * RATE)
    ph = 0.0
    for i in range(m):
        t = i / m
        ph += 2 * math.pi * (160 - 90 * t) / RATE
        out[i] += math.sin(ph) * (1 - t) ** 2.5 * 0.3
    # a few gurgles
    t0 = int(0.06 * RATE)
    while t0 < n - 1000:
        f = rnd.uniform(300, 600)
        m = int(rnd.uniform(0.02, 0.05) * RATE)
        ph = 0.0
        for i in range(m):
            if t0 + i >= n:
                break
            ph += 2 * math.pi * f * (1 + i / m) / RATE
            out[t0 + i] += math.sin(ph) * (1 - i / m) * 0.18
        t0 += int(rnd.uniform(0.08, 0.15) * RATE)
    return out


def remoraHum():
    """The Remora's cabin: a low, rhythmic propeller thrum underwater (no high whine)."""
    dur = 2.0
    n = int(dur * RATE)
    out = []
    ph = 0.0
    ph2 = 0.0
    for i in range(n):
        t = i / dur
        # the propeller blade rate: a low throb (~9 Hz), like water being pushed
        blade = 0.5 + 0.5 * math.sin(2 * math.pi * 9 * t)
        ph += 2 * math.pi * 48 / RATE
        ph2 += 2 * math.pi * 72 / RATE
        s = math.sin(ph) * 0.28 * (0.6 + 0.4 * blade) + math.sin(ph2) * 0.09
        s *= 0.85 + 0.15 * math.sin(2 * math.pi * 0.7 * t)
        edge = min(1.0, t * 4, (1 - t) * 4)  # seamless loop
        out.append(s * edge)
    return out


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    rnd = random.Random(7)
    write(os.path.join(out, 'recoger.wav'), chime(880, 0.16, 0.45) + chime(1320, 0.24, 0.3))
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
    write_dry(os.path.join(out, 'musica.wav'), musica())
    write(os.path.join(out, 'salpicon.wav'), salpicon(rnd))
    write(os.path.join(out, 'remora.wav'), remoraHum())


if __name__ == '__main__':
    main()
