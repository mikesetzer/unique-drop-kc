# Unique Drop KC

RuneLite plugin that records official killcounts when collection-log uniques drop from bosses and raids, and shows them on hover in a side panel.

Example tooltip: `Last received at 412, 400, 224 kc` (up to 5 entries, newest first).

History is stored per RuneLite profile via the config system.

## Requirements

- **Loot Tracker** enabled (`LootReceived`)
- **Chat Commands** enabled (official KC in the RS profile `killcount` group)

## What is tracked

- Sources: NPC kills, plus Chambers of Xeric, Theatre of Blood, Tombs of Amascut, Fractured Archive
- Items: collection-log item IDs only
- Storage: at most 5 KCs per item per source (no backfill)

## Development (local RuneLite)

**Prerequisites:** JDK 11+ (Eclipse Temurin 11 or 17), network access to `repo.runelite.net`. If `gradle` uses Java 8 by default, set `JAVA_HOME` to your JDK 11+ install before building (for example `C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot` on Windows).

### Path A — Gradle `run` (recommended)

```powershell
cd C:\Git\unique-drop-kc
.\gradlew run
```

This starts RuneLite in developer mode with the plugin loaded. Enable **Loot Tracker** and **Chat Commands**, then enable **Unique Drop KC** in the plugin list (wrench icon) and turn on **Show side panel**.

**Debug testing (optional):** In plugin config, open the **Debug** section, set unlock phrase to `ENABLE DEBUG`, enable **Debug mode**, then use `::udkc help` in-game.

In IntelliJ, run the green triangle on the `run` task in `build.gradle`.

### Path B — Sideload JAR

```powershell
.\gradlew build
# or: .\gradlew shadowJar
```

Copy `build\libs\unique-drop-kc-1.0.0.jar` (or `unique-drop-kc-1.0.0-all.jar`) to:

`%USERPROFILE%\.runelite\sideloaded-plugins\`

Start RuneLite in **developer mode** so sideloaded plugins load.

### Tests

```powershell
.\gradlew test
```

## Manual test checklist

1. Boss unique drops → appears in panel; hover shows current official KC
2. Same unique again → newest-first list grows, capped at 5
3. Non-clog boss loot → not listed
4. Raid unique → listed under raid name with raid KC
5. Skilling loot → ignored
6. Restart client → histories persist
7. No official KC yet → no crash / no history until KC exists
8. With Collection Log Luck enabled → clog overlay unchanged; this panel still works

## Plugin Hub

Distributed via [RuneLite Plugin Hub](https://github.com/runelite/plugin-hub). Source lives in this repository; the hub lists a manifest pointing at a specific commit.

**Submit / update:** Fork `runelite/plugin-hub`, push branch `unique-drop-kc` with `plugins/unique-drop-kc` pointing at the latest commit on this repo, then open a PR to `runelite/plugin-hub` `master`. Manifest pins:

```text
repository=https://github.com/mikesetzer/unique-drop-kc.git
commit=17c615c58fdbed02b8c24e16729070c9b6dbb076
```

(Update `commit=` after each plugin release.)
