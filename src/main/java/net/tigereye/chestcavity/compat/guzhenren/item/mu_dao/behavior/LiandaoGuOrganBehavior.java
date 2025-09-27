package net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.util.NBTWriter;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Active behaviour for 镰刀蛊. Handles resource consumption, cooldown management
 * and the delayed blade wave attack sequence.
 */
public enum LiandaoGuOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "liandaogu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final ResourceLocation JIAN_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jian_dao_increase_effect");
    private static final ResourceLocation JIN_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jin_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String COOLDOWN_KEY = "LiandaoGuCooldown";

    private static final double BASE_DAMAGE = 30.0;
    private static final double BASE_ZHENYUAN_COST = 120.0;
    private static final double BASE_JINGLI_COST = 30.0;

    private static final int CHARGE_DURATION_TICKS = 20;
    private static final int RELEASE_TELEGRAPH_TICKS = 5;
    private static final int MIN_COOLDOWN_TICKS = 160; // 8 seconds
    private static final int COOLDOWN_VARIANCE_TICKS = 80; // +0-80 ticks (8-12s window)

    private static final double WAVE_LENGTH = 8.0;
    private static final double WAVE_HALF_WIDTH = 1.2;
    private static final double WAVE_HALF_HEIGHT = 1.5;
    private static final double KNOCKBACK_FORCE = 1.2;

    private static final int EFFECT_DURATION_TICKS = 20; // 1 second

    private static final double EPSILON = 1.0E-4;

        // 顶部常量区 —— 新增两个角度来控制刀光倾斜/斜切
    private static final double SLASH_PITCH_DEG = -25.0; // 负值=朝下斜 “/”，正值=朝上斜 “\”
    private static final double SLASH_ROLL_DEG  = 35.0;  // 绕forward的滚转，决定刀光条带的“歪斜感”


        // 工具方法 —— 基于 forward 生成 right / upLocal，避免正上/正下奇异
    private static class Basis {
        final Vec3 f;  // forward (单位向量)
        final Vec3 r;  // right    (单位向量)
        final Vec3 u;  // upLocal  (单位向量，垂直于 f 和 r)
        Basis(Vec3 f, Vec3 r, Vec3 u){ this.f=f; this.r=r; this.u=u; }
    }

    private static Basis makeBasis(Vec3 forward) { // NEW
        Vec3 f = forward.normalize();
        // 与世界Up接近平行时，选一个备用Up避免叉积退化
        Vec3 worldUp = Math.abs(f.y) > 0.95 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
        Vec3 r = f.cross(worldUp);
        if (r.lengthSqr() < EPSILON) r = new Vec3(1,0,0);
        r = r.normalize();
        Vec3 u = r.cross(f).normalize();
        return new Basis(f, r, u);
    }

        // 把俯仰和滚转角应用到方向上 —— 产生“斜向主轴 dir”与“宽度轴 wAxis”
    private static Vec3 computeSlashDir(Basis b, double pitchDeg) { // NEW
        double a = Math.toRadians(pitchDeg);
        // 在 forward 与 upLocal 张成的平面内旋转
        return b.f.scale(Math.cos(a)).add(b.u.scale(Math.sin(a))).normalize();
    }

    private static Vec3 computeWidthAxis(Basis b, double rollDeg) { // NEW
        double g = Math.toRadians(rollDeg);
        // 宽度方向在 right 与 upLocal 张成的平面内再“歪”一点
        return b.r.scale(Math.cos(g)).add(b.u.scale(Math.sin(g))).normalize();
    }


    static {
        OrganActivationListeners.register(ABILITY_ID, LiandaoGuOrganBehavior::activateAbility);
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT);
        ensureChannel(cc, JIN_DAO_INCREASE_EFFECT);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // 镰刀蛊没有需要恢复的点数，仅使用冷却；慢速心跳不触发行为。
        return;
    }

    private static LinkageChannel ensureChannel(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        return context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long gameTime = level.getGameTime();
        long nextAllowed = readCooldown(organ);
        if (nextAllowed > gameTime) {
            return;
        }

        if (!tryConsumeResources(player)) {
            return;
        }

        int cooldown = MIN_COOLDOWN_TICKS;
        RandomSource random = player.getRandom();
        if (COOLDOWN_VARIANCE_TICKS > 0) {
            cooldown += random.nextInt(COOLDOWN_VARIANCE_TICKS + 1);
        }
        writeCooldown(organ, gameTime + cooldown);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);

        double swordMultiplier = 1.0 + ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT).get();
        double metalMultiplier = 1.0 + ensureChannel(cc, JIN_DAO_INCREASE_EFFECT).get();
        double totalMultiplier = Math.max(0.0, swordMultiplier * metalMultiplier);
        double damageAmount = BASE_DAMAGE * totalMultiplier;

        Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);

 

        Vec3 look = player.getLookAngle();
        Vec3 fallback = player.getForward();
        Vec3 baseForward = look.lengthSqr() < EPSILON ? fallback : look;
        Vec3 forward = baseForward.lengthSqr() < EPSILON
                ? new Vec3(0.0, 0.0, 1.0)
                : baseForward.normalize();

                // NEW: 建立局部基并求“斜向主轴”和“宽度轴”
        Basis basis = makeBasis(forward);
        Vec3 dir   = computeSlashDir(basis, SLASH_PITCH_DEG);
        Vec3 wAxis = computeWidthAxis(basis, SLASH_ROLL_DEG);

        // 上移一格，再往前推出一格
        Vec3 shiftedOrigin = origin.add(0.0, 1.0, 0.0).add(dir.scale(1.0));
        playChargeStartEffects(serverLevel, player);
        spawnChargeParticles(serverLevel, player, 0);
        schedule(serverLevel, () -> spawnChargeParticles(serverLevel, player, 1), 1);
        schedule(serverLevel, () -> spawnChargeParticles(serverLevel, player, 2), 2);

        Vec3 telegraphOrigin = shiftedOrigin.add(dir.scale(0.6)); // 用 dir 而不是 forward
        schedule(serverLevel, () -> playTelegraph(serverLevel, telegraphOrigin, dir, wAxis), CHARGE_DURATION_TICKS);

        Vec3 impactCenter = shiftedOrigin.add(dir.scale(WAVE_LENGTH * 0.75));
        schedule(serverLevel, () -> applyBladeWave(
                serverLevel, player, cc, origin, dir, wAxis, damageAmount, impactCenter
        ), RELEASE_TELEGRAPH_TICKS);
    }

    private void handleNonPlayerSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) { }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        // 玩家受击不触发镰刀蛊的被动释放，直接退出
        if (victim instanceof Player) {
            return damage;
        }
        Level level = victim.level();
        if (!(level instanceof ServerLevel server)) {
            return damage;
        }
        long gameTime = server.getGameTime();
        long nextAllowed = readCooldown(organ);
        if (nextAllowed > gameTime) {
            return damage;
        }
        RandomSource random = victim.getRandom();
        if (random.nextInt(10) != 0) {
            return damage;
        }

        // 计算从受击者指向攻击者的方向
        LivingEntity attacker = null;
        if (source != null) {
            if (source.getEntity() instanceof LivingEntity le) {
                attacker = le;
            } else if (source.getDirectEntity() instanceof LivingEntity le2) {
                attacker = le2;
            }
        }
        Vec3 origin = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
        Vec3 forward;
        if (attacker != null) {
            forward = attacker.position().subtract(origin).normalize();
        } else {
            forward = victim.getLookAngle();
        }
        if (forward.lengthSqr() < EPSILON) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }

        // 伤害倍率：剑道与金道联动
        double swordMultiplier = 1.0 + ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT).get();
        double metalMultiplier = 1.0 + ensureChannel(cc, JIN_DAO_INCREASE_EFFECT).get();
        double totalMultiplier = Math.max(0.0, swordMultiplier * metalMultiplier);
        double damageAmount = BASE_DAMAGE * totalMultiplier;

        Basis basis = makeBasis(forward);
        Vec3 dir   = computeSlashDir(basis, SLASH_PITCH_DEG);
        Vec3 wAxis = computeWidthAxis(basis, SLASH_ROLL_DEG);
        Vec3 impactCenter = origin.add(dir.scale(WAVE_LENGTH * 0.75));

        // 写入冷却并同步
        int cooldown = MIN_COOLDOWN_TICKS + (COOLDOWN_VARIANCE_TICKS > 0 ? random.nextInt(COOLDOWN_VARIANCE_TICKS + 1) : 0);
        writeCooldown(organ, gameTime + cooldown);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);

        // 立即释放刀光，指向攻击者
        applyBladeWave(server, victim, cc, origin, dir, wAxis, damageAmount, impactCenter);
        return damage;
    }

    private static void playChargeStartEffects(ServerLevel serverLevel, Player player) {
        double x = player.getX();
        double y = player.getY(0.5);
        double z = player.getZ();
        serverLevel.playSound(null, x, y, z, SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.7f, 0.7f);
        serverLevel.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private static void spawnChargeParticles(ServerLevel serverLevel, Player player, int stage) {
        if (!player.isAlive()) {
            return;
        }
        double centerX = player.getX();
        double centerY = player.getY(0.7 + 0.1 * stage);
        double centerZ = player.getZ();
        RandomSource random = player.getRandom();
        double radius = 0.6 + stage * 0.1;
        /*for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * Mth.TWO_PI + random.nextDouble() * 0.2;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    centerX + offsetX,
                    centerY,
                    centerZ + offsetZ,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }*/
        serverLevel.sendParticles(ParticleTypes.ENCHANT, centerX, centerY, centerZ, 6, 0.2, 0.25, 0.2, 0.01);
    }

    private static void playTelegraph(ServerLevel s, Vec3 origin, Vec3 dir, Vec3 wAxis) { // CHANGED
        if (dir.lengthSqr() < EPSILON) return;

        s.playSound(null, origin.x, origin.y, origin.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
        s.playSound(null, origin.x, origin.y, origin.z, SoundEvents.BLAZE_SHOOT,          SoundSource.PLAYERS, 0.5f, 1.4f);
        s.playSound(null, origin.x, origin.y, origin.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.3f, 1.6f);

        int segments = 22;
        int stripes  = 3; // 横向“条带”层数，营造可见刀面
        double halfW = WAVE_HALF_WIDTH * 0.9;

        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 center = origin.add(dir.scale(t * WAVE_LENGTH));

            for (int k = -stripes; k <= stripes; k++) {
                double w = (k / (double) stripes) * halfW;
                Vec3 p = center.add(wAxis.scale(w));
                s.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.04, 0.02, 0.04, 0.0);
                if (i % 2 == 0) {
                    s.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.06, 0.04, 0.06, 0.01);
                }
            }
        }
        Vec3 flash = origin.add(dir.scale(0.5));
        s.sendParticles(ParticleTypes.FLASH, flash.x, flash.y, flash.z, 1, 0, 0, 0, 0);
    }
    private static void applyBladeWave(
            ServerLevel server,
            LivingEntity user,
            ChestCavityInstance cc,
            Vec3 origin,
            Vec3 dir,
            Vec3 wAxis,
            double damageAmount,
            Vec3 impactCenter
    ) { // CHANGED

        if (user == null || !user.isAlive()) {
            return;
        }

        AABB hitbox = new AABB(origin, origin.add(dir.scale(WAVE_LENGTH)))
                .inflate(WAVE_HALF_WIDTH, WAVE_HALF_HEIGHT, WAVE_HALF_WIDTH);

        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, hitbox,
                e -> e != user && e.isAlive());

        DamageSource damageSource = createBladeDamageSource(user);
        for (LivingEntity target : targets) {
            float dmg = (float) damageAmount;
            if (dmg > 0) {
                target.hurt(damageSource, dmg);
            }
            target.knockback(KNOCKBACK_FORCE, -dir.x, -dir.z); // 用斜向 knockback
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION_TICKS, 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION_TICKS, 0, false, true, true));
            server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        }

        server.playSound(null, impactCenter.x, impactCenter.y, impactCenter.z, SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 0.7f, 1.1f);
        server.playSound(null, impactCenter.x, impactCenter.y, impactCenter.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.2f);

        spawnResidualParticles(server, origin, dir, wAxis); // 余晖粒子也用斜向
        affectBlocksAlongSlash(server, origin, dir, wAxis, user);
    }

    private static DamageSource createBladeDamageSource(LivingEntity user) {
        if (user instanceof Player player) {
            return player.damageSources().playerAttack(player);
        }
        return user.damageSources().mobAttack(user);
    }

    private static void spawnResidualParticles(ServerLevel s, Vec3 origin, Vec3 dir, Vec3 wAxis) { // CHANGED
        Vec3 end = origin.add(dir.scale(WAVE_LENGTH));
        RandomSource rnd = s.getRandom();

        for (int i = 0; i < 28; i++) {
            double t = rnd.nextDouble();
            double w = (rnd.nextDouble() * 2 - 1) * (WAVE_HALF_WIDTH * 0.8);
            Vec3 p = origin.add(dir.scale(t * WAVE_LENGTH)).add(wAxis.scale(w));

            s.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.08, 0.06, 0.08, 0.01);
            s.sendParticles(ParticleTypes.POOF,  p.x, p.y, p.z, 1, 0.12, 0.04, 0.12, 0.02);
            if (rnd.nextInt(5) == 0) {
                s.sendParticles(ParticleTypes.FLASH, p.x, p.y + 0.08, p.z, 1, 0, 0, 0, 0);
            }
        }
        s.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 12, 0.35, 0.35, 0.35, 0.05);
    }


    private static void affectBlocks(ServerLevel serverLevel, Player player) {
        Direction facing = player.getDirection();
        BlockPos basePos = player.blockPosition().relative(facing, 2);
        for (int dy = 0; dy < 3; dy++) {
            for (int offset = -1; offset <= 1; offset++) {
                BlockPos targetPos;
                if (facing.getAxis() == Direction.Axis.X) {
                    targetPos = basePos.offset(0, dy, offset);
                } else {
                    targetPos = basePos.offset(offset, dy, 0);
                }
                processBlock(serverLevel, player, targetPos);
            }
        }
    }

    private static void affectBlocksAlongSlash(ServerLevel s, Vec3 origin, Vec3 dir, Vec3 wAxis, LivingEntity user) { // OPTIONAL
        if (!(user instanceof Player player)) {
            return;
        }
        // 取靠近玩家前方 2.5 格处作为切割带中心线
        Vec3 base = origin.add(dir.scale(2.5));
        // 沿 wAxis 采样 [-1,0,1] 三列，沿 dir 向前 [0,1,2] 三段，并在竖直方向 y 偏移 [0,1,2]
        for (int w = -1; w <= 1; w++) {
            for (int step = 0; step <= 2; step++) {
                Vec3 line = base.add(dir.scale(step)).add(wAxis.scale(w));
                BlockPos pos = BlockPos.containing(line);
                for (int dy = 0; dy < 3; dy++) {
                    processBlock(s, player, pos.above(dy));
                }
            }
        }
    }


    private static void processBlock(ServerLevel serverLevel, Player player, BlockPos pos) {
        BlockState state = serverLevel.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        if (isSoftBlock(state)) {
            serverLevel.destroyBlock(pos, true, player);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.25, 0.25, 0.25, 0.1);
        } else {
            serverLevel.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.PLAYERS, 0.5f, 1.1f);
                        // 额外的金属"叮"声
            serverLevel.playSound(null, pos,
                    SoundEvents.NETHERITE_BLOCK_HIT, // 也可以换成 NETHERITE_BLOCK_HIT
                    SoundSource.PLAYERS, 0.3f, 0.8f);

            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
        }
    }

    private static boolean isSoftBlock(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.WOOL);
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && ORGAN_ID.equals(id)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryConsumeResources(Player player) {
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return false;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        OptionalDouble jingliBeforeOpt = handle.getJingli();
        if (jingliBeforeOpt.isEmpty()) {
            return false;
        }
        double jingliBefore = jingliBeforeOpt.getAsDouble();
        if (jingliBefore + EPSILON < BASE_JINGLI_COST) {
            return false;
        }
        OptionalDouble jingliAfterOpt = handle.adjustJingli(-BASE_JINGLI_COST, true);
        if (jingliAfterOpt.isEmpty()) {
            return false;
        }
        OptionalDouble zhenyuanResult = handle.consumeScaledZhenyuan(BASE_ZHENYUAN_COST);
        if (zhenyuanResult.isPresent()) {
            return true;
        }
        handle.adjustJingli(BASE_JINGLI_COST, true);
        return false;
    }

    private static void schedule(ServerLevel level, Runnable runnable, int delayTicks) {
        if (delayTicks <= 0) {
            runnable.run();
            return;
        }
        level.getServer().execute(() -> schedule(level, runnable, delayTicks - 1));
    }

    private static long readCooldown(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0L;
        }
        CompoundTag tag = data.copyTag();
        return tag.getLong(COOLDOWN_KEY);
    }

    private static void writeCooldown(ItemStack stack, long value) {
        NBTWriter.updateCustomData(stack, tag -> tag.putLong(COOLDOWN_KEY, value));
    }
}
