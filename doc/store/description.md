# Weak Spot Mining

Brings Fortnite-style **weak spot harvesting** to Minecraft 1.12.2 (Forge), 1.20.1 (Forge) and 1.21.1 (NeoForge).

While you mine, farm, ride, draw a bow and more, a glowing circle — a **weak spot** — appears. Aim at it to speed things up. Hit spots in a row to build a **combo**: the hit sound climbs the scale and the boost grows.

## Features

- **19 kinds of weak spots**, from mining and farming to travel, combat, falling and even sleeping
- **Target practice**: craft a Weak Spot Target and play a 30-second aiming mini-game anywhere. Your best score unlocks new marker looks (star shape, silver sparkle, gold sparks, rainbow)
- **Weak Spot Shards**: hits sometimes drop a shard (more often at high combos). Craft a **Combo Hourglass** (combos last longer for 5 minutes), a **Weak Spot Charm** (bigger weak spots in your off hand) or a **Weak Spot Monocle** (shows where each spot moves next)
- **Combo system** with a rising hit sound and a per-kind multiplier shown on screen
- **Stats, milestones and advancements**: hits per kind, rewards at milestones (announced to everyone on the server), an advancement tab and a server leaderboard (`/weakspot top`)
- **Customizable**: turn each kind on or off, change marker colors, shapes and size, pick hit sounds and scales
- **In-game guide book** that explains every kind (English and Japanese)
- **Multiplayer friendly**: see other players' hits, and server admins can tune or disable each kind

## Kinds of weak spots

| Kind | How | Effect |
|---|---|---|
| Mining | Aim at a block (the spot shows before you dig), then hold left click | Digs faster (even faster with a combo); break a block you hit to start the next one at once, so it stays useful with fast tools; repairs your tool a little when the block breaks |
| Growth | Hold right click with an empty hand on a growing crop or sapling (sneak to use it on any block that changes over time, including modded ones) | Grows it faster |
| Harvest | Hold right click with an empty hand on a ripe crop | Harvests and replants it |
| Machine | Sneak and hold right click with an empty hand on a furnace or other machine | Speeds the machine up |
| Animal | Hold right click with an empty hand on an animal (sneak for horses, villagers, etc.) | Speeds up growing up, breeding cooldown, wool, eggs (and armadillo scutes on 1.21.1) and villager trade restocking |
| Fishing | Hold a fishing rod and aim at the spot near the bobber | Fish bite sooner |
| Bow | Aim at the spot while drawing a bow (and a trident or crossbow on 1.20.1 and 1.21.1) | Draws faster; after a full draw, arrows hit harder (a crossbow keeps the bonus until it fires) |
| Melee | Hold a sword or axe (or a mace on 1.21.1) near enemies and aim at the spot | Charges up your next attack; with a sword, the sweep hits the whole crowd too |
| Throwing | Hold ender pearls, snowballs, etc. (and wind charges on 1.21.1) and aim at the spot | Your next throw flies faster and farther |
| Eating | Aim at the spot while eating or drinking | Finishes sooner |
| Vehicle | Aim at the spot while riding a horse, pig, minecart or boat (and a camel, strider, chest boat or raft on 1.20.1 and 1.21.1) | Moves faster |
| Ladder | Aim at the spot while climbing a ladder or vines | Climbs faster |
| Sprint | Aim at the spot while sprinting | Runs faster |
| Elytra | Aim at the spot while flying | Launches you where you look |
| Portal | Aim at the spot while standing in a nether portal | Travels sooner |
| Swim | Aim at the spot while swimming | A short dash, then faster swimming |
| Fall | Aim at the spot while falling from a height | Takes less fall damage |
| Sleeping | Move the cursor over the marker on the sleep screen at night | Morning comes sooner |
| Enchanting | Click the marker on the enchanting table screen | Rerolls the three offers (costs nothing) |

Growth, harvest, machine and animal spots also appear while you hold an item that does nothing on right click (pickaxes, sticks, ingots...).

## Controls

| Key | Action |
|---|---|
| **K** | Weak Spot menu (stats, hit sounds, per-kind on/off, colors and shapes, settings, guide book) |
| **HOME** | Turn all weak spots on or off (for example while building) |
| **Right click** (holding a Weak Spot Target) | 30-second target practice |
| **Right click** (holding a Combo Hourglass) | Combos last longer for 5 minutes |

Keys can be changed under Options → Controls → Weak Spot Mining.

## Installation

Supported versions (Minecraft Java Edition):

| Minecraft | Loader | Developed and tested with | Notes |
|---|---|---|---|
| **1.12.2** | Forge | 14.23.5.2860 | Much older Forge builds may not run every feature |
| **1.20.1** | Forge (47.1.32 or later) | 47.1.32 | |
| **1.21.1** | NeoForge | 21.1.264 | |

Download the file for your Minecraft version; all of them have the same features.

- **Single player**: install it on your client.
- **Multiplayer**: install it on the server **and** on every player's client, all on the same minor version (for example 1.13.x) and the same Minecraft version.

## Settings

Open the Mods menu, or edit `config/weakspot.cfg` (1.12.2) / `config/weakspot-server.toml` and `config/weakspot-client.toml` (1.20.1, 1.21.1). Settings are grouped into server and client categories, and every setting has a description in English. Server settings (such as turning off a kind) are sent to connected players automatically.

## Bug reports

Type `/weakspot bug` in chat. You get a link to a GitHub issue with your environment already filled in.

- Issues: https://github.com/zeusisgood/weakspot/issues
- Source code: https://github.com/zeusisgood/weakspot

## License

[MIT License](https://github.com/zeusisgood/weakspot/blob/main/LICENSE).

- **Modpacks are welcome.** No permission needed. If you add it to a pack, I'd love to hear about it through a [GitHub issue](https://github.com/zeusisgood/weakspot/issues/new/choose) ("Modpack notice"), but it's entirely optional.
- When sharing the mod, a link to this page is appreciated.

*Not affiliated with Epic Games or Mojang. "Fortnite" is a trademark of Epic Games, Inc.*
