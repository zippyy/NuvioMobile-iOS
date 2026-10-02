#!/usr/bin/env python3
"""Run production Live TV common + iOS regression tests on an Apple simulator.

Requires the normal Full iOS build environment and engine XCFramework. Unlike the
previous isolated JVM harness, this executes the actual platform gzip bindings.
"""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
subprocess.run([str(root / 'gradlew'), ':composeApp:iosSimulatorArm64Test',
                '--console=plain', '--no-configuration-cache', '--max-workers=1',
                '--tests', 'com.nuvio.app.features.livetv.*'], cwd=root, check=True)
