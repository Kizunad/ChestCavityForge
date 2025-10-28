package net.tigereye.chestcavity.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.registration.CCStatusEffects;
import net.tigereye.chestcavity.registration.CCTags;
import net.tigereye.chestcavity.skill.ActivationBootstrap;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.CommonOrganUtil;

public class OrganActivationListeners {

  private static Map<ResourceLocation, List<BiConsumer<LivingEntity, ChestCavityInstance>>>
      abilityIDMap = new HashMap<>();

  public static void register() {
    register(CCOrganScores.CREEPY, OrganActivationListeners::ActivateCreepy);
    register(CCOrganScores.DRAGON_BREATH, OrganActivationListeners::ActivateDragonBreath);
    register(CCOrganScores.DRAGON_BOMBS, OrganActivationListeners::ActivateDragonBombs);
    register(CCOrganScores.FORCEFUL_SPIT, OrganActivationListeners::ActivateForcefulSpit);
    register(CCOrganScores.FURNACE_POWERED, OrganActivationListeners::ActivateFurnacePowered);
    register(CCOrganScores.IRON_REPAIR, OrganActivationListeners::ActivateIronRepair);
    register(CCOrganScores.PYROMANCY, OrganActivationListeners::ActivatePyromancy);
    register(CCOrganScores.GHASTLY, OrganActivationListeners::ActivateGhastly);
    register(CCOrganScores.GRAZING, OrganActivationListeners::ActivateGrazing);
    register(CCOrganScores.SHULKER_BULLETS, OrganActivationListeners::ActivateShulkerBullets);
    register(CCOrganScores.SILK, OrganActivationListeners::ActivateSilk);
  }

  public static void register(
      ResourceLocation id, BiConsumer<LivingEntity, ChestCavityInstance> ability) {
    abilityIDMap.computeIfAbsent(id, ignored -> new ArrayList<>()).add(ability);
  }

  public static boolean activate(ResourceLocation id, ChestCavityInstance cc) {
    if (abilityIDMap.containsKey(id)) {
      for (BiConsumer<LivingEntity, ChestCavityInstance> ability : abilityIDMap.get(id)) {
        ability.accept(cc.owner, cc);
      }
      return true;
    }
    // Fallback: try lazy-register known abilities by class name to avoid early classloading
    // silent fallback
    if (tryLazyRegister(id)) {
      for (BiConsumer<LivingEntity, ChestCavityInstance> ability :
          abilityIDMap.getOrDefault(id, List.of())) {
        ability.accept(cc.owner, cc);
      }
      return abilityIDMap.containsKey(id);
    } else {
      return false;
    }
  }

  // Best-effort lazy registration for optional compat abilities, to avoid <clinit> crashes at mod
  // init.
  private static boolean tryLazyRegister(ResourceLocation id) {
    if (id == null) return false;
    ActivationBootstrap.ensureLoaded(id);
    if (abilityIDMap.containsKey(id)) {
      return true;
    }
    String ns = id.getNamespace();
    String path = id.getPath();
    try {
      if ("guzhenren".equals(ns)) {
        Class<?> clazz = null;
        if ("huo_gu".equals(path)) {
          clazz =
              Class.forName(
                  "net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoYiGuOrganBehavior");
        } else if ("wuxing_gui_bian".equals(path) || "wuxing_gui_bian_config".equals(path)) {
          clazz =
              Class.forName(
                  "net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.gui_bian.WuxingGuiBianBehavior");
        } else if (path.startsWith("wuxing_hua_hen")) {
          clazz =
              Class.forName(
                  "net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.hua_hen.WuxingHuaHenBehavior");
        }

        if (clazz != null) {
          // Force initialise and touch static fields to ensure constructor ran
          try {
            java.lang.reflect.Field f = clazz.getDeclaredField("INSTANCE");
            f.setAccessible(true);
            Object inst = f.get(null);
            ChestCavity.LOGGER.info(
                "[compat][activate] loaded class {} instancePresent={} after forName",
                clazz.getName(),
                inst != null);
          } catch (Throwable ignored) {
          }
          boolean present = abilityIDMap.containsKey(id);
          ChestCavity.LOGGER.info(
              "[compat][activate] post-load ability present={} for id={}", present, id);
          return present;
        }
      }
    } catch (Throwable t) {
      // keep silent on fallback failures
    }
    return false;
  }

  public static void ActivateCreepy(LivingEntity entity, ChestCavityInstance cc) {
    if (cc.getOrganScore(CCOrganScores.CREEPY) < 1) {
      return;
    }
    if (entity.hasEffect(CCStatusEffects.EXPLOSION_COOLDOWN)) {
      return;
    }
    float explosion_yield = cc.getOrganScore(CCOrganScores.EXPLOSIVE);
    ChestCavityUtil.destroyOrgansWithKey(cc, CCOrganScores.EXPLOSIVE);
    CommonOrganUtil.explode(entity, explosion_yield);
    if (entity.isAlive()) {
      entity.addEffect(
          new MobEffectInstance(
              CCStatusEffects.EXPLOSION_COOLDOWN,
              ChestCavity.config.EXPLOSION_COOLDOWN,
              0,
              false,
              false,
              true));
    }
  }

