package net.tigereye.chestcavity.guscript.data;

import net.minecraft.world.item.ItemStack;

/** Callback used to iterate over every slot inside a {@link GuScriptAttachment}. */
@FunctionalInterface
public interface GuScriptSlotVisitor {

  /**
   * Invoked for each slot in the attachment.
   *
   * @param pageIndex zero-based page index currently being visited
   * @param slotIndex zero-based slot index within the page (including the binding slot)
   * @param stack mutable {@link ItemStack} reference stored in the slot
   */
  void visit(int pageIndex, int slotIndex, ItemStack stack);
}
