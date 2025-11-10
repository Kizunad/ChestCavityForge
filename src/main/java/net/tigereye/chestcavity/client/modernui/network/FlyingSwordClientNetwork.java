package net.tigereye.chestcavity.client.modernui.network;

import net.minecraft.client.Minecraft;

public final class FlyingSwordClientNetwork {
  private FlyingSwordClientNetwork() {}

  public static void requestWithdraw(int index1) {
    var conn = Minecraft.getInstance().getConnection();
    if (conn != null) {
      conn.send(new FlyingSwordWithdrawPayload(index1));
    }
  }

  public static void requestDeposit(int index1) {
    var conn = Minecraft.getInstance().getConnection();
    if (conn != null) {
      conn.send(new FlyingSwordDepositPayload(index1));
    }
  }
}