  public static void ActivateDragonBreath(LivingEntity entity, ChestCavityInstance cc) {
    // if(entity.world.isClient){
    //    return; //this is spawning entities, this is no place for a client
    // }
    float breath = cc.getOrganScore(CCOrganScores.DRAGON_BREATH);
    if (entity instanceof Player) {
      ((Player) entity).causeFoodExhaustion(breath * .6f);
    }
    if (breath <= 0) {
      return;
    }

    if (!entity.hasEffect(CCStatusEffects.DRAGON_BREATH_COOLDOWN)) {
      entity.addEffect(
          new MobEffectInstance(
              CCStatusEffects.DRAGON_BREATH_COOLDOWN,
              ChestCavity.config.DRAGON_BREATH_COOLDOWN,
              0,
              false,
              false,
              true));
      cc.projectileQueue.add(CommonOrganUtil::spawnDragonBreath);
    }
  }

  public static void ActivateDragonBombs(LivingEntity entity, ChestCavityInstance cc) {
    // if(entity.world.isClient){
    //    return; //this is spawning entities, this is no place for a client
    // }
    float projectiles = cc.getOrganScore(CCOrganScores.DRAGON_BOMBS);
    if (projectiles < 1) {
      return;
    }
    if (!entity.hasEffect(CCStatusEffects.DRAGON_BOMB_COOLDOWN)) {
      CommonOrganUtil.queueDragonBombs(entity, cc, (int) projectiles);
    }
  }

  public static void ActivateForcefulSpit(LivingEntity entity, ChestCavityInstance cc) {
    // if(entity.world.isClient){
    //    return; //we are spawning entities, this is no place for a client
    // }
    float projectiles = cc.getOrganScore(CCOrganScores.FORCEFUL_SPIT);
    if (projectiles < 1) {
      return;
    }
    if (!entity.hasEffect(CCStatusEffects.FORCEFUL_SPIT_COOLDOWN)) {
      CommonOrganUtil.queueForcefulSpit(entity, cc, (int) projectiles);
    }
  }

  public static void ActivateFurnacePowered(LivingEntity entity, ChestCavityInstance cc) {
    int furnacePowered = Math.round(cc.getOrganScore(CCOrganScores.FURNACE_POWERED));
    if (furnacePowered < 1) {
      return;
    }

    ItemStack fuelStack = findFuelItem(cc.owner);
    if (fuelStack.isEmpty()) {
      return;
    }

    int fuelValue = getFuelValue(fuelStack);
    if (fuelValue <= 0) {
      return;
    }

    int amplifier = 0;
    int duration = fuelValue;
    MobEffectInstance existing = cc.owner.getEffect(CCStatusEffects.FURNACE_POWER);
    if (existing != null) {
      amplifier = Math.min(furnacePowered - 1, existing.getAmplifier() + 1);
      duration += existing.getDuration();
    }

    cc.owner.removeEffect(CCStatusEffects.FURNACE_POWER);
    cc.owner.addEffect(
        new MobEffectInstance(
            CCStatusEffects.FURNACE_POWER, duration, amplifier, false, false, true));
    fuelStack.shrink(1);
  }

  private static ItemStack findFuelItem(LivingEntity entity) {
    ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
    if (!mainHand.isEmpty() && getFuelValue(mainHand) > 0) {
      return mainHand;
    }
    ItemStack offHand = entity.getItemBySlot(EquipmentSlot.OFFHAND);
    if (!offHand.isEmpty() && getFuelValue(offHand) > 0) {
      return offHand;
    }
    return ItemStack.EMPTY;
  }

  private static int getFuelValue(ItemStack stack) {
    return stack.isEmpty() ? 0 : stack.getItem().getBurnTime(stack, RecipeType.SMELTING);
  }

  public static void ActivateIronRepair(LivingEntity entity, ChestCavityInstance cc) {
    float ironRepair =
        cc.getOrganScore(CCOrganScores.IRON_REPAIR)
            - cc.getChestCavityType().getDefaultOrganScore(CCOrganScores.IRON_REPAIR);
    // test for ability
    if (ironRepair <= 0) {
      return;
    }
    // test for cooldown
    if (cc.owner.hasEffect(CCStatusEffects.IRON_REPAIR_COOLDOWN)) {
      return;
    }
    // test for missing health
    if (cc.owner.getHealth() >= cc.owner.getMaxHealth()) {
      return;
    }
    // test for iron
    ItemStack itemStack = cc.owner.getItemBySlot(EquipmentSlot.MAINHAND);
    if (itemStack.isEmpty() || !itemStack.is(CCTags.IRON_REPAIR_MATERIAL)) {
      itemStack = cc.owner.getItemBySlot(EquipmentSlot.OFFHAND);
      if (itemStack.isEmpty() || !itemStack.is(CCTags.IRON_REPAIR_MATERIAL)) {
        return;
      }
    }

    // success! heal target
    cc.owner.heal(cc.owner.getMaxHealth() * ChestCavity.config.IRON_REPAIR_PERCENT);
    entity.playSound(SoundEvents.IRON_GOLEM_REPAIR, .75f, 1);
    cc.owner.addEffect(
        new MobEffectInstance(
            CCStatusEffects.IRON_REPAIR_COOLDOWN,
            (int) (ChestCavity.config.IRON_REPAIR_COOLDOWN / ironRepair),
            0,
            false,
            false,
            true));
    itemStack.shrink(1);
  }

