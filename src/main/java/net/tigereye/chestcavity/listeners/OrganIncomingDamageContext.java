package net.tigereye.chestcavity.listeners;

import net.minecraft.world.item.ItemStack;

public class OrganIncomingDamageContext {
  public final ItemStack organ;
  public final OrganIncomingDamageListener listener;

  public OrganIncomingDamageContext(ItemStack organ, OrganIncomingDamageListener listener) {
    this.organ = organ;
    this.listener = listener;
  }
}
