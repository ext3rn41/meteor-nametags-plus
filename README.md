![Banner](banner.png)

Meteor addon for prettier and more informative nametags.  
Extends the default Meteor `Nametags` module with custom text layout, distance-based scaling, item/enchant rendering, and a totem pop counter.

> License : GPL-3.0

---

## Features

### General
- Select which entities to render nametags for (players, items, item frames, TNT, living entities, etc.).
- Ignore filters:
  - Ignore self in 3rd person / freecam.
  - Ignore friends.
  - Ignore “bot” players (no valid GameMode).
- Culling:
  - Only render nametags within a configurable range.
  - Limit the maximum number of nametags rendered.
- Distance-based scaling:
  - `min-scale` / `max-scale` with smooth interpolation by distance.

### Player Nametags
- Health:
  - Integer or float mode.
  - Color based on percentage (green → amber → red).
- GameMode:
  - Short codes (`S`, `C`, `A`, `Sp`) or `BOT` fallback.
  - Optional square brackets.
- Ping:
  - Show ping in ms, optional brackets.
- Distance:
  - Show distance in meters.
  - Flat color or distance-based gradient.
- Custom text order:
  - Fully configurable using tokens:
    - `%GAMEMODE%`, `%USERNAME%`, `%HEALTH%`, `%PING%`, `%DISTANCE%`, `%TOTEM%`
  - Example:
    - `[%GAMEMODE%] %USERNAME% %HEALTH% %PING% %DISTANCE% %TOTEM%`

### Item Display
- Show equipped items above the nametag:
  - Main hand, off-hand, full armor.
- Options:
  - Ignore hand items.
  - Ignore empty slots.
  - Configurable spacing between items.
- Durability:
  - None / total / percentage.
- Enchantments:
  - Toggle on/off.
  - Whitelist which enchants are shown.
  - Trim name length, position (above / on top), and text scale.

### Other Entities
- Items:
  - Show item name and (optionally) stack count.
- TNT & TNT Minecarts:
  - Show remaining fuse time (`h / m / s`).
- Generic living entities:
  - Entity type + health.
- Generic entities:
  - Entity type only.

### Totem Pop Counter
- Tracks how many times a player has popped a Totem.
- Counter decreases per pop:
  - `-1`, `-2`, `-3`, ...
- Shown through `%TOTEM%` token in the nametag.
- Hidden if the player has not popped any totems.
- Rendered in red.

---

## Installation

1. Install [Fabric](https://fabricmc.net/) & [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version.
2. Install [Meteor Client](https://meteorclient.com/).
3. Download or build the `meteor-nametags-plus` jar.
4. Put the jar into your `mods` folder (alongside Meteor).
5. Launch the game and enable `NametagsPlus` in the Meteor GUI.

---

## Building from Source

```bash
git clone https://github.com/ext3rn41/meteor-nametags-plus.git
cd meteor-nametags-plus
./gradlew build
