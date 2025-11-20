package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.soul;

/**
 * Soul level tiers based on accumulated hun po. Each tier is defined by the minimal number of
 * "person souls" (100 hun po per person) required to enter it.
 */
public enum HunDaoSoulLevelTier {
  ONE_PERSON(1L, "soul_level.chestcavity.one_person"),
  TEN_PERSON(10L, "soul_level.chestcavity.ten_person"),
  HUNDRED_PERSON(100L, "soul_level.chestcavity.hundred_person"),
  THOUSAND_PERSON(1_000L, "soul_level.chestcavity.thousand_person"),
  TEN_THOUSAND_PERSON(10_000L, "soul_level.chestcavity.ten_thousand_person"),
  HUNDRED_THOUSAND_PERSON(100_000L, "soul_level.chestcavity.hundred_thousand_person"),
  MILLION_PERSON(1_000_000L, "soul_level.chestcavity.million_person"),
  TEN_MILLION_PERSON(10_000_000L, "soul_level.chestcavity.ten_million_person"),
  HUNDRED_MILLION_PERSON(100_000_000L, "soul_level.chestcavity.hundred_million_person"),
  ARAN(1_000_000_000L, "soul_level.chestcavity.aran"),
  TEN_ARAN(10_000_000_000L, "soul_level.chestcavity.ten_aran"),
  HUNDRED_ARAN(100_000_000_000L, "soul_level.chestcavity.hundred_aran"),
  THOUSAND_ARAN(1_000_000_000_000L, "soul_level.chestcavity.thousand_aran"),
  TEN_THOUSAND_ARAN(10_000_000_000_000L, "soul_level.chestcavity.ten_thousand_aran"),
  HUNDRED_THOUSAND_ARAN(100_000_000_000_000L, "soul_level.chestcavity.hundred_thousand_aran"),
  MILLION_ARAN(1_000_000_000_000_000L, "soul_level.chestcavity.million_aran"),
  TEN_MILLION_ARAN(10_000_000_000_000_000L, "soul_level.chestcavity.ten_million_aran"),
  YI_ARAN(100_000_000_000_000_000L, "soul_level.chestcavity.yi_aran");

  private final long minPersonUnits;
  private final String translationKey;

  HunDaoSoulLevelTier(long minPersonUnits, String translationKey) {
    this.minPersonUnits = minPersonUnits;
    this.translationKey = translationKey;
  }

  public long getMinPersonUnits() {
    return minPersonUnits;
  }

  public String getTranslationKey() {
    return translationKey;
  }
}
