package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.registration.CCAttachments;

/** Adjusts a linkage channel by a signed amount. Positive increases, negative decreases. */
public record AdjustLinkageChannelAction(ResourceLocation channelId, double amount)
    implements Action {

  public static final String ID = "linkage.adjust";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "通道调整 " + channelId + " (" + amount + ")";
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
  }
}
