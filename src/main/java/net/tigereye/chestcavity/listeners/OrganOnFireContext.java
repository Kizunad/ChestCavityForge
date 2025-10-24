package net.tigereye.chestcavity.listeners;

import net.minecraft.world.item.ItemStack;

public class OrganOnFireContext {
  public final ItemStack organ;
  public final OrganOnFireListener listener;

  public OrganOnFireContext(ItemStack organ, OrganOnFireListener listener) {
    this.organ = organ;
    this.listener = listener;
  }
}
