#!/usr/bin/env python3
"""Portable policy/storage tests, without Gradle locks or UI/platform compilation.

Compiles production shuffle/artwork sources and unchanged portable model declarations.
Only opaque stream fields, platform defaults/profile lookup and locking have test adapters.
This does NOT replace the full Kotlin/iOS compile or UI acceptance tests.
"""
import glob
import os
import pathlib
import re
import subprocess
import tempfile
import urllib.request

root = pathlib.Path(__file__).resolve().parents[1]
cache = pathlib.Path.home() / '.gradle/caches/modules-2/files-2.1'
scratch = pathlib.Path.home() / '.hermes/cache/scratch'
scratch.mkdir(parents=True, exist_ok=True)
main = root / 'composeApp/src/commonMain/kotlin/com/nuvio/app'

def jar(group, artifact):
    matches = sorted(glob.glob(str(cache / group / artifact / '*' / '*' / '*.jar')))
    if not matches:
        matches = sorted(glob.glob(str(pathlib.Path.home() / '.gradle/wrapper/dists/gradle-9*/**/lib' / (artifact + '-*.jar')), recursive=True))
    if not matches:
        raise SystemExit(f'Missing cached dependency: {group}:{artifact}')
    return matches[-1]

def braced(source, marker):
    start = source.index(marker)
    opening = source.index('{', start)
    depth = 1
    end = opening + 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[start:end]

compiler = [jar('org.jetbrains.kotlin', a) for a in ['kotlin-compiler-embeddable', 'kotlin-stdlib', 'kotlin-script-runtime', 'kotlin-reflect', 'kotlin-daemon-embeddable']]
compiler += [jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm'), jar('org.jetbrains', 'annotations')]
libs = [jar('org.jetbrains.kotlin', 'kotlin-stdlib'), jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm'), jar('org.jetbrains.kotlinx', 'kotlinx-serialization-core-jvm'), jar('org.jetbrains.kotlinx', 'kotlinx-serialization-json-jvm')]
plugin = jar('org.jetbrains.kotlin', 'kotlin-serialization-compiler-plugin-embeddable')
testlib = scratch / 'kotlin-test-2.3.0.jar'
if not testlib.exists():
    urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-test/2.3.0/kotlin-test-2.3.0.jar', testlib)
libs.append(str(testlib))
java = '/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/java'

with tempfile.TemporaryDirectory(prefix='shuffle-artwork-tests-', dir=scratch) as temp:
    temp = pathlib.Path(temp)
    def put(name, text):
        (temp / name).write_text(text)
    for feature, excluded in [('shuffle', {'DetailShuffleControls.kt', 'EpisodeShuffleRuntime.kt'}), ('artwork', {'ArtworkCompose.kt', 'ArtworkControls.kt'})]:
        for path in (main / 'features' / feature).glob('*.kt'):
            if path.name in excluded:
                continue
            text = path.read_text()
            marker = 'internal expect object Platform'
            if marker in text:
                declaration = braced(text, marker)
                name = re.search(r'object (\w+)', declaration).group(1)
                interface = 'EpisodeShuffleStorage' if feature == 'shuffle' else 'ArtworkStorage'
                text = text.replace(declaration, f'internal object {name} : {interface} {{ override fun load(profileId: Int): String? = error("Inject test storage"); override fun save(profileId: Int, payload: String) = error("Inject test storage") }}')
            put(path.name, text)
    models = (main / 'features/details/MetaDetailsModels.kt').read_text()
    video = re.search(r'data class MetaVideo\([\s\S]*?\n\)', models).group()
    put('Video.kt', 'package com.nuvio.app.features.details\nimport com.nuvio.app.features.streams.StreamItem\n' + video)
    put('StreamAdapter.kt', 'package com.nuvio.app.features.streams\nclass StreamItem')
    progress = (main / 'features/watchprogress/WatchProgressModels.kt').read_text()
    complete = braced((main / 'features/watching/domain/WatchingPolicies.kt').read_text(), 'fun isProgressComplete(').replace('fun isProgressComplete(', 'internal fun isWatchProgressComplete(')
    put('Progress.kt', 'package com.nuvio.app.features.watchprogress\nimport kotlinx.serialization.Serializable\nimport com.nuvio.app.features.tracking.TrackingAttributedItem\nprivate const val CompletionThresholdFraction = 0.90\nprivate const val WatchProgressCompletionPercentThreshold = 90f\nprivate const val WatchProgressSourceLocal = "local"\n@Serializable\n' + braced(progress, 'data class WatchProgressEntry(') + '\n' + complete)
    tracking = braced((main / 'features/tracking/TrackingAttribution.kt').read_text(), 'interface TrackingAttributedItem')
    put('Tracking.kt', 'package com.nuvio.app.features.tracking\n' + tracking)
    next_rule = braced((main / 'features/player/skip/PlayerNextEpisodeRules.kt').read_text(), '    fun resolveNextEpisode(')
    put('Next.kt', 'package com.nuvio.app.features.player.skip\nimport com.nuvio.app.features.details.MetaVideo\nobject PlayerNextEpisodeRules {\n' + next_rule + '\n}')
    put('ProfileAdapter.kt', 'package com.nuvio.app.features.profiles\nobject ProfileRepository { var activeProfileId = 1 }')
    put('LocksAdapter.kt', 'package kotlinx.atomicfu.locks\nopen class SynchronizedObject\ninline fun <T> synchronized(lock: SynchronizedObject, block: () -> T): T = kotlin.synchronized(lock, block)')
    put('ReleaseParser.kt', (main / 'core/time/EpisodeReleaseDateParser.kt').read_text())
    put('DateAdapter.kt', '''package com.nuvio.app.core.time
internal object EpisodeReleaseDatePlatform {
    fun nowEpochMs(): Long = System.currentTimeMillis()
    fun localIsoDateAtEpochMs(epochMs: Long): String? = java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
    fun localDateTimeToEpochMs(normalizedIsoDateTime: String): Long? = runCatching { java.time.LocalDateTime.parse(normalizedIsoDateTime).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
}
''')
    invocations = []
    testroot = root / 'composeApp/src/commonTest/kotlin/com/nuvio/app/features'
    for feature in ['shuffle', 'artwork']:
        for path in (testroot / feature).glob('*.kt'):
            text = path.read_text()
            methods = re.findall(r'@Test\s+fun\s+(`[^`]+`|\w+)\s*\(', text)
            if methods:
                cls = re.search(r'class (\w+)', text).group(1)
                for method in methods:
                    invocations.append(f'com.nuvio.app.features.{feature}.{cls}().{method}()')
            put('test-' + path.name, text.replace('import kotlin.test.Test', '').replace('@Test', ''))
    put('Runner.kt', 'fun main() {\n' + '\n'.join(invocations) + f'\nprintln("PASS: {len(invocations)} shuffle/artwork tests")\n' + '}')
    out = temp / 'tests.jar'
    subprocess.run([java, '-cp', os.pathsep.join(compiler), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-Xplugin=' + plugin, '-classpath', os.pathsep.join(libs), '-d', str(out), *map(str, temp.glob('*.kt'))], check=True)
    subprocess.run([java, '-cp', os.pathsep.join([str(out), *libs]), 'RunnerKt'], check=True)
