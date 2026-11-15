#!/usr/bin/env python3
"""Extract the resources of a MIDlet JAR into a data directory for the port.

Everything except .class files is copied with its original path. Audio that the
PSP cannot play natively is converted next to the original file:
  *.amr -> *.amr.wav   (ffmpeg)
  *.mid -> *.mid.wav   (tools/assets/midi2wav.py)

Usage: prepare_assets.py GAME.jar OUTDIR
"""
import os
import shutil
import subprocess
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))


def main():
    jar, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    with zipfile.ZipFile(jar) as z:
        for info in z.infolist():
            name = info.filename
            if name.endswith('/') or name.endswith('.class'):
                continue
            if name.startswith('/') or '..' in name.split('/'):
                continue
            dst = os.path.join(out, name)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with z.open(info) as src, open(dst, 'wb') as f:
                shutil.copyfileobj(src, f)
    ffmpeg = shutil.which('ffmpeg')
    for root, _, files in os.walk(out):
        for fn in files:
            path = os.path.join(root, fn)
            low = fn.lower()
            if low.endswith('.amr'):
                target = path + '.wav'
                if os.path.exists(target):
                    continue
                if not ffmpeg:
                    print('warning: ffmpeg not found, %s stays silent' % fn)
                    continue
                subprocess.run([ffmpeg, '-loglevel', 'error', '-y', '-i', path, '-ac', '1', '-ar', '22050',
                                '-sample_fmt', 's16', target], check=True)
            elif low.endswith('.mid') or low.endswith('.midi'):
                target = path + '.wav'
                if os.path.exists(target):
                    continue
                subprocess.run([sys.executable, '-I', os.path.join(HERE, 'midi2wav.py'), path, target],
                               check=True)
    print('assets ready in', out)


if __name__ == '__main__':
    main()
