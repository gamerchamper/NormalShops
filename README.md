# NormalShops

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20+-3D852C?style=for-the-badge)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-17-F29111?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/API-Paper%20%2F%20Spigot-004BC4?style=for-the-badge)](https://docs.papermc.io/)

**NormalShops** is a [Paper](https://papermc.io/) / [Spigot](https://www.spigotmc.org/)–compatible Minecraft server plugin that adds **player-owned item shops** on chests and barrels. Players set a **price** (paid in **items**, not Vault currency), define **products** sold per purchase, manage **internal stock** or optional **stockpile** chests, and **collect earnings** at each shop. Everything is driven through **GUIs**, with an optional **villager merchant** buy UI.

| | |
| --- | --- |
| **API version** | `1.20` (see `plugin.yml`) |
| **Authors** | GameChampCrafted, Ridewithit |

## Links

| | |
| --- | --- |
| **Website & downloads** | [NormalShops on Modrinth](https://modrinth.com/plugin/normalshops) |
| **Documentation** | This README and bundled `config.yml` / `gui.yml` / `messages/*.yml` in the plugin data folder |
| **Support** | [Modrinth — NormalShops](https://modrinth.com/plugin/normalshops) (listing feedback & reports). *Optional:* add your Discord invite or GitHub Issues URL in this README when you publish them. |
| **Donate** | *Replace with your link:* [Ko-fi](https://ko-fi.com/) · [Patreon](https://www.patreon.com/) — or use [Modrinth](https://modrinth.com/plugin/normalshops) if you enable creator features there |

---

## Table of contents

1. [Links](#links)
2. [Features](#features)
3. [Screenshots (placeholders)](#screenshots-placeholders)
4. [Requirements & installation](#requirements--installation)
5. [How shops work (gameplay)](#how-shops-work-gameplay)
6. [Commands](#commands)
7. [Permissions](#permissions)
8. [Configuration (`config.yml`)](#configuration-configyml)
9. [GUI layout & icons (`gui.yml`)](#gui-layout--icons-guiyml)
10. [Messages & locales](#messages--locales)
11. [Data files & folders](#data-files--folders)
12. [Backups & recovery](#backups--recovery)
13. [Integrations](#integrations)
14. [Admin & moderation](#admin--moderation)
15. [Troubleshooting](#troubleshooting)
16. [License](#license)
17. [Contributing](#contributing)

---

## Features

> **Screenshot:** `docs/images/01-feature-overview.png` — In-world shop with display + optional stats hologram; shows the “storefront” players see.

- **Create shops** by right-clicking an **empty** single **chest** or **barrel** while holding any **sign** (not sneaking).
- **Price = item payment**: buyers pay by having matching items removed from their inventory (no Vault dependency).
- **Products**: one or more item stacks per sale; supports bundle-style multi-product listings.
- **Stock**: internal “stock chest” storage on the shop block, plus optional **stockpiles** (linked chests/barrels within range).
- **Earnings**: each successful sale increments stored earnings; owners collect **stacks of the price item** (`price amount × number of sales`) from the shop.
- **Trusted players**: grant others access to manage a shop (editor, stock, settings—same as owner for management UIs).
- **Customization**: themes/sounds, custom shop name, glass/frame displays, sale text, optional shop block swap (where allowed).  
  **Screenshot:** `docs/images/11-customize-menu.png`
- **Analytics**: per-shop lifetime stats (sales, revenue in price units, stock movement, impressions, market comparison menu).  
  **Screenshot:** `docs/images/15-shop-analytics.png`
- **Public browser**: `/shops` lists in-stock shops with optional **region filter** and **teleport**; `/viewshops` lists **your** shops and shops you’re trusted on for **remote** access.
- **Optional** private **TextDisplay** stats holograms above shops (owner/trusted only).
- **CoreProtect** logging when the plugin is present and the API is enabled.

---

## Screenshots (placeholders)

Add images under **`docs/images/`** (create the folder in the repo) and replace the markdown below with real files, or keep the table as a checklist while you capture shots.

**How to wire images on GitHub:** commit PNG or WebP files, then use:

`![Alt text](docs/images/your-file.png)`

| Suggested file | What to show |
| --- | --- |
| `docs/images/01-feature-overview.png` | Shop in the world (block, item display, optional glass/frame/sale text). |
| `docs/images/02-create-shop-gui.png` | **Create Shop** GUI: price slot, product area, Create / Back. |
| `docs/images/03-shop-access-menu.png` | **Shop access** choice (stock / editor / trade / collect earnings) for owner or trusted. |
| `docs/images/04-owner-panel.png` | Main **owner** control panel (collect, change listing, customize, settings, stockpile, etc.). |
| `docs/images/05-buy-merchant.png` | Buyer **villager merchant** UI (default `villager-trading-menu: true`). |
| `docs/images/06-buy-chest-gui.png` | Classic **Buy** chest GUI (`villager-trading-menu: false`). |
| `docs/images/07-stock-chest-editor.png` | **Stock chest** / internal stocking GUI. |
| `docs/images/08-stockpile-link.png` | **Connect stockpile** mode (tether or prompt in chat) + linked chest. |
| `docs/images/09-stockpile-browser.png` | **Linked vaults** read-only browser (if used). |
| `docs/images/11-customize-menu.png` | **Customize** menu (theme, sound, displays, name). |
| `docs/images/12-settings-menu.png` | **Settings** (notifications, stock warning, stats hologram, unlimited stock for admins). |
| `docs/images/13-public-shops-browser.png` | **`/shops`** public list + pagination / teleport entry. |
| `docs/images/14-my-shops-viewshops.png` | **`/viewshops`** “My shops” list (owned + trusted). |
| `docs/images/15-shop-analytics.png` | **Analytics** screen (revenue, sales, market compare). |
| `docs/images/16-delete-admin-menu.png` | **Shift-sneak delete** menu (owner vs admin row, op-only tools). |
| `docs/images/17-region-setpoint.png` | **`/normalshops region make`** + **`/setpoint`** corners (optional F3 or map). |

**Optional hero strip** (replace when assets exist):

```markdown
<p align="center">
  <img src="docs/images/hero-shops-row.png" alt="NormalShops hero" width="720"/>
</p>
```

---

## Requirements & installation

1. **Server**: Paper, Spigot, or a compatible Bukkit API fork.
2. **Minecraft**: aligned with declared `api-version` in `plugin.yml` (currently `1.20`). Newer server versions may work if Bukkit remains compatible.
3. **Java**: **17**.

**Install**

1. Drop `NormalShops-*.jar` into the server’s `plugins/` folder.
2. Start the server once to generate `plugins/NormalShops/` (or legacy `plugins/ClickShop/` if that folder already exists—see [Data folder resolution](#data-folder-resolution)).
3. Edit `config.yml`, `gui.yml`, and `messages/<locale>.yml` as needed. Most `config.yml` changes require a **full restart** (comments in default config state this explicitly). **`/normalshops reload`** reloads config, messages, GUI config, related managers, and backup timers **without** a full restart where implemented.

---

## How shops work (gameplay)

### Creating a shop

> **Screenshot:** `docs/images/02-create-shop-gui.png` — Full Create Shop flow (optional: split into `02a-sign-on-chest.png` + `02b-create-shop-gui.png`).

1. Place a **single** chest or barrel (not a double chest).
2. Leave the container **completely empty**.
3. Hold **any sign** in your main hand (do **not** sneak).
4. **Right-click** the chest/barrel.

If you have `normalshops.create` and are under the per-player shop limit, the **Create Shop** GUI opens: set **price** (one stack defining what one “purchase unit” costs), set **product(s)**, then confirm **Create Shop**.

**Restrictions**

- **Double chests** are rejected (`create-double-chest` message).
- **Non-empty** containers are rejected (`create-not-empty`).
- Existing shop at location is rejected (`create-shop-exist`).

### Buying

> **Screenshots:** `docs/images/05-buy-merchant.png` and/or `docs/images/06-buy-chest-gui.png` — Same shop, both buy UIs if you toggle `villager-trading-menu` for comparison.

- **Customers** (not owner/trusted) who right-click the shop open either:
  - the **villager merchant** UI (`villager-trading-menu: true`, default), or
  - the classic **Buy** chest GUI (`villager-trading-menu: false`).
- Requires `normalshops.buy`.
- The buyer must carry **enough of the price item** (same type/meta according to `full-nbt-check`). Money plugins are **not** used.
- **`buy-cooldown-seconds`**: optional per-shop-block cooldown between purchases (classic GUI + merchant UI).

### Owner / trusted access

> **Screenshots:** `docs/images/03-shop-access-menu.png`, `docs/images/04-owner-panel.png` — Access chooser then main owner tools.

- **Right-click while sneaking** with `normalshops.delete` opens the **Delete / admin** menu (see [Admin](#admin--moderation)).
- **Owner or trusted** (not sneaking for normal flow): **Shop access** menu—open **stock chest**, **editor**, **trade preview**, or **collect earnings** from the shop as appropriate.

### Stock and stockpiles

> **Screenshots:** `docs/images/07-stock-chest-editor.png`, `docs/images/08-stockpile-link.png`, `docs/images/09-stockpile-browser.png` — Stocking, linking a stockpile, optional vault browser.

- **Internal stock**: items stored in the shop’s virtual stock chest (via GUI) supply sales.
- **Stockpiles**: extra chests/barrels linked in the world. With `protect-stockpiles: true`, only the owning shop’s context can open them; `normalshops.bypass-stockpile` overrides for staff.

### Earnings

- Each sale increments an **earnings counter** (not Vault). Collecting from the shop pays out **`price` item × `price.getAmount()` × `sales`**.
- **Admin shops** (`admin` flag in data—normally set via staff tools) **do not** accrue earnings (`incrementEarnings` is a no-op for admin shops).

### Breaking shops and stockpiles

- Owners break their own shop through the **Delete Shop** flow (see messages `break-confirm`).
- Operators breaking others’ blocks should use **shift + right-click → Delete Shop** (`break-operator-confirm`).
- Stockpiles cannot be broken while still linked (`stockpile-remove-first`).

---

## Commands

> **Screenshots:** `docs/images/13-public-shops-browser.png` — `/shops` list; `docs/images/14-my-shops-viewshops.png` — `/viewshops`; optional `docs/images/chat-teleport-hint.png` if you show chat clickables.

| Command | Aliases | Permission | Description |
| --- | --- | --- | --- |
| `/normalshops` | `/nshops` | (subcommand-specific) | Root command; see subcommands below. |
| `/normalshops view` | — | `normalshops.viewshops` | Same as `/viewshops`: open **My Shops** GUI (owned + trusted). |
| `/viewshops` | — | `normalshops.viewshops` | Opens **My Shops** list. |
| `/viewshops open <worldUuid> <x> <y> <z>` | — | `normalshops.viewshops` | Internal/lore-driven remote open: validates UUID world and block coords, then opens **Shop access** if you own or are trusted. |
| `/shops` | — | `normalshops.shops` | Opens public **in-stock** shop browser (respects `shops-command-enabled` and optional regions). |
| `/shops <query…>` | — | `normalshops.shops` | Same as `/shops item <query…>`: filter browser to shops selling fuzzy-matched item **materials**. |
| `/shops item <query…>` | — | `normalshops.shops` | Explicit item filter form. |
| `/normalshops reload` | — | `normalshops.reload` | Reloads settings, messages, `gui.yml`, hologram manager, shop backup service config, and restarts auto-backup scheduling. |
| `/normalshops version` | — | `normalshops.version` | Prints jar version + Modrinth update comparison (needs release-style version strings like `26.1.2-v1.0.9`). |
| `/normalshops delete` | — | `normalshops.delete` | **Player only**: raycast up to **12** blocks; deletes the targeted **registered** shop (operator/staff use). |
| `/normalshops restore` | — | `normalshops.restore` | **Player only**: stand on/look at **single chest** or **barrel** (not double chest); restores latest `shop-backups.yml` snapshot for that block if no active shop exists. |
| `/normalshops stats-holograms list` | — | `normalshops.stats-holograms` | Reports private stats `TextDisplay` entities near shops. |
| `/normalshops stats-holograms cleanup` | — | `normalshops.stats-holograms` | Removes drifted/duplicate stats holograms near loaded shops. |
| `/normalshops region make` | — | `normalshops.region` | Starts **region editor**: you must run `/setpoint` twice at floor corners. |
| `/normalshops region cancel` | — | `normalshops.region` | Cancels region editor session. |
| `/setpoint` | — | `normalshops.region` | Records a corner for `/shops` menu region (after `region make`). Saves to `config.yml` → `shops-menu-regions`. |

**Usage hints**

- Tab completion on `/normalshops` exposes subcommands the sender is allowed to use.
- If `/shops` is disabled in config, players with permission still hit `feature-disabled` unless they are not the target audience—actually **all** players get the disabled message when `shops-command-enabled` is false.

---

## Permissions

| Permission | Default | Meaning |
| --- | --- | --- |
| `normalshops.create` | `true` | Create new shops via sign interaction. |
| `normalshops.buy` | `true` | Buy from others’ shops. |
| `normalshops.customize` | `true` | Use customize menu (displays, name, block, theme—subject to sub-perms). |
| `normalshops.display` | `true` | Build **custom displays** (also needs `normalshops.customize`). |
| `normalshops.sale-text` | `true` | Add **sale text** on displays (also needs `normalshops.customize`). |
| `normalshops.stockpile` | `true` | Connect/manage **stockpiles**. |
| `normalshops.delete` | `op` | Opens **shift-sneak** delete menu; **destroying another player’s shop** from that menu still additionally requires **operator** (see `AdminGuiSecurity`). |
| `normalshops.bypass-stockpile` | `op` | Open **any** stockpile when `protect-stockpiles` is on. |
| `normalshops.unlimited-stock` | `op` | Toggle **unlimited stock** in shop settings (no earnings/notifications while on). |
| `normalshops.coreprotect-test` | `op` | Reserved / diagnostic hooks for CoreProtect API tests. |
| `normalshops.coreprotect-debug` | `op` | Debug logging for CoreProtect integration. |
| `normalshops.reload` | `op` | `/normalshops reload`. |
| `normalshops.version` | `true` | `/normalshops version`. |
| `normalshops.update-notify` | `op` | Join notification when a newer Modrinth release is detected (ops always considered). |
| `normalshops.stats-holograms` | `op` | `/normalshops stats-holograms …`. |
| `normalshops.restore` | `op` | `/normalshops restore`. |
| `normalshops.viewshops` | `true` | `/viewshops`, `/normalshops view`, remote open. |
| `normalshops.shops` | `true` | `/shops` browser + teleports. |
| `normalshops.region` | `op` | `/normalshops region …` and `/setpoint`. |

**Wildcards**

- Grant `normalshops.*` only if you intend full admin; several nodes are intentionally dangerous (`unlimited-stock`, `bypass-stockpile`, `delete`).

---

## Configuration (`config.yml`)

Key settings (defaults from shipped `config.yml`; `config-version` tracks migrations).

| Key | Type | Description |
| --- | --- | --- |
| `config-version` | int | Internal schema version (do not remove). |
| `language` | string | Locale code for `messages/<language>.yml` (e.g. `en_US`). |
| `check-update` | bool | Query Modrinth on startup; drives join notifications for ops / `update-notify`. |
| `protect-stockpiles` | bool | Restrict stockpile access to owner context; bypass with `normalshops.bypass-stockpile`. |
| `display-view-range` | int | How far shop display entities are visible (blocks/chunks per implementation). |
| `display-out-of-stock` | bool | When `true`, empty shops show **bedrock** and hide item/sale display until restocked; real block type is stored and restored. |
| `private-shop-stats-holograms` | bool | Floating stock/earnings text above shops for **owner + trusted** only. |
| `stats-hologram-auto-cleanup-ticks` | int | Tick interval to clean stacked/drifted stats displays (`0` = manual only). |
| `buy-cooldown-seconds` | int | Per-shop purchase cooldown (`0` = off). |
| `operator-delete-shop-collect` | bool | If someone with `delete` removes a shop, whether earnings auto-collect rules extend to operator deletes. |
| `visualize-tethers` | bool | Show rope-style visuals while linking stockpiles. |
| `max-connection-distance` | int | Max link distance between a shop and its stockpiles. |
| `shop-limit-per-player` | int | Max shops per UUID. |
| `stockpile-limit-per-shop` | int | Max stockpiles per shop. |
| `block-piston` | bool | When `true`, piston events are listened to (mitigate shop blocks moved by pistons). |
| `full-nbt-check` | bool | Stricter item matching for price/stock/product (`true` = full NBT similarity). |
| `villager-trading-menu` | bool | `true` = merchant UI for buyers; `false` = classic chest GUI. |
| `gui-item-leak-sweep-ticks` | int | Interval to strip leaked GUI “fake” items from inventories (`0` = off). |
| `physical-item-proxy-enabled` | bool | Advanced anti-dupe proxy item pipeline for stock/earnings (default off). |
| `physical-item-proxy-max-units-per-stack` | int | Max units per proxy stack when proxies enabled. |
| `physical-item-proxy-resolve-ticks` | int | Tick interval to resolve/truncate proxy items (`0` = off). |
| `recover-shop-files` | bool | **Danger:** one-shot recovery mode resyncing `data.yml` and `shops/*.yml`; auto-disables after run; requires restart workflow per comments. |
| `auto-data-backup-enabled` | bool | Periodic copies to `backups/auto/<timestamp>/`. |
| `auto-data-backup-interval-minutes` | int | Snapshot interval (1–1440). |
| `auto-data-backup-max-snapshots` | int | Retention count for auto backups. |
| `shops-command-enabled` | bool | Master toggle for `/shops`. |
| `shops-menu-teleport-cooldown-seconds` | int | Cooldown between teleports from public browser. |
| `shops-menu-regions` | list | Horizontal **XZ** regions; when **non-empty**, only shops whose block lies inside **any** region appear in `/shops`. Empty = no filter (still in-stock only). |

### `shops-menu-regions` examples

**Modern corners format** (two or more corners; Y ignored):

```yaml
shops-menu-regions:
  - corners:
      - { world: world, x: -100, z: -100 }
      - { world: world, x: 100, z: 100 }
```

**Legacy min/max** (same rectangle; Y keys ignored if present):

```yaml
shops-menu-regions:
  - world: world
    min-x: -100
    max-x: 100
    min-z: -100
    max-z: 100
```

After editing regions manually, **restart** or use **`/normalshops reload`** so `ShopsMenuRegions` reloads.

---

## GUI layout & icons (`gui.yml`)

> **Screenshot:** `docs/images/gui-layout-reference.png` — Optional: one menu with slot roles labeled in an image editor (or side-by-side: `gui.yml` snippet + matching in-game GUI).

- **`gui-config-version`**: tracked for forward-compatible merges on reload/updates.
- **`icons.*`**: Bukkit `Material` names for buttons/backgrounds (see [Spigot Material javadoc](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)).
- **`layouts.*`**: ASCII grids (`X` = filler) with **role keys** mapping characters to buttons (`price-product` / `trading`, `owner`, `settings`, `public-shops`, `my-shops`, etc.).

**Reload**: `/normalshops reload` merges GUI config. Comments note legacy fallback: a `shop-gui` section in `config.yml` can still supply `trading-layout` / `owner-layout` if keys are missing from `gui.yml`.

---

## Messages & locales

- Files live in `plugins/NormalShops/messages/<locale>.yml` (or under resolved data folder).
- Bundled locales in the jar: **`en_US`**, **`de_DE`**, **`tr_TR`**. Other `language` codes create a new file seeded from English on first run.
- **`messages-version`**: bumped when new keys ship; missing keys merge from jar defaults on startup.
- Supports **`&` color codes** and placeholders like `{owner}`, `{location}`, `{price}`, `{seconds}`, etc. (see `en_US.yml` for the full set).

**Reload**: `/normalshops reload` reloads message files.

---

## Data files & folders

All paths are under the **resolved data folder** (see below).

| Path | Purpose |
| --- | --- |
| `config.yml` | Main plugin settings. |
| `gui.yml` | Layouts and icon materials. |
| `messages/<locale>.yml` | Chat/GUI strings. |
| `data.yml` | Serialized `shop-manager` (global UUID maps, stockpile map, warnings). |
| `shops/<player-uuid>.yml` | Per-owner `player-shop-manager` (that player’s shops). |
| `shop-backups.yml` | Archive of shop snapshots for `/normalshops restore`. |
| `backups/auto/<yyyy-MM-dd_HH-mm-ss>/` | Periodic full copies when auto backup is enabled. |

### Data folder resolution

- Default: `plugins/NormalShops/`.
- **Legacy migration**: if `plugins/ClickShop/` **already exists** as a directory, the plugin **uses that folder instead** for all data (log line: `Using ClickShop data folder: …`).

---

## Backups & recovery

- **Automatic**: `auto-data-backup-*` copies `data.yml`, `shops/*.yml`, and `shop-backups.yml` on a timer.
- **Manual restore**: `/normalshops restore` (permission `normalshops.restore`) uses `shop-backups.yml` + targeted chest/barrel.
- **`recover-shop-files`**: emergency resync path documented in `config.yml`—use only when you understand the risk.

---

## Integrations

### CoreProtect (optional)

- If **CoreProtect** is installed and its API enables, NormalShops logs creates, deletes, buys, and various staff actions to CoreProtect and maintains an internal **shop history** list for admin menus.
- If CoreProtect is absent, informational log lines note logging is skipped—shops still work.

### Modrinth updates

- HTTP check to Modrinth project **`DNLfdXJm`** (`check-update`).
- **`/normalshops version`** shows running version vs latest parsed semver (expects version strings like `26.1.2-v1.0.9`).
- **Join notifications** for ops and `normalshops.update-notify` when the server is behind.

### Vault / economy plugins

- **Not used.** Price and earnings are always **item-based** (`ItemStack`).

---

## Admin & moderation

> **Screenshot:** `docs/images/16-delete-admin-menu.png` — Delete menu as **non-op** (owner path) vs **op** (admin buttons visible); blur sensitive names if needed.

### Shift-sneak delete menu

- Players with **`normalshops.delete`** can shift–right-click a shop to open **DeleteShopMenu**.
- **Owner** sees delete + back; **admin row** (force owner change, force edit, delete other’s shop, history) is visible but **admin actions require `normalshops.delete` AND `Player.isOp()`** (`AdminGuiSecurity`). Attempts without op are logged as **exploit detected** and broadcast to online ops.

### `/normalshops delete`

- Staff with `normalshops.delete`: look at a shop (≤ 12 blocks), run command; runs same deletion path with CoreProtect logging.

### Unlimited stock

- **`normalshops.unlimited-stock`**: allows toggling unlimited stock in settings. While enabled, the shop does **not** collect earnings or buy notifications (see button lore in `en_US.yml`).

### Stats holograms

> **Screenshot:** `docs/images/12-settings-menu.png` (per-shop toggle) + optional `docs/images/stats-hologram-above-shop.png` (in-world TextDisplay as owner/trusted).

- Enable globally with `private-shop-stats-holograms`; per-shop toggle in settings GUI.
- Maintenance: `/normalshops stats-holograms list|cleanup` (`normalshops.stats-holograms`).

---

## Troubleshooting

| Symptom | Things to check |
| --- | --- |
| Cannot create shop | Empty single chest/barrel? Holding a **sign**? Not sneaking? Under `shop-limit-per-player`? Has `normalshops.create`? |
| “You don’t have enough money” (`buy-no-money`) | Price is **items**, not coins—buyer needs the actual price items in inventory. |
| Stockpile won’t open | `protect-stockpiles` + wrong player? Staff needs `normalshops.bypass-stockpile`. |
| `/shops` empty | No in-stock shops, or **regions** exclude your mall, or `shops-command-enabled: false`. |
| Update check / version compare odd | Use a **release-style** jar version (`…-v1.0.x`) so semver comparison matches Modrinth. |
| GUI items stuck in inventory | `gui-item-leak-sweep-ticks` > 0 (default 100); `/normalshops reload` after edits. |
| Shops disappeared after piston | Avoid moving shop blocks; `block-piston` registers protection listener when true. |
| Full config changes ignored | Many options require **server restart** (per default `config.yml` header). |

---

## License

If the repository contains a `LICENSE` file, follow that file. Otherwise, confirm licensing with the maintainers before redistribution.

---

## Contributing

Issues and pull requests are welcome on the repository that hosts this `README.md`. When reporting bugs, include **server software + version**, **NormalShops jar version**, relevant **`config.yml` snippets** (redact secrets), and **steps to reproduce**.
