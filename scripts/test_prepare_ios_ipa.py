import importlib.util
from pathlib import Path
import plistlib
import struct
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('packager', Path(__file__).with_name('prepare-ios-ipa.py'))
packager = importlib.util.module_from_spec(spec)
spec.loader.exec_module(packager)


def binary(minimum=0x100100, kind=2, dependencies=(), symbols=1):
    commands = [struct.pack('<6I', 0x32, 24, 2, minimum, 0x1a0200, 0),
                struct.pack('<6I', 2, 24, 0, symbols, 0, 0)]
    for dependency in dependencies:
        name = dependency.encode() + b'\0'
        size = (24 + len(name) + 7) // 8 * 8
        commands.append(struct.pack('<6I', 0xc, size, 24, 0, 0, 0) + name + b'\0' * (size - 24 - len(name)))
    body = b''.join(commands)
    return struct.pack('<8I', 0xfeedfacf, 0x100000c, 0, kind, len(commands), len(body), 0, 0) + body


class PackageTest(unittest.TestCase):
    def make_ipa(self, directory, *, referenced=False, stub_symbols=0, real_framework=False):
        path = Path(directory) / 'input.ipa'
        app = 'Payload/Nuvio.app'
        framework = app + '/Frameworks/Libmpv.framework'
        minimum = '16.1' if real_framework else '100.0'
        dependencies = ['@rpath/Libmpv.framework/Libmpv'] if referenced else []
        with zipfile.ZipFile(path, 'w') as archive:
            archive.writestr(app + '/Info.plist', plistlib.dumps({'CFBundleExecutable': 'Nuvio', 'CFBundleIdentifier': 'com.nuvio.media', 'MinimumOSVersion': '16.1'}))
            app_entry = zipfile.ZipInfo(app + '/Nuvio')
            app_entry.external_attr = 0o100755 << 16
            archive.writestr(app_entry, binary(dependencies=dependencies))
            archive.writestr(app + '/asset.txt', b'unchanged artwork')
            archive.writestr(framework + '/Info.plist', plistlib.dumps({'CFBundleExecutable': 'Libmpv', 'CFBundleIdentifier': 'com.mpvkit.Libmpv', 'MinimumOSVersion': minimum}))
            archive.writestr(framework + '/Libmpv', binary(minimum=0x100100 if real_framework else 0x640000, kind=6, symbols=1 if real_framework else stub_symbols))
            archive.writestr(app + '/trailing-resource.txt', b'preserve entries after removed frameworks')
        return path

    def test_removes_empty_stub_preserves_executable_resources_and_permissions(self):
        with tempfile.TemporaryDirectory() as directory:
            source = self.make_ipa(directory)
            output = Path(directory) / 'fixed.ipa'
            report = packager.prepare(source, output)
            self.assertEqual(report['removed_count'], 1)
            with zipfile.ZipFile(source) as before, zipfile.ZipFile(output) as after:
                for name in after.namelist():
                    self.assertEqual(after.read(name), before.read(name))
                    self.assertEqual(after.getinfo(name).external_attr, before.getinfo(name).external_attr)
                self.assertFalse(any('.framework/' in n for n in after.namelist()))

    def test_refuses_to_remove_dynamically_loaded_stub(self):
        with tempfile.TemporaryDirectory() as directory:
            source = self.make_ipa(directory, referenced=True)
            with self.assertRaisesRegex(ValueError, 'dynamically referenced'):
                packager.prepare(source, Path(directory) / 'fixed.ipa')

    def test_refuses_ios100_framework_with_symbols(self):
        with tempfile.TemporaryDirectory() as directory:
            source = self.make_ipa(directory, stub_symbols=1)
            with self.assertRaisesRegex(ValueError, 'not a verified empty'):
                packager.prepare(source, Path(directory) / 'fixed.ipa')

    def test_keeps_real_supported_dynamic_framework(self):
        with tempfile.TemporaryDirectory() as directory:
            source = self.make_ipa(directory, referenced=True, real_framework=True)
            output = Path(directory) / 'fixed.ipa'
            self.assertEqual(packager.prepare(source, output)['removed_count'], 0)
            with zipfile.ZipFile(output) as after:
                self.assertIn('Payload/Nuvio.app/Frameworks/Libmpv.framework/Libmpv', after.namelist())


if __name__ == '__main__':
    unittest.main()
