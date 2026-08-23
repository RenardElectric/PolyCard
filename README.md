<p align="center">
  <img src="src/main/resources/assets/polycard/icon.png" alt="PolyCard icon" width="256"  style="image-rendering: pixelated;">
</p>

<h1 align="center">PolyCard</h1>

<p align="center">
  <strong>Collectible card system - find cards and equip them for buffs.</strong>
</p>

<p align="center">
  <a href="https://github.com/RenardElectric/polycard/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/RenardElectric/polycard/build.yml?branch=master&amp;label=build"></a>
  <img alt="Minecraft 26.2" src="https://img.shields.io/badge/Minecraft-26.2-3C8527">
  <img alt="Fabric Loader 0.19.3 or newer" src="https://img.shields.io/badge/Fabric%20Loader-0.19.3%2B-DBD0B4">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&amp;logoColor=white">
  <a href="LICENSE"><img alt="MIT License" src="https://img.shields.io/github/license/RenardElectric/polycard"></a>
</p>

<p align="center">
  <a href="#features">Features</a> ·
  <a href="#card-collection">Cards</a> ·
  <a href="#rarity-system">Rarities</a> ·
  <a href="#commands">Commands</a> ·
  <a href="#installation">Installation</a> ·
  <a href="#building-from-source">Building</a>
</p>

PolyCard is a Fabric mod that turns exploration, combat, animal interactions and structure loot into
a collectible-card progression system. Each card grants themed effects, stronger rarities inherit the
effects below them and players can equip up to five cards at once.

## Features

- **24 card families** split across Passive, Neutral, Hostile and Misc groups.
- **Themed acquisition events** including breeding, taming, mob kills, summons and structure loot.
- **Five-rarity progression** from Common to Legendary, with each card supporting its own subset.
- **Stacking card powers:** a card includes the effects of its rarity and every lower supported rarity.
- **Five-card loadouts** managed through an in-game equipment GUI or by using a card directly.
- **Card upgrading:** combine ten matching cards into one card of the next supported rarity.
- **Collection advancements** for individual rarities, complete card families, groups and the full set.
- **Mod fully server-side**

## How it plays

1. **Trigger a card event.** Breed, tame, fight, summon, explore or loot, according to a card's theme.
2. **Roll its supported rarities.** Every rarity gets an independent chance; the highest success wins.
3. **Equip the card.** Use the card directly or open `/polycard equip` to manage up to five slots.
4. **Build and improve a loadout.** Collect duplicates, combine stacks of ten and pursue advancements.

> [!TIP]
> Run `/polycard info <cardGroup> <cardType>` in-game for a card's acquisition condition, supported
> rarities, configured roll chances and effect descriptions.

## Card collection

| Group       | Card type IDs                                                                     |
|-------------|-----------------------------------------------------------------------------------|
| **Passive** | `cow`, `squid`, `chicken`, `bat`, `horse`, `turtle`                               |
| **Neutral** | `iron_golem`, `enderman`, `piglin`, `zombified_piglin`, `bee`, `wolf`             |
| **Hostile** | `ender_dragon`, `wither`, `zombie`, `creeper`, `elder_werewolf`, `alpha_werewolf` |
| **Misc**    | `inventory`, `life`, `totem`, `lucky`, `nether`, `spectator`                      |

Card types do not all begin at Common or support every rarity. The in-game info command is the
authoritative guide for each card's available tiers and effects.

## Rarity system

The global rarity order is:

`Common` → `Uncommon` → `Rare` → `Epic` → `Legendary`

A card at a given rarity includes that rarity's effect plus all effects from its lower supported tiers.
Acquisition uses a separate random roll for every supported rarity, evaluated from lowest to highest:

1. A rarity succeeds when its roll is lower than its configured probability.
2. A later, higher successful rarity replaces any earlier selection.
3. The highest successful rarity is awarded; if every roll fails, no card is awarded.

> [!IMPORTANT]
> A configured probability is an **independent roll chance**, not the final probability of receiving
> exactly that rarity. Higher successful rolls replace lower ones.

<details>
<summary><strong>How final rarity probabilities are calculated</strong></summary>

For rarity `i` with configured probability `pᵢ`:

```text
P(exactly rarity i) = pᵢ × product(1 - pⱼ) for every higher rarity j
P(no card)          = product(1 - pⱼ) for every supported rarity j
```

For independent **20% Common** and **7% Uncommon** rolls:

| Outcome  | Annouced chance |            Calculation | Final chance |
|----------|----------------:|-----------------------:|-------------:|
| Common   |         **20%** |       `20% × (1 - 7%)` |    **18.6%** |
| Uncommon |          **7%** |                   `7%` |       **7%** |
| No card  |           **-** | `(1 - 20%) × (1 - 7%)` |    **74.4%** |

The total chance to receive any card is therefore **25.6%**.

</details>

## Equipment and progression

