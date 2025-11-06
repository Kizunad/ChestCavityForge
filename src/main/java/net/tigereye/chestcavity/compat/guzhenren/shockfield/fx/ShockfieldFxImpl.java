package net.tigereye.chestcavity.compat.guzhenren.shockfield.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.PhaseKind;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.ShockfieldFxService;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.ShockfieldState;
import net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher;

/**
 * 剑荡蛊特效实现：精致而节制的粒子+音效。
 *
 * <p>设计理念：
 * - 少量高质量粒子
 * - 剑意的诗意表达
 * - 真元震荡的视觉语言
 */
public final class ShockfieldFxImpl implements ShockfieldFxService {

  // ==================== 特效ID ====================
  private static final ResourceLocation WAVE_CREATE = ChestCavity.id("shockfield/wave_create");
  private static final ResourceLocation WAVE_PULSE = ChestCavity.id("shockfield/wave_pulse");
  private static final ResourceLocation HIT_ENEMY = ChestCavity.id("shockfield/hit_enemy");
  private static final ResourceLocation HIT_ALLY = ChestCavity.id("shockfield/hit_ally");
  private static final ResourceLocation EXTINGUISH = ChestCavity.id("shockfield/extinguish");
  private static final ResourceLocation SUBWAVE_CREATE = ChestCavity.id("shockfield/subwave_create");
  private static final ResourceLocation INTERFERENCE_CONSTRUCT =
      ChestCavity.id("shockfield/interference_construct");
  private static final ResourceLocation INTERFERENCE_DESTRUCT =
      ChestCavity.id("shockfield/interference_destruct");

  // ==================== 波源创建 ====================
  /**
   * 🌀 主波启动（Shockfield 启动）
   *
   * <p>"剑荡起，一念激涌真元，气浪自心而出，万物随之共振。"
   * <p>真元震荡化作无形之波，天地的尘与光都被推开，留下环形的静默。
   */
  @Override
  public void onWaveCreate(ServerLevel level, ShockfieldState state) {
    Vec3 center = state.getCenter();

    // 1. 真元爆发：向外推开的冲击波
    // 使用 CLOUD 粒子模拟气浪推开的效果
    for (int i = 0; i < 8; i++) {
      double angle = (Math.PI * 2.0 * i) / 8.0;
      double dx = Math.cos(angle) * 0.5;
      double dz = Math.sin(angle) * 0.5;
      level.sendParticles(
          ParticleTypes.CLOUD,
          center.x + dx,
          center.y + 0.1,
          center.z + dz,
          1,
          dx * 0.3,
          0.05,
          dz * 0.3,
          0.1);
    }

    // 2. 剑意涌动：青白色的真元螺旋
    // 使用 SOUL_FIRE_FLAME 表现剑意
    for (int i = 0; i < 12; i++) {
      double angle = (Math.PI * 2.0 * i) / 12.0 + Math.random() * 0.3;
      double radius = 0.3 + Math.random() * 0.2;
      double dx = Math.cos(angle) * radius;
      double dz = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.SOUL_FIRE_FLAME,
          center.x + dx,
          center.y + 0.1,
          center.z + dz,
          1,
          0.0,
          0.15,
          0.0,
          0.01);
    }

    // 3. 音效：低沉的剑鸣 + 真元震荡
    level.playSound(
        null,
        center.x,
        center.y,
        center.z,
        SoundEvents.TRIDENT_RIPTIDE_1,
        SoundSource.PLAYERS,
        0.6F,
        0.7F);

