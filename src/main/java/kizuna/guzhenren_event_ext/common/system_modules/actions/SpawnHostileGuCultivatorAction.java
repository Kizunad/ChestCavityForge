package kizuna.guzhenren_event_ext.common.system_modules.actions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.util.EntitySpawnUtil;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import kizuna.guzhenren_event_ext.common.util.GuCultivatorPersistentUtil;

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
 *   "entity_tag": "blood_moon_boss",      // 可选：实体标签，用于击杀追踪和奖励系统
 *   "preferred_dao_tags": ["jiandao", "xue_dao"], // 可选：优先道类标签数组（装备蛊虫时优先选择这些道类，不足时从主池补全）
 *   "custom_daohen": [                    // 可选：自定义道痕值数组
 *     {
 *       "daohen": "daohen_jiandao",       // 道痕字段名（如 daohen_jiandao = 剑道道痕）
 *       "value": 100.0,                   // 道痕值
 *       "override": true                  // 是否覆盖（true=覆盖，false=叠加），默认true
 *     },
 *     {
 *       "daohen": "daohen_xuedao",
 *       "value": 50.0,
 *       "override": false                 // 叠加模式：在现有值基础上增加
 *     }
 *   ]
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
        String customName = definition.has("custom_name") ? definition.get("custom_name").getAsString() : null;
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

            // 3.1 调用实体自身的 finalizeSpawn，以触发模组原生的初始化流程（含变量/道具/AI挂钩）。
            try {
                if (entity instanceof Mob mob) {
                    var diff = level.getCurrentDifficultyAt(mob.blockPosition());
                    mob.finalizeSpawn(level, diff, net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
                }
            } catch (Throwable t) {
                GuzhenrenEventExtension.LOGGER.warn("调用实体 finalizeSpawn 失败，将继续后续镜像与装备：{}", t.getMessage());
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

            // 5. （可选）提升到同等境界（仅资源顶满） + 装备蛊虫
            if (entity instanceof LivingEntity livingEntity) {
                if (promoteToSameRealm) {
                    // 顶满资源，避免因资源不足导致 NPC 无法施放蛊虫
                    topOffGuResources(livingEntity);
                    // 依据玩家转数，从对应Tag中随机6个蛊虫并装备到实体 ItemHandler 槽
                    tryEquipGuWormsByZhuanshu(livingEntity, serverPlayer, level, random);
                } else {
                    // 顶满资源，避免低资源导致无法施放
                    topOffGuResources(livingEntity);
                    // 装备蛊虫
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

    // 旧的属性倍增逻辑已移除（KISS）

    // 旧的基础属性镜像已移除（KISS）

    /**
     * 镜像玩家的蛊真人核心战斗资源
     * <p>
     * 使用 GuzhenrenResourceBridge 通过反射访问玩家和实体的变量数据，
     * 然后将玩家的核心属性（转数、阶段、真元、魂魄、精力、道痕等）复制到实体。
     */
    // 旧的资源镜像已移除（KISS）

    /**
     * 将 NPC 的核心战斗资源（真元/精力/魂魄）顶至各自上限，保证技能可用。
     */
    private void topOffGuResources(LivingEntity target) {
        try {
            var targetHandleOpt = GuzhenrenResourceBridge.open(target);
            if (targetHandleOpt.isEmpty()) return;
            var h = targetHandleOpt.get();
            try {
                h.getMaxZhenyuan().ifPresent(max -> h.setZhenyuan(max));
            } catch (Throwable ignored) {}
            try {
                h.getMaxJingli().ifPresent(max -> h.setJingli(max));
            } catch (Throwable ignored) {}
            try {
                h.read("zuida_hunpo").ifPresent(max -> h.writeDouble("hunpo", max));
            } catch (Throwable ignored) {}
            // 念头、稳定度等按需补充
        } catch (Throwable t) {
            GuzhenrenEventExtension.LOGGER.debug("顶满 NPC 资源失败：{}", t.getMessage());
        }
    }

    /**
     * 镜像所有道痕值
     * <p>
     * 遍历所有已知的道痕字段，将玩家的道痕值复制到目标实体。
     * 使用 GuzhenrenResourceBridge 的通用 read/writeDouble 方法。
     */
    // 旧的道痕镜像已移除（KISS）

    /**
     * 应用自定义道痕配置
     * <p>
     * JSON格式示例：
     * <pre>
     * [
     *   {"daohen": "daohen_jiandao", "value": 100.0, "override": true},
     *   {"daohen": "daohen_xuedao", "value": 50.0, "override": false}
     * ]
     * </pre>
     * <p>
     * override = true: 直接覆盖道痕值 <br>
     * override = false (或缺省): 叠加到现有道痕值
     *
     * @param entity            目标实体
     * @param customDaohenArray 自定义道痕配置数组
     */
    // 旧的自定义道痕应用已移除（KISS）

    /**
     * 基于玩家转数选择对应的标签 guzhenren:yewaigushiguchong{tier}，随机6个物品并装备到护甲与手部。
     * 
     * @param preferredDaoTags 可选的优先道类标签列表，优先从这些道类中选择蛊虫，不足时从主池补全
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

        java.util.ArrayList<Item> pool = new java.util.ArrayList<>();
        try {
            var builtTag = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOrCreateTag(ItemTags.create(tagId));
            builtTag.iterator().forEachRemaining(h -> { try { pool.add(h.value()); } catch (Throwable ignored) {} });
        } catch (Throwable ignored) {}
        if (pool.isEmpty()) return;

        ItemStack[] selections = new ItemStack[6];
        for (int i = 0; i < 6; i++) {
            Item item = pool.get(random.nextInt(pool.size()));
            selections[i] = new ItemStack(item);
        }
        applyGuStacksToEntityHandler(target, selections);
        applyWildGuTierPersistentKeys(target, tier, null);
    }

    /**
     * 从单一池中随机选择6个蛊虫并装备
     */
    private void selectAndEquipFromPool(LivingEntity target, java.util.ArrayList<Item> pool, RandomSource random, int tier) {
        ItemStack[] selections = new ItemStack[6];
        for (int i = 0; i < 6; i++) {
            Item item = pool.get(random.nextInt(pool.size()));
            selections[i] = new ItemStack(item);
        }

        // 写入 NeoForge ItemHandler（实体专用 0..5 槽）
        applyGuStacksToEntityHandler(target, selections);
        // 写入必要持久化键（与野外蛊修保持一致，便于 AI/流程识别）
        applyWildGuTierPersistentKeys(target, tier, null);
    }

    /**
     * 优先从优先池选择，不足时从主池补全
     */
    private void selectAndEquipWithPreference(
        LivingEntity target,
        java.util.ArrayList<Item> preferredPool,
        java.util.ArrayList<Item> mainPool,
        RandomSource random,
        int tier
    ) {
        ItemStack[] selections = new ItemStack[6];
        int fromPreferred = 0;
        int fromMain = 0;

        for (int i = 0; i < 6; i++) {
            if (!preferredPool.isEmpty()) {
                // 优先从优先池选择
                Item item = preferredPool.get(random.nextInt(preferredPool.size()));
                selections[i] = new ItemStack(item);
                fromPreferred++;
            } else {
                // 优先池为空，从主池选择
                Item item = mainPool.get(random.nextInt(mainPool.size()));
                selections[i] = new ItemStack(item);
                fromMain++;
            }
        }

        GuzhenrenEventExtension.LOGGER.debug(
            "蛊虫装备统计：优先道类 {} 个，主池补全 {} 个",
            fromPreferred, fromMain
        );

        // 写入 NeoForge ItemHandler（实体专用 0..5 槽）
        applyGuStacksToEntityHandler(target, selections);
        // 写入必要持久化键（与野外蛊修保持一致，便于 AI/流程识别）
        applyWildGuTierPersistentKeys(target, tier, null);
    }

    /**
     * 将 6 个蛊虫堆叠写入实体的 ItemHandler 槽（0..5）。
     * 若实体未提供该能力，记录告警日志。
     */
    private void applyGuStacksToEntityHandler(LivingEntity target, ItemStack[] selections) {
        try {
            Object cap = target.getCapability(ItemHandler.ENTITY, null);
            if (cap instanceof IItemHandlerModifiable handler) {
                int slots = handler.getSlots();
                for (int i = 0; i < Math.min(6, slots); i++) {
                    ItemStack s = selections[i] == null ? ItemStack.EMPTY : selections[i];
                    if (s == null) s = ItemStack.EMPTY;
                    handler.setStackInSlot(i, s);
                }
                GuzhenrenEventExtension.LOGGER.debug("已写入实体 ItemHandler 槽位 0..{} 的蛊虫", Math.min(5, slots - 1));
                // 装备后预热冷却，令 AI 能在下个 tick 立即尝试使用
                primeGuUsageCooldowns(target);
                // 立即尝试一次触发（复制 GuShiGuChong1Procedure 的行为做一次预热）
                attemptImmediateGuUse(target, handler);
                return;
            }
            GuzhenrenEventExtension.LOGGER.warn("实体不提供 ItemHandler 能力，无法装备蛊虫（将无法触发 NPC 蛊虫逻辑）：{}", target.getName().getString());
        } catch (Throwable t) {
            GuzhenrenEventExtension.LOGGER.error("写入实体 ItemHandler 槽位失败", t);
        }
    }

    /**
     * 写入“野外蛊师转数/阶段/转数名”持久化键，便于 NPC 蛊修逻辑识别。
     * stageNullable: 1..4（初/中/高/巅）；null 则使用默认“中阶(2)”。
     */
    private void applyWildGuTierPersistentKeys(LivingEntity target, int tier, Integer stageNullable) {
        try {
            GuCultivatorPersistentUtil.setTierAndStage(target, tier, stageNullable);
        } catch (Throwable t) {
            GuzhenrenEventExtension.LOGGER.error("写入 NPC 蛊修持久化键失败", t);
        }
    }

    /**
     * 将与蛊虫施放相关的 CD 置零，帮助实体在下一 tick 就尝试施放。
     * 这些键在模组原生逻辑中缺省即为 0（不存在时 getDouble=0），此处显式置零以避免偶发残留。
     */
    private void primeGuUsageCooldowns(LivingEntity target) {
        try {
            var tag = target.getPersistentData();
            tag.putDouble("蛊虫CD", 0.0);
            tag.putDouble("蛊虫1CD", 0.0);
            tag.putDouble("蛊虫2CD", 0.0);
            tag.putDouble("蛊虫3CD", 0.0);
            tag.putDouble("蛊虫4CD", 0.0);
            tag.putDouble("蛊虫5CD", 0.0);
            // 杀招：至少 10s (200t) 后再允许首次释放，避免一生成就放大招
            double current = tag.getDouble("杀招CD");
            tag.putDouble("杀招CD", Math.max(current, 200.0));
        } catch (Throwable t) {
            GuzhenrenEventExtension.LOGGER.warn("初始化蛊虫/杀招冷却失败，使用缺省 0：{}", t.getMessage());
        }
    }

    /**
     * 直接把第一个非空槽(0..4)的蛊虫塞入主手并 swing 一次，然后清空，等价于 GuShiGuChong1Procedure 的最小复刻。
     * 用于生成当刻立即触发一次，帮助后续 tick 走上正轨。
     */
    private void attemptImmediateGuUse(LivingEntity target, IItemHandlerModifiable handler) {
        try {
            int pick = -1;
            int limit = Math.min(5, handler.getSlots() - 1);
            for (int i = 0; i <= limit; i++) {
                if (!handler.getStackInSlot(i).isEmpty()) { pick = i; break; }
            }
            if (pick < 0) return;

            ItemStack stack = handler.getStackInSlot(pick).copy();
            if (stack.isEmpty()) return;
            // 放主手并挥动
            target.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            target.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            // 清空主手，避免持久占用
            target.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } catch (Throwable t) {
            GuzhenrenEventExtension.LOGGER.debug("尝试立即触发蛊虫失败：{}", t.getMessage());
        }
    }
}
