package net.tigereye.chestcavity.soul.fakeplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.tigereye.chestcavity.soul.registry.SoulHurtResult;
import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandlerRegistry;
import net.tigereye.chestcavity.soul.fakeplayer.SoulRuntimeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * 灵魂假人：基于 FakePlayer 的定制实现
 *
 * 目标
 * - 在服务端以“接近真实玩家”的方式参与战斗/受击/生命与属性变化，供灵魂系统复原与演示。
 * - 直接使用 FakePlayer 自身进行渲染，以便保持与真实玩家一致的外观/皮肤路径。
 */
public class SoulPlayer extends FakePlayer {

    private final @Nullable UUID ownerId;
    private final UUID soulId;
    private long lastFoodTick = Long.MIN_VALUE;

    private SoulPlayer(ServerLevel level, GameProfile profile, UUID soulId, @Nullable UUID ownerId) {
        super(level, profile);
        this.ownerId = ownerId;
        this.soulId = soulId;
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setInvulnerable(false);
        applySurvivalDefaults();
    }

    public static SoulPlayer create(ServerPlayer owner, UUID soulId, GameProfile profile) {
        SoulPlayer soulPlayer = new SoulPlayer(owner.serverLevel(), profile, soulId, owner.getUUID());
        soulPlayer.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        soulPlayer.reapplyPosition();
        soulPlayer.setCustomNameVisible(false);
        soulPlayer.bootstrapCapabilities();
        return soulPlayer;
    }

    private void applySurvivalDefaults() {
        var abilities = this.getAbilities();
        abilities.flying = false;
        abilities.instabuild = false;
        abilities.invulnerable = false;
        abilities.mayBuild = true;
        this.onUpdateAbilities();
        this.setHealth(this.getMaxHealth());
        this.setGameMode(GameType.SURVIVAL);
    }

    private void bootstrapCapabilities() {
        // TODO: 当附件子系统落地后，通过 AttachCapabilitiesEvent 注册所需能力
    }

    public Optional<UUID> getOwnerId() {
        return Optional.ofNullable(ownerId);
    }

    public UUID getSoulId() {
        return soulId;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return false;
    }

    @Override
    public boolean canHarmPlayer(Player player) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 让注册表有机会取消/修改伤害
        SoulHurtResult routed = SoulRuntimeHandlerRegistry.onHurt(this, source, amount);
        switch (routed.action()) {
            case CANCEL:
                return false;
            case APPLY:
                return routed.appliedResult();
            case MODIFY:
                return SoulRuntimeUtil.applyVanillaHurt(this, source, routed.amount());
            case PASS:
            default:
                return SoulRuntimeUtil.applyVanillaHurt(this, source, amount);
        }
    }

    @Override
    public void tick() {
        this.noPhysics = false;
        this.setNoGravity(false);
        SoulRuntimeHandlerRegistry.onTickStart(this);
        super.tick();
        this.travel(Vec3.ZERO);
        // TODO: AI / behaviour tree hooks
        SoulRuntimeHandlerRegistry.onTickEnd(this);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // Death should fully remove entity + profile data and associated UUIDs
        SoulFakePlayerSpawner.onSoulPlayerRemoved(this, "die");
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        // Generic removal keeps profile by default; do not purge unless it was a death
        SoulFakePlayerSpawner.onSoulPlayerRemoved(this, String.valueOf(reason));
    }

    public long getLastFoodTick() {
        return lastFoodTick;
    }

    public void setLastFoodTick(long tick) {
        this.lastFoodTick = tick;
    }
}
