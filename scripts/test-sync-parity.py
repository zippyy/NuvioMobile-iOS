#!/usr/bin/env python3
"""Run pure sync parity Kotlin tests without starting Gradle/Xcode.
Uses a cached Gradle 9.x Kotlin compiler and serialization jars. This does not
compile Compose or exercise iOS Keychain; run the normal app build separately.
"""
import os
import pathlib
import re
import subprocess
import sys
import tempfile

root = pathlib.Path(__file__).resolve().parents[1]
libs = sorted((pathlib.Path.home() / '.gradle/wrapper/dists').glob('gradle-9*/**/lib/kotlin-compiler-embeddable-*.jar'))
if not libs:
    sys.exit('No cached Gradle 9.x Kotlin compiler available')
lib = libs[-1].parent
java = pathlib.Path(os.environ.get('JAVA_HOME', '/Applications/Android Studio.app/Contents/jbr/Contents/Home')) / 'bin/java'
java_command = str(java) if java.exists() else 'java'
scratch_root = pathlib.Path(os.environ.get('TMPDIR', str(pathlib.Path.home() / '.hermes/cache/scratch')))
scratch_root.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='sync-parity-', dir=scratch_root) as directory:
    scratch = pathlib.Path(directory)
    names = []
    tests = sorted((root / 'composeApp/src/commonTest/kotlin/com/nuvio/app/core/sync').glob('Reshaped*Test.kt'))
    for test in tests:
        text = test.read_text().replace('import kotlin.test.Test', '').replace('@Test ', '').replace('@Test\n', '')
        text = text.replace('import kotlin.test.assertEquals', 'import com.nuvio.app.core.sync.assertEquals').replace('import kotlin.test.assertFailsWith', 'import com.nuvio.app.core.sync.assertFailsWith')
        (scratch / test.name).write_text(text)
        class_match = re.search(r'class (\w+)', text)
        if class_match is None:
            sys.exit(f'No test class in {test}')
        cls = class_match.group(1)
        names.extend(f'{cls}().{name}()' for name in re.findall(r'fun (\w+)\(\)', text))
    if not names:
        sys.exit('No sync parity tests discovered')
    (scratch / 'Main.kt').write_text('package com.nuvio.app.core.sync\nfun assertEquals(a: Any?, b: Any?) { check(a == b) { "Expected $a, got $b" } }\ninline fun <reified T: Throwable> assertFailsWith(block: () -> Unit) { try { block() } catch (e: Throwable) { check(e is T); return }; error("Expected exception") }\nfun main() { ' + '; '.join(names) + '; println("PASS ' + str(len(names)) + ' tests") }')
    cp = ':'.join(str(p) for p in lib.glob('*.jar'))
    sources = list(scratch.glob('*.kt')) + list((root / 'composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync').glob('Reshaped*kt'))
    subprocess.run([java_command, '-cp', cp, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-classpath', cp, '-d', str(scratch / 'tests.jar')] + [str(p) for p in sources], check=True)
    subprocess.run([java_command, '-cp', str(scratch / 'tests.jar') + ':' + cp, 'com.nuvio.app.core.sync.MainKt'], check=True)
