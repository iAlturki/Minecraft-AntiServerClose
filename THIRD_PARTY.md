# Minecraft AntiServerClose — dependency notices

Project by **[iAlturki](https://github.com/iAlturki)**. Original project components are credited to iAlturki. Existing dependency authors and notices remain intact.

## Botcraft / protocolCraft

- Upstream: [adepierre/Botcraft](https://github.com/adepierre/Botcraft).
- Revision: [`0809455c61b9e2584bd9b1c2ee777fa0e1765b56`](https://github.com/adepierre/Botcraft/tree/0809455c61b9e2584bd9b1c2ee777fa0e1765b56).
- License: GNU GPL version 3, retained in [LICENSE](LICENSE).
- Official Linux 26.3 binary archive published **2026-10-02**. SHA-256: `44a8098c2fe196b1626643edba51e44a14fac04246eb1050c0006e9b0edfba90`.
- `libbotcraft.so` and `libprotocolCraft.so` are extracted unchanged from that archive. The unused original AFK executable is omitted.
- The project client is compiled from [native/AfkClient.cpp](native/AfkClient.cpp), using [TeleportClient.hpp](native/TeleportClient.hpp). A separate [timestamp library](native/TimestampFix.cpp) corrects expiry conversion only in the child process.
- The full upstream source and all nine pinned submodule source trees accompany each release in the **corresponding-source** archive. [third-party/lock.json](third-party/lock.json) records revisions, URLs, and SHA-256 digests. `tools/fetch_dependencies.py` verifies downloads.

## Other components

Botcraft's upstream source/build includes Asio, Catch2, GLFW, GLM, Dear ImGui, imgui-node-editor, OpenSSL through openssl-cmake, rectpack2D, zlib, and bundled GLAD/stb headers. Their original license and copyright files are retained in [third-party/licenses](third-party/licenses) and in the complete source archive. Inclusion in the source archive does not imply that every component is exercised by this minimal client.

Fabric Loader **0.19.5** is a compile-time dependency downloaded from the [official Fabric Maven](https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.19.5/). Fabric Loader and Fabric API must already be installed on the server; their JARs are not bundled into this mod.

The project release does not contain a Minecraft client/server JAR, a Microsoft password, or any user's authentication cache or world. System runtime libraries are supplied by the Linux host.

## Rebuilding the upstream libraries

The release includes the original upstream CMake scripts and workflow under `upstream/Botcraft` in the corresponding-source archive. Its `.github/workflows/botcraft_build.yml` records upstream build options. To reconstruct the upstream build, use that pinned source tree, its included submodule sources, and those options; installing the original compiler/system dependencies and allowing upstream CMake to fetch Minecraft assets may be necessary. Do not redistribute Minecraft assets.

For the project client, protocol fixes, Java supervisor, and JAR, follow [docs/BUILD.md](docs/BUILD.md). The usual project build reuses the checksum-locked official libraries; it does not claim their compilation is reproduced bit for bit.

Maintained by **iAlturki**.
