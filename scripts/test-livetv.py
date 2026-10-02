#!/usr/bin/env python3
"""Run portable Live TV behavior tests using cached Kotlin jars, without Gradle locks."""
import glob, os, pathlib, subprocess, tempfile
root = pathlib.Path(__file__).resolve().parents[1]
cache = pathlib.Path.home() / '.gradle/caches/modules-2/files-2.1'
def jar(group, artifact):
    matches = sorted(glob.glob(str(cache / group / artifact / '*' / '*' / '*.jar')))
    if not matches:
        matches = sorted(glob.glob(str(pathlib.Path.home() / '.gradle/wrapper/dists/gradle-9*/**/lib' / (artifact+'-*.jar')), recursive=True))
    if not matches: raise SystemExit(f'Missing cached dependency: {group}:{artifact}')
    return matches[-1]
compiler = [jar('org.jetbrains.kotlin', a) for a in ['kotlin-compiler-embeddable', 'kotlin-stdlib', 'kotlin-script-runtime', 'kotlin-reflect', 'kotlin-daemon-embeddable']]
compiler += [jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm'), jar('org.jetbrains', 'annotations')]
libs = [jar('org.jetbrains.kotlin', 'kotlin-stdlib'), jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm'), jar('org.jetbrains.kotlinx', 'kotlinx-serialization-core-jvm'), jar('org.jetbrains.kotlinx', 'kotlinx-serialization-json-jvm')]
scratch = pathlib.Path.home()/'.hermes/cache/scratch'
scratch.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='live-tests-', dir=scratch) as temp:
    temp = pathlib.Path(temp)
    (temp/'Immutable.kt').write_text('package androidx.compose.runtime\nannotation class Immutable')
    sources = [p for p in (root/'composeApp/src/commonMain/kotlin/com/nuvio/app/features/livetv').glob('*.kt') if p.name not in ['LiveTvScreen.kt','LiveTvPlatform.kt','LiveTvPlayback.kt']]
    tests = root/'composeApp/src/commonTest/kotlin/com/nuvio/app/features/livetv/LiveTvBehaviorTest.kt'
    out = temp/'tests.jar'
    java = '/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/java'
    subprocess.run([java,'-cp',os.pathsep.join(compiler),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',os.pathsep.join(libs),'-d',str(out),str(temp/'Immutable.kt'),*[str(p) for p in sources],str(tests)],check=True)
    subprocess.run([java,'-cp',os.pathsep.join([str(out),*libs]),'com.nuvio.app.features.livetv.LiveTvBehaviorTestKt'],check=True)
