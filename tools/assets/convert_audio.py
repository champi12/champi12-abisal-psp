#!/usr/bin/env python3
"""Downmix and resample WAVs to the game's format, then pack them for the PSP.

Output is 22050 Hz mono. Looping tracks are closed (splice point + tiny equal-power fade)
so the mixer can wrap them with no click and no silence.

Most sounds are written as IMA/DVI 4-bit ADPCM inside a standard WAV container (format
0x0011): about 4x smaller in the data folder. The runtime decodes them back to 16-bit PCM
when it loads them, so playback does not change at all. Files where 4-bit would be
audible (low SNR) are kept as plain 16-bit PCM; the choice is made per file by measuring.

Usage: convert_audio.py SRC_DIR OUT_DIR
"""
import array
import math
import os
import struct
import sys
import wave

RATE = 22050

# files that must stay 16-bit whatever the measurement says (add names here)
PCM_KEEP = set()

IMA_INDEX = [-1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8]
IMA_STEP = [
    7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60,
    66, 73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371,
    408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878,
    2066, 2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845,
    8630, 9493, 10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794,
    32767]

BLOCK = 256          # WAV block align
SPB = (BLOCK - 4) * 2 + 1  # 505 samples per block (mono)


# ---------------------------------------------------------------- ADPCM

def ima_step(nib, pred, idx):
    step = IMA_STEP[idx]
    diff = step >> 3
    if nib & 1:
        diff += step >> 2
    if nib & 2:
        diff += step >> 1
    if nib & 4:
        diff += step
    if nib & 8:
        pred -= diff
    else:
        pred += diff
    pred = -32768 if pred < -32768 else (32767 if pred > 32767 else pred)
    idx += IMA_INDEX[nib]
    idx = 0 if idx < 0 else (88 if idx > 88 else idx)
    return pred, idx


def ima_quant(sample, pred, idx):
    """Pick the 4-bit code that lands closest to `sample` after decoding."""
    step = IMA_STEP[idx]
    diff = sample - pred
    nib = 0
    if diff < 0:
        nib = 8
        diff = -diff
    vpdiff = step >> 3
    if diff >= step:
        nib |= 4
        diff -= step
        vpdiff += step
    if diff >= step >> 1:
        nib |= 2
        diff -= step >> 1
        vpdiff += step >> 1
    if diff >= step >> 2:
        nib |= 1
        vpdiff += step >> 2
    if nib & 8:
        pred -= vpdiff
    else:
        pred += vpdiff
    pred = -32768 if pred < -32768 else (32767 if pred > 32767 else pred)
    idx += IMA_INDEX[nib]
    idx = 0 if idx < 0 else (88 if idx > 88 else idx)
    return nib, pred, idx


def encode_ima(pcm):
    out = bytearray()
    idx = 0
    i = 0
    n = len(pcm)
    last = 0
    while i < n:
        pred = pcm[i]
        last = pred
        out += struct.pack('<hBB', pred, idx, 0)
        for k in range(1, SPB):
            t = pcm[i + k] if i + k < n else last
            last = t
            nib, pred, idx = ima_quant(t, pred, idx)
            if k & 1:
                lo = nib
            else:
                out.append((nib << 4) | lo)  # first nibble of the byte is the LOW one
        if (SPB - 1) & 1:
            out.append(lo)
        i += SPB
    return bytes(out)


def decode_ima(data, frames):
    out = []
    idx = 0
    for b in range(0, len(data) - BLOCK + 1, BLOCK):
        p = data[b:b + BLOCK]
        pred = struct.unpack('<h', p[0:2])[0]
        idx = p[2] if p[2] <= 88 else 88
        out.append(pred)
        for i in range(4, BLOCK):
            byte = p[i]
            for half in range(2):
                nib = (byte & 0x0F) if half == 0 else (byte >> 4)
                pred, idx = ima_step(nib, pred, idx)
                out.append(pred)
    return out[:frames]


def snr_db(ref, got):
    n = min(len(ref), len(got))
    if n == 0:
        return 99.0
    se = 0.0
    sp = 0.0
    for i in range(n):
        d = ref[i] - got[i]
        se += d * d
        sp += ref[i] * ref[i]
    if se <= 1e-9:
        return 99.0
    return 10.0 * math.log10(max(sp, 1e-9) / se)


# ---------------------------------------------------------------- WAV writers

def write_pcm_wav(path, pcm):
    arr = array.array('h', pcm)
    data = arr.tobytes()
    hdr = b'RIFF' + struct.pack('<I', 36 + len(data)) + b'WAVE'
    hdr += b'fmt ' + struct.pack('<IHHIIHH', 16, 1, 1, RATE, RATE * 2, 2, 16)
    hdr += b'data' + struct.pack('<I', len(data))
    with open(path, 'wb') as f:
        f.write(hdr)
        f.write(data)


