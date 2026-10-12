# Minecraft AntiServerClose — changelog

Created and maintained by **iAlturki**.

## 0.5.0-beta.1 — 2026-10-12

First public beta, based on the privately tested Fabric/Botcraft 26.3 solution.

- Server-hosted, Microsoft-authenticated native client; no unattended PC process.
- Public server address/port configuration and explicit account-cache consent.
- Authenticated account-name mismatch detection; Microsoft login only.
- Same-account session yield and bounded reconnect cooldown.
- Temporary protection for the bot player entity only.
- Certificate timestamp and protocol-777 teleport acknowledgement fixes.
- Disabled defaults, native diagnostics, shutdown cleanup, and a launch/shutdown lock.
- iAlturki branding, account setup/rollback guidance, targeted checks, GitHub CI, checksums, and corresponding source with retained dependency notices.

Live validation applies to the predecessor; the public beta has local build/protocol/supervisor validation. See [docs/VERIFICATION.md](docs/VERIFICATION.md).

© 2026 **iAlturki**.
