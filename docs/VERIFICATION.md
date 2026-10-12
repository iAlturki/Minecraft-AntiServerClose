# Minecraft AntiServerClose — validation record

Project by **[iAlturki](https://github.com/iAlturki)**. Record dated **2026-10-12**.

## Live-tested foundation

The private predecessor, version **0.4.0**, was tested on a Linux x86-64 free-plan server **on a Free Minecraft Servers Host Provider**, using Minecraft **26.3**, protocol **777**, Fabric Loader **0.19.5**, Java **25**, and Fabric API. Online mode and the whitelist remained enabled. The test server also used Carpet, Distant Horizons, Lithium, FerriteCore, TT20, and the provider's own mod.

- A real Microsoft-authenticated account appeared in Minecraft's player list.
- With the host panel closed, the bot remained online across idle intervals beyond the provider's 15-minute limit. The final corrected-build observation included an **uninterrupted 20-minute-20-second interval between checks**, after earlier running time, with no intervening panel actions, console commands, renewals, human joins, or status probes by the tester.
- The server did not stop in that observation. This is evidence of that server's player-based idle behavior at the time; it is not a universal host guarantee or a long-term uptime measurement.
- A same-account human connection replaced the bot. The supervisor yielded, then the bot returned after logout and cooldown. This mechanism was observed in the predecessor during the same test session.
- After the corrected 26.3 teleport reply was installed, saved coordinates remained stable through bot reconnects. Health was 20 in the observed state checks. The owner independently joined twice with a real Minecraft client and confirmed return to the expected position.
- The native timestamp and teleport fixes, temporary bot-entity damage protection, normal shutdown behavior, and session-supervisor checks passed their targeted tests.

Private account identifiers, hostname, IP, logs, world records, credentials, screenshots, and backups are deliberately excluded from this repository.

## Public beta build

**0.5.0-beta.1** repackages that foundation as Minecraft AntiServerClose by iAlturki. It introduces:

- A new mod ID, Java package, configuration directory, and project branding.
- Configurable explicit hostname/IPv4 and port, with disabled-by-default settings.
- A Microsoft-only native entrypoint and authenticated-name mismatch check.
- Bounds and fallback behavior for malformed cooldown settings.
- A launch/shutdown lock to prevent a child being started after shutdown.
- Checksum-locked dependencies, retained notices, source archives, and CI builds.
- No normal coordinate or certificate-expiry logging from the project fixes.

Before publication, the public build passed:

| Check | Evidence |
|:--|:--|
| Timestamp conversion | 16 values compared with Python datetime, including 0–9 fractional digits, UTC offsets, leap day, and pre-epoch input; four malformed inputs fail closed |
| Teleport acknowledgements | Three protocol-777 wire packets matched independent Python `struct` encoding for absolute, relative, and mixed changes; unknown initial relative position rejected |
| Native startup | `--help` passed using the child-only `LD_PRELOAD` environment; unsupported/incomplete/offline-login arguments rejected without authentication |
| Supervisor | Existing human session, replacement yield, disconnect cleanup, lookup failure, exact bot-entity damage protection, shutdown cleanup, address validation, cooldown bounds, and launch-after-shutdown checks |
| Package | Java compilation, GPL/attribution resources, required native files, default-disabled configuration, release checksums, and private-data scan |

**The public beta has not been installed on the live test server.** Its account mismatch behavior has not been tested against a freshly approved Microsoft account. Do the setup guide's own idle and real-client handover tests before relying on it. GitHub Actions reports its own independent build result after publication.

## Not established

Other free hosts, other Minecraft versions, Windows/ARM servers, long-term retention renewal, months of uptime, exact CPU/RAM cost, resilience against every mod, and immunity to provider suspension or data loss have not been verified. A successful ping is less conclusive than a friend actually joining. A host can change its idle policy or block the process at any time.

Maintained by **iAlturki**. See [SETUP.md](SETUP.md) for your own verification steps.