    // 通过 AbilityFxDispatcher 触发客户端特效（如果有自定义实现）
    AbilityFxDispatcher.play(level, WAVE_CREATE, center, Vec3.ZERO, Vec3.ZERO, null, null, 1.0F);
  }

  // ==================== 波场扩散 ====================
  /**
   * 🌊 主波扩散（每秒一圈）
   *
   * <p>"波涌若潮，剑意层层荡开。"
   * <p>青白的气圈自脚下蔓延，如水面涟漪，又似剑鸣的回音，扩散至天地之间。
   */
  @Override
  public void onWaveTick(ServerLevel level, ShockfieldState state) {
    Vec3 center = state.getCenter();
    double radius = state.getRadius();
    double amplitude = state.getAmplitude();

    // 只在波前位置生成粒子（环形）
    // 根据振幅决定粒子密度
    int particleCount = Math.max(2, (int) (amplitude * 20.0));

    // 控制tick频率：只在特定tick生成粒子（避免过于密集）
    long age = state.getAge(level.getGameTime());
    if (age % 10 != 0) { // 每10 tick（0.5秒）生成一次
      return;
    }

    for (int i = 0; i < particleCount; i++) {
      double angle = (Math.PI * 2.0 * i) / particleCount;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.1;

      // 青白色气圈：使用 END_ROD 粒子（细腻的青白光）
      level.sendParticles(
          ParticleTypes.END_ROD,
          x,
          y,
          z,
          1,
          0.0,
          0.0,
          0.0,
          0.0);

      // 偶尔添加 SOUL 粒子增强剑意感
      if (i % 3 == 0) {
        level.sendParticles(
            ParticleTypes.SOUL,
            x,
            y,
            z,
            1,
            0.0,
            0.05,
            0.0,
            0.01);
      }
    }

    // 音效：轻柔的风声 + 剑鸣回响（音量随振幅衰减）
    if (age % 20 == 0) { // 每秒一次音效
      float volume = (float) (amplitude * 0.3);
      float pitch = 1.0F + (float) (state.getPeriod() - 1.0) * 0.2F;
      level.playSound(
          null,
          center.x,
          center.y,
          center.z,
          SoundEvents.BEACON_AMBIENT,
          SoundSource.PLAYERS,
          volume,
          pitch);
    }

    // 客户端特效
    AbilityFxDispatcher.play(
        level,
        WAVE_PULSE,
        center,
        new Vec3(radius, amplitude, 0),
        Vec3.ZERO,
        null,
        null,
        (float) amplitude);
  }

  // ==================== 命中目标 ====================
  /**
   * 💥 命中敌方（Wave Impact）
   *
   * <p>"无形之剑，斩于意念之前。"
   * <p>波锋掠过血肉，敌体被气刃撕裂；伤口中荡起的，是剑心回响的余震。
   */
  @Override
  public void onHit(
      ServerLevel level, ShockfieldState state, LivingEntity target, double damageApplied) {
    Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
    Vec3 waveDir = targetPos.subtract(state.getCenter()).normalize();

    // 判断是敌方还是友方（简化判断：damageApplied > 0 为敌方）
    if (damageApplied > 0.0) {
      playHitEnemy(level, targetPos, waveDir, damageApplied);
    } else {
      playHitAlly(level, targetPos, target);
    }
  }

  /**
   * 敌方命中特效：气刃撕裂
   */
  private void playHitEnemy(ServerLevel level, Vec3 pos, Vec3 direction, double damage) {
    // 1. 气刃撕裂：SWEEP_ATTACK 粒子表现斩击
    level.sendParticles(
        ParticleTypes.SWEEP_ATTACK,
        pos.x,
        pos.y,
        pos.z,
        1,
        0.0,
        0.0,
        0.0,
        0.0);

    // 2. 剑心回响：青白色爆裂
    for (int i = 0; i < 8; i++) {
      double angle = (Math.PI * 2.0 * i) / 8.0;
      double dx = Math.cos(angle) * 0.3;
      double dz = Math.sin(angle) * 0.3;
      level.sendParticles(
          ParticleTypes.SOUL_FIRE_FLAME,
          pos.x,
          pos.y,
          pos.z,
          1,
          dx,
          0.2,
          dz,
          0.15);
    }

    // 3. 血雾效果（根据伤害量）
    int bloodCount = Math.min(5, (int) (damage * 0.5));
    for (int i = 0; i < bloodCount; i++) {
      level.sendParticles(
          ParticleTypes.DAMAGE_INDICATOR,
          pos.x + (Math.random() - 0.5) * 0.5,
          pos.y + (Math.random() - 0.5) * 0.5,
          pos.z + (Math.random() - 0.5) * 0.5,
          1,
          0.0,
          0.0,
          0.0,
          0.1);
    }

    // 音效：锐利的剑击 + 气刃破空
    float pitch = 1.0F + (float) Math.random() * 0.3F;
    level.playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.PLAYER_ATTACK_SWEEP,
        SoundSource.PLAYERS,
        0.8F,
        pitch);

    // 客户端特效
    AbilityFxDispatcher.play(
        level,
        HIT_ENEMY,
        pos,
        direction,
        direction,
        null,
        null,
        (float) Math.min(2.0, damage * 0.1));
  }

  /**
   * 🪶 命中友方 / 飞剑（Wave Resonance）
   *
   * <p>"同心共振，剑意循环。"
   * <p>波光拂过，盟者与飞剑皆获共鸣，剑身微颤，如在呼吸持有者的气息。
   */
  private void playHitAlly(ServerLevel level, Vec3 pos, LivingEntity target) {
    // 1. 共鸣涟漪：温和的青白色波纹
    for (int i = 0; i < 6; i++) {
      double angle = (Math.PI * 2.0 * i) / 6.0;
      double radius = 0.4;
      double dx = Math.cos(angle) * radius;
      double dz = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.GLOW,
          pos.x + dx,
          pos.y,
          pos.z + dz,
          1,
          0.0,
          0.1,
          0.0,
          0.02);
    }

    // 2. 剑意呼吸：SOUL 粒子螺旋上升
    for (int i = 0; i < 3; i++) {
      double angle = Math.random() * Math.PI * 2.0;
      double radius = 0.2;
      double dx = Math.cos(angle) * radius;
      double dz = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.SOUL,
          pos.x + dx,
          pos.y,
          pos.z + dz,
          1,
          0.0,
          0.2,
          0.0,
          0.05);
    }

    // 音效：和谐的共鸣音
    level.playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.AMETHYST_BLOCK_CHIME,
        SoundSource.PLAYERS,
        0.5F,
        1.2F);

    // 客户端特效
    AbilityFxDispatcher.play(level, HIT_ALLY, pos, Vec3.ZERO, Vec3.ZERO, null, target, 1.0F);
  }

  // ==================== 二级波包 ====================
  /**
   * 二级波包创建（飞剑触碰）
   *
   * <p>当主圈触碰飞剑，在命中点生成次级波。
   */
  @Override
  public void onSubwaveCreate(ServerLevel level, ShockfieldState parent, ShockfieldState sub) {
    Vec3 center = sub.getCenter();

    // 1. 涟光闪烁：剑身共鸣的视觉表现
    for (int i = 0; i < 8; i++) {
      double angle = (Math.PI * 2.0 * i) / 8.0;
      double radius = 0.25;
      double dx = Math.cos(angle) * radius;
      double dz = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.ELECTRIC_SPARK,
          center.x + dx,
          center.y + 0.1,
          center.z + dz,
          1,
          0.0,
          0.05,
          0.0,
          0.01);
    }

    // 2. 锋刃暗淡：少量 ASH 粒子表现耐久消耗
    for (int i = 0; i < 3; i++) {
      level.sendParticles(
          ParticleTypes.ASH,
          center.x + (Math.random() - 0.5) * 0.3,
          center.y + 0.1,
          center.z + (Math.random() - 0.5) * 0.3,
          1,
          0.0,
          0.05,
          0.0,
          0.01);
    }

    // 音效：轻微的剑鸣 + 金属摩擦
    level.playSound(
        null,
        center.x,
        center.y,
        center.z,
        SoundEvents.TRIDENT_HIT,
        SoundSource.PLAYERS,
        0.4F,
        1.5F);

    // 客户端特效
    AbilityFxDispatcher.play(
        level, SUBWAVE_CREATE, center, Vec3.ZERO, Vec3.ZERO, null, null, 0.6F);
  }

  // ==================== 波场熄灭 ====================
  /**
   * 🔚 震荡熄灭（Shockfield 终止）
   *
   * <p>"剑意回寂，波息如初。"
   * <p>振幅终散，真元消散于风，唯余轻吟一声，似剑在梦中安眠。
   */
  @Override
  public void onExtinguish(ServerLevel level, ShockfieldState state, ExtinguishReason reason) {
    Vec3 center = state.getCenter();
    double radius = state.getRadius();

    // 根据熄灭原因播放不同效果
    switch (reason) {
      case DAMPED_OUT -> playExtinguishNatural(level, center, radius);
      case LIFETIME_ENDED -> playExtinguishLifetime(level, center, radius);
      case OWNER_REMOVED -> playExtinguishRemoved(level, center);
    }
  }

  /**
   * 自然衰减熄灭：温和消散
   */
  private void playExtinguishNatural(ServerLevel level, Vec3 center, double radius) {
    // 气息收束：向中心汇聚的粒子
    int count = Math.max(4, (int) (radius * 0.5));
    for (int i = 0; i < count; i++) {
      double angle = (Math.PI * 2.0 * i) / count;
      double x = center.x + Math.cos(angle) * radius * 0.5;
      double z = center.z + Math.sin(angle) * radius * 0.5;
      double y = center.y + 0.1;

      // 向中心缓慢移动
      double dx = (center.x - x) * 0.1;
      double dz = (center.z - z) * 0.1;

      level.sendParticles(
          ParticleTypes.SOUL,
          x,
          y,
          z,
          1,
          dx,
          0.0,
          dz,
          0.05);
    }

    // 音效：轻柔的消散音
    level.playSound(
        null,
        center.x,
        center.y,
        center.z,
        SoundEvents.BEACON_DEACTIVATE,
        SoundSource.PLAYERS,
        0.3F,
        0.8F);

    // 客户端特效
    AbilityFxDispatcher.play(level, EXTINGUISH, center, Vec3.ZERO, Vec3.ZERO, null, null, 0.5F);
  }

  /**
   * 寿命结束：平静终结
   */
  private void playExtinguishLifetime(ServerLevel level, Vec3 center, double radius) {
    // 剑意安眠：缓慢上升的 SOUL 粒子
    for (int i = 0; i < 12; i++) {
      double angle = (Math.PI * 2.0 * i) / 12.0;
      double r = radius * 0.3;
      double x = center.x + Math.cos(angle) * r;
      double z = center.z + Math.sin(angle) * r;

      level.sendParticles(
          ParticleTypes.SOUL,
          x,
          center.y + 0.1,
          z,
          1,
          0.0,
          0.15,
          0.0,
          0.03);
    }

    // 音效：悠长的剑鸣余韵
    level.playSound(
        null,
        center.x,
        center.y,
        center.z,
        SoundEvents.BELL_RESONATE,
        SoundSource.PLAYERS,
        0.4F,
        1.5F);

    // 客户端特效
    AbilityFxDispatcher.play(level, EXTINGUISH, center, Vec3.ZERO, Vec3.ZERO, null, null, 1.0F);
  }

  /**
   * 强制移除：突然消失
   */
  private void playExtinguishRemoved(ServerLevel level, Vec3 center) {
    // 急速消散：向四周爆开
    for (int i = 0; i < 8; i++) {
      double angle = (Math.PI * 2.0 * i) / 8.0;
      double dx = Math.cos(angle) * 0.5;
      double dz = Math.sin(angle) * 0.5;

      level.sendParticles(
          ParticleTypes.POOF,
          center.x,
          center.y + 0.1,
          center.z,
          1,
          dx,
          0.1,
          dz,
          0.2);
    }

    // 音效：短促的断音
    level.playSound(
        null,
        center.x,
        center.y,
        center.z,
        SoundEvents.FIRE_EXTINGUISH,
        SoundSource.PLAYERS,
        0.5F,
        1.5F);
  }

  // ==================== 干涉特效（预留） ====================
  /**
   * ⚔️ Construct 干涉（波之共鸣） - 预留接口
   *
   * <p>"两道剑波同频共鸣，刹那之间，天地皆鸣。"
   * <p>当波与波相遇，光与气相融，剑意骤然放大，化作毁灭性的共振脉冲。
   */
  public void playInterferenceConstruct(
      ServerLevel level, Vec3 pos, ShockfieldState wave1, ShockfieldState wave2) {
    // 共鸣爆发：强烈的光爆
    for (int i = 0; i < 16; i++) {
      double angle = (Math.PI * 2.0 * i) / 16.0;
      double dx = Math.cos(angle) * 0.8;
      double dz = Math.sin(angle) * 0.8;
      level.sendParticles(
          ParticleTypes.FLASH,
          pos.x,
          pos.y,
          pos.z,
          1,
          dx,
          0.0,
          dz,
          0.5);
    }

    // 音效：震撼的共鸣
    level.playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.LIGHTNING_BOLT_THUNDER,
        SoundSource.PLAYERS,
        0.6F,
        1.8F);

    AbilityFxDispatcher.play(
        level, INTERFERENCE_CONSTRUCT, pos, Vec3.ZERO, Vec3.ZERO, null, null, 1.5F);
  }

  /**
   * 💠 Destruct 干涉（波之相杀） - 预留接口
   *
   * <p>"剑波错位，气脉逆流，天地俱寂。"
   * <p>共振失衡的一刻，光焰熄灭为灰，空气塌陷成真空，连剑鸣都被吞噬。
   */
  public void playInterferenceDestruct(
      ServerLevel level, Vec3 pos, ShockfieldState wave1, ShockfieldState wave2) {
    // 真空塌陷：向内聚集的黑暗
    for (int i = 0; i < 12; i++) {
      double angle = (Math.PI * 2.0 * i) / 12.0;
      double x = pos.x + Math.cos(angle) * 0.6;
      double z = pos.z + Math.sin(angle) * 0.6;

      // 向中心收缩
      double dx = (pos.x - x) * 0.3;
      double dz = (pos.z - z) * 0.3;

      level.sendParticles(
          ParticleTypes.SMOKE,
          x,
          pos.y,
          z,
          1,
          dx,
          0.0,
          dz,
          0.1);
    }

    // 光焰熄灭
    level.sendParticles(
        ParticleTypes.LARGE_SMOKE,
        pos.x,
        pos.y + 0.5,
        pos.z,
        8,
        0.3,
        0.3,
        0.3,
        0.05);

    // 音效：压抑的爆裂音
    level.playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.GENERIC_EXPLODE,
        SoundSource.PLAYERS,
        0.4F,
        0.5F);

    AbilityFxDispatcher.play(
        level, INTERFERENCE_DESTRUCT, pos, Vec3.ZERO, Vec3.ZERO, null, null, 1.0F);
  }
}
