package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;

public final class CCDamageSources {
  public static final ResourceKey<DamageType> HEARTBLEED = register("heartbleed");
  public static final ResourceKey<DamageType> ORGAN_REJECTION = register("organ_rejection");
  public static final ResourceKey<DamageType> ALCOHOL_OVERDOSE = register("alcohol_overdose");
  public static final ResourceKey<DamageType> SHOU_PI_GU_CRASH = register("shou_pi_gu_crash");

  private CCDamageSources() {}

  private static ResourceKey<DamageType> register(String path) {
    return ResourceKey.create(Registries.DAMAGE_TYPE, ChestCavity.id(path));
  }

  public static DamageSource heartbleed(LivingEntity entity) {
    return entity.level().damageSources().source(HEARTBLEED);
  }

  public static DamageSource organRejection(LivingEntity entity) {
    return entity.level().damageSources().source(ORGAN_REJECTION);
  }

  public static DamageSource alcoholOverdose(LivingEntity entity) {
    return entity.level().damageSources().source(ALCOHOL_OVERDOSE);
  }

  /**
   * 兽皮蛊嵌甲冲撞造成的真实伤害。
   *
   * <p>绑定攻击者本体，确保死亡归属与反馈一致，同时允许通过数据标签声明绕过护甲与护盾等属性减免。
   */
  public static DamageSource shouPiGuCrash(LivingEntity attacker) {
    return attacker.level().damageSources().source(SHOU_PI_GU_CRASH, attacker);
  }
}
