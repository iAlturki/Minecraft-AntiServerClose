# Minecraft AntiServerClose — source builds

Created and maintained by **[iAlturki](https://github.com/iAlturki)**.

## Linux

Build on Linux **x86-64**, preferably Ubuntu **24.04** to match the locally tested native toolchain. You need **Python 3.10+**, a C++20-capable **g++**, and a **JDK 25** with `javac` and `java` on PATH or `JAVA_HOME` set. Building on a newer distribution may raise the runtime library requirements of the resulting executable.

```bash
git clone https://github.com/iAlturki/Minecraft-AntiServerClose.git
cd Minecraft-AntiServerClose
python3 tools/fetch_dependencies.py
python3 tools/build_native.py
python3 build.py
python3 tools/package_sources.py
```

`fetch_dependencies.py` downloads only public software/source archives, checks their SHA-256 digests against `third-party/lock.json`, and extracts them under `.build/`. The original upstream binary URL uses a mutable `latest` release; a changed digest is rejected. The project's first beta release mirrors the exact original archive for future rebuilds.

`build_native.py` compiles the project client and timestamp correction against the pinned upstream headers/libraries. It runs the independent wire-format and timestamp checks, then native no-login argument tests. It does not log in to Microsoft, start a Minecraft world, or contact your server.

`build.py` compiles the Fabric initializer against the official Fabric Loader compile dependency, runs the supervisor checks, and builds a JAR with deterministic ZIP entry ordering/timestamps. Native binary differences between toolchains can still change the JAR digest; this is not a claim of fully reproducible upstream binaries.

`package_sources.py` creates a release source archive from an explicit project allowlist plus the complete pinned upstream source tree and submodules. It excludes Git metadata and runtime account/world directories. License notices are also included in the JAR.

## Windows with existing WSL

Use an **existing Ubuntu 24.04 x86-64 WSL installation** with Python and g++ available; you only need it for compilation. No bot runs on Windows after server deployment.

1. Clone the repository in PowerShell, then `cd Minecraft-AntiServerClose`.
2. Run `python tools/fetch_dependencies.py`.
3. In your WSL shell, navigate to the cloned directory's `/mnt/c/...` path and run `python3 tools/build_native.py`.
4. Back in PowerShell, run:

   ```powershell
   python build.py --jdk-bin 'C:\Path\To\JDK25\bin'
   python tools/package_sources.py
   ```

5. Use the JAR in `dist/` on a compatible Linux host. Do not upload the Windows compile dependency, source build directories, or compiler toolchain to the Minecraft server.

## Release contents

| Asset | Purpose |
|:--|:--|
| `Minecraft-AntiServerClose-26.3-0.5.0-beta.1.jar` | Install this one file in `mods/` |
| `SHA256SUMS.txt` | SHA-256 checksums for release assets |
| `Minecraft-AntiServerClose-0.5.0-beta.1-corresponding-source.tar.gz` | Project source, original Botcraft source, dependency source, and original build scripts/notices |
| `Botcraft-26.3-upstream-linux.zip` | Exact checksum-verified upstream binary archive, mirrored for source-build availability; not a mod to install |

The upstream libraries are unchanged. To rebuild them instead of reusing the archive, follow the original upstream scripts supplied in the corresponding-source archive and [THIRD_PARTY.md](../THIRD_PARTY.md). Upstream CMake may fetch Minecraft assets during compilation; the project release does not redistribute those assets.

## GitHub Actions

[build.yml](../.github/workflows/build.yml) builds and checks the project on Ubuntu 24.04, then uploads a downloadable build artifact. Release publication is separate: the repository owner can review the artifact and publish it under **iAlturki**. CI never needs a Microsoft/Minecraft account or a hosting-panel login.

When changing versions, update `resources/fabric.mod.json`, download links, the changelog, and documentation together. Preserve **iAlturki** credits and all dependency notices.
