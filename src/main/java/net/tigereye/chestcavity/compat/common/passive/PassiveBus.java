package net.tigereye.chestcavity.compat.common.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenCompatBootstrap;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.registration.ActivationHookRegistry;

public final class PassiveBus {
  private static final Map<String, List<Supplier<PassiveHook>>> PENDING_FACTORIES = new HashMap<>();
  private static final Map<String, List<PassiveHook>> ENABLED_PASSIVES = new HashMap<>();

  /**
   * Registers a factory for a passive ability.
   *
   * @param familyOrItemId The family or item ID to associate this passive with.
   * @param factory A supplier that creates a new instance of the passive hook.
   */
  public static void register(String familyOrItemId, Supplier<PassiveHook> factory) {
    PENDING_FACTORIES.computeIfAbsent(familyOrItemId, k -> new ArrayList<>()).add(factory);
  }

  /**
   * Initializes the passive bus, subscribing to the necessary events and enabling the passives
   * that belong to the enabled families.
   */
  public static void init() {
    for (String familyId : ActivationHookRegistry.getEnabledFamilies()) {
      if (PENDING_FACTORIES.containsKey(familyId)) {
        List<PassiveHook> passives = new ArrayList<>();
        for (Supplier<PassiveHook> factory : PENDING_FACTORIES.get(familyId)) {
          passives.add(factory.get());
        }
        ENABLED_PASSIVES.put(familyId, passives);
      }
    }

    NeoForge.EVENT_BUS.addListener(PassiveBus::onEntityTick);
    NeoForge.EVENT_BUS.addListener(PassiveBus::onLivingHurt);
    NeoForge.EVENT_BUS.addListener(PassiveBus::onLivingAttack);
  }

  private static void onEntityTick(EntityTickEvent.Post event) {
    if (event.getEntity() instanceof LivingEntity) {
      LivingEntity entity = (LivingEntity) event.getEntity();
      if (entity.level().isClientSide) {
        return;
      }
      ChestCavityEntity.of(entity)
          .map(ChestCavityEntity::getChestCavityInstance)
          .ifPresent(
              cc -> {
                for (List<PassiveHook> family : ENABLED_PASSIVES.values()) {
                  for (PassiveHook passive : family) {
                    passive.onTick(entity, cc, entity.level().getGameTime());
                  }
                }
              });
    }
  }

  private static void onLivingHurt(LivingIncomingDamageEvent event) {
    LivingEntity entity = event.getEntity();
    if (entity.level().isClientSide) {
      return;
    }
    ChestCavityEntity.of(entity)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(
            cc -> {
              for (List<PassiveHook> family : ENABLED_PASSIVES.values()) {
                for (PassiveHook passive : family) {
                  passive.onHurt(
                      entity,
                      event.getSource(),
                      event.getAmount(),
                      cc,
                      entity.level().getGameTime());
                }
              }
            });
  }

  private static void onLivingAttack(LivingIncomingDamageEvent event) {
    if (event.getSource().getEntity() instanceof LivingEntity) {
      LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
      if (attacker.level().isClientSide) {
        return;
      }
      LivingEntity target = event.getEntity();
      ChestCavityEntity.of(attacker)
          .map(ChestCavityEntity::getChestCavityInstance)
          .ifPresent(
              cc -> {
                for (List<PassiveHook> family : ENABLED_PASSIVES.values()) {
                  for (PassiveHook passive : family) {
                    passive.onHitMelee(attacker, target, cc, attacker.level().getGameTime());
                  }
                }
              });
    }
  }
}
