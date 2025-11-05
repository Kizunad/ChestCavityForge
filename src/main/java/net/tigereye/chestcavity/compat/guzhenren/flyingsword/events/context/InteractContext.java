package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 飞剑交互事件上下文
 */
public class InteractContext {
  public final FlyingSwordEntity sword;
  public final Player interactor;
  public final InteractionHand hand;
  public final boolean isOwner;

  /** 是否取消默认行为（召回） */
  public boolean cancelDefault = false;

  /** 自定义返回结果（如果cancelDefault=true） */
  public InteractionResult customResult = InteractionResult.PASS;

  public InteractContext(
      FlyingSwordEntity sword, Player interactor, InteractionHand hand, boolean isOwner) {
    this.sword = sword;
    this.interactor = interactor;
    this.hand = hand;
    this.isOwner = isOwner;
  }
}
