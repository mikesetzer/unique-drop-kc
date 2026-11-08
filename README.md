# Unique Drop KC

RuneLite plugin that records official killcounts when collection-log uniques drop from bosses and raids, and shows them on hover in a side panel.

Example tooltip: `Last received at 412, 400, 224 kc` (up to 5 entries, newest first).

## Requirements

- **Loot Tracker** enabled (`LootReceived`)
- **Chat Commands** enabled (official KC in the RS profile `killcount` group)

## What is tracked

- Sources: NPC kills, plus Chambers of Xeric, Theatre of Blood, Tombs of Amascut, Fractured Archive
- Items: collection-log item IDs only
- Storage: at most 5 KCs per item per source (no backfill)

## Build / sideload

```bash
./gradlew build
```

Load the jar from RuneLite’s sideloaded plugins folder (or use Plugin Hub external plugins).

## Manual test checklist

1. Boss unique drops → appears in panel; hover shows current official KC
2. Same unique again → newest-first list grows, capped at 5
3. Non-clog boss loot → not listed
4. Raid unique → listed under raid name with raid KC
5. Skilling loot → ignored
6. Restart client → histories persist
7. No official KC yet → no crash / no history until KC exists
8. With Collection Log Luck enabled → clog overlay unchanged; this panel still works
