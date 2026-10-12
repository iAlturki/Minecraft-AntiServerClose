# Copyright (c) 2026 iAlturki — GPL-3.0-only
"""Package corresponding source from an explicit allowlist, with no runtime data."""
import hashlib
import json
from pathlib import Path
import tarfile

ROOT = Path(__file__).resolve().parent.parent
FILES = ('README.md', 'AUTHORS', 'NOTICE', 'LICENSE', 'THIRD_PARTY.md', 'CHANGELOG.md', 'CONTRIBUTING.md', 'SECURITY.md', 'build.py', '.gitignore', '.gitattributes')
DIRS = ('src', 'native', 'checks', 'resources', 'tools', 'docs', 'assets', '.github', 'third-party')


def main():
    version = json.loads((ROOT / 'resources/fabric.mod.json').read_text())['version']
    dist = ROOT / 'dist'
    dist.mkdir(exist_ok=True)
    target = dist / f'Minecraft-AntiServerClose-{version}-corresponding-source.tar.gz'
    prefix = f'Minecraft-AntiServerClose-{version}'
    upstream = ROOT / '.build/upstream'
    if not (upstream / '3rdparty/asio/asio/include/asio.hpp').is_file():
        raise SystemExit('Upstream sources incomplete; run tools/fetch_dependencies.py')
    with tarfile.open(target, 'w:gz', compresslevel=6) as archive:
        for name in FILES:
            archive.add(ROOT / name, arcname=prefix + '/' + name)
        for folder in DIRS:
            for path in sorted((ROOT / folder).rglob('*')):
                if path.is_file() and '__pycache__' not in path.parts:
                    archive.add(path, arcname=prefix + '/' + path.relative_to(ROOT).as_posix())
        for path in sorted(upstream.rglob('*')):
            if path.is_file() and '.git' not in path.parts:
                archive.add(path, arcname=prefix + '/upstream/Botcraft/' + path.relative_to(upstream).as_posix())
    checksum = hashlib.sha256(target.read_bytes()).hexdigest()
    with (dist / 'SHA256SUMS.txt').open('a', encoding='ascii') as output:
        output.write(f'{checksum}  {target.name}\n')
    print(f'Packaged complete upstream source tree and project source: {target.name}')


if __name__ == '__main__': main()
