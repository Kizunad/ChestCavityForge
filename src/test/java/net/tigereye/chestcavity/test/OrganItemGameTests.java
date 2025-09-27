package net.tigereye.chestcavity.test;

import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.items.VenomGland;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.registration.CCStatusEffects;
import net.tigereye.chestcavity.util.CommonOrganUtil;

@GameTestHolder(ChestCavity.MODID)
public class OrganItemGameTests {

    @GameTest(templateNamespace = ChestCavity.MODID, template = "empty")
    public void venomGlandAppliesDefaultPoison(GameTestHelper helper) {
        Level level = helper.getLevel();
        Zombie attacker = Objects.requireNonNull(EntityType.ZOMBIE.create(level));
        Pig target = Objects.requireNonNull(EntityType.PIG.create(level));

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        double spawnX = origin.getX() + 0.5D;
        double spawnY = origin.getY() + 1D;
        double spawnZ = origin.getZ() + 0.5D;
        attacker.moveTo(spawnX, spawnY, spawnZ, attacker.getYRot(), attacker.getXRot());
        target.moveTo(spawnX + 0.5D, spawnY, spawnZ, target.getYRot(), target.getXRot());
        level.addFreshEntity(attacker);
        level.addFreshEntity(target);

        VenomGland gland = (VenomGland) CCItems.VENOM_GLAND.get();
        ItemStack organ = new ItemStack(gland);
        DamageSource source = level.damageSources().mobAttack(attacker);
        float baseDamage = 4.0F;
        float returnedDamage = gland.onHit(source, attacker, target, null, organ, baseDamage);

        helper.assertTrue(returnedDamage == baseDamage, "Venom gland should preserve incoming damage value");
        helper.assertTrue(target.hasEffect(MobEffects.POISON), "Target should receive default poison effect");
        helper.assertTrue(attacker.hasEffect(CCStatusEffects.VENOM_COOLDOWN), "Attacker should receive venom cooldown");
        helper.succeed();
    }

    @GameTest(templateNamespace = ChestCavity.MODID, template = "empty")
    public void venomGlandAppliesCustomPotionEffects(GameTestHelper helper) {
        Level level = helper.getLevel();
        Zombie attacker = Objects.requireNonNull(EntityType.ZOMBIE.create(level));
        Pig target = Objects.requireNonNull(EntityType.PIG.create(level));

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        double spawnX = origin.getX() + 0.5D;
        double spawnY = origin.getY() + 1D;
        double spawnZ = origin.getZ() + 0.5D;
        attacker.moveTo(spawnX, spawnY, spawnZ, attacker.getYRot(), attacker.getXRot());
        target.moveTo(spawnX + 0.5D, spawnY, spawnZ, target.getYRot(), target.getXRot());
        level.addFreshEntity(attacker);
        level.addFreshEntity(target);

        VenomGland gland = (VenomGland) CCItems.VENOM_GLAND.get();
        ItemStack organ = new ItemStack(gland);
        CommonOrganUtil.setStatusEffects(organ, List.of(new MobEffectInstance(MobEffects.WITHER, 100, 1)));

        DamageSource source = level.damageSources().mobAttack(attacker);
        gland.onHit(source, attacker, target, null, organ, 6.0F);

        helper.assertTrue(target.hasEffect(MobEffects.WITHER), "Custom potion effect should be applied to the target");
        helper.assertTrue(!target.hasEffect(MobEffects.POISON), "Default poison should not be applied alongside custom effects");
        helper.assertTrue(attacker.hasEffect(CCStatusEffects.VENOM_COOLDOWN), "Custom payload should still trigger cooldown");
        helper.succeed();
    }

    @GameTest(templateNamespace = ChestCavity.MODID, template = "empty")
    public void venomGlandHonorsExistingCooldown(GameTestHelper helper) {
        Level level = helper.getLevel();
        Zombie attacker = Objects.requireNonNull(EntityType.ZOMBIE.create(level));
        Pig target = Objects.requireNonNull(EntityType.PIG.create(level));

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        double spawnX = origin.getX() + 0.5D;
        double spawnY = origin.getY() + 1D;
        double spawnZ = origin.getZ() + 0.5D;
        attacker.moveTo(spawnX, spawnY, spawnZ, attacker.getYRot(), attacker.getXRot());
        target.moveTo(spawnX + 0.5D, spawnY, spawnZ, target.getYRot(), target.getXRot());
        level.addFreshEntity(attacker);
        level.addFreshEntity(target);

        int expectedDuration = ChestCavity.config.VENOM_COOLDOWN - 1;
        attacker.addEffect(new MobEffectInstance(CCStatusEffects.VENOM_COOLDOWN, expectedDuration, 0));

        VenomGland gland = (VenomGland) CCItems.VENOM_GLAND.get();
        ItemStack organ = new ItemStack(gland);
        DamageSource source = level.damageSources().mobAttack(attacker);
        gland.onHit(source, attacker, target, null, organ, 5.0F);

        helper.assertTrue(!target.hasEffect(MobEffects.POISON), "Cooldown should block venom application");
        helper.assertTrue(attacker.hasEffect(CCStatusEffects.VENOM_COOLDOWN), "Attacker should keep the pre-existing cooldown");
        helper.assertTrue(attacker.getEffect(CCStatusEffects.VENOM_COOLDOWN).getDuration() == expectedDuration,
                "Cooldown duration should remain unchanged when attack is blocked");
        helper.succeed();
    }
}
