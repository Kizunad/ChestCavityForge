package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yin_yang;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YinYangZhuanShenGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

public class YinYangZhuanShenGuCalculator {

    public static void applyModeAttributes(ServerPlayer player, YinYangDualityAttachment attachment) {
        Mode mode = attachment.currentMode();
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            AttributeOps.removeById(maxHealth, YinYangZhuanShenGuTuning.MAX_HEALTH_YANG_MODIFIER);
            AttributeOps.removeById(maxHealth, YinYangZhuanShenGuTuning.MAX_HEALTH_YIN_MODIFIER);
            double amount = mode == Mode.YANG ? 1.0D : -0.5D;
            ResourceLocation id =
                mode == Mode.YANG ? YinYangZhuanShenGuTuning.MAX_HEALTH_YANG_MODIFIER : YinYangZhuanShenGuTuning.MAX_HEALTH_YIN_MODIFIER;
            AttributeModifier modifier =
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            AttributeOps.replaceTransient(maxHealth, id, modifier);
        }

        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            AttributeOps.removeById(attack, YinYangZhuanShenGuTuning.ATTACK_YANG_MODIFIER);
            AttributeOps.removeById(attack, YinYangZhuanShenGuTuning.ATTACK_YIN_MODIFIER);
            double amount = mode == Mode.YANG ? -0.25D : 1.0D;
            ResourceLocation id = mode == Mode.YANG ? YinYangZhuanShenGuTuning.ATTACK_YANG_MODIFIER : YinYangZhuanShenGuTuning.ATTACK_YIN_MODIFIER;
            AttributeModifier modifier =
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            AttributeOps.replaceTransient(attack, id, modifier);
        }

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            AttributeOps.removeById(armor, YinYangZhuanShenGuTuning.ARMOR_YANG_MODIFIER);
            AttributeOps.removeById(armor, YinYangZhuanShenGuTuning.ARMOR_YIN_MODIFIER);
            double amount = mode == Mode.YANG ? 1.0D : -0.5D;
            ResourceLocation id = mode == Mode.YANG ? YinYangZhuanShenGuTuning.ARMOR_YANG_MODIFIER : YinYangZhuanShenGuTuning.ARMOR_YIN_MODIFIER;
            AttributeModifier modifier =
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            AttributeOps.replaceTransient(armor, id, modifier);
        }

        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            AttributeOps.removeById(movement, YinYangZhuanShenGuTuning.MOVE_YANG_MODIFIER);
            AttributeOps.removeById(movement, YinYangZhuanShenGuTuning.MOVE_YIN_MODIFIER);
            double amount = mode == Mode.YANG ? -0.25D : 0.333333D;
            ResourceLocation id = mode == Mode.YANG ? YinYangZhuanShenGuTuning.MOVE_YANG_MODIFIER : YinYangZhuanShenGuTuning.MOVE_YIN_MODIFIER;
            AttributeModifier modifier =
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            AttributeOps.replaceTransient(movement, id, modifier);
        }

        AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            AttributeOps.removeById(knockback, YinYangZhuanShenGuTuning.KNOCKBACK_YANG_MODIFIER);
            if (mode == Mode.YANG) {
                AttributeModifier modifier =
                    new AttributeModifier(
                        YinYangZhuanShenGuTuning.KNOCKBACK_YANG_MODIFIER,
                        0.5D,
                        AttributeModifier.Operation.ADD_VALUE);
                AttributeOps.replaceTransient(knockback, YinYangZhuanShenGuTuning.KNOCKBACK_YANG_MODIFIER, modifier);
            }
        }
        attachment.pool(mode).setAttackSnapshot(player.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    public static void clampHealth(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        double max = attr.getValue();
        if (player.getHealth() > max) {
            player.setHealth((float) max);
        }
    }

    public static void sendFailure(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    public static void sendAction(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    public static void playBodySwitchFx(ServerPlayer player, Mode newMode) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = player.position();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2;
            double offsetX = Math.cos(angle) * 0.6;
            double offsetZ = Math.sin(angle) * 0.6;
            level.sendParticles(
                ParticleTypes.ENCHANT,
                x + offsetX,
                y + 1.0,
                z + offsetZ,
                1,
                0.0,
                0.1,
                0.0,
                0.02);
        }

        if (newMode == Mode.YIN) {
            for (int i = 0; i < 30; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = Math.random() * 1.6;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                level.sendParticles(ParticleTypes.SOUL, x + offsetX, y + 0.1, z + offsetZ, 1, 0.0, 0.1, 0.0, 0.01);
            }
            for (int i = 0; i < 24; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = Math.random() * 1.6;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                level.sendParticles(ParticleTypes.PORTAL, x + offsetX, y + 0.1, z + offsetZ, 1, 0.0, 0.1, 0.0, 0.02);
            }
        } else {
            for (int i = 0; i < 12; i++) {
                double angle = (i / 12.0) * Math.PI * 2;
                double offsetX = Math.cos(angle) * 1.2;
                double offsetZ = Math.sin(angle) * 1.2;
                level.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    x + offsetX,
                    y + 0.5,
                    z + offsetZ,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0);
            }
            for (int i = 0; i < 20; i++) {
                double offsetX = (Math.random() - 0.5) * 1.5;
                double offsetY = Math.random() * 1.0;
                double offsetZ = (Math.random() - 0.5) * 1.5;
                level.sendParticles(ParticleTypes.CLOUD, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.05, 0.0, 0.01);
            }
        }

        for (int i = 0; i < 12; i++) {
            double offsetX = (Math.random() - 0.5) * 0.4;
            double offsetZ = (Math.random() - 0.5) * 0.4;
            level.sendParticles(ParticleTypes.END_ROD, x + offsetX, y + 0.5, z + offsetZ, 1, 0.0, 0.15, 0.0, 0.05);
        }

        level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.0f);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    public static void playPassiveYangFx(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = player.position();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        level.sendParticles(ParticleTypes.HEART, x, y + 1.2, z, 1, 0.2, 0.1, 0.2, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, x, y + 1.0, z, 1, 0.2, 0.1, 0.2, 0.01);
    }

    public static void playPassiveYinFx(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = player.position();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        level.sendParticles(ParticleTypes.SOUL, x, y + 1.2, z, 1, 0.2, 0.1, 0.2, 0.01);
        level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.0, z, 1, 0.2, 0.1, 0.2, 0.01);
    }

    /**
     * 运行阴/阳模式下的被动资源与状态结算。
     */
    public static void runPassives(
        ServerPlayer player, YinYangDualityAttachment attachment, ResourceHandle handle, long now) {
        Mode mode = attachment.currentMode();
        FoodData foodData = player.getFoodData();
        if (mode == Mode.YANG) {
            handle.adjustJingli(YinYangZhuanShenGuTuning.YANG_JINGLI_PER_TICK, true);
            player.heal(YinYangZhuanShenGuTuning.YANG_HEAL_PER_TICK);
            foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - YinYangZhuanShenGuTuning.YANG_HUNGER_COST));
            handle.adjustHunpo(YinYangZhuanShenGuTuning.YANG_HUNPO_DELTA, true);
            handle.adjustNiantou(YinYangZhuanShenGuTuning.YANG_NIANTOU_DELTA, true);
            if (now % YinYangZhuanShenGuTuning.PASSIVE_FX_INTERVAL_TICKS == 0) {
                playPassiveYangFx(player);
            }
        } else {
            handle.adjustHunpo(YinYangZhuanShenGuTuning.YIN_HUNPO_PER_TICK, true);
            handle.adjustNiantou(YinYangZhuanShenGuTuning.YIN_NIANTOU_PER_TICK, true);
            handle.adjustZhenyuan(YinYangZhuanShenGuTuning.YIN_ZHENYUAN_PER_TICK, true);
            handle.adjustJingli(YinYangZhuanShenGuTuning.YIN_JINGLI_DELTA, true);
            foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - YinYangZhuanShenGuTuning.YIN_HUNGER_COST));
            if (now % YinYangZhuanShenGuTuning.PASSIVE_FX_INTERVAL_TICKS == 0) {
                playPassiveYinFx(player);
            }
        }
    }
}
