package nightfall.core;
import java.util.*;
import net.minecraft.client.Minecraft;
import nightfall.gui.ClickGui;
import nightfall.gui.ChunkMap;
import static nightfall.core.Setting.*;
public final class Modules {
  public static final List<Module> ALL = new ArrayList<>();
  public static Module FREELOOK, FREECAM, HUD, GUI, PESP, EESP, STORAGE, SPAWNER, VIEW, BASE, MAP, SOUND, OPSEC, ITEM, BLOCK_HIGHLIGHT, BLOCK_ESP, SLOW_SWING, NOTIFIER, STAFF, ANTIVANISH, DARK_STEEL_SKY;
  private static Module add(Module m, Setting... s) { m.settings.addAll(List.of(s)); ALL.add(m); return m; }
  public static void init() {
    FREELOOK = add(new Module("Freelook", "Kamera frei drehen, Laufrichtung bleibt", Category.MOVEMENT, 86) {
      public void onEnable() { if (!Cam.active()) Cam.start(); Cam.freelook = true; }
      public void onDisable() { Cam.freelook = false; } });
    FREELOOK.hold = true;
    FREECAM = add(new Module("Freecam", "Freie Kamera (WASD, Space, Shift)", Category.MOVEMENT, 67) {
      public void onEnable() { Cam.start(); Cam.freecam = true; Cam.speed = s("Speed").d(); }
      public void onDisable() { Cam.freecam = false; } }, num("Speed", 12, 2, 60, 1));
    PESP = add(new Module("Player ESP", "Box, Name, Distanz, Leben, Tracer und Server-/invsee-Kurzbefehl /nfinvsee", Category.VISUALS, -1) {},
      bool("Box", true), bool("Name", true), bool("Distance", true), bool("Health", true), bool("Tracer", false),
      num("Range", 128, 16, 256, 8), color("Color", 0.75), bool("Notify", false));
    EESP = add(new Module("Entity Intelligence", "Villager, Trader, Rahmen, Armorstand, Minecart, Allay", Category.VISUALS, -1) {},
      bool("Villager", true), bool("Wandering Trader", true), bool("Item Frame", true), bool("Glow Item Frame", true),
      bool("Armor Stand", true), bool("Minecart", true), bool("Allay", true),
      bool("Box", true), bool("Name", true), bool("Tracer", false), num("Range", 96, 16, 256, 8), color("Color", 0.55), bool("Notify", false));
    STORAGE = add(new Module("Storage ESP", "Truhen, Barrels, Shulker in geladenen Chunks", Category.VISUALS, -1) {},
      bool("Chest", true), bool("Trapped Chest", true), bool("Barrel", true), bool("Shulker Box", true), bool("Ender Chest", false),
      bool("Ignore Fake Chunks", true), bool("Box", true), bool("Tracer", false), num("Range", 96, 16, 256, 8), bool("Notify", false));
    SPAWNER = add(new Module("Spawner ESP", "Zeigt geladene Spawner; bekannte Fake-Spawner werden gefiltert", Category.VISUALS, -1) {},
      bool("Ignore Fake Chunks", true), bool("Box", true), bool("Tracer", false), num("Range", 96, 16, 256, 8), color("Color", 0.12), bool("Notify", false));
    VIEW = add(new Module("Viewmodel", "Position, Groesse und Neigung von Hand und Item", Category.VISUALS, -1) {},
      mode("Preset", 0, "Custom", "Knife", "Low"), num("X", 0, -1, 1, 0.01), num("Y", 0, -1, 1, 0.01), num("Z", 0, -1, 1, 0.01),
      num("Scale", 1, 0.3, 1.6, 0.01), num("Rot X", 0, -90, 90, 1), num("Rot Y", 0, -90, 90, 1), num("Rot Z", 0, -90, 90, 1));
    BASE = add(new Module("OG Base Finder", "Bewertet geladene Chunks (Lager, Redstone, Bauten) mit Score und Konfidenz", Category.BASE_FINDING, -1) {},
      num("Min Score", 50, 10, 90, 5), num("Min Evidence", 2, 2, 5, 1), num("Scan Speed", 1, 1, 4, 1),
      num("Scan Blocks/Tick", 2048, 512, 16384, 512), num("Scan Budget (ms)", 1, 1, 4, 1), num("Range", 12, 4, 24, 1),
      num("Max Stored Chunks", 256, 64, 1024, 64), num("Max Sus Chunks", 64, 8, 256, 8), num("Near Recheck Ticks", 40, 20, 200, 20),
      bool("Natural Filter", true), bool("Show Decoys", false), bool("Auto Mark Vanished Bases", true), bool("Ignore Bedrock Corner Spawners", true),
      num("Decoy Y Range", 4, 0, 16, 1), bool("Overlay", true), bool("Notify", true), bool("Auto Clear RTP", true), num("RTP Threshold", 128, 64, 512, 16), num("Overlay Range", 6, 2, 16, 1));
    MAP = add(new Module("Chunk Explorer", "Oeffnet die Chunk-Karte mit Scores und Details", Category.BASE_FINDING, -1) {
      public void onEnable() { enabled = false; Minecraft.getInstance().setScreen(new ChunkMap()); } });
    HUD = add(new Module("HUD", "Watermark, Modulliste, Koordinaten", Category.VISUALS, -1) {});
    HUD.enabled = true;
    GUI = add(new Module("ClickGUI", "Oeffnet das Menue", Category.MISC, 344) {
      public void onEnable() { enabled = false; Minecraft.getInstance().setScreen(new ClickGui()); } });
    SOUND = add(new Module("GUI Sounds", "Klick-Sound, Stil und Lautstaerke", Category.MISC, -1) {},
      bool("Enabled", true), mode("Style", 0, "Soft", "Mechanical", "Digital", "Minimal"), num("Volume", 0.65, 0, 1, 0.05));
    OPSEC = add(new Module("Streamer Privacy", "Lokaler Datenschutz fuer HUD und Stream-Aufnahmen", Category.OPSEC, -1) {},
      bool("Hide Coordinates", true), bool("Hide Module List", true), bool("Hide FPS", true));
    ITEM = add(new Module("Item ESP", "Markiert fallengelassene Items", Category.VISUALS, -1) {},
      bool("Box", true), bool("Name", true), bool("Tracer", false), bool("Notify", false),
      num("Range", 96, 8, 256, 8), color("Color", 0.78));
    BLOCK_HIGHLIGHT = add(new Module("Block Highlight", "Klare 3D-Outline mit einstellbarem Uebergang beim Zielwechsel", Category.VISUALS, -1) {},
      bool("Enabled", true), num("Transition (ms)", 120, 0, 500, 10));
    BLOCK_HIGHLIGHT.enabled = true;
    BLOCK_ESP = add(new Module("Block ESP", "Markiert ausgewaehlte Bloecke mit individuellen Farben; Mini-Map optional", Category.VISUALS, -1) {},
      bool("Box", true), bool("Name", true), bool("Tracer", false), bool("Mini-map", false),
      num("Range", 48, 8, 128, 8), num("Vertical Range", 32, 8, 128, 8),
      bool("Chest", true), color("Color Chest", 0.10),
      bool("Trapped Chest", true), color("Color Trapped Chest", 0.98),
      bool("Barrel", true), color("Color Barrel", 0.08),
      bool("Shulker Box", true), color("Color Shulker Box", 0.82),
      bool("Ender Chest", false), color("Color Ender Chest", 0.52),
      bool("Spawner", true), color("Color Spawner", 0.88),
      bool("Ignore Fake Chunks", true), bool("Diamond Ore", true), color("Color Diamond Ore", 0.50),
      bool("Ancient Debris", true), color("Color Ancient Debris", 0.04),
      bool("Emerald Ore", false), color("Color Emerald Ore", 0.36),
      bool("Gold Ore", false), color("Color Gold Ore", 0.14),
      bool("Iron Ore", false), color("Color Iron Ore", 0.60),
      bool("Redstone Ore", false), color("Color Redstone Ore", 0.00),
      bool("Lapis Ore", false), color("Color Lapis Ore", 0.62),
      bool("Copper Ore", false), color("Color Copper Ore", 0.07),
      bool("Obsidian", false), color("Color Obsidian", 0.78),
      bool("Crying Obsidian", false), color("Color Crying Obsidian", 0.88),
      bool("Beacon", false), color("Color Beacon", 0.14),
      bool("Hopper", false), color("Color Hopper", 0.08),
      bool("Furnace", false), color("Color Furnace", 0.04),
      bool("Blast Furnace", false), color("Color Blast Furnace", 0.02),
      bool("Smoker", false), color("Color Smoker", 0.06),
      bool("Dispenser", false), color("Color Dispenser", 0.58),
      bool("Dropper", false), color("Color Dropper", 0.62),
      bool("Piston", false), color("Color Piston", 0.11),
      bool("Sticky Piston", false), color("Color Sticky Piston", 0.28),
      bool("Observer", false), color("Color Observer", 0.72),
      bool("End Portal Frame", false), color("Color End Portal Frame", 0.42),
      bool("Respawn Anchor", false), color("Color Respawn Anchor", 0.86),
      bool("Lodestone", false), color("Color Lodestone", 0.50));
    SLOW_SWING = add(new Module("Slow Swing", "Stellt die lokale Hand-Swing-Animation langsamer dar", Category.VISUALS, -1) {},
      num("Speed", 0.65, 0.15, 1.0, 0.05));
    NOTIFIER = add(new Module("Notifier", "Zentrale, einstellbare Nightfall-Benachrichtigungen", Category.MISC, -1) {},
      bool("Enabled", true), bool("Sound", true), num("Duration", 4, 1, 10, 1), num("Cooldown", 5, 1, 30, 1));
    NOTIFIER.enabled = true;
    STAFF = add(new Module("Staff List", "Tab-Informationen und manuell gepflegte Staff-Hinweise", Category.MISC, -1) {},
      bool("Show Known Staff", true), bool("Notify", true));
    // Staff List is useful immediately after adding names; unlike optional ESP overlays,
    // keep this lightweight local watch enabled by default.
    STAFF.enabled = true;
    ANTIVANISH = add(new Module("AntiVanish", "Meldet wiederholte Tab-/Welt-Abweichungen; kein sicherer Vanish-Nachweis", Category.MISC, -1) {},
      bool("Notify", true), num("Range", 64, 16, 256, 16), num("Missing Tab Samples", 3, 2, 8, 1));
    DARK_STEEL_SKY = add(new Module("Dark-Steel Sky", "Dunkelstahl-Farblook fuer Himmel, Abendrot, Wolken und Nebel", Category.VISUALS, -1) {},
      num("Strength", 0.72, 0.0, 1.0, 0.02), num("Fog Strength", 0.38, 0.0, 1.0, 0.02),
      num("Cloud Strength", 0.62, 0.0, 1.0, 0.02));
  }
}
