package kizuna.guzhenren_event_ext.common.system_modules.actions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.util.EntitySpawnUtil;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

import java.util.List;

/**
 * Action：在玩家附近生成随机敌对蛊修实体
 *
 * JSON 配置格式：
 * {
 *   "type": "guzhenren_event_ext:spawn_hostile_gucultivator",
 *   "count": 3,                           // 必需：生成数量
 *   "min_distance": 5.0,                  // 可选：最小距离（格），默认5.0
 *   "max_distance": 10.0,                 // 可选：最大距离（格），默认10.0
 *   "health_multiplier": 1.5,             // 可选：生命值倍数，默认1.0（不增益）
 *   "damage_multiplier": 1.3,             // 可选：攻击力倍数，默认1.0
 *   "speed_multiplier": 1.2,              // 可选：移动速度倍数，默认1.0
 *   "promote_to_same_realm": true,        // 可选：是否提升到同等境界（复制玩家属性并装备蛊虫），默认false
 *   "custom_name": "§c强大的蛊修",        // 可选：自定义名称（支持颜色代码）
 *   "notification_message": "§c附近出现了敌对蛊修！", // 可选：向玩家发送的通知消息
 *   "entity_tag": "blood_moon_boss"       // 可选：实体标签，用于击杀追踪和奖励系统
 * }
 */
public class SpawnHostileGuCultivatorAction implements IAction {

    /**
     * 所有可用的蛊修实体ID（来自 docs/蛊修EntityList.md）
     */
    private static final List<String> CULTIVATOR_IDS = List.of(
        // 男性蛊修
        "guzhenren:beiyuannanguxiu",
        "guzhenren:beiyuannanguxiu_1",
        "guzhenren:donghainangushi",
        "guzhenren:donghainangushi_2",
        "guzhenren:nan_gu_xiu",
        "guzhenren:nan_gu_xiu_1",
        "guzhenren:ximonangushi",
        "guzhenren:ximonangushi_2",
        "guzhenren:zhongzhounanguxiu",
        "guzhenren:zhongzhounanguxiu_1",
        // 女性蛊修
        "guzhenren:beiyuannvguxiu",
        "guzhenren:beiyuannvguxiu_1",
        "guzhenren:donghainvgushi",
        "guzhenren:donghainvgushi_2",
        "guzhenren:nv_gu_xiu",
        "guzhenren:nv_gu_xiu_1",
        "guzhenren:ximonvgushi",
        "guzhenren:ximonvgushi_2",
        "guzhenren:zhongzhounvgushi",
        "guzhenren:zhongzhounvgushi_1"
    );