- Equip up to **five cards** through `/polycard equip`.
- Use a card item directly to equip it without opening the command first.
- Hold at least **ten identical cards in the main hand** and run `/polycard combine` to create the next
  rarity. Cards already at their highest supported rarity cannot be combined further.
- Equipped cards are stored with the world per player UUID and survive server restarts.
- Active cooldowns and scheduled runtime effect state are session-only and reset when the server stops.

## Commands

| Command                                                         | Access      | Description                                                                                   |
|-----------------------------------------------------------------|-------------|-----------------------------------------------------------------------------------------------|
| `/polycard`                                                     | Everyone    | Show the installed PolyCard version, authors, and description.                                |
| `/polycard help`                                                | Everyone    | List only the commands available to the caller.                                               |
| `/polycard info <cardGroup> <cardType>`                         | Everyone    | Show acquisition, rarity chances, and effects for one card type.                              |
| `/polycard equip`                                               | Players     | Open your five-slot equipment manager.                                                        |
| `/polycard equip <player>`                                      | Gamemasters | Open another online player's equipment manager.                                               |
| `/polycard combine`                                             | Players     | Consume ten matching cards held in the main hand and award the next rarity.                   |
| `/polycard cooldown`                                            | Players     | Show active card-effect cooldowns in ticks.                                                   |
| `/polycard give <players> <cardGroup> <cardType> [rarityLevel]` | Gamemasters | Give a supported card to one or more players. Without a rarity, uses the card's minimum tier. |
| `/polycard test <cardGroup> <cardType> [cardsNumber]`           | Gamemasters | Simulate rarity rolls. Defaults to 1,000; accepts 1–10,000.                                   |

> [!NOTE]
> Command arguments provide tab completion for valid groups, card types and rarities where applicable.

## Installation

### Requirements

| Component     | Current requirement                                            |
|---------------|----------------------------------------------------------------|
| PolyCard      | `1.0.0`                                                        |
| Minecraft     | `26.2`                                                         |
| Java          | `25` or newer                                                  |
| Fabric Loader | `0.19.3` or newer                                              |
| Fabric API    | `0.158.0+26.2` or newer compatible build                       |
| SGUI          | Bundled inside the PolyCard JAR; no separate download required |

### Install a release

1. Install the matching [Fabric Loader](https://fabricmc.net/use/) profile or dedicated server.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) in the `mods` directory.
3. Download the matching PolyCard JAR from [GitHub Releases](https://github.com/RenardElectric/polycard/releases).
4. Place the PolyCard JAR in the `mods` directory.
5. For multiplayer, use the matching PolyCard version on the server.

> [!IMPORTANT]
> Gameplay state and effects are all server-side, but the JAR also contains PolyCard's textures
> and other assets. To avoid missing textures, the server must provide a matching texture pack to
> clients. [Polymer](https://modrinth.com/mod/polymer)'s server side texture pack generator can be used for this.

## Data and configuration

- PolyCard currently has **no user-editable configuration file**.
- Equipped-card data is saved inside the world as Minecraft `SavedData`, keyed by player UUID.
- Invalid or over-limit stored equipment is validated and repaired when loaded.
- Cooldowns, scheduled tasks and temporary effect state are not persisted across server restarts.

Back up the world as usual before removing the mod or moving a save between incompatible versions.

## Building from source

### Prerequisites

- Git
- JDK 25
- No system Gradle installation is required; the repository includes the Gradle 9.7 wrapper.

Clone the repository:

```bash
git clone https://github.com/RenardElectric/polycard.git
cd polycard
```

Generate data before building so generated advancements and item models match the Java definitions.

<details open>
<summary><strong>Windows PowerShell</strong></summary>

```powershell
.\gradlew.bat runDatagen --stacktrace
.\gradlew.bat build --stacktrace
```

</details>

<details>
<summary><strong>Linux / macOS</strong></summary>

```bash
chmod +x ./gradlew
./gradlew runDatagen --stacktrace
./gradlew build --stacktrace
```

</details>

Build artifacts are written to `build/libs/`. The GitHub Actions build follows the same sequence:
`runDatagen` first, then `build`.

### Repository layout

| Path                                       | Purpose                                                                               |
|--------------------------------------------|---------------------------------------------------------------------------------------|
| `src/main/java/polycube/polycard`          | Server runtime, card definitions and effects, commands, events, GUIs, and persistence |
| `src/client/java/polycube/polycard/client` | Data generation entrypoint and generated-model helpers                                |
| `src/main/resources`                       | Fabric metadata, mixins, access widener, textures, models, and other packaged assets  |
| `.github/workflows`                        | Build and release automation                                                          |
| `.github/scripts`                          | Shared CI metadata, release, and summary scripts                                      |

Before submitting a change, run both build commands above and confirm there are no errors.

## Authors and license

PolyCard is made by **RenardElectric** and **Timeo** for the PolyCube Team.

This project is available under the [MIT License](LICENSE).
