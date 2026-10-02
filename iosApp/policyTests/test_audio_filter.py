"""Exercise the exact lavfi expression used by MPV with real FFmpeg PCM."""
import math
import pathlib
import struct
import subprocess
import os

root = pathlib.Path(__file__).resolve().parents[2]
binary = pathlib.Path(os.environ['TMPDIR']) / 'mpv-policy-tests'
subprocess.run(['swiftc', str(root / 'iosApp/iosApp/Player/MPVPlaybackPolicy.swift'),
                str(root / 'iosApp/policyTests/main.swift'), '-o', str(binary)], check=True)
filter_graph = subprocess.check_output([str(binary), '--filter'], text=True).splitlines()[0]
samples = [-1.0, -.75, -.41, -.4, -.2, 0, .2, .4, .41, .75, 1.0] * 100
pcm = struct.pack('<' + 'f' * len(samples), *samples)
result = subprocess.run(['ffmpeg', '-v', 'error', '-f', 'f32le', '-ar', '48000', '-ac', '2',
                         '-i', 'pipe:0', '-af', filter_graph, '-f', 'f32le', 'pipe:1'],
                        input=pcm, capture_output=True, check=True)
actual = struct.unpack('<' + 'f' * (len(result.stdout) // 4), result.stdout)
assert len(actual) == len(samples)
for source, output in zip(samples, actual):
    x = source * 2
    magnitude = abs(x)
    expected = math.copysign(magnitude if magnitude <= .8 else .8 + .2 * math.tanh((magnitude - .8) / .2), x)
    assert abs(output - expected) < 1e-6, (source, output, expected)
    assert abs(output) <= 1
print(f'PASS: {len(samples)} stereo PCM samples, linear gain below knee, tanh peaks, no clipping')