    @Override
    public void execute(Player player, JsonObject definition) {
        // 仅在服务端执行
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        RandomSource random = serverPlayer.getRandom();

        // ==================== JSON 参数解析 ====================
        int count = definition.has("count") ? definition.get("count").getAsInt() : 1;
        double minDistance = definition.has("min_distance") ? definition.get("min_distance").getAsDouble() : 5.0;
        double maxDistance = definition.has("max_distance") ? definition.get("max_distance").getAsDouble() : 10.0;
        double healthMultiplier = definition.has("health_multiplier") ? definition.get("health_multiplier").getAsDouble() : 1.0;
        double damageMultiplier = definition.has("damage_multiplier") ? definition.get("damage_multiplier").getAsDouble() : 1.0;
        double speedMultiplier = definition.has("speed_multiplier") ? definition.get("speed_multiplier").getAsDouble() : 1.0;
        String customName = definition.has("custom_name") ? definition.get("custom_name").getAsString() : null;
        String notificationMessage = definition.has("notification_message") ? definition.get("notification_message").getAsString() : null;
        String entityIdOverride = definition.has("entity_id") ? definition.get("entity_id").getAsString() : null;
        String dialogue = definition.has("dialogue_message") ? definition.get("dialogue_message").getAsString() : null;
        boolean promoteToSameRealm = definition.has("promote_to_same_realm") && definition.get("promote_to_same_realm").getAsBoolean();
        String entityTag = definition.has("entity_tag") ? definition.get("entity_tag").getAsString() : null;

        // 参数验证（用户选择：不限制数量）
        count = Math.max(1, count); // 至少生成1个
        minDistance = Math.max(3.0, minDistance); // 最小距离至少3格
        maxDistance = Math.max(minDistance, maxDistance); // 最大距离不能小于最小距离

        int successCount = 0;

        // ==================== 生成循环 ====================
        for (int i = 0; i < count; i++) {
            // 1. 随机选择蛊修实体ID
            String cultivatorId = entityIdOverride != null && !entityIdOverride.isBlank()
                ? entityIdOverride
                : CULTIVATOR_IDS.get(random.nextInt(CULTIVATOR_IDS.size()));
            ResourceLocation entityId = ResourceLocation.parse(cultivatorId);

            // 2. 计算生成位置（玩家周围随机位置）
            Vec3 spawnPos = findSpawnPosition(serverPlayer, level, random, minDistance, maxDistance);
            if (spawnPos == null) {
                GuzhenrenEventExtension.LOGGER.warn("Failed to find spawn position for GuCultivator near player {}, using fallback position", serverPlayer.getName().getString());
                // 用户选择：强制生成 - 使用简单的后备位置
                spawnPos = getFallbackSpawnPosition(serverPlayer, random, minDistance);
            }

            // 3. 生成实体
            Entity entity = EntitySpawnUtil.spawn(
                level,
                entityId,
                spawnPos,
                random.nextFloat() * 360.0f, // 随机朝向
                0.0f,
                false // 启用AI
            );

            if (entity == null) {
                GuzhenrenEventExtension.LOGGER.warn("Failed to spawn GuCultivator {} near player {}", cultivatorId, serverPlayer.getName().getString());
                continue;
            }

            // 4. 设置敌对目标（用户选择：仅攻击触发者）
            if (entity instanceof Mob mob) {
                // 添加攻击触发者的Goal
                mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                    mob,
                    Player.class,
                    10,    // checkInterval
                    true,  // mustSee
                    false, // mustReach
                    (target) -> target.getUUID().equals(serverPlayer.getUUID()) // 仅攻击触发者
                ));

                // 立即设置目标
                mob.setTarget(serverPlayer);
            }

            // 5. 应用属性增益 + （可选）提升到同等境界/镜像玩家基础属性 + 装备蛊虫
            if (entity instanceof LivingEntity livingEntity) {
                // 5.1 属性倍数调整
                applyAttributeBuffs(livingEntity, healthMultiplier, damageMultiplier, speedMultiplier);

                // 5.2 同境界镜像：复制玩家基础属性（生命、攻击、防御、速度）
                if (promoteToSameRealm) {
                    mirrorPlayerBaseAttributes(livingEntity, serverPlayer);
                    // 5.3 依据玩家转数，从对应Tag中随机6个蛊虫并装备到护甲槽与手部
                    tryEquipGuWormsByZhuanshu(livingEntity, serverPlayer, level, random);
                }
            }

            // 6. 设置自定义名称（可选）
            if (customName != null && !customName.isEmpty()) {
                entity.setCustomName(Component.literal(customName));
                entity.setCustomNameVisible(true);
            }

            // 6.5. 设置实体标签（用于击杀追踪）
            if (entityTag != null && !entityTag.isEmpty()) {
                entity.getPersistentData().putString("guzhenren_event_ext:entity_tag", entityTag);
                GuzhenrenEventExtension.LOGGER.debug("Tagged entity {} with tag: {}", cultivatorId, entityTag);
            }

            successCount++;
            GuzhenrenEventExtension.LOGGER.debug("Spawned hostile GuCultivator {} at {} for player {}",
                cultivatorId, spawnPos, serverPlayer.getName().getString());

