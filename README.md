# Nightfall Client — Integrated Upgrade 0.3.0
- **Block ESP (Base Finder):** select chests, barrels, shulkers, spawners and several ore types individually; each block type has its own color. The module can draw projected boxes, crosshair-origin tracers and a 2D mini-map. Scanning is limited to chunks already loaded by the client and a configurable horizontal/vertical range.


Fabric client mod for Minecraft Java **1.21.11** / **Java 21**.

## Implemented in this update

- **ESP tracers originate at the screen centre / crosshair**, not at the player's feet. Targets outside the current view can produce directional edge arrows and a centre-to-edge guide.
- **Block Highlight** draws a thin white projected 3D wireframe around the block under the crosshair. The `Transition (ms)` setting controls how smoothly the outline moves when switching between targeted blocks: `0` is instant, higher values are smoother/slower. The edges remain straight and antialiased; no tracer is drawn to the targeted block.
- **Sus-Chunk world overlay** projects the analysed chunk's ground surface into the 3D view and draws a translucent coloured chunk polygon with a brighter border and label. Green/yellow/orange/red still represent the existing analysis score bands. Normal/low-score chunks are limited by the configurable `Overlay Range` to avoid filling the whole view.
- **Chunk Explorer** now has a persistent details pane; click a chunk to pin its coordinates, score, evidence, coverage and analysis notes.
- **RTP / teleport cleanup:** with `OG Base Finder → Auto Clear RTP` enabled, a horizontal displacement greater than the configured threshold (default 128 blocks) clears stored results, scan age and notification-deduplication state. Dimension changes clear them too.
- **Item ESP** supports boxes, names/counts, crosshair-origin tracers and range. Optional module notifications are available for Item ESP, Player ESP, Entity Intelligence, Storage ESP and Spawner ESP.
- **Central Notifier** offers enable/disable, sound, duration and cooldown. The Base Finder and the ESP modules can each opt into notifications.
- **Slow Swing** slows the local player's hand-swing animation ticker. It does **not** change attack cooldown, hit registration or server-side combat timing.
- **Staff List:** names are user-maintained. Use `/nfstaff enable|disable`, `/nfstaff add <name>`, `/nfstaff remove <name>`, `/nfstaff list`, `/nfstaff status`, `/nfstaff debug` and `/nfstaff reload`, or edit `config/nightfall-staff.txt`. The file accepts one exact username per line, or comma-/semicolon-separated names. External file edits are reloaded automatically (polled once per second). The Staff List panel is independent from the main HUD/module list; enable `Staff List → Show Known Staff` to display it. The module can also be switched with `/nfstaff enable` and `/nfstaff disable`.
- **AntiVanish diagnostic:** can flag a nearby player entity visible in the world but absent from tab for multiple consecutive samples. Range and required samples are configurable. This is only a tab/world discrepancy warning, **not proof of vanish** and not a way to reveal players that the server never sends to the client.
- **Resource pack bundled in the mod JAR:** the CS-knife pack is registered as a built-in pack named `Nightfall CS Knife`. Enable it in Minecraft's Resource Packs screen. Fabric's built-in pack registration makes it selectable; it is not silently forced on.
- HUD watermark, coordinates and active-module list are right-aligned. The Staff List panel is pinned at the upper-left and remains visible when the normal HUD is disabled. The ClickGUI canvas and Chunk Explorer details layout have been enlarged and cleaned up with the purple palette.

## Important limitations / not completed in this pass

- Base analysis remains a **heuristic over client-loaded chunks only**. The score is not a probability, and no client can inspect chunks/blocks that the server has not sent.
- Staff identification is manual-list based because a player name alone is not evidence of staff status.
- AntiVanish cannot infer players hidden from both the entity stream and tab list. Servers differ in how they expose tab, entity and vanish information.
- **Dark-Steel Sky:** a configurable local color treatment is applied after 1.21.11 resolves sky, sunrise/sunset, cloud and fog environment attributes. It is a tint/grade, not a replacement skybox texture or animated custom celestial renderer; in-game visual testing is still required.
- The white block outline is rendered as a projected wireframe in the HUD pass. It is visually smoothed but is not an opaque world-depth geometry replacement.
- This source update has **not been compile-tested** in this environment: Java 21 is present, but Gradle is not installed and the environment cannot resolve external hosts to download it. Run the GitHub Actions workflow or `gradle --no-daemon clean build` with network access before installing the JAR.


