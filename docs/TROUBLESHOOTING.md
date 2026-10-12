# Minecraft AntiServerClose — troubleshooting

Created and maintained by **[iAlturki](https://github.com/iAlturki)**.

| Symptom | What to check |
|:--|:--|
| No generated configuration | Confirm Minecraft 26.3, Fabric Loader 0.19.5+, Fabric API, Java 25+, and that the server loaded this JAR |
| `Unsupported native platform` | This release is Linux x86-64 only. Do not rename an incompatible executable or change security settings to force it |
| Native diagnostic fails | Read `diagnostics.txt` privately. Missing glibc/libstdc++ symbols, execution restrictions, or a host that forbids child processes require a compatible environment; the mod stays inactive |
| Bot stays disabled | Check `enabled`, `authentication-approved`, exact profile name, and `address=hostname:port`. Blank settings are intentional defaults |
| No device prompt | Log out that account from this server first. Check the newest `bot-console.log` lines, whether credentials are already cached, and whether the supervisor auto-disabled after ten minutes |
| Microsoft code fails | It may be expired or from a previous attempt. Disable the bot, then re-enable to generate a fresh attempt. Enter the latest code only on the Microsoft page shown by that attempt |
| Wrong authenticated account | The public client exits with code 42 and the supervisor disables. Stop it, delete only its account cache, correct `username`, and approve the intended account |
| Whitelist rejection | Use `whitelist add YourMinecraftName` in your own console; retain the whitelist and online mode |
| Connection times out | Verify the client-facing address and port with a real Minecraft client. A backend allocation can differ from a public wake/proxy address. Host network rules may block a process connecting outward to its own server |
| `Invalid signature for profile public key` | Verify the timestamp fix is packaged and the native diagnostic passed. Do not disable authentication or secure-profile validation; preserve sanitized error text for a bug report |
| Bot repeatedly leaves | Host AFK rules, mod requirements, account expiry, a renamed account, network errors, or another supervisor may be responsible. Check the private log; don't increase reconnect frequency below the supported range |
| Bot does not return after you leave | Wait for the 60–600 second cooldown; check whether an account is still listed, the config is disabled, or the host stopped the whole container |
| Host still sleeps with the bot online | Its idle logic may ignore bots or require actual activity. This project cannot guarantee compatibility and should not be described as working on that host without a controlled test |
| You spawn incorrectly or lose items | Set `enabled=false` immediately, stop normally, preserve current player/world files, and compare with your pre-test backup. Never restore the entire world blindly over other players' progress |
| Server shuts down after days | This mod does not renew host retention deadlines, prevent maintenance, or stop control-plane termination. Check the provider's retention rules separately |

For public issues, share only version/platform details, the native exit code, a short sanitized error, and what you tested. **Do not attach credential caches, full logs, Microsoft device codes, server exports, player records, or worlds.** See [SECURITY.md](../SECURITY.md).

© 2026 **iAlturki**.
