package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** 客户端注册鱼鳞蛊相关主动技能的热键提示（仅保留鱼跃破浪）。 */
public final class BianHuaDaoClientAbilities {

  private BianHuaDaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    // 按规范：仅以字面 ResourceLocation 加入，避免类常量触发早期类加载
    register(ResourceLocation.parse("guzhenren:yu_yue"));
    register(ResourceLocation.parse("guzhenren:yu_qun"));
    register(ResourceLocation.parse("guzhenren:yu_shi_summon"));
    register(ResourceLocation.parse("guzhenren:yin_yang_zhuan_shen_gu/body"));
    // 兽皮：仅展示 combo 版（roll_evasion/qian_jia_crash），item 旧 ID 已移除
    // 同步展示 combo 技能（与 item 桥接一致）
    register(ResourceLocation.parse("guzhenren:shou_pi_roll_evasion"));
    register(ResourceLocation.parse("guzhenren:shou_pi_qian_jia_crash"));
  }

  private static void register(ResourceLocation id) {
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(id)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(id);
    }
  }
}