            // 6+. 对玩家说话（对话仅示意，使用系统消息模拟）
            if (dialogue != null && !dialogue.isEmpty()) {
                String speaker = customName != null && !customName.isEmpty() ? customName : entity.getName().getString();
                serverPlayer.sendSystemMessage(Component.literal("§7" + speaker + "：§r" + dialogue));
            }
        }

        // 7. 向玩家发送通知消息（用户选择：JSON可配置）
        if (notificationMessage != null && !notificationMessage.isEmpty() && successCount > 0) {
            // 替换占位符 {count}
            String finalMessage = notificationMessage.replace("{count}", String.valueOf(successCount));
            serverPlayer.sendSystemMessage(Component.literal(finalMessage));
        }

        GuzhenrenEventExtension.LOGGER.info("Successfully spawned {} GuCultivator(s) near player {}", successCount, serverPlayer.getName().getString());
    }

    /**
     * 寻找玩家周围的合适生成位置
     *
     * @return 合适的生成位置，如果10次尝试都失败则返回null
     */
    private Vec3 findSpawnPosition(ServerPlayer player, ServerLevel level, RandomSource random, double minDist, double maxDist) {
        Vec3 playerPos = player.position();

        // 尝试最多10次
        for (int attempt = 0; attempt < 10; attempt++) {
            // 随机角度和距离
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = minDist + random.nextDouble() * (maxDist - minDist);
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;

            // 计算地面高度
            BlockPos targetPos = new BlockPos(
                (int) (playerPos.x + offsetX),
                (int) playerPos.y,
                (int) (playerPos.z + offsetZ)
            );

            BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos);

            // 验证位置有效性
            if (groundPos.getY() < level.getMinBuildHeight() || groundPos.getY() > level.getMaxBuildHeight() - 3) {
                continue;
            }

            // 检查是否有足够空间（2格高）
            BlockPos checkPos1 = groundPos.above();
            BlockPos checkPos2 = groundPos.above(2);
            if (!level.getBlockState(checkPos1).isAir() || !level.getBlockState(checkPos2).isAir()) {
                continue;
            }

            return new Vec3(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
        }

        return null; // 10次尝试都失败
    }

    /**
     * 后备生成位置（强制生成）
     * 用户选择：当找不到合适位置时，强制在玩家附近生成
     */
    private Vec3 getFallbackSpawnPosition(ServerPlayer player, RandomSource random, double minDist) {
        Vec3 playerPos = player.position();
        double angle = random.nextDouble() * Math.PI * 2;
        double offsetX = Math.cos(angle) * minDist;
        double offsetZ = Math.sin(angle) * minDist;

        return playerPos.add(offsetX, 0, offsetZ);
    }

    /**
     * 应用属性增益
     *
     * @param entity 目标实体
     * @param healthMult 生命值倍数
     * @param damageMult 攻击力倍数
     * @param speedMult 移动速度倍数
     */
    private void applyAttributeBuffs(LivingEntity entity, double healthMult, double damageMult, double speedMult) {
        // 1. 生命值增益
        if (healthMult != 1.0) {
            AttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentMax = maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(currentMax * healthMult);
                entity.setHealth(entity.getMaxHealth()); // 回满血
                GuzhenrenEventExtension.LOGGER.debug("Applied health buff: {} -> {}", currentMax, entity.getMaxHealth());
            }
        }

        // 2. 攻击力增益
        if (damageMult != 1.0) {
            AttributeInstance attackAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttr != null) {
                double currentAttack = attackAttr.getBaseValue();
                attackAttr.setBaseValue(currentAttack * damageMult);
                GuzhenrenEventExtension.LOGGER.debug("Applied attack buff: {} -> {}", currentAttack, attackAttr.getBaseValue());
            }
        }

        // 3. 移动速度增益
        if (speedMult != 1.0) {
            AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                double currentSpeed = speedAttr.getBaseValue();
                speedAttr.setBaseValue(currentSpeed * speedMult);
                GuzhenrenEventExtension.LOGGER.debug("Applied speed buff: {} -> {}", currentSpeed, speedAttr.getBaseValue());
            }
        }
    }

    /**
     * 将玩家当前基础属性镜像到目标实体：生命上限、攻击力、防御（护甲/护甲韧性）、移动速度。
     */
    private void mirrorPlayerBaseAttributes(LivingEntity target, ServerPlayer player) {
        // 生命上限
        AttributeInstance pMaxHp = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance tMaxHp = target.getAttribute(Attributes.MAX_HEALTH);
        if (pMaxHp != null && tMaxHp != null) {
            tMaxHp.setBaseValue(pMaxHp.getBaseValue());
            target.setHealth(target.getMaxHealth());
        }
        // 攻击力
        AttributeInstance pAtk = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance tAtk = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (pAtk != null && tAtk != null) {
            tAtk.setBaseValue(pAtk.getBaseValue());
        }
        // 护甲与护甲韧性
        AttributeInstance pArmor = player.getAttribute(Attributes.ARMOR);
        AttributeInstance tArmor = target.getAttribute(Attributes.ARMOR);
        if (pArmor != null && tArmor != null) {
            tArmor.setBaseValue(pArmor.getBaseValue());
        }
        AttributeInstance pArmorTough = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        AttributeInstance tArmorTough = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (pArmorTough != null && tArmorTough != null) {
            tArmorTough.setBaseValue(pArmorTough.getBaseValue());
        }
        // 移动速度
        AttributeInstance pSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance tSpeed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (pSpeed != null && tSpeed != null) {
            tSpeed.setBaseValue(pSpeed.getBaseValue());
        }
    }

    /**
     * 基于玩家转数选择对应的标签 guzhenren:yewaigushiguchong{tier}，随机6个物品并装备到护甲与手部。
     */
    private void tryEquipGuWormsByZhuanshu(LivingEntity target, ServerPlayer player, ServerLevel level, RandomSource random) {
        int tier = 1;
        try {
            var handleOpt = GuzhenrenResourceBridge.open(player);
            if (handleOpt.isPresent()) {
                double zhuanshu = handleOpt.get().getZhuanshu().orElse(1.0);
                // 约束到 [1,5]
                tier = (int) Math.max(1, Math.min(5, Math.round(zhuanshu)));
            }
        } catch (Throwable t) {
            GuzhenrenEventExtension.LOGGER.warn("Failed to read player's zhuanshu, defaulting to tier 1", t);
            tier = 1;
        }

        ResourceLocation tagId = ResourceLocation.fromNamespaceAndPath("guzhenren", "yewaigushiguchong" + tier);
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);

        var regOpt = level.registryAccess().lookup(Registries.ITEM);
        if (regOpt.isEmpty()) {
            return;
        }
        var registry = regOpt.get();
        var holdersOpt = registry.get(tagKey);
        if (holdersOpt.isEmpty()) {
            return;
        }
        HolderSet<Item> holders = holdersOpt.get();
        if (holders.size() <= 0) {
            return;
        }

        // 收集候选列表
        java.util.ArrayList<Item> pool = new java.util.ArrayList<>();
        for (Holder<Item> h : holders) {
            try {
                pool.add(h.value());
            } catch (Throwable ignored) {
            }
        }
        if (pool.isEmpty()) {
            return;
        }

        // 随机选择6个（可重复，当候选不足时）
        ItemStack[] selections = new ItemStack[6];
        for (int i = 0; i < 6; i++) {
            Item item = pool.get(random.nextInt(pool.size()));
            selections[i] = new ItemStack(item);
        }

        // 装备顺序：头、胸、腿、脚、主手、副手
        tryEquip(target, EquipmentSlot.HEAD, selections[0]);
        tryEquip(target, EquipmentSlot.CHEST, selections[1]);
        tryEquip(target, EquipmentSlot.LEGS, selections[2]);
        tryEquip(target, EquipmentSlot.FEET, selections[3]);
        tryEquip(target, EquipmentSlot.MAINHAND, selections[4]);
        tryEquip(target, EquipmentSlot.OFFHAND, selections[5]);
    }

    private void tryEquip(LivingEntity target, EquipmentSlot slot, ItemStack stack) {
        try {
            if (!stack.isEmpty()) {
                target.setItemSlot(slot, stack);
            }
        } catch (Throwable ignored) {
            // 某些实体可能不支持所有槽位，静默跳过
        }
    }
}
