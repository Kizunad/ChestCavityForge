package net.tigereye.chestcavity.registration;

import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstanceFactory;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.WuxingHuaHenAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastState;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.soul.container.SoulContainer;

public final class CCAttachments {

  public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
      DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ChestCavity.MODID);

  public static final DeferredHolder<AttachmentType<?>, AttachmentType<ChestCavityInstance>>
      CHEST_CAVITY =
          ATTACHMENT_TYPES.register(
              "chest_cavity",
              () ->
                  AttachmentType.builder(CCAttachments::createInstance)
                      .serialize(new ChestCavitySerializer())
                      .build());

  public static final DeferredHolder<AttachmentType<?>, AttachmentType<GuScriptAttachment>>
      GUSCRIPT =
          ATTACHMENT_TYPES.register(
              "guscript",
              () ->
                  AttachmentType.builder(GuScriptAttachment::create)
                      .serialize(new GuScriptAttachment.Serializer())
                      .build());

  public static final DeferredHolder<AttachmentType<?>, AttachmentType<SoulBeastState>>
      SOUL_BEAST_STATE =
          ATTACHMENT_TYPES.register(
              "soul_beast_state",
              () ->
                  AttachmentType.builder(SoulBeastState::new)
                      .serialize(new SoulBeastStateSerializer())
                      .build());

  public static final DeferredHolder<AttachmentType<?>, AttachmentType<SoulContainer>>
      SOUL_CONTAINER =
          ATTACHMENT_TYPES.register(
              "soul_container",
              () ->
                  AttachmentType.builder(CCAttachments::createSoulContainer)
                      .serialize(new SoulContainerSerializer())
                      .build());

  public static final DeferredHolder<AttachmentType<?>, AttachmentType<YinYangDualityAttachment>>
      YIN_YANG_DUALITY =
          ATTACHMENT_TYPES.register(
              "yin_yang_duality",
              () ->
                  AttachmentType.builder(CCAttachments::createYinYangDuality)
                      .serialize(new YinYangDualityAttachment.Serializer())
                      .build());

  public static final DeferredHolder<AttachmentType<?>, AttachmentType<WuxingHuaHenAttachment>>
      WUXING_HUA_HEN =
          ATTACHMENT_TYPES.register(
              "wuxing_hua_hen",
              () ->
                  AttachmentType.builder(CCAttachments::createWuxingHuaHen)
                      .serialize(new WuxingHuaHenAttachment.Serializer())
                      .build());

  private CCAttachments() {}

  private static ChestCavityInstance createInstance(IAttachmentHolder holder) {
    if (!(holder instanceof LivingEntity living)) {
      throw new IllegalStateException(
          "Chest cavity attachment can only be applied to living entities");
    }
    ChestCavityInstance instance = ChestCavityInstanceFactory.newChestCavityInstance(living);
    instance.initializeRandomFillersOnSpawn();
    return instance;
  }

  public static ChestCavityInstance getChestCavity(LivingEntity entity) {
    return entity.getData(CHEST_CAVITY.get());
  }

  public static Optional<ChestCavityInstance> getExistingChestCavity(LivingEntity entity) {
    return entity.getExistingData(CHEST_CAVITY.get());
  }

  public static GuScriptAttachment getGuScript(LivingEntity entity) {
    return entity.getData(GUSCRIPT.get());
  }

  public static Optional<GuScriptAttachment> getExistingGuScript(LivingEntity entity) {
    return entity.getExistingData(GUSCRIPT.get());
  }

  public static SoulBeastState getSoulBeastState(LivingEntity entity) {
    return entity.getData(SOUL_BEAST_STATE.get());
  }

  public static Optional<SoulBeastState> getExistingSoulBeastState(LivingEntity entity) {
    return entity.getExistingData(SOUL_BEAST_STATE.get());
  }

  private static SoulContainer createSoulContainer(IAttachmentHolder holder) {
    if (!(holder instanceof Player player)) {
      throw new IllegalStateException("Soul container attachment can only be applied to players");
    }
    return new SoulContainer(player);
  }

  private static YinYangDualityAttachment createYinYangDuality(IAttachmentHolder holder) {
    if (!(holder instanceof Player)) {
      throw new IllegalStateException(
          "YinYangDuality attachment can only be applied to players");
    }
    return new YinYangDualityAttachment();
  }

  private static WuxingHuaHenAttachment createWuxingHuaHen(IAttachmentHolder holder) {
    if (!(holder instanceof Player)) {
      throw new IllegalStateException(
          "WuxingHuaHen attachment can only be applied to players");
    }
    return new WuxingHuaHenAttachment();
  }

  public static SoulContainer getSoulContainer(Player player) {
    return player.getData(SOUL_CONTAINER.get());
  }

  public static Optional<SoulContainer> getExistingSoulContainer(Player player) {
    return player.getExistingData(SOUL_CONTAINER.get());
  }

  public static YinYangDualityAttachment getYinYangDuality(Player player) {
    return player.getData(YIN_YANG_DUALITY.get());
  }

  public static Optional<YinYangDualityAttachment> getExistingYinYangDuality(Player player) {
    return player.getExistingData(YIN_YANG_DUALITY.get());
  }

  public static WuxingHuaHenAttachment getWuxingHuaHen(Player player) {
    return player.getData(WUXING_HUA_HEN.get());
  }

  public static Optional<WuxingHuaHenAttachment> getExistingWuxingHuaHen(Player player) {
    return player.getExistingData(WUXING_HUA_HEN.get());
  }

  private static class ChestCavitySerializer
      implements IAttachmentSerializer<CompoundTag, ChestCavityInstance> {
    @Override
    public ChestCavityInstance read(
        IAttachmentHolder holder,
        CompoundTag tag,
        net.minecraft.core.HolderLookup.Provider provider) {
      if (!(holder instanceof LivingEntity living)) {
        throw new IllegalStateException(
            "Chest cavity attachment can only be read for living entities");
      }
      ChestCavityInstance instance = ChestCavityInstanceFactory.newChestCavityInstance(living);
      if (!tag.isEmpty()) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("ChestCavity", tag.copy());
        instance.fromTag(wrapper, living, provider);
      }
      return instance;
    }

    @Override
    public CompoundTag write(
        ChestCavityInstance attachment, net.minecraft.core.HolderLookup.Provider provider) {
      CompoundTag wrapper = new CompoundTag();
      attachment.toTag(wrapper, provider);
      return wrapper.getCompound("ChestCavity");
    }
  }

  private static class SoulContainerSerializer
      implements IAttachmentSerializer<CompoundTag, SoulContainer> {
    @Override
    public SoulContainer read(
        IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
      if (!(holder instanceof Player player)) {
        throw new IllegalStateException("Soul container attachment can only be read for players");
      }
      SoulContainer container = new SoulContainer(player);
      if (!tag.isEmpty()) {
        try {
          container.loadNBT(tag, provider);
        } catch (Throwable t) {
          // 容错：任何读取异常都不应导致玩家数据整体损坏，回退为空容器并记录错误日志。
          net.tigereye.chestcavity.ChestCavity.LOGGER.error(
              "[soul] Failed to read SoulContainer for player {} — resetting to empty container",
              player.getGameProfile().getName(),
              t);
        }
      }
      return container;
    }

    @Override
    public CompoundTag write(SoulContainer attachment, HolderLookup.Provider provider) {
      return attachment.saveNBT(provider);
    }
  }

  private static class SoulBeastStateSerializer
      implements IAttachmentSerializer<CompoundTag, SoulBeastState> {
    @Override
    public SoulBeastState read(
        IAttachmentHolder holder,
        CompoundTag tag,
        net.minecraft.core.HolderLookup.Provider provider) {
      SoulBeastState state = new SoulBeastState();
      state.load(tag);
      return state;
    }

    @Override
    public CompoundTag write(
        SoulBeastState attachment, net.minecraft.core.HolderLookup.Provider provider) {
      return attachment.save();
    }
  }
}
