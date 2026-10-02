#!/usr/bin/env python3
"""Remove unreferenced MPVKit iOS-100 dummy dylibs from an unsigned IPA.

Xcode embeds these SwiftPM static-library placeholders even though MPV is
linked into the app. Never lower their deployment target or remove real dylibs.
"""
import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import plistlib
import struct
import tempfile
import zipfile


def version(value):
    return tuple((list(map(int, str(value).split('.'))) + [0, 0])[:3])


def macho(data):
    """Read 64-bit Mach-O slices, including universal wrappers, without macOS."""
    magic = data[:4]
    if magic in (b'\xca\xfe\xba\xbe', b'\xca\xfe\xba\xbf'):
        count = struct.unpack_from('>I', data, 4)[0]
        wide = magic[-1] == 0xbf
        stride = 32 if wide else 20
        result = []
        for i in range(count):
            offset, size = struct.unpack_from('>QQ' if wide else '>II', data, 16 + i * stride)
            if offset + size > len(data):
                raise ValueError('Invalid universal Mach-O slice')
            result.extend(macho(data[offset:offset + size]))
        return result
    if magic not in (b'\xcf\xfa\xed\xfe', b'\xfe\xed\xfa\xcf'):
        raise ValueError('Expected a 64-bit Mach-O executable, not a static archive')
    endian = '<' if magic == b'\xcf\xfa\xed\xfe' else '>'
    header = struct.unpack_from(endian + '8I', data)
    result = {'cpu': header[1], 'type': header[3], 'dependencies': [],
              'minimum': None, 'platform': None, 'symbols': None, 'exports': False}
    offset = 32
    end = offset + header[5]
    for _ in range(header[4]):
        command, size = struct.unpack_from(endian + 'II', data, offset)
        if size < 8 or offset + size > end or end > len(data):
            raise ValueError('Invalid Mach-O load command')
        if command in (0xc, 0x80000018, 0x8000001f, 0x20, 0x80000023):
            name_offset = struct.unpack_from(endian + 'I', data, offset + 8)[0]
            result['dependencies'].append(data[offset + name_offset:offset + size].split(b'\0')[0].decode())
        elif command == 0x32:
            platform, minimum = struct.unpack_from(endian + 'II', data, offset + 8)
            result['platform'] = platform
            result['minimum'] = (minimum >> 16, (minimum >> 8) & 255, minimum & 255)
        elif command == 0x25:
            minimum = struct.unpack_from(endian + 'I', data, offset + 8)[0]
            result['platform'] = 2
            result['minimum'] = (minimum >> 16, (minimum >> 8) & 255, minimum & 255)
        elif command == 2:
            result['symbols'] = struct.unpack_from(endian + 'I', data, offset + 12)[0]
        elif command in (0x80000033, 0x22, 0x80000022):
            field = offset + (8 if command == 0x80000033 else 40)
            export_offset, export_size = struct.unpack_from(endian + 'II', data, field)
            if export_offset + export_size > len(data):
                raise ValueError('Invalid Mach-O export trie')
            result['exports'] |= bool(data[export_offset:export_offset + export_size].strip(b'\0'))
        offset += size
    return [result]


def prepare(source, output, report_path=None):
    source, output = Path(source), Path(output)
    with zipfile.ZipFile(source) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or archive.testzip():
            raise ValueError('Duplicate ZIP entries or damaged IPA')
        if any('/_CodeSignature/' in n or n.endswith('.mobileprovision') for n in names):
            raise ValueError('Input must be unsigned; sign the repaired IPA afterwards')
        main_plists = [n for n in names if n.startswith('Payload/') and n.count('/') == 2 and n.endswith('.app/Info.plist')]
        if len(main_plists) != 1:
            raise ValueError('IPA must contain exactly one Payload app')
        app_root = main_plists[0].rsplit('/', 1)[0]
        bundles, binaries = {}, {}
        for name in names:
            if name.endswith('/Info.plist') and name.startswith(app_root + '/'):
                root = name.rsplit('/', 1)[0]
                if root.endswith(('.app', '.appex', '.framework')):
                    info = plistlib.loads(archive.read(name))
                    executable = root + '/' + info['CFBundleExecutable']
                    data = archive.read(executable)
                    bundles[root] = info
                    binaries[executable] = macho(data)
        main = bundles[app_root]
        removed = []
        for root, info in bundles.items():
            if not root.endswith('.framework') or version(info.get('MinimumOSVersion', '0')) != (100, 0, 0):
                continue
            slices = binaries[root + '/' + info['CFBundleExecutable']]
            stub = info.get('CFBundleIdentifier', '').startswith('com.mpvkit.') and slices and all(
                s['type'] == 6 and s['platform'] == 2 and s['minimum'] == (100, 0, 0)
                and s['symbols'] == 0 and not s['exports']
                and all(d == '/usr/lib/libSystem.B.dylib' for d in s['dependencies']) for s in slices)
            if not stub:
                raise ValueError('Unsupported iOS-100 framework is not a verified empty MPV stub: ' + root)
            removed.append(root)
        stub_names = {Path(r).stem for r in removed}
        for executable, slices in binaries.items():
            for s in slices:
                for dependency in s['dependencies']:
                    if Path(dependency).name in stub_names:
                        raise ValueError('Cannot remove a dynamically referenced stub: ' + dependency)
        for root, info in bundles.items():
            if root in removed:
                continue
            if version(info.get('MinimumOSVersion', '0')) > version(main['MinimumOSVersion']):
                raise ValueError('Embedded bundle requires newer iOS than the app: ' + root)
            if root.endswith('.appex') and not info['CFBundleIdentifier'].startswith(main['CFBundleIdentifier'] + '.'):
                raise ValueError('Extension bundle identifier does not match the app')
            for s in binaries[root + '/' + info['CFBundleExecutable']]:
                if s['cpu'] != 0x100000c or s['platform'] != 2:
                    raise ValueError('Non-device ARM64 executable: ' + root)
        output.parent.mkdir(parents=True, exist_ok=True)
        fd, temporary = tempfile.mkstemp(dir=output.parent, suffix='.ipa.tmp')
        os.close(fd)
        try:
            with zipfile.ZipFile(temporary, 'w') as repaired:
                for entry in archive.infolist():
                    if not any(entry.filename == r + '/' or entry.filename.startswith(r + '/') for r in removed):
                        repaired.writestr(copy.copy(entry), archive.read(entry))
            with zipfile.ZipFile(temporary) as repaired:
                if repaired.testzip():
                    raise ValueError('Repaired IPA CRC check failed')
                hashes = {}
                for name in repaired.namelist():
                    data = repaired.read(name)
                    if data != archive.read(name):
                        raise ValueError('Retained payload was modified: ' + name)
                    if name in binaries:
                        hashes[name] = hashlib.sha256(data).hexdigest()
            report = {'removed_unreferenced_ios100_stubs': removed,
                      'removed_count': len(removed), 'retained_executable_sha256': hashes,
                      'payload_bytes_preserved': True, 'archive_crc_passed': True,
                      'device_installation_verified': False,
                      'ipa_sha256': hashlib.sha256(Path(temporary).read_bytes()).hexdigest()}
        except BaseException:
            Path(temporary).unlink(missing_ok=True)
            raise
    os.replace(temporary, output)
    if report_path:
        Path(report_path).write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('ipa', type=Path)
    parser.add_argument('--output', type=Path, help='Default: replace input atomically')
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    print(json.dumps(prepare(args.ipa, args.output or args.ipa, args.report), indent=2))
