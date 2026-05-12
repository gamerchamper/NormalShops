# NormalShops

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20+-3D852C?style=for-the-badge)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-17-F29111?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/API-Paper%20%2F%20Spigot-004BC4?style=for-the-badge)](https://docs.papermc.io/)

**NormalShops** is a [Paper](https://papermc.io/) / [Spigot](https://www.spigotmc.org/) plugin for **player-owned item shops** on chests and barrels: item-based prices, stock and stockpiles, paged earnings, GUIs or villager trading, analytics, trusted players, and optional **CoreProtect** logging.

| | |
| --- | --- |
| **API version** | `1.20` (`plugin.yml`) |
| **Authors** | GameChampCrafted, Ridewithit |

---

## Documentation

| Doc | What you get |
| --- | --- |
| **[GUIDE.md](GUIDE.md)** | Full walkthrough: screenshots, feature tables, creating/buying/stock/earnings/piles, admin & safety, permissions, commands, config, support |
| **This README** | Quick install, links, and pointers |

---

## Links

| | |
| --- | --- |
| **Website & downloads** | [NormalShops on Modrinth](https://modrinth.com/plugin/normalshops) |
| **Source & screenshots** | [github.com/gamerchamper/NormalShops](https://github.com/gamerchamper/NormalShops) · [`imgs/`](https://github.com/gamerchamper/NormalShops/tree/main/imgs) |
| **Support** | [GitHub Issues](https://github.com/gamerchamper/NormalShops/issues) · [Modrinth](https://modrinth.com/plugin/normalshops) |
| **Donate** | *Add your link:* [Ko-fi](https://ko-fi.com/) · [Patreon](https://www.patreon.com/) — or Modrinth creator tools on the project page |

---

## Preview

<p align="center">
<a href="GUIDE.md" title="Full guide"><img width="260" alt="NormalShops storefront" src="https://raw.githubusercontent.com/gamerchamper/NormalShops/main/imgs/shop.png" /></a>
<a href="GUIDE.md" title="Full guide"><img width="320" alt="/shops browser GUI" src="https://raw.githubusercontent.com/gamerchamper/NormalShops/main/imgs/shops-command.webp" /></a>
</p>
<p align="center">
<a href="GUIDE.md" title="Full guide"><img width="640" alt="GUI overview" src="https://raw.githubusercontent.com/gamerchamper/NormalShops/main/imgs/gui-overview.webp" /></a>
</p>
<p align="center">
<a href="GUIDE.md" title="Full guide"><img width="560" alt="How to make a shop (animated)" src="https://raw.githubusercontent.com/gamerchamper/NormalShops/main/imgs/imgs_make-shop%281%29.gif" /></a>
</p>

*Admin menu and longer explanations: **[GUIDE.md](GUIDE.md)**.*

---

## Quick install

1. **Java 17** · **Paper / Spigot 1.20.x** (see `plugin.yml` `api-version`).
2. Put **`NormalShops-*.jar`** in `plugins/`.
3. Restart the server. Data folder: `plugins/NormalShops/` (or legacy `plugins/ClickShop/` if it already exists).
4. Edit **`config.yml`**, **`gui.yml`**, **`messages/<locale>.yml`**. Many `config.yml` options need a **full restart**; **`/normalshops reload`** (op) reloads config, GUI, messages, and related services where supported.

Optional: **[CoreProtect](https://www.spigotmc.org/resources/coreprotect.8631/)** for richer audit trails.

---

## At a glance

- **Create:** any **sign** + right-click **empty** single **chest** or **barrel** (not sneaking).
- **Economy:** **Item** prices and payouts (no Vault).
- **Buyers:** classic GUI or **villager merchant** (`villager-trading-menu`).
- **Staff:** shift-sneak hub with history, emulate sale, force edit / transfer — sensitive actions need **op** in addition to `normalshops.delete`.
- **Commands:** `/shops`, `/viewshops`, `/normalshops …` — see **[GUIDE.md § Commands](GUIDE.md#commands)** or `plugin.yml`.

---

## License

If the repository contains a `LICENSE` file, follow that file. Otherwise, confirm licensing with the maintainers before redistribution.

---

## Contributing

Issues and pull requests are welcome. Include **server software + version**, **NormalShops jar version**, and **steps to reproduce**. Prefer focused diffs that match existing style.

For product copy, screenshots, and feature narrative, extend **[GUIDE.md](GUIDE.md)** so this README stays a short entry point.
