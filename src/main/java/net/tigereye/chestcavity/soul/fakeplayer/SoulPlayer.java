package net.tigereye.chestcavity.soul.fakeplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
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
        // 参照玩家受击流程，实现最小可用的服务器端结算（吸收护盾→生命值）
        if (this.level().isClientSide() || this.isRemoved()) {
            return false;
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        float remaining = Math.max(0.0F, amount);
        if (remaining == 0.0F) {
            return false;
        }
        float absorption = this.getAbsorptionAmount();
        if (absorption > 0.0F) {
            float absorbed = Math.min(absorption, remaining);
            this.setAbsorptionAmount(absorption - absorbed);
            remaining -= absorbed;
        }
        if (remaining <= 0.0F) {
            return true;
        }
        float currentHealth = this.getHealth();
        float newHealth = Math.max(0.0F, currentHealth - remaining);
        this.getCombatTracker().recordDamage(source, remaining);
        this.setHealth(newHealth);
        this.markHurt();
        this.invulnerableTime = 20;
        this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
        if (newHealth <= 0.0F) {
            this.die(source);
        }
        return true;
    }

    @Override
    public void tick() {
        this.noPhysics = false;
        this.setNoGravity(false);
        super.tick();
        this.travel(Vec3.ZERO);
        // TODO: AI / behaviour tree hooks
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        SoulFakePlayerSpawner.onSoulPlayerRemoved(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        SoulFakePlayerSpawner.onSoulPlayerRemoved(this);
    }
}