## Integrated Base Finder, ESP, Sky and Staff update

- Learned fake-base profiles are isolated per server and dimension. On a reconnect or world-instance change, transient chunk results are cleared so old flags cannot bleed into a different session.
- One `EspChunkFilter` is shared by Block ESP, Storage ESP and Spawner ESP. Its `Ignore Fake Chunks` option only removes locally learned fake-pattern chunks; a single corner decoy spawner does not hide the entire chunk.
- Block ESP and Storage/Spawner ESP scan incrementally. Storage/Spawner scans process at most four loaded chunks per tick and stop at a short time budget, publishing the new overlay snapshot when the task completes.
- `Dark-Steel Sky` is available in the Visuals category. `Strength`, `Fog Strength` and `Cloud Strength` tune the local palette. The hook targets Minecraft 1.21.11 environment-attribute resolution (sky, sunset, clouds and fog); it does not fetch hidden world data.
- Staff List checks exact Minecraft profile names from the server tab. The HUD panel no longer depends on the active-module list or the main HUD being enabled. `/nfstaff debug` shows saved names, tab names, exact matches and AntiVanish candidate counts.
- AntiVanish samples visible world players that are missing from the tab repeatedly; it is configurable by `Range` and `Missing Tab Samples`. The warning explicitly says that this is not proof of staff vanish.

## Build

From the project root:

```sh
gradle --no-daemon clean build
```

The JAR is written to `build/libs/`. The workflow in `.github/workflows/build.yml` builds with Java 21 and uploads `build/libs/*.jar` as an artifact.


## Block ESP

Das Block-ESP-Modul kann im Nightfall-ClickGUI aktiviert und konfiguriert werden.
Es markiert ausgewählte Blöcke in bereits geladenen Client-Chunks, unterstützt eigene
Farben je Blocktyp sowie Boxen, Namen und optionale Tracer. Die Mini-Map ist optional
und standardmäßig ausgeschaltet, damit kein dauerhaftes Zusatzfenster auf dem Bildschirm
liegt. Das Konfigurationsmenü öffnet sich nur bei Tastendruck (standardmäßig Right Shift).
Die Blockauswahl umfasst Lagerblöcke, Spawner, Erze und weitere technische bzw.
strukturbezogene Blöcke. Entity ESP bleibt separat über Entity Intelligence konfigurierbar.

Das Modul ergänzt kein zusätzliches Freecam. Die bereits vorhandene Nightfall-Freecam-
Funktion bleibt als eigenständiges Modul unverändert. Die Erkennung nutzt ausschließlich
Blöcke aus Chunks, die der Minecraft-Client bereits geladen hat.

### ESP line rendering update

The ESP now uses a shared antialiased line rasterizer for tracers and overlay edges. Entity/block boxes are projected as the twelve edges of a 3D AABB instead of screen-space rectangles. The targeted-block highlight keeps straight, antialiased 3D edges and has a configurable world-space transition when the crosshair moves between blocks (`Transition (ms)`, 0–500; 0 disables easing). Line segments are clipped to the viewport and camera near plane.

## OG Base Finder: Limits und Fake-Base-Muster

Der OG Base Finder ist speicherbegrenzt: Standardmaessig werden hoechstens 256 Chunk-Ergebnisse und 64 sichtbare Verdachts-Chunks gehalten. Der Round-Robin-Scheduler analysiert immer nur einen Chunk gleichzeitig und verteilt dessen Arbeit ueber mehrere Ticks. Neu ist ein zweifacher Schutz gegen Lag-Spitzen: `Scan Blocks/Tick` / `Scan Speed` setzen eine harte Obergrenze, `Scan Budget (ms)` begrenzt zusaetzlich die Laufzeit pro Tick (Standard 1 ms). Die Cluster-Auswertung wurde von quadratischen Positionsvergleichen auf eine exakte 3D-Praefixsumme umgestellt; dadurch bleiben auch uebereinander gestapelte Bloecke korrekt getrennt, waehrend Nachbarschaftsabfragen konstant viele Array-Zugriffe benoetigen.

