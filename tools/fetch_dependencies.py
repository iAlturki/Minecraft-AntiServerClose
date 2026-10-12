# Copyright (c) 2026 iAlturki — GPL-3.0-only
"""Fetch checksum-locked public dependencies. Never handles Minecraft accounts."""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path, PurePosixPath
import shutil
import urllib.error
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent.parent


def fetch(spec, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.is_file() and hashlib.sha256(target.read_bytes()).hexdigest() == spec['sha256']:
        return target
    last = None
    for url in [spec['url'], *spec.get('mirrors', [])]:
        try:
            request = urllib.request.Request(url, headers={'User-Agent': 'iAlturki-Minecraft-AntiServerClose'})
            with urllib.request.urlopen(request, timeout=90) as response:
                data = response.read()
            if hashlib.sha256(data).hexdigest() != spec['sha256']:
                raise ValueError('Downloaded checksum differs from third-party/lock.json')
            target.write_bytes(data)
            return target
        except (OSError, ValueError, urllib.error.URLError) as error:
            last = error
    raise RuntimeError(f'Could not fetch verified dependency {target.name}: {last}')


def extract_source(archive_path, destination):
    destination.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive_path) as archive:
        for info in archive.infolist():
            path = PurePosixPath(info.filename)
            if path.is_absolute() or '..' in path.parts:
                raise ValueError('Unsafe path in upstream archive')
            if len(path.parts) <= 1 or info.is_dir(): continue
            output = destination.joinpath(*path.parts[1:])
            if not output.resolve().is_relative_to(destination.resolve()):
                raise ValueError('Unsafe upstream extraction target')
            output.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(info) as src, output.open('wb') as dst: shutil.copyfileobj(src, dst)


def main():
    lock = json.loads((ROOT / 'third-party/lock.json').read_text())
    tasks = [
        (lock['fabric_loader'], ROOT / '.build/fabric-loader-0.19.5.jar'),
        (lock['botcraft_binary'], ROOT / '.build/botcraft-linux-26.3.zip'),
        (lock['botcraft_source'], ROOT / '.build/botcraft-source.zip'),
    ]
    tasks.extend((spec, ROOT / '.build/sources' / (spec['name'] + '.zip')) for spec in lock['submodules'])
    with ThreadPoolExecutor(max_workers=4) as executor:
        list(executor.map(lambda item: fetch(*item), tasks))
    upstream = ROOT / '.build/upstream'
    extract_source(ROOT / '.build/botcraft-source.zip', upstream)
    for spec in lock['submodules']:
        extract_source(ROOT / '.build/sources' / (spec['name'] + '.zip'), upstream / spec['path'])
    with zipfile.ZipFile(ROOT / '.build/botcraft-linux-26.3.zip') as archive:
        for name in ('libbotcraft.so', 'libprotocolCraft.so'):
            candidates = [entry for entry in archive.namelist() if PurePosixPath(entry).name == name]
            if len(candidates) != 1: raise ValueError(f'Expected one {name} in the locked binary archive')
            target = ROOT / '.build/native' / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(candidates[0]))
    print('Verified public dependencies and corresponding upstream sources. No account data accessed.')


if __name__ == '__main__': main()