def write_ima_wav(path, adpcm, frames):
    block = BLOCK
    avg = RATE * block // SPB
    fmt = struct.pack('<HHIIHHHH', 0x11, 1, RATE, avg, block, 4, 2, SPB)
    body = b'fmt ' + struct.pack('<I', len(fmt)) + fmt
    body += b'fact' + struct.pack('<II', 4, frames)
    body += b'data' + struct.pack('<I', len(adpcm)) + adpcm
    if len(adpcm) & 1:
        body += b'\0'
    with open(path, 'wb') as f:
        f.write(b'RIFF' + struct.pack('<I', 4 + len(body)) + b'WAVE')
        f.write(body)


# ---------------------------------------------------------------- conversion

def convert(src, dst, loop_mode=False):
    with wave.open(src, 'rb') as w:
        nch = w.getnchannels()
        sw = w.getsampwidth()
        rate = w.getframerate()
        n = w.getnframes()
        raw = w.readframes(n)
    if sw != 2:
        return None
    vals = struct.unpack('<%dh' % (len(raw) // 2), raw)
    mono = []
    if nch == 2:
        for i in range(0, len(vals) - 1, 2):
            mono.append((vals[i] + vals[i + 1]) / 65536.0)
    else:
        for v in vals:
            mono.append(v / 32768.0)
    ratio = RATE / float(rate)
    outn = int(len(mono) * ratio)
    out = []
    ln = len(mono)
    for i in range(outn):
        pos = i / ratio
        j = int(pos)
        f = pos - j
        a = mono[j] if j < ln else 0.0
        b = mono[j + 1] if j + 1 < ln else a
        out.append(a + (b - a) * f)

    if loop_mode and len(out) > 2 * int(0.05 * RATE):
        # 1) drop leading/trailing near-silence (some sources carry up to a second)
        thr = 0.012
        a = 0
        while a < len(out) and abs(out[a]) < thr:
            a += 1
        b = len(out) - 1
        while b > a and abs(out[b]) < thr:
            b -= 1
        if b - a > int(0.4 * RATE):
            pad = int(0.02 * RATE)
            a = max(0, a - pad)
            b = min(len(out) - 1, b + pad)
            out = out[a:b + 1]
        # 2) splice where the start best CONTINUES the end (value + slope)
        n = len(out)
        best = 0
        bestscore = 1e9
        maxd = min(int(0.6 * RATE), n // 3)
        for d in range(0, maxd):
            v = out[d] - out[n - 1]
            s1 = out[d + 1] - out[d] if d + 1 < n else 0.0
            s2 = out[n - 1] - out[n - 2]
            score = abs(v) * 2.0 + abs(s1 - s2)
            if score < bestscore:
                bestscore = score
                best = d
        out = out[best:]
        # 3) 6 ms equal-power fade: hides any residual click, too short to hear a dip
        xf = int(0.006 * RATE)
        n = len(out)
        if n > 2 * xf:
            for i in range(xf):
                k = i / xf
                out[i] = out[i] * (k ** 0.5) + out[n - xf + i] * ((1.0 - k) ** 0.5)
            out = out[:n - xf]

    pcm = []
    for s in out:
        v = int(s * 32767)
        pcm.append(-32768 if v < -32768 else (32767 if v > 32767 else v))

    info = {'frames': len(pcm), 'adpcm': False, 'snr': None}
    name = os.path.basename(dst)
    # a loop is listened to for minutes: keep its quality high. A one-shot hit is masked by
    # the rest of the game, so 4 bits are fine there even with a modest SNR.
    min_db = 30.0 if loop_mode else 16.0
    # Only compress what already fits in RAM: a bigger file is streamed by the runtime, and
    # decoding it to PCM would cost memory instead of saving it.
    fits_ram = len(pcm) * 2 <= 400000
    if name not in PCM_KEEP and fits_ram and len(pcm) > SPB:
        enc = encode_ima(pcm)
        dec = decode_ima(enc, len(pcm))
        db = snr_db(pcm, dec)
        info['snr'] = db
        if db >= min_db:
            write_ima_wav(dst, enc, len(pcm))
            info['adpcm'] = True
            return info
    write_pcm_wav(dst, pcm)
    return info


def main():
    src, out = sys.argv[1], sys.argv[2]
    if not os.path.isdir(src):
        return
    os.makedirs(out, exist_ok=True)
    for name in sorted(os.listdir(src)):
        if not name.lower().endswith('.wav'):
            continue
        try:
            loop_mode = ('ambient' in name) or ('musica' in name) or ('go_loop' in name) or ('remora_go' in name) or ('loop' in name)
            info = convert(os.path.join(src, name), os.path.join(out, name), loop_mode)
            if info is None:
                print('audio FALLO (formato):', name)
                continue
            kind = 'ADPCM' if info['adpcm'] else 'PCM'
            extra = '' if info['snr'] is None else '  snr %.1f dB' % info['snr']
            print('audio %-5s %-22s %5.2fs%s' % (kind, name, info['frames'] / RATE, extra))
        except Exception as e:
            print('audio FALLO', name, e)


if __name__ == '__main__':
    main()
