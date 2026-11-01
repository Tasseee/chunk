package nightfall.core;
public enum Category {
  COMBAT("Combat"), MOVEMENT("Movement"), PLAYER("Player"), VISUALS("Visuals"), WORLD("World"),
  BASE_FINDING("Base Finding"), DONUT("Donut"), MISC("Misc"), UTILITY("Utility"),
  PROTECTION("Protection"), OPSEC("OPSEC"), QA("QA Testing");
  public final String label;
  Category(String l) { label = l; }
}
