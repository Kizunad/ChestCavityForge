package net.tigereye.chestcavity.world.spawn;

import java.util.function.Consumer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;

/** 常用配置器工具集合，便于在 Builder 中直接添加。 */
public final class SpawnConfigurators {

  private SpawnConfigurators() {}

  @FunctionalInterface
  public interface MobConfigurator extends SpawnConfigurator {
    void accept(Mob mob);

    @Override
    default void configure(SpawnedMobContext context) {
      accept(context.mob());
    }
  }

  @FunctionalInterface
  public interface ChestCavityConfigurator extends SpawnConfigurator {
    void accept(ChestCavityInstance instance, SpawnedMobContext context);

    @Override
    default void configure(SpawnedMobContext context) {
      ChestCavityInstance instance = context.existingChestCavity().orElseGet(context::chestCavity);
      accept(instance, context);
    }
  }

  @FunctionalInterface
  public interface GuScriptConfigurator extends SpawnConfigurator {
    void accept(GuScriptAttachment attachment, SpawnedMobContext context);

    @Override
    default void configure(SpawnedMobContext context) {
      GuScriptAttachment attachment = context.existingGuScript().orElseGet(context::guScript);
      accept(attachment, context);
    }
  }

  public static SpawnConfigurator ofChestCavity(Consumer<ChestCavityInstance> consumer) {
    return (ChestCavityConfigurator) (instance, context) -> consumer.accept(instance);
  }

  public static SpawnConfigurator ofGuScript(Consumer<GuScriptAttachment> consumer) {
    return (GuScriptConfigurator) (attachment, context) -> consumer.accept(attachment);
  }

  public static SpawnConfigurator ofMob(Consumer<Mob> consumer) {
    return (MobConfigurator) consumer::accept;
  }

  public static BrainConfigurator wrapBrain(Consumer<Brain<?>> consumer) {
    return (context, brain) -> consumer.accept(brain);
  }
}