Auch Block ESP scannt grosse Suchbereiche jetzt inkrementell mit eigenem Zeitbudget, statt bei jedem Aktualisierungslauf den kompletten 3D-Suchraum in einem Tick abzulaufen. Storage ESP, Spawner ESP und Block ESP verwenden jetzt dieselbe zentrale `EspChunkFilter`-Policy. Mit `Ignore Fake Chunks` koennen sie Chunks ausblenden, die lokal durch `/nfsus fake` oder ein gespeichertes Muster als False Positive markiert wurden. Storage-/Spawner-Block-Entity-Scans laufen nun in kleinen Chunk-Batches mit einem Zeitbudget ueber mehrere Ticks statt als kompletter synchroner Radius-Scan alle 20 Ticks. Ein einzelner erkannter Fake-Spawner blendet nicht pauschal den ganzen Chunk aus; er wird nur als Spawner gefiltert. Der Spawner-Corner-Filter gilt auch fuer Spawner im Block ESP.

Fuer einen starken Lager- oder Maschinenhinweis muessen die Bloecke nicht nur in ausreichender Zahl vorkommen, sondern auch in einem zusammenhaengenden Nahbereich liegen. Weit verstreute Einzelgruppen werden als Kontext angezeigt, aber nicht als starker Beweis gewertet. Das ist eine konservative Anti-False-Positive-Heuristik, keine Garantie, jede Fake- oder echte Base zu erkennen.

Ein vom Spieler bestaetigtes False-Positive-Muster kann lokal gelernt werden:
- `/nfsus fake` speichert das Profil des aktuellen Chunks.
- `/nfsus fake <chunkX> <chunkZ>` speichert das Profil eines bereits geladenen Chunks; die Werte sind Chunk-Koordinaten, keine Block-Koordinaten.
- `/nfsus list` zeigt die Anzahl gespeicherter Muster.
- `/nfsus remove <nummer>` entfernt ein Muster.
- `/nfsus clear` loescht alle Muster.

Muster bleiben auf 32 begrenzt und werden getrennt nach Serveradresse und Dimension in Dateien `config/nightfall-fake-base-patterns-<hash>.txt` gespeichert; ein Muster von Server A kann dadurch nicht versehentlich Chunks auf Server B ausblenden. Der Abgleich blendet spaeter sehr aehnliche Profile aus; er verlangt mindestens zwei unabhaengige Merkmalsgruppen (z. B. Lager plus Redstone), damit ein Lager-Untermerkmal oder ein dichter Cluster nicht als zweites Signal durchrutscht. Er ist absichtlich konservativ und ersetzt keine sichere Base-Erkennung.

### Player ESP / invsee

Bei aktiviertem Player ESP kannst du `/nfinvsee <spielername>` verwenden. Nightfall leitet damit den normalen Server-Befehl `/invsee <spielername>` weiter, wenn der Spieler aktuell im geladenen Player-ESP-Bereich ist. Ob der Befehl existiert und ob dein Account ihn verwenden darf, entscheidet ausschliesslich der Server. Der Client kann fremde Inventare nicht aus lokalen Entity-Daten auslesen.

### HugoSMP / Anti-Leak / AntiESP

Die oeffentlich gelistete Ressource `m1kareal hugosmp` beschreibt `No Block Rotation`, `Deepslate to Stone`, `UHC Ores`, `Bedrock to Stone` und `Tuff to Stone` (Minecraft 1.21.4–1.21.11). `Don't leak your base!` nennt zusaetzlich das Verbergen von Erzen, Bedrock, Tuff, Andesit, Granit, Diorit, Kies sowie Biome-Farben. Das sind Textur-/Darstellungs-Tarnungen. Nightfall wertet fuer die Chunk-Analyse die empfangenen Block-IDs und Block-Entities aus, nicht die Texturrotation oder Biome-Farbe; das Pack allein kann diese Merkmale daher nicht aus den Client-Blockdaten entfernen.

