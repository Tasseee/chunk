# Nightfall 0.3.0 — Integrated update

## OG Base Finder
- Keeps scan work bounded with round-robin scheduling and block/time budgets.
- Uses spatial cluster checks to avoid counting scattered containers or redstone as one strong base signal.
- Learns false-positive fingerprints with at least two independent feature groups.
- Scopes learned false-positive patterns to the current server and dimension.
- Clears transient scan results when the world instance or server connection changes.
- Keeps the existing automatic vanished-base marker and observed AntiESP block-data-change warning.

## ESP
- Uses one shared `EspChunkFilter` for Block ESP, Storage ESP and Spawner ESP.
- Keeps a single fake spawner from hiding an entire chunk.
- Slices Storage ESP and Spawner ESP block-entity scans into batches of up to four loaded chunks per tick, with an additional short time budget.
- Continues to time-slice Block ESP's block-state scan.

## Dark-Steel Sky
- Adds a configurable local color grade for sky, sunrise/sunset, clouds and fog through Minecraft 1.21.11 environment-attribute resolution.
- This is a tint/grade, not a replacement skybox, starfield or celestial animation system.

## Staff List / AntiVanish
- Accepts exact usernames one per line or separated by commas/semicolons/whitespace in `config/nightfall-staff.txt`.
- Reloads external edits by checking the file once per second (content and modified time).
- Adds `/nfstaff enable`, `/nfstaff disable`, and expands `/nfstaff debug`.
- Pins the Staff List HUD panel independently of the main HUD/module list.
- Adds configurable AntiVanish range and required missing-tab samples; warnings remain explicitly non-conclusive.
- Persists safe Staff List, AntiVanish and Dark-Steel Sky enable states.

## Verification status
- Java source lexical/bracket validation: passed.
- Mixin JSON and Fabric mod JSON parsing: passed.
- `BaseFingerprint` similarity checks: 4/4 passed.
- Full Fabric/Gradle build: not run in this environment because Gradle is unavailable.
