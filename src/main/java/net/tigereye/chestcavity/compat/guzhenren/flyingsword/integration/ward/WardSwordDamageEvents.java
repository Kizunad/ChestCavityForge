package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward.IncomingThreat;
import net.tigereye.chestcavity.compat.guzhenren.registry.GRDamageTags;
import net.tigereye.chestcavity.compat.guzhenren.util.DamagePipeline;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 护幕飞剑伤害事件处理器
 * <p>
 * 监听玩家受到的伤害事件，触发护幕飞剑的拦截逻辑。
 * <ul>
 *   <li>检查玩家是否装备了"剑幕·反击之幕"器官</li>
 *   <li>构建 {@link IncomingThreat} 对象</li>
 *   <li>调用 {@link WardSwordService#onIncomingThreat} 尝试拦截</li>
 *   <li>如果拦截成功（C阶段），记录日志（D阶段将实现伤害减免）</li>
 * </ul>
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class WardSwordDamageEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(WardSwordDamageEvents.class);

    private static final String MOD_ID = "guzhenren";

    /**
     * 剑幕·反击之幕器官ID
     * TODO: 确保这个ID与实际的器官注册ID一致
     */
    private static final ResourceLocation BLOCKSHIELD_ORGAN_ID =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "blockshield");

    private WardSwordDamageEvents() {}

    /**
     * 监听玩家受到伤害事件，触发护幕拦截
     * <p>
     * 使用 HIGHEST 优先级，在大部分伤害修改之前运行
     *
     * @param event 伤害事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // 跳过标记为绕过器官钩子的伤害
        if (event.getSource().is(GRDamageTags.BYPASS_ORGAN_HOOKS)) {
            return;
        }

        // 跳过已在处理中的伤害（防止递归）
        if (DamagePipeline.guarded()) {
            return;
        }

        // 只处理玩家受到的伤害
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 检查玩家是否装备了剑幕器官
        if (!hasBlockShieldOrgan(player)) {
            return;
        }

        // 获取护幕服务实例
        DefaultWardSwordService service = DefaultWardSwordService.getInstance();

        // 如果没有护幕飞剑，跳过
        if (!service.hasWardSwords(player)) {
            return;
        }

        // 构建 IncomingThreat 对象
        IncomingThreat threat = buildThreat(event.getSource(), player);
        if (threat == null) {
            return;
        }

        try {
            // 尝试拦截
            boolean intercepted = service.onIncomingThreat(threat);

            if (intercepted) {
                LOGGER.debug("Ward sword intercept initiated for player {} against threat from {}",
                    player.getName().getString(),
                    threat.attacker() != null ? threat.attacker().getName().getString() : "unknown");

                // D阶段：拦截成功时减免伤害
                // 根据护幕设计，拦截成功时伤害应该清零或大幅减免
                // 这里实现"穿甲保留 30%" 规则：拦截成功后，玩家只受到 30% 的伤害
                float originalDamage = event.getAmount();
                float reducedDamage = originalDamage * WardConfig.ARMOR_PENETRATION_FACTOR;
                event.setAmount(reducedDamage);

                LOGGER.debug("Damage reduced from {} to {} ({}% armor penetration)",
                    originalDamage, reducedDamage, (int)(WardConfig.ARMOR_PENETRATION_FACTOR * 100));
            }

        } catch (Exception e) {
            LOGGER.error("Error processing ward sword intercept for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * 从伤害源构建 IncomingThreat 对象
     *
     * @param source 伤害源
     * @param target 受害玩家
     * @return IncomingThreat 对象，如果无法构建则返回 null
     */
    private static IncomingThreat buildThreat(DamageSource source, Player target) {
        Entity directEntity = source.getDirectEntity();
        Entity attacker = source.getEntity();

        // 如果没有直接实体，可能是环境伤害，暂不处理
        if (directEntity == null && attacker == null) {
            return null;
        }

        // 确定攻击者（优先使用实际攻击者，其次使用直接实体）
        LivingEntity attackerEntity = null;
        if (attacker instanceof LivingEntity living) {
            attackerEntity = living;
        } else if (directEntity instanceof LivingEntity living) {
            attackerEntity = living;
        }

        // 如果是投射物攻击
        if (directEntity instanceof Projectile projectile) {
            Vec3 velocity = projectile.getDeltaMovement();

            return new IncomingThreat(
                attackerEntity,
                target,
                projectile.position(),
                velocity,
                velocity.length(),
                IncomingThreat.Type.PROJECTILE,
                target.level().getGameTime()
            );
        }

        // 如果是近战攻击
        if (attackerEntity != null) {
            // 近战攻击：使用攻击者的速度作为方向
            Vec3 attackerPos = attackerEntity.position();
            Vec3 toTarget = target.position().subtract(attackerPos).normalize();
            double speed = attackerEntity.getDeltaMovement().length();

            // 如果攻击者静止，使用一个默认速度
            if (speed < 0.1) {
                speed = 0.5; // 假设近战攻击速度
            }

            Vec3 velocity = toTarget.scale(speed);

            return new IncomingThreat(
                attackerEntity,
                target,
                attackerPos,
                velocity,
                speed,
                IncomingThreat.Type.MELEE,
                target.level().getGameTime()
            );
        }

        return null;
    }

    /**
     * 检查玩家是否装备了剑幕·反击之幕器官
     *
     * @param player 玩家
     * @return 是否装备了剑幕器官
     */
    private static boolean hasBlockShieldOrgan(Player player) {
        return ChestCavityEntity.of(player)
            .map(ChestCavityEntity::getChestCavityInstance)
            .filter(cc -> cc.inventory != null)
            .map(cc -> {
                for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
                    var stack = cc.inventory.getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    ResourceLocation itemId =
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemId != null && itemId.equals(BLOCKSHIELD_ORGAN_ID)) {
                        return true;
                    }
                }
                return false;
            })
            .orElse(false);
    }
}