Das oeffentliche `alex-9347/AntiESP`-Projekt wurde laut README urspruenglich fuer HugoSMP entwickelt. Seine README beschreibt fuer den Beta-Verlauf: oberhalb Y=5 gefaelschte Deepslate-Updates unter Y=0, beim Abstieg unter Y=5 wieder echte Blockupdates. Nightfall zaehlt deshalb unterirdische Erze und Bedrock als reine Datenintegritaets-Merkmale (nicht als Base-Score). Wenn bei einer erneuten Analyse nach dem Abstieg beides deutlich zunimmt, meldet Nightfall einen moeglichen serverseitigen AntiESP-Datenwechsel und prueft nahe geladene Chunks erneut. Das ist ein beobachtungsbasierter Hinweis, kein Beweis fuer genau dieses Plugin.

Wichtige Grenze: Die v1.0.0-Stable-Release-Notiz des Repositories behauptet, dass echte Daten unterhalb der Deepslate-Schicht gar nicht mehr an den Client gesendet werden; dieselbe Release-Seite warnt zugleich vor zu vielen Paketen in Tests. Wenn der Server dauerhaft nur Fake-Blockzustaende sendet, gibt es fuer einen Client keine echte Blockinformation, die Nightfall wiederherstellen koennte. Der Finder kann diese fehlenden Daten weder erraten noch serverseitige AntiESP-Schutzmassnahmen umgehen. Ob und welche Version auf dem aktuellen HugoSMP-Live-Server laeuft, ist durch die oeffentlichen Quellen nicht bestaetigt.

Quellen: [m1kareal hugosmp auf Modrinth](https://modrinth.com/project/ohDo7H36), [Don't leak your base!](https://modrinth.com/project/dont-leak-your-base), [AntiESP README](https://github.com/alex-9347/AntiESP), [AntiESP Releases](https://github.com/alex-9347/AntiESP/releases).

### Ada ESP

Im hier vorliegenden Nightfall-Quellcode ist kein separates Modul oder Paket namens `Ada ESP` enthalten. Die Filterung gelernter Fake-Chunks wurde stattdessen in Nightfalls eigenes Block ESP, Storage ESP und Spawner ESP integriert. Fuer eine Aenderung an einem eigenstaendigen Ada-ESP-Projekt waere dessen Quellcode erforderlich; es wurde hier nichts an einer nicht vorhandenen Fremdmodifikation vorgetaeuscht.


### Automatische Markierung verschwundener Bases

Wenn ein zuvor sichtbarer Sus-Chunk in der Nähe des Spielers nach einem Abstieg erneut analysiert wird und seine Hinweise deutlich zurückgehen (mindestens 10 Score-Punkte bzw. weniger Beweise oder ein fast vollständig verschwundener Score), markiert Nightfall den Chunk automatisch als **SUS WEG**. Dafür muss der Chunk beim erneuten Scan noch geladen sein; ein normales Chunk-Unloading wird ausdrücklich nicht als verschwundene Base gewertet. Nach einem Höhenabfall von mindestens 8 Blöcken wird ein eventuell laufender Scan verworfen, damit keine gemischten Blockzustände aus der Zeit vor und nach dem Server-Update ausgewertet werden.

Die Markierung erscheint blau im Welt-Overlay und in der Chunk-Karte, mitsamt vorherigem Score und Y-Höhenwechsel. Sie bleibt lokal in `config/nightfall-vanished-bases.csv` gespeichert (maximal 512 Markierungen, getrennt nach Dimension). Befehle: `/nfsus vanished` zeigt gespeicherte Markierungen der aktuellen Dimension; `/nfsus clearvanished` löscht sie. Die Erkennung ist ein Hinweis auf geänderte/verschwundene Chunk-Daten, kein Beweis, dass dort definitiv eine echte Base steht. Die Option `OG Base Finder → Auto Mark Vanished Bases` ist standardmäßig aktiviert.
