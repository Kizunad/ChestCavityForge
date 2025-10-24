package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.registration.CCAttachments;

/** Temporarily adjusts a linkage channel, then reverts it after the given duration in ticks. */
public record AdjustLinkageChannelTemporaryAction(
    ResourceLocation channelId, double amount, int durationTicks) implements Action {

  public static final String ID = "linkage.adjust_temporary";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "通道临时调整 " + channelId + " (" + amount + ", " + durationTicks + "t)";
  }

  @Override
  public void execute(GuScriptContext context) {
    if (channelId == null || context == null) {
      return;
    }
    Player performer = context.performer();
    if (performer == null) {
      return;
    }
    ChestCavityInstance cc = CCAttachments.getChestCavity(performer);
    if (cc == null) {
      return;
    }
    ActiveLinkageContext linkage = LinkageManager.getContext(cc);
    linkage.getOrCreateChannel(channelId).adjust(amount);

    if (performer.level() instanceof ServerLevel server && durationTicks > 0) {
      net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps.schedule(
          server,
          () ->
              LinkageManager.getContext(CCAttachments.getChestCavity(performer))
                  .getOrCreateChannel(channelId)
                  .adjust(-amount),
          durationTicks);
    }
  }
}
