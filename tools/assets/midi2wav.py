#!/usr/bin/env python3
"""Tiny General-MIDI-ish synthesizer: renders a .mid file to 16-bit mono WAV.

Each instrument family gets a simple oscillator with an ADSR envelope; percussion
(channel 10) is rendered as filtered noise bursts. Good enough for background music
on hardware without a MIDI synth.
"""
import math
import random
import struct
import sys
import wave

RATE = 22050


def read_var(d, p):
    v = 0
    while True:
        b = d[p]
        p += 1
        v = (v << 7) | (b & 0x7f)
        if not b & 0x80:
            return v, p


def parse(data):
    if data[:4] != b'MThd':
        raise ValueError('not a MIDI file')
    hl = struct.unpack('>I', data[4:8])[0]
    fmt, ntrk, div = struct.unpack('>HHH', data[8:14])
    p = 8 + hl
    events = []  # (tick, kind, chan, a, b)
    for _ in range(ntrk):
        if data[p:p + 4] != b'MTrk':
            break
        ln = struct.unpack('>I', data[p + 4:p + 8])[0]
        q = p + 8
        end = q + ln
        tick = 0
        status = 0
        while q < end:
            dt, q = read_var(data, q)
            tick += dt
            b = data[q]
            if b & 0x80:
                status = b
                q += 1
            if status == 0xFF:
                typ = data[q]
                l, q = read_var(data, q + 1)
                if typ == 0x51:
                    events.append((tick, 'tempo', 0, int.from_bytes(data[q:q + 3], 'big'), 0))
                q += l
            elif status in (0xF0, 0xF7):
                l, q = read_var(data, q)
                q += l
            else:
                hi = status & 0xF0
                ch = status & 0x0F
                if hi in (0xC0, 0xD0):
                    a = data[q]
                    q += 1
                    events.append((tick, hi, ch, a, 0))
                else:
                    a, b2 = data[q], data[q + 1]
                    q += 2
                    events.append((tick, hi, ch, a, b2))
        p = end
    events.sort(key=lambda e: e[0])
    return div, events


def to_seconds(div, events):
    tempo = 500000
    last_tick = 0
    t = 0.0
    out = []
    for tick, kind, ch, a, b in events:
        t += (tick - last_tick) * tempo / 1e6 / div
        last_tick = tick
        if kind == 'tempo':
            tempo = a
        else:
            out.append((t, kind, ch, a, b))
    return out


def voice(prog, freq, n, vel):
    """Render one note of n samples."""
    fam = prog // 8
    amp = vel / 127.0 * 0.25
    out = [0.0] * n
    att = int(0.01 * RATE)
    rel = int(0.08 * RATE)
    ph = 0.0
    inc = freq / RATE
    for i in range(n):
        ph += inc
        x = ph - math.floor(ph)
        if fam in (0, 1, 2):         # pianos, chromatic perc, organs: soft square/sine mix
            s = 0.6 * math.sin(2 * math.pi * x) + 0.4 * (1 if x < 0.5 else -1) * 0.5
            decay = math.exp(-i / (RATE * (0.8 if fam != 2 else 6.0)))
        elif fam in (4, 5, 6, 11):   # bass, strings, ensemble, synth pad: saw
            s = 2 * x - 1
            decay = 1.0 if fam != 4 else math.exp(-i / (RATE * 1.5))
        elif fam in (7, 8, 9):       # brass, reed, pipe: triangle
            s = 4 * abs(x - 0.5) - 1
            decay = 1.0
        else:                        # everything else: sine
            s = math.sin(2 * math.pi * x)
            decay = math.exp(-i / (RATE * 2.0))
        env = min(1.0, i / att) if att else 1.0
        if i > n - rel:
            env *= max(0.0, (n - i) / rel)
        out[i] = s * amp * env * decay
    return out


def drum(note, n, vel):
    amp = vel / 127.0 * 0.3
    out = [0.0] * n
    rnd = random.Random(note)
    if note in (35, 36):  # kick
        for i in range(n):
            t = i / RATE
            out[i] = math.sin(2 * math.pi * (60 + 80 * math.exp(-t * 30)) * t) * math.exp(-t * 12) * amp * 1.5
        return out
    length = 0.05 if note in (42, 44, 46) else 0.18
    last = 0.0
    for i in range(n):
        t = i / RATE
        noise = rnd.uniform(-1, 1)
        last = last * 0.5 + noise * 0.5 if note in (38, 40) else noise
        out[i] = last * math.exp(-t / length) * amp
    return out


def render(events, total_extra=1.0):
    notes = []
    active = {}
    progs = [0] * 16
    for t, kind, ch, a, b in events:
        if kind == 0xC0:
            progs[ch] = a
        elif kind == 0x90 and b > 0:
            active[(ch, a)] = (t, b, progs[ch])
        elif kind == 0x80 or (kind == 0x90 and b == 0):
            if (ch, a) in active:
                st, vel, pg = active.pop((ch, a))
                notes.append((st, t, ch, a, vel, pg))
    end = max([n[1] for n in notes] + [0]) + total_extra
    buf = [0.0] * int(end * RATE + 1)
    for st, et, ch, note, vel, pg in notes:
        dur = max(0.05, et - st)
        if ch == 9:
            n = int(0.4 * RATE)
            s = drum(note, n, vel)
        else:
            n = int((dur + 0.08) * RATE)
            s = voice(pg, 440.0 * 2 ** ((note - 69) / 12.0), n, vel)
        o = int(st * RATE)
        for i, v in enumerate(s):
            if o + i < len(buf):
                buf[o + i] += v
    peak = max(1e-6, max(abs(x) for x in buf))
    g = min(1.0, 0.9 / peak)
    return [int(max(-1, min(1, x * g)) * 32767) for x in buf]


def main():
    src, dst = sys.argv[1], sys.argv[2]
    div, ev = parse(open(src, 'rb').read())
    samples = render(to_seconds(div, ev))
    w = wave.open(dst, 'wb')
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(RATE)
    w.writeframes(struct.pack('<%dh' % len(samples), *samples))
    w.close()


if __name__ == '__main__':
    main()
