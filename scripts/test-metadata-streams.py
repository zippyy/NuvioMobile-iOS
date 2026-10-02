#!/usr/bin/env python3
"""Run isolated common Kotlin metadata/resolver behavior tests without a Gradle build.
Requires a cached Gradle 9.4.1 distribution and Java 17+; test libraries are fetched
from Maven Central into Hermes scratch when absent. No app/source stubs are used.
"""
import glob
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
HOME = Path.home()
WORK = Path(os.environ.get('TMPDIR', str(HOME / '.hermes/cache/scratch'))) / 'metadata-stream-tests'
WORK.mkdir(parents=True, exist_ok=True)
libs = glob.glob(str(HOME / '.gradle/wrapper/dists/gradle-9.4.1-bin/*/gradle-9.4.1/lib'))
if not libs:
    raise SystemExit('A cached Gradle 9.4.1 distribution is required; run ./gradlew --version first.')
java = shutil.which('java')
if os.environ.get('JAVA_HOME'):
    java = str(Path(os.environ['JAVA_HOME']) / 'bin/java')
elif Path('/opt/homebrew/opt/openjdk@17/bin/java').exists():
    java = '/opt/homebrew/opt/openjdk@17/bin/java'
dependencies = {
    'kotlin-test.jar': 'org/jetbrains/kotlin/kotlin-test/2.3.0/kotlin-test-2.3.0.jar',
    'kotlin-test-junit.jar': 'org/jetbrains/kotlin/kotlin-test-junit/2.3.0/kotlin-test-junit-2.3.0.jar',
    'junit.jar': 'junit/junit/4.13.2/junit-4.13.2.jar',
    'hamcrest.jar': 'org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar',
}
for filename, artifact in dependencies.items():
    target = WORK / filename
    if not target.exists():
        urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/' + artifact, target)
classpath = os.pathsep.join(glob.glob(libs[0] + '/*.jar') + [str(WORK / name) for name in dependencies])
base = ROOT / 'composeApp/src'
features = 'kotlin/com/nuvio/app/features/'
sources = [base / 'commonMain' / (features + name) for name in [
    'mdblist/MdbListClient.kt', 'torrent/TorrServerResolver.kt',
    'trailer/YouTubeStreamResolver.kt', 'trailer/TrailerPlaybackSource.kt',
]]
tests = [base / 'commonTest' / (features + name) for name in [
    'mdblist/MdbListClientTest.kt', 'torrent/TorrServerResolverTest.kt',
    'trailer/YouTubeStreamResolverTest.kt',
]]
output = WORK / 'tests.jar'
subprocess.run([java, '-cp', classpath, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
                '-no-stdlib', '-no-reflect', '-classpath', classpath, '-d', str(output)]
               + [str(p) for p in sources + tests], check=True, cwd=ROOT)
classes = ['com.nuvio.app.features.' + p.parent.name + '.' + p.stem for p in tests]
subprocess.run([java, '-cp', str(output) + os.pathsep + classpath, 'org.junit.runner.JUnitCore']
               + classes, check=True, cwd=ROOT)
