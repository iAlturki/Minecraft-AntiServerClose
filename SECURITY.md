# Minecraft AntiServerClose — account safety

Project created and maintained by **[iAlturki](https://github.com/iAlturki)**.

The bot authenticates through Microsoft device login. Reusable credentials are cached on the hosting server under `config/minecraft-antiserverclose/`. The directory is made owner-only on POSIX systems, but the host operator, backups, other code running as the same account, and people with sufficient panel/SFTP access may still read it.

Never upload `botcraft_cached_credentials.json`, device codes, credentials, full logs, host exports, or worlds to GitHub. Never ask another person to paste their Microsoft password or token into this project's configuration. CI and source builds require no Minecraft login.

To disable local access, set `enabled=false`, confirm the bot has stopped, and delete its cached credentials file. If exposure is suspected, local deletion alone is insufficient: review/revoke the application's permission and sessions through the Microsoft account's security settings. See the [setup guide](docs/SETUP.md#7-stop-sign-out-or-remove-it).

Do not disclose a security issue by posting exploit details or account data in a public issue. Use **GitHub's private vulnerability reporting** if it is enabled for this repository. If it is unavailable, open a minimal issue asking the maintainer **iAlturki** for a private reporting channel, with no sensitive details.

No host uptime, player-state integrity, account isolation from the host, or retention guarantee is implied. Follow your host's policies and keep independent world backups.
