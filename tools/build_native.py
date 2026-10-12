# Copyright (c) 2026 iAlturki — GPL-3.0-only
"""Build the two project components and test the protocol fixes on Linux x86-64."""
import os
from pathlib import Path
import platform
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent


def main():
    if platform.system() != 'Linux' or platform.machine() not in ('x86_64', 'amd64'):
        raise SystemExit('Build native components on Linux x86-64; Windows users can use Ubuntu 24.04 in WSL')
    upstream = ROOT / '.build/upstream'
    native = ROOT / '.build/native'
    checks = ROOT / '.build/native-checks'
    checks.mkdir(parents=True, exist_ok=True)
    if not (upstream / 'botcraft/include/botcraft/Game/ConnectionClient.hpp').is_file():
        raise SystemExit('Run python3 tools/fetch_dependencies.py first')
    includes = ['-I', str(upstream / 'botcraft/include'), '-I', str(upstream / 'protocolCraft/include'), '-I', str(ROOT / 'native')]
    compiler = os.environ.get('CXX', 'g++')
    privacy = f'-ffile-prefix-map={ROOT}=.'
    common = [compiler, '-std=c++20', '-O2', privacy, '-DPROTOCOL_VERSION=777', *includes]
    link = ['-L', str(native), '-lbotcraft', '-lprotocolCraft']
    subprocess.run([*common, str(ROOT / 'native/AfkClient.cpp'), *link, '-Wl,-rpath,$ORIGIN', '-o', str(native / 'AntiServerCloseClient')], check=True)
    subprocess.run([compiler, '-std=c++20', '-O2', privacy, '-fPIC', '-shared', str(ROOT / 'native/TimestampFix.cpp'), '-o', str(native / 'libantiserverclose-timestamp-fix.so')], check=True)
    subprocess.run([*common, str(ROOT / 'checks/TeleportChecks.cpp'), *link, '-Wl,-rpath,$ORIGIN/../native', '-o', str(checks / 'teleport-checks')], check=True)
    subprocess.run([compiler, '-std=c++20', '-O2', privacy, '-DTIMESTAMP_FIX_TEST_MAIN', str(ROOT / 'native/TimestampFix.cpp'), '-o', str(checks / 'timestamp-check')], check=True)
    subprocess.run([sys.executable, str(ROOT / 'checks/check_teleports.py'), str(checks / 'teleport-checks')], check=True)
    subprocess.run([sys.executable, str(ROOT / 'checks/check_timestamps.py'), str(checks / 'timestamp-check')], check=True)
    # Run the same child-only library environment used by the Fabric supervisor.
    env = dict(os.environ, LD_LIBRARY_PATH=str(native), LD_PRELOAD=str(native / 'libantiserverclose-timestamp-fix.so'))
    result = subprocess.run([str(native / 'AntiServerCloseClient'), '--help'], check=True, capture_output=True, text=True, env=env)
    assert 'by iAlturki' in result.stdout and '--expected-username' in result.stdout
    for argv in [[], ['--login', 'OfflinePlayer'], ['--address'], ['--expected-username', 'invalid name']]:
        result = subprocess.run([str(native / 'AntiServerCloseClient'), *argv], capture_output=True, env=env)
        assert result.returncode == 2, (argv, result.returncode)
    print('PASS: native execution, Microsoft-only argument validation, teleport and timestamp fixes.')


if __name__ == '__main__': main()
