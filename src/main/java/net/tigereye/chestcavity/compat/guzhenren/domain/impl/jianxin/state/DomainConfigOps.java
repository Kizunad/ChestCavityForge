package net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.state;

import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.registration.CCAttachments;

/** Helper to read effective JianXin domain config from player attachments. */
public final class DomainConfigOps {
  private DomainConfigOps() {}

  public static double radiusScale(Player owner) {
    if (owner == null) return 1.0;
    Optional<SwordDomainConfigAttachment> opt = CCAttachments.getExistingSwordDomainConfig(owner);
    return opt.map(SwordDomainConfigAttachment::getRadiusScale).orElse(1.0);
  }

  public static double effectScale(Player owner) {
    if (owner == null) return 1.0;
    Optional<SwordDomainConfigAttachment> opt = CCAttachments.getExistingSwordDomainConfig(owner);
    return opt.map(SwordDomainConfigAttachment::getEffectScale).orElse(1.0);
  }
}