  public static void ActivateGhastly(LivingEntity entity, ChestCavityInstance cc) {
    // if(entity.world.isClient){
    //    return; //this is spawning entities, this is no place for a client
    // }
    float ghastly = cc.getOrganScore(CCOrganScores.GHASTLY);
    if (ghastly < 1) {
      return;
    }
    if (!entity.hasEffect(CCStatusEffects.GHASTLY_COOLDOWN)) {
      CommonOrganUtil.queueGhastlyFireballs(entity, cc, (int) ghastly);
    }
  }

  private static void ActivateGrazing(LivingEntity entity, ChestCavityInstance cc) {
    float grazing = cc.getOrganScore(CCOrganScores.GRAZING);
    if (grazing <= 0) {
      return;
    }
    BlockPos blockPos = entity.getOnPos(); // .below();
    boolean ateGrass = false;
    if (entity.level().getBlockState(blockPos).is(Blocks.GRASS_BLOCK)
        || entity.level().getBlockState(blockPos).is(Blocks.MYCELIUM)) {
      entity.level().setBlock(blockPos, Blocks.DIRT.defaultBlockState(), 2);
      ateGrass = true;
    } else if (entity.level().getBlockState(blockPos).is(Blocks.CRIMSON_NYLIUM)
        || entity.level().getBlockState(blockPos).is(Blocks.WARPED_NYLIUM)) {
      entity.level().setBlock(blockPos, Blocks.NETHERRACK.defaultBlockState(), 2);
      ateGrass = true;
    }
    if (ateGrass) {
      int duration;
      if (entity.hasEffect(CCStatusEffects.RUMINATING)) {
        MobEffectInstance ruminating = entity.getEffect(CCStatusEffects.RUMINATING);
        duration =
            (int)
                Math.min(
                    ChestCavity.config.RUMINATION_TIME
                        * ChestCavity.config.RUMINATION_GRASS_PER_SQUARE
                        * ChestCavity.config.RUMINATION_SQUARES_PER_STOMACH
                        * grazing,
                    ruminating.getDuration()
                        + (ChestCavity.config.RUMINATION_TIME
                            * ChestCavity.config.RUMINATION_GRASS_PER_SQUARE));
      } else {
        duration =
            ChestCavity.config.RUMINATION_TIME * ChestCavity.config.RUMINATION_GRASS_PER_SQUARE;
      }
      entity.addEffect(
          new MobEffectInstance(CCStatusEffects.RUMINATING, duration, 0, false, false, true));
    }
  }

  public static void ActivatePyromancy(LivingEntity entity, ChestCavityInstance cc) {
    // if(entity.world.isClient){
    //    return; //we are spawning entities, this is no place for a client
    // }
    float pyromancy = cc.getOrganScore(CCOrganScores.PYROMANCY);
    if (pyromancy < 1) {
      return;
    }
    if (!entity.hasEffect(CCStatusEffects.PYROMANCY_COOLDOWN)) {
      CommonOrganUtil.queuePyromancyFireballs(entity, cc, (int) pyromancy);
    }
  }

  public static void ActivateShulkerBullets(LivingEntity entity, ChestCavityInstance cc) {
    // if(entity.world.isClient){
    //    return; //we are spawning entities, this is no place for a client
    // }
    float projectiles = cc.getOrganScore(CCOrganScores.SHULKER_BULLETS);
    if (projectiles < 1) {
      return;
    }
    if (!entity.hasEffect(CCStatusEffects.SHULKER_BULLET_COOLDOWN)) {
      CommonOrganUtil.queueShulkerBullets(entity, cc, (int) projectiles);
    }
  }

  public static void ActivateSilk(LivingEntity entity, ChestCavityInstance cc) {
    if (cc.getOrganScore(CCOrganScores.SILK) == 0) {
      return;
    }
    if (entity.hasEffect(CCStatusEffects.SILK_COOLDOWN)) {
      return;
    }
    if (CommonOrganUtil.spinWeb(entity, cc, cc.getOrganScore(CCOrganScores.SILK))) {
      entity.addEffect(
          new MobEffectInstance(
              CCStatusEffects.SILK_COOLDOWN,
              ChestCavity.config.SILK_COOLDOWN,
              0,
              false,
              false,
              true));
    }
  }
}
