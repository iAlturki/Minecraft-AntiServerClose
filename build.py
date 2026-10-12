# Copyright (c) 2026 iAlturki — GPL-3.0-only
"""Compile, run supervisor checks, and package the Linux-native Fabric release."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
NATIVE_FILES = ('AntiServerCloseClient', 'libbotcraft.so', 'libprotocolCraft.so', 'libantiserverclose-timestamp-fix.so')


def main():
    parser = argparse.ArgumentParser(description='Build Minecraft AntiServerClose by iAlturki')
    parser.add_argument('--jdk-bin', type=Path, help='Directory containing javac and java; otherwise JAVA_HOME/PATH')
    args = parser.parse_args()
    suffix = '.exe' if os.name == 'nt' else ''
    jdk = args.jdk_bin or (Path(os.environ['JAVA_HOME']) / 'bin' if os.environ.get('JAVA_HOME') else None)
    def tool(name):
        path = jdk / (name + suffix) if jdk else shutil.which(name)
        if not path or not Path(path).is_file():
            raise SystemExit(f'JDK tool {name} not found; pass --jdk-bin or set JAVA_HOME')
        return str(path)
    dependency = ROOT / '.build/fabric-loader-0.19.5.jar'
    if not dependency.is_file():
        raise SystemExit('Run python tools/fetch_dependencies.py first')
    lock = json.loads((ROOT / 'third-party/lock.json').read_text())
    if hashlib.sha256(dependency.read_bytes()).hexdigest() != lock['fabric_loader']['sha256']:
        raise SystemExit('Fabric Loader dependency hash does not match the lock file')
    native = ROOT / '.build/native'
    for name in NATIVE_FILES:
        if not (native / name).is_file():
            raise SystemExit('Native files missing; build them on Linux with tools/build_native.py')
    build_dir = ROOT / '.build/classes'
    build_dir.mkdir(parents=True, exist_ok=True)
    sources = sorted((ROOT / 'src').rglob('*.java'))
    subprocess.run([tool('javac'), '--release', '17', '-encoding', 'UTF-8', '-classpath', str(dependency), '-d', str(build_dir), *map(str, sources)], check=True)
    classpath = os.pathsep.join((str(build_dir), str(dependency)))
    test_dir = ROOT / '.build/test-classes'
    test_dir.mkdir(exist_ok=True)
    subprocess.run([tool('javac'), '--release', '17', '-encoding', 'UTF-8', '-classpath', classpath, '-d', str(test_dir), str(ROOT / 'checks/SupervisorChecks.java')], check=True)
    subprocess.run([tool('java'), '-classpath', os.pathsep.join((str(test_dir), classpath)), 'SupervisorChecks'], check=True)
    version = json.loads((ROOT / 'resources/fabric.mod.json').read_text())['version']
    dist = ROOT / 'dist'
    dist.mkdir(exist_ok=True)
    jar = dist / f'Minecraft-AntiServerClose-26.3-{version}.jar'
    entries = {}
    for path in build_dir.rglob('*.class'):
        entries[path.relative_to(build_dir).as_posix()] = path.read_bytes()
    for path in (ROOT / 'resources').rglob('*'):
        if path.is_file(): entries[path.relative_to(ROOT / 'resources').as_posix()] = path.read_bytes()
    for name in NATIVE_FILES: entries['native/' + name] = (native / name).read_bytes()
    for name in ('LICENSE', 'NOTICE', 'AUTHORS', 'THIRD_PARTY.md'):
        entries['META-INF/' + name] = (ROOT / name).read_bytes()
    for path in (ROOT / 'third-party/licenses').rglob('*'):
        if path.is_file(): entries['META-INF/third-party/' + path.name] = path.read_bytes()
    for path in sorted(entries):
        if any(part in path.lower() for part in ('credential', 'bot-console', 'server.properties')):
            raise SystemExit(f'Refusing private runtime file: {path}')
    # Fixed entry times and sorted paths avoid machine-specific ZIP timestamps.
    with zipfile.ZipFile(jar, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (2026, 10, 12, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data)
    checksum = hashlib.sha256(jar.read_bytes()).hexdigest()
    (dist / 'SHA256SUMS.txt').write_text(f'{checksum}  {jar.name}\n', encoding='ascii')
    print(f'Built {jar.name}\nSHA256 {checksum}')


if __name__ == '__main__': main()
