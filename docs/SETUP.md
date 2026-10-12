# Minecraft AntiServerClose — setup

Created and maintained by **[iAlturki](https://github.com/iAlturki)**.

## 1. Check that your server can run it

Use Minecraft Java **26.3**, Fabric Loader **0.19.5+**, Java **25+**, and **Fabric API**. The bundled native client is Linux **x86-64**; its `--help` diagnostic must pass on your host. A provider can block native child processes or outbound connections even when it allows normal Fabric mods. Other versions/platforms are unsupported by this release.

Read your provider's automation and idle-server policies. This method might violate them. On a Free Minecraft Servers Host Provider, check the current idle policy and renewal/deletion deadlines separately; they vary by provider. There is no provider endorsement here.

**You need an existing licensed Minecraft Java account you own.** Nothing needs to keep running on your PC after setup. No cloud account, scheduled host-panel task, or third-party bot service is required.

## 2. Make a backup

1. Tell active players you are restarting the server.
2. In the server console, run `save-all flush` and wait for confirmation.
3. Stop Minecraft normally. Download a copy of the world folder(s) and keep it outside the hosting account. If you use a host backup export, check that it really contains your world.
4. Treat backups containing `config/`, host settings, player records, or account caches as private. A world backup does not require publishing these files.
5. Record the Minecraft version, existing mods, and the account's position/inventory before testing. Do not use an unbacked-up valuable player for the first trial.

No code can guarantee safety against a provider deleting files, terminating the container, or another mod changing player state.

## 3. Install

1. Download the beta JAR and `SHA256SUMS.txt` from [Releases](https://github.com/iAlturki/Minecraft-AntiServerClose/releases/tag/v0.5.0-beta.1).
2. Compare the file's SHA-256 with the release checksum. In Windows PowerShell:

   ```powershell
   Get-FileHash -Algorithm SHA256 -LiteralPath '.\Minecraft-AntiServerClose-26.3-0.5.0-beta.1.jar'
   ```

   On Linux: `sha256sum Minecraft-AntiServerClose-26.3-0.5.0-beta.1.jar`.
3. Upload the JAR into the server's `mods/` directory while Minecraft is stopped. Upload **one** version only; remove or disable any older/selfbot experimental version first. Never run two account supervisors together.
4. Keep Fabric API installed. Leave `online-mode=true` and your existing whitelist enabled. Do not weaken account or signed-profile checks to make the bot work.
5. If your own Minecraft configuration kicks idle players, set `player-idle-timeout=0` in `server.properties` while the server is stopped. This controls Minecraft's idle kick only; it does not change the host's sleep policy.
6. Start normally. The mod creates `config/minecraft-antiserverclose/` with owner-only directory permissions on a POSIX host, extracts its native files, and performs a no-login `--help` check.
7. Check for `[iAlturki AntiServerClose] Native test passed`. If it fails, read `diagnostics.txt` privately and follow [Troubleshooting](TROUBLESHOOTING.md). Do not approve an account before fixing a failed native check.

## 4. Configure your account and address

Edit **`config/minecraft-antiserverclose/antiserverclose.properties`**. The generated file initially contains blank fields and stays disabled.

```properties
# Minecraft AntiServerClose by iAlturki
enabled=true
username=YourMinecraftName
address=your-server.example.com:25565
authentication-approved=true
reconnect-seconds=90
```

- `username`: your Minecraft Java profile name, **not** a Microsoft email or password. It must match the account you authenticate. The public client exits and the supervisor disables itself if it sees a different authenticated name.
- `address`: a hostname or IPv4 address with an explicit port between 1 and 65535. Prefer the same **public proxy address and port a real player uses**. Some hosts' allocated backend IP/port cannot wake or accept connections the same way. Verify the address using your own real Minecraft client first. There is no universal port override; `25565` is just an example. IPv6 is not supported in this configuration validator.
- `authentication-approved`: `true` means **you consent to reusable credentials being stored on this hosting server**. The host operator and anyone with sufficient file access may be able to read them. Set it only for a host/account you trust.
- `reconnect-seconds`: bounded to 60–600; invalid values use 90. This is a cooldown, not an exact promise of reconnection timing.

Whitelist the selected name if needed, using your normal server console:

```text
whitelist add YourMinecraftName
```

The account does **not** need operator permissions. Save the properties file and log out that account from other Minecraft clients. Configuration is checked approximately once per second; a restart is usually unnecessary after a successful diagnostic. Authentication cannot begin while that account is already in the server's player list.

## 5. Approve the Microsoft device login yourself

1. Read **`config/minecraft-antiserverclose/bot-console.log`** using the server file manager or SFTP.
2. Find the most recent device-login prompt from this startup. Follow the Microsoft verification URL it gives and enter its current code. If the code expires or fails, do not keep submitting it.
3. Sign in to the Microsoft account that owns the profile configured above. Review the application and permissions shown. Complete passwords, MFA, and account approval yourself on Microsoft's page.
4. Approve only a login **you intentionally started**. No password, MFA code, session token, or device code belongs in a GitHub issue, commit, screenshot, or shared chat.
5. Authentication credentials are cached in **`config/minecraft-antiserverclose/botcraft_cached_credentials.json`**. Keep that entire directory private. The mod never needs you to paste that file into settings.
6. Watch the Minecraft console for a real join using your selected profile. The supervisor then reports that the account is present in the real player list and bot-session damage protection is active.

If login does not complete within ten minutes, the supervisor sets `enabled=false` to avoid repeated prompts. Resolve the problem, then set `enabled=true` again to attempt a fresh login. If you authenticated the wrong account, disable the mod, remove only its cached credentials file while the child is stopped, correct the name, and repeat the approval yourself.

## 6. Verify before leaving it unattended

1. Run `list` in the console. Confirm the selected account is online as a real player.
2. Check that its stored position, health, and inventory are expected. The mod does not intentionally move, mine, use items, or change its game mode; server ticks and other mods can still affect it.
3. Close the host panel and leave the server without human players for **longer than that host's idle threshold**, preferably at least twice the threshold. Avoid periodic panel actions or status probes during this test.
4. Have a real friend connect normally after that idle interval. A server-list ping alone proves less than a successful join.
5. If sharing your normal account, connect with that account from your real Minecraft client. The server may report the previous session being replaced; the supervisor should log `Yielding` and stop its bot process. Verify your position and inventory and that human damage behaves normally.
6. Leave the server. After the cooldown, confirm the bot returns. Repeat the real-client handover once more before depending on it.
7. Keep renewing the host's retention deadline separately. This mod does not click Renew or extend a seven-day deletion timer.

Using the same account for both sessions involves a brief race around connection timing. An account you already own exclusively for the bot reduces this risk. A human on a different Minecraft server with that account is not visible to this supervisor; it only checks its own server.

## 7. Stop, sign out, or remove it

1. Set `enabled=false` and save. Wait for the account to leave the server's `list` output.
2. To remove the mod, stop Minecraft normally, remove its JAR from `mods/`, and restart. No saved invulnerability or game-mode changes need to be undone by this mod.
3. To remove local account access, delete **only** `config/minecraft-antiserverclose/botcraft_cached_credentials.json` after the bot is stopped. Do not delete your world or player data.
4. Local deletion does not invalidate copies of credentials made elsewhere. If you no longer trust the host or suspect exposure, review/revoke the application permission and active sessions through your Microsoft account security settings. You may have to sign in again in legitimate clients afterward.
5. Keep world backups separate from credential caches. If there is a player-state problem, stop the bot first, preserve the current world/player file, and restore deliberately from a known-good backup. Do not blindly roll back other players or the entire world.

Project and guide © 2026 **iAlturki**. See [NOTICE](../NOTICE) and [LICENSE](../LICENSE).
