package net.tigereye.chestcavity.soul.fakeplayer.service;

/** Soul 假人模块的服务定位器。后续可替换为依赖注入或配置化实现。 */
public final class SoulFakePlayerServices {

  private static final SoulIdentityService IDENTITY_SERVICE = new DefaultSoulIdentityService();

  private SoulFakePlayerServices() {}

  public static SoulIdentityService identity() {
    return IDENTITY_SERVICE;
  }
}
