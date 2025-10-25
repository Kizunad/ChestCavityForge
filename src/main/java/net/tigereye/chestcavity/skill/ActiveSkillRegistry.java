package net.tigereye.chestcavity.skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.YuLinGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.BingJiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.ShuangXiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.behavior.QingFengLunOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LeGuDunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LuoXuanGuQiangguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.behavior.ShanGuangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.behavior.XiaoGuangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.GuiQiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.JianYingGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.behavior.LeiDunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HuaShiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HuangLuoTianNiuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.LongWanQuQuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ManLiTianNiuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.XiongHaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ZhiZhuangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ZiLiGengShengGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jin_dao.behavior.TiePiGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.CaoQunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.JiuYeShengJiCaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.LiandaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.ShengJiYeOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.BaiYinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.ChiTieSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.HuangJinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.QingTongSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.ZaijinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.behavior.JiuChongOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.synergy.SheShengQuYiSynergyBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.synergy.hun_dao.HunShouHuaSynergyBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.tian_dao.behavior.ShouGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior.TuQiangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior.YinShiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieFeiguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieWangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XiediguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XueYiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XueZhanGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoLongGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoYiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoYouGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior.YuanLaoGuFifthTierBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.behavior.BaiYunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.behavior.YinYunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.CountdownOps;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

/** Central registry for Guzhenren active skills. Provides metadata and ability dispatch. */
public final class ActiveSkillRegistry {

  public enum TriggerResult {
    SUCCESS,
    NOT_REGISTERED,
    NO_CHEST_CAVITY,
    MISSING_ORGAN,
    ABILITY_NOT_REGISTERED
  }

  public record ActiveSkillEntry(
      ResourceLocation skillId,
      ResourceLocation abilityId,
      ResourceLocation organId,
      List<String> tags,
      String description,
      String sourceHint,
      CooldownHint cooldownHint) {
    public ActiveSkillEntry {
      tags = List.copyOf(tags);
    }
  }

  private static final Map<ResourceLocation, ActiveSkillEntry> ENTRIES = new LinkedHashMap<>();
  private static boolean bootstrapped = false;

  private ActiveSkillRegistry() {}

  public static void bootstrap() {
    if (bootstrapped) {
      return;
    }
    if (!ModList.get().isLoaded("guzhenren")) {
      bootstrapped = true;
      return;
    }
    bootstrapped = true;

    register(
        "guzhenren:jiu_chong",
        "guzhenren:jiu_chong",
        "guzhenren:jiu_chong",
        tags("输出"),
        "消耗酒精储备施放醉酒吐息，触发醉拳循环",
        "compat/guzhenren/item/shi_dao/behavior/JiuChongOrganBehavior.java:46",
        () -> {
          ensureClassLoaded(JiuChongOrganBehavior.INSTANCE);
        });

    register(
        "guzhenren:gui_wu",
        "guzhenren:gui_wu",
        "guzhenren:guiqigu",
        tags("控制", "输出"),
        "启动鬼雾脚本，黑雾范围施加失明与缓慢",
        "compat/guzhenren/item/hun_dao/behavior/GuiQiGuOrganBehavior.java:57",
        () -> {
          ensureClassLoaded(GuiQiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:qing_feng_lun_gu/dash",
        "guzhenren:qing_feng_lun_gu/dash",
        "guzhenren:qing_feng_lun_gu",
        tags("机动", "位移"),
        "疾风冲刺：消耗真元直线突进 6 格并击退敌人，冷却 6 秒",
        "compat/guzhenren/item/feng_dao/behavior/QingFengLunOrganBehavior.java",
        () -> {
          ensureClassLoaded(QingFengLunOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:qing_feng_lun_gu/wind_slash",
        "guzhenren:qing_feng_lun_gu/wind_slash",
        "guzhenren:qing_feng_lun_gu",
        tags("机动", "输出"),
        "风裂步：冲刺后短时间内释放 5 格风刃造成伤害与击退",
        "compat/guzhenren/item/feng_dao/behavior/QingFengLunOrganBehavior.java",
        () -> {
          ensureClassLoaded(QingFengLunOrganBehavior.INSTANCE);
        });

    register(
        "guzhenren:qing_feng_lun_gu/wind_domain",
        "guzhenren:qing_feng_lun_gu/wind_domain",
        "guzhenren:qing_feng_lun_gu",
        tags("增益", "领域", "机动"),
        "风神领域：消耗满层风势开启 10 秒领域，提升移速并减缓来袭弹道",
        "compat/guzhenren/item/feng_dao/behavior/QingFengLunOrganBehavior.java",
        () -> {
          ensureClassLoaded(QingFengLunOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:synergy/hun_shou_hua",
        "guzhenren:synergy/hun_shou_hua",
        "guzhenren:dahungu",
        tags("联动", "魂兽", "仪式"),
        "小魂蛊与大魂蛊联动：引导魂兽化仪式，成功后永久化身魂兽",
        "compat/guzhenren/item/synergy/hun_dao/HunShouHuaSynergyBehavior.java",
        () -> {
          ensureClassLoaded(HunShouHuaSynergyBehavior.INSTANCE);
        });

    register(
        "guzhenren:le_gu_dun_gu",
        "guzhenren:le_gu_dun_gu",
        "guzhenren:le_gu_dun_gu",
        tags("防御"),
        "满\"不屈\"后消耗真元获得短暂无敌与高抗性",
        "compat/guzhenren/item/gu_dao/behavior/LeGuDunGuOrganBehavior.java:49",
        () -> {
          ensureClassLoaded(LeGuDunGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("骨盾就绪", null));

    register(
        "guzhenren:leidungu_active",
        "guzhenren:leidungu_active",
        "guzhenren:leidungu",
        tags("防御", "控制"),
        "释放雷枢冲击周围敌人，造成感电 DoT 并短时迟缓",
        "compat/guzhenren/item/lei_dao/behavior/LeiDunGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(LeiDunGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:yin_yun_gu",
        "guzhenren:yin_yun_gu",
        "guzhenren:yin_yun_gu",
        tags("输出", "控制"),
        "消耗全部阴纹拉扯周围敌人并延迟引下多道雷狱，对范围敌人造成雷击并施加虚弱",
        "compat/guzhenren/item/yun_dao_cloud/behavior/YinYunGuOrganBehavior.java:166",
        () -> {
          ensureClassLoaded(YinYunGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:yu_qun",
        "guzhenren:yu_qun",
        "guzhenren:yu_lin_gu",
        tags("控制", "机动"),
        "鱼群：在水中或潮湿环境中发射水灵体齐射，击飞并减速前方敌人",
        "compat/guzhenren/item/bian_hua_dao/behavior/skills/YuQunSkill.java",
        () -> {
          ensureClassLoaded(YuLinGuBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("鱼群冷却结束", null));

    register(
        "guzhenren:yu_yue",
        "guzhenren:yu_yue",
        "guzhenren:yu_lin_gu",
        tags("机动", "位移"),
        "鱼跃破浪：水中高速冲刺破浪，潮湿状态下也可短距离跃进并获得缓降",
        "compat/guzhenren/item/bian_hua_dao/behavior/skills/YuYueSkill.java",
        () -> {
          ensureClassLoaded(YuLinGuBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("鱼跃冷却结束", null));

    register(
        "guzhenren:yu_shi_summon",
        "guzhenren:yu_shi_summon",
        "guzhenren:yu_lin_gu",
        tags("召唤", "支援"),
        "饵祭召鲨：消耗鲨鱼蛊材召唤协战鲨鱼，存在 120 秒且最多同时五条",
        "compat/guzhenren/item/bian_hua_dao/behavior/skills/YuShiSummonSharkSkill.java",
        () -> {
          ensureClassLoaded(YuLinGuBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("饵祭冷却结束", null));

    register(
        "guzhenren:tiepi/hardening",
        "guzhenren:tiepi/hardening",
        "guzhenren:t_tie_pi_gu",
        tags("增益", "防御", "输出"),
        "硬化：15 秒力量/防御/挖掘提升，期间每秒消耗精力与饱食；高阶延长持续并附加 5% 真实伤害",
        "compat/guzhenren/item/jin_dao/behavior/TiePiGuBehavior.java",
        () -> {
          ensureClassLoaded(TiePiGuBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("硬化就绪", null));

    register(
        "guzhenren:tiepi/ironwall",
        "guzhenren:tiepi/ironwall",
        "guzhenren:t_tie_pi_gu",
        tags("防御", "护盾"),
        "铁壁：3 秒减伤 5%、移速 -15%，阶段加成延长持续与冷却缩减；吸收伤害累积铁皮 SP",
        "compat/guzhenren/item/jin_dao/behavior/TiePiGuBehavior.java",
        () -> {
          ensureClassLoaded(TiePiGuBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("铁壁就绪", null));

    register(
        "guzhenren:tiepi/heavy_blow",
        "guzhenren:tiepi/heavy_blow",
        "guzhenren:t_tie_pi_gu",
        tags("输出", "爆发"),
        "沉击：蓄力至多 8 秒，下一次近战 +30% 伤害并附击退；命中方块直接破坏，阶段强化增伤与抗性",
        "compat/guzhenren/item/jin_dao/behavior/TiePiGuBehavior.java",
        () -> {
          ensureClassLoaded(TiePiGuBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("沉击就绪", null));

    register(
            "guzhenren:tiepi/slam_fist",
            "guzhenren:tiepi/slam_fist",
            "guzhenren:t_tie_pi_gu",
            tags("联动", "近战"),
            "重拳沉坠：铁皮蛊 + 铁骨系联动，扇形重拳造成 100% 攻击伤害并刷新沉击冷却",
            "compat/guzhenren/item/jin_dao/behavior/TiePiGuBehavior.java",
            () -> {
              ensureClassLoaded(TiePiGuBehavior.INSTANCE);
            })
        .withCooldownToast("重拳沉坠就绪");

    register(
        "guzhenren:skill/shou_pi_gu_drum",
        "guzhenren:skill/shou_pi_gu_drum",
        "guzhenren:shou_pi_gu",
        tags("防御", "反射"),
        "硬皮鼓动：5 秒内防御系数+6%，软反伤额外+10%，并获得 50% 击退抗性，冷却 20 秒",
        "compat/guzhenren/item/bian_hua_dao/behavior/ShouPiGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(ShouPiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:skill/shou_pi_gu_roll",
        "guzhenren:skill/shou_pi_gu_roll",
        "guzhenren:shou_pi_gu",
        tags("机动", "防御"),
        "翻滚脱力：向前翻滚最多 3 格，0.6 秒内承伤-60%，并对最近敌人施加 1 秒缓慢，冷却 14 秒",
        "compat/guzhenren/item/bian_hua_dao/behavior/ShouPiGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(ShouPiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:synergy/qian_jia_chong_zhuang",
        "guzhenren:synergy/qian_jia_chong_zhuang",
        "guzhenren:shou_pi_gu",
        tags("联动", "输出", "防御"),
        "嵌甲冲撞：与虎皮蛊、铁骨蛊共鸣突进 4 格，爆发过去 5 秒软反伤累计的 35%（上限=8+攻击×0.6）并获得 0.5 秒免伤，冷却 18 秒",
        "compat/guzhenren/item/bian_hua_dao/behavior/ShouPiGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(ShouPiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:skill/yinshi_tunnel",
        "guzhenren:skill/yinshi_tunnel",
        "guzhenren:y_yin_shi_gu",
        tags("机动", "防御"),
        "石潜：短距穿石闪位并获得吸收护盾与抗击退，冷却视转数 16~18 秒",
        "compat/guzhenren/item/tu_dao/behavior/YinShiGuOrganBehavior.java:240",
        () -> {
          ensureClassLoaded(YinShiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:skill/yinshi_statue",
        "guzhenren:skill/yinshi_statue",
        "guzhenren:y_yin_shi_gu",
        tags("防御", "保命"),
        "石像化：五转解锁，2.5 秒石肤抗性与免击退，冷却 60 秒",
        "compat/guzhenren/item/tu_dao/behavior/YinShiGuOrganBehavior.java:310",
        () -> {
          ensureClassLoaded(YinShiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:shou_gu",
        "guzhenren:shou_gu",
        "guzhenren:shou_gu",
        tags("防御", "续命"),
        "清空最多三层寿纹，6 秒内将 50% 伤害转为寿债并快速回血，结束时返还缓死冷却时间",
        "compat/guzhenren/item/tian_dao/behavior/ShouGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(ShouGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:shi_nian_shou_gu",
        "guzhenren:shi_nian_shou_gu",
        "guzhenren:shi_nian_shou_gu",
        tags("防御", "续命"),
        "换命・续命进阶版：持续 6 秒转化 60% 伤害为寿债并强化治疗，缓死冷却额外回填",
        "compat/guzhenren/item/tian_dao/behavior/ShouGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(ShouGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:bainianshougu",
        "guzhenren:bainianshougu",
        "guzhenren:bainianshougu",
        tags("防御", "续命"),
        "百年寿蛊版主动技：将 65% 伤害延后偿还并大幅自愈，风险更高但回报最大",
        "compat/guzhenren/item/tian_dao/behavior/ShouGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(ShouGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:bai_yun_gu/cloud_step",
        "guzhenren:bai_yun_gu/cloud_step",
        "guzhenren:bai_yun_gu",
        tags("输出", "防御"),
        "引爆云堆造成范围伤害并赋予抗性，层数高时生成雾域",
        "compat/guzhenren/item/yun_dao_cloud/behavior/BaiYunGuOrganBehavior.java:69",
        () -> {
          ensureClassLoaded(BaiYunGuOrganBehavior.INSTANCE);
        });

    register(
        "guzhenren:xie_di_gu_detonate",
        "guzhenren:xie_di_gu_detonate",
        "guzhenren:xie_di_gu",
        tags("输出", "辅助"),
        "引爆储存血滴造成真实伤害与流血，同时回复资源",
        "compat/guzhenren/item/xue_dao/behavior/XiediguOrganBehavior.java:63",
        () -> {
          ensureClassLoaded(XiediguOrganBehavior.INSTANCE);
        });

    register(
            "guzhenren:skill/xie_wang_cast",
            "guzhenren:skill/xie_wang_cast",
            "guzhenren:xie_wang_gu",
            tags("控制", "领域"),
            "投掷血丝囊在落点展开血网域，6 秒内禁跳并削弱攻速",
            "compat/guzhenren/item/xue_dao/behavior/skills/XieWangCastSkill.java",
            () -> {
              ensureClassLoaded(XieWangGuOrganBehavior.INSTANCE);
            })
        .withCooldownToast("血丝回复");

    register(
            "guzhenren:skill/xie_wang_pull",
            "guzhenren:skill/xie_wang_pull",
            "guzhenren:xie_wang_gu",
            tags("控制", "位移"),
            "缚脉拉拽血网内的敌人，向域心拖行最多 3 格",
            "compat/guzhenren/item/xue_dao/behavior/skills/XieWangPullSkill.java",
            () -> {
              ensureClassLoaded(XieWangGuOrganBehavior.INSTANCE);
            })
        .withCooldownToast("缚脉牵引就位");

    register(
            "guzhenren:skill/xie_wang_anchor",
            "guzhenren:skill/xie_wang_anchor",
            "guzhenren:xie_wang_gu",
            tags("控制", "守点"),
            "在脚下凝血成锚，强化血网禁跳并延长持续",
            "compat/guzhenren/item/xue_dao/behavior/skills/XieWangAnchorSkill.java",
            () -> {
              ensureClassLoaded(XieWangGuOrganBehavior.INSTANCE);
            })
        .withCooldownToast("凝血锚稳定");

    register(
            "guzhenren:skill/xie_wang_blind",
            "guzhenren:skill/xie_wang_blind",
            "guzhenren:xie_wang_gu",
            tags("控制", "干扰"),
            "在指定位置释放血雾，使敌人短暂失明并削弱投射物",
            "compat/guzhenren/item/xue_dao/behavior/skills/XieWangBlindSkill.java",
            () -> {
              ensureClassLoaded(XieWangGuOrganBehavior.INSTANCE);
            })
        .withCooldownToast("血雾消散");

    register(
            "guzhenren:skill/xie_wang_constrict",
            "guzhenren:skill/xie_wang_constrict",
            "guzhenren:xie_wang_gu",
            tags("输出", "持续"),
            "将血网转为倒刺态，周期造成魔法伤并施加出血",
            "compat/guzhenren/item/xue_dao/behavior/skills/XieWangConstrictSkill.java",
            () -> {
              ensureClassLoaded(XieWangGuOrganBehavior.INSTANCE);
            })
        .withCooldownToast("血刺收束待命");

    register(
        "guzhenren:xuezhangu",
        "guzhenren:xuezhangu",
        "guzhenren:xuezhangu",
        tags("输出", "增强"),
        "消耗生命与真元爆发血誓，斩击周围敌人并瞬时灌满战血，短时间内大幅提升吸血与攻击",
        "compat/guzhenren/item/xue_dao/behavior/XueZhanGuOrganBehavior.java:264",
        () -> {
          ensureClassLoaded(XueZhanGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    // 血衣蛊 (Xue Yi Gu) - 4 active skills
    register(
        "guzhenren:xue_yi_gu/xue_yong_pi_shen",
        "guzhenren:xue_yi_gu/xue_yong_pi_shen",
        "guzhenren:xueyigu",
        tags("防御", "光环", "流血"),
        "开关型光环，半径2格每0.5秒施加流血DoT，每秒消耗生命与资源，冷却8秒",
        "compat/guzhenren/item/xue_dao/behavior/skills/XueYongPiShenSkill.java",
        () -> {
          ensureClassLoaded(XueYiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("血涌披身就绪", null));

    register(
        "guzhenren:xue_yi_gu/xue_shu_shou_jin",
        "guzhenren:xue_yi_gu/xue_shu_shou_jin",
        "guzhenren:xueyigu",
        tags("输出", "控制", "流血"),
        "发射血束对命中目标施加缓慢IV(2秒)与流血10/秒×5秒，冷却16秒",
        "compat/guzhenren/item/xue_dao/behavior/skills/XueShuShouJinSkill.java",
        () -> {
          ensureClassLoaded(XueYiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("血束收紧就绪", null));

    register(
        "guzhenren:xue_yi_gu/xue_feng_ji_bi",
        "guzhenren:xue_yi_gu/xue_feng_ji_bi",
        "guzhenren:xueyigu",
        tags("防御", "治疗", "吸收"),
        "将附近敌人流血值转为临时生命(Absorption)并清除自身流血类减益，冷却20秒",
        "compat/guzhenren/item/xue_dao/behavior/skills/XueFengJiBiSkill.java",
        () -> {
          ensureClassLoaded(XueYiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("血缝急闭就绪", null));

    register(
        "guzhenren:xue_yi_gu/yi_xue_fan_ci",
        "guzhenren:xue_yi_gu/yi_xue_fan_ci",
        "guzhenren:xueyigu",
        tags("防御", "反伤", "流血"),
        "3秒窗口将承受近战伤害的50%以流血形式反射给攻击者(单次有上限)，冷却25秒",
        "compat/guzhenren/item/xue_dao/behavior/skills/YiXueFanCiSkill.java",
        () -> {
          ensureClassLoaded(XueYiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("溢血反刺就绪", null));

    register(
        "guzhenren:luo_xuan_gu_qiang_gu",
        "guzhenren:luo_xuan_gu_qiang_gu",
        "guzhenren:luo_xuan_gu_qiang_gu",
        tags("输出"),
        "消耗骨枪充能发射穿刺投射物，受骨道/力道增益影响",
        "compat/guzhenren/item/gu_dao/behavior/LuoXuanGuQiangguOrganBehavior.java:54",
        () -> {
          ensureClassLoaded(LuoXuanGuQiangguOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("骨枪充能完成", null));

    register(
        "guzhenren:xie_fei_gu",
        "guzhenren:xie_fei_gu",
        "guzhenren:xie_fei_gu",
        tags("输出", "控制"),
        "付出生命与真元喷出血雾，造成持续伤害并施加失明/中毒",
        "compat/guzhenren/item/xue_dao/behavior/XieFeiguOrganBehavior.java:63",
        () -> {
          ensureClassLoaded(XieFeiguOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:liandaogu",
        "guzhenren:liandaogu",
        "guzhenren:liandaogu",
        tags("输出"),
        "蓄力释放长条刀光，对面前敌人造成高额斩击与击退",
        "compat/guzhenren/item/mu_dao/behavior/LiandaoGuOrganBehavior.java:49",
        () -> {
          ensureClassLoaded(LiandaoGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:sheng_ji_xie_burst",
        "guzhenren:sheng_ji_xie_burst",
        "guzhenren:sheng_ji_xie",
        tags("辅助", "治疗"),
        "催动生机叶，瞬发生机脉冲治疗自身与附近友方并赋予短时抗性/再生",
        "compat/guzhenren/item/mu_dao/behavior/ShengJiYeOrganBehavior.java",
        () -> {
          ensureClassLoaded(ShengJiYeOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:jiu_xie_sheng_ji_cao_cui_sheng",
        "guzhenren:jiu_xie_sheng_ji_cao_cui_sheng",
        "guzhenren:jiu_xie_sheng_ji_cao",
        tags("辅助", "治疗"),
        "催动九叶生机，治疗周围友方并赋予防御增益，阶段越高效果越强",
        "compat/guzhenren/item/mu_dao/behavior/JiuYeShengJiCaoOrganBehavior.java:0",
        () -> {
          ensureClassLoaded(JiuYeShengJiCaoOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    // 草裙蛊：木道系防御/恢复器官，提供护幕、治疗、替身吸收与防御姿态四个主动技
    register(
        "guzhenren:cao_qun_gu/a1",
        "guzhenren:caoqungu_a1",
        "guzhenren:caoqungu",
        tags("防御", "护盾"),
        "藤裙护幕：消耗真元展开木藤护幕并附带近战反伤，阶段提升护幕吸收上限",
        "compat/guzhenren/item/mu_dao/behavior/CaoQunGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(CaoQunGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("藤裙护幕就绪", null));

    register(
        "guzhenren:cao_qun_gu/a2",
        "guzhenren:caoqungu_a2",
        "guzhenren:caoqungu",
        tags("治疗", "回复"),
        "露润回春：为范围内队友施加持续治疗并可随阶段扩展目标或半径",
        "compat/guzhenren/item/mu_dao/behavior/CaoQunGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(CaoQunGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("露润回春就绪", null));

    register(
        "guzhenren:cao_qun_gu/a3",
        "guzhenren:caoqungu_a3",
        "guzhenren:caoqungu",
        tags("防御", "减伤"),
        "木心替身：短时间替身吸收伤害并在结束时按吸收量自愈",
        "compat/guzhenren/item/mu_dao/behavior/CaoQunGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(CaoQunGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("木心替身就绪", null));

    register(
        "guzhenren:cao_qun_gu/a4",
        "guzhenren:caoqungu_a4",
        "guzhenren:caoqungu",
        tags("防御", "姿态"),
        "叶影庇佑：进入防御姿态减伤并可正面格挡，姿态结束后获得短暂再生",
        "compat/guzhenren/item/mu_dao/behavior/CaoQunGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(CaoQunGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("叶影庇佑就绪", null));

    register(
        "guzhenren:qing_tong_she_li_gu",
        "guzhenren:qing_tong_she_li_gu",
        "guzhenren:qing_tong_she_li_gu",
        tags("防御"),
        "入定 3 秒获得抗性 II，并重置冷却计时",
        "compat/guzhenren/item/ren_dao/behavior/QingTongSheLiGuOrganBehavior.java:26",
        () -> {
          ensureClassLoaded(QingTongSheLiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:zi_jin_she_li_gu",
        "guzhenren:zi_jin_she_li_gu",
        "guzhenren:zi_jin_she_li_gu",
        tags("辅助", "治疗"),
        "燃烧 50% 生命与真元展开 15 秒领域，每秒恢复友方 10% 资源",
        "compat/guzhenren/item/ren_dao/behavior/ZaijinSheLiGuOrganBehavior.java:44",
        () -> {
          ensureClassLoaded(ZaijinSheLiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:shan_guang_gu_flash",
        "guzhenren:shan_guang_gu_flash",
        "guzhenren:shan_guang_gu",
        tags("输出", "控制"),
        "闪现造成范围伤害并致盲减速，施放者获得短暂加速",
        "compat/guzhenren/item/guang_dao/behavior/ShanGuangGuOrganBehavior.java:61",
        () -> {
          ensureClassLoaded(ShanGuangGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan(
            "闪爆就绪", formatCooldownSeconds(ShanGuangGuOrganBehavior.getActiveCooldownTicks())));

    register(
        "guzhenren:xiao_guang_illusion",
        "guzhenren:xiao_guang_illusion",
        "guzhenren:xiaoguanggu",
        tags("控制", "防御"),
        "消耗真元召唤幻映分身诱导火力，期间触发折影与光遁强化",
        "compat/guzhenren/item/guang_dao/behavior/XiaoGuangGuOrganBehavior.java:289",
        () -> {
          ensureClassLoaded(XiaoGuangGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("幻映分身就绪", null));

    register(
        "guzhenren:bai_yin_she_li_gu",
        "guzhenren:bai_yin_she_li_gu",
        "guzhenren:bai_yin_she_li_gu",
        tags("防御"),
        "激活十秒抗性 II，期间一次致命伤改判为 1 HP + 1 秒无敌",
        "compat/guzhenren/item/ren_dao/behavior/BaiYinSheLiGuOrganBehavior.java:34",
        () -> {
          ensureClassLoaded(BaiYinSheLiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:chi_tie_she_li_gu",
        "guzhenren:chi_tie_she_li_gu",
        "guzhenren:chi_tie_she_li_gu",
        tags("辅助", "治疗"),
        "消耗真元与魂魄即刻回复 20%（上限 200）生命",
        "compat/guzhenren/item/ren_dao/behavior/ChiTieSheLiGuOrganBehavior.java:30",
        () -> {
          ensureClassLoaded(ChiTieSheLiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:huang_jin_she_li_gu",
        "guzhenren:huang_jin_she_li_gu",
        "guzhenren:huang_jin_she_li_gu",
        tags("防御", "控制"),
        "6 秒抗性 III + 免击退，自身缓慢并对 8 格敌人施加缓慢 IV",
        "compat/guzhenren/item/ren_dao/behavior/HuangJinSheLiGuOrganBehavior.java:31",
        () -> {
          ensureClassLoaded(HuangJinSheLiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:huo_gu",
        "guzhenren:huo_gu",
        "guzhenren:huo_gu",
        tags("输出", "控制"),
        "扣除真元与饥饿后激活灼烧光环，对敌灼烧并叠加缓慢",
        "compat/guzhenren/item/yan_dao/behavior/HuoYiGuOrganBehavior.java:50",
        () -> {
          ensureClassLoaded(HuoYiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:huo_you_gu",
        "guzhenren:huo_you_gu",
        "guzhenren:huo_you_gu",
        tags("输出", "反应"),
        "消耗全部燃油喷射火焰波；命中油涂层且火衣激活时引爆并暂时禁用火衣",
        "compat/guzhenren/item/yan_dao/behavior/HuoYouGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(HuoYouGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:huo_long_gu_breath",
        "guzhenren:huo_long_gu_breath",
        "guzhenren:huolonggu",
        tags("输出", "爆发"),
        "喷吐龙焰冲击，叠加龙焰印记并触发短暂悬停",
        "compat/guzhenren/item/yan_dao/behavior/HuoLongGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(HuoLongGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("龙焰充能", null));

    register(
        "guzhenren:huo_long_gu_hover",
        "guzhenren:huo_long_gu_hover",
        "guzhenren:huolonggu",
        tags("机动", "防御"),
        "凝空悬停稳定瞄准，并向标记目标释放龙火回响",
        "compat/guzhenren/item/yan_dao/behavior/HuoLongGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(HuoLongGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("悬停就绪", null));

    register(
        "guzhenren:huo_long_gu_ascend",
        "guzhenren:huo_long_gu_ascend",
        "guzhenren:huolonggu",
        tags("机动"),
        "消耗与俯冲等量真元高速升空，进入龙脊战焰姿态并快速上升",
        "compat/guzhenren/item/yan_dao/behavior/HuoLongGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(HuoLongGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("龙升就绪", null));

    register(
        "guzhenren:huo_long_gu_dive",
        "guzhenren:huo_long_gu_dive",
        "guzhenren:huolonggu",
        tags("输出", "处决"),
        "化作龙焰俯冲轰炸地面，按扣血比例放大爆炸伤害",
        "compat/guzhenren/item/yan_dao/behavior/HuoLongGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(HuoLongGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("龙降就绪", null));

    // 单窍·火炭蛊：极限技（四转）
    register(
        "guzhenren:dan_qiao_huo_tan_gu",
        "guzhenren:dan_qiao_huo_tan_gu",
        "guzhenren:dan_qiao_huo_tan_gu",
        tags("输出", "爆发"),
        "清空范围目标的炭压并按层数引发3段炭核风暴，对命中者施加火免短窗（四转解锁）",
        "compat/guzhenren/item/yan_dao/behavior/HuoTanGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(
              net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoTanGuOrganBehavior
                  .INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:jian_ying_fenshen",
        "guzhenren:jian_ying_fenshen",
        "guzhenren:jian_ying_gu",
        tags("输出", "召唤"),
        "支付真元与精力召唤剑影分身协同作战，随器官数量扩充",
        "compat/guzhenren/item/jian_dao/behavior/JianYingGuOrganBehavior.java:61",
        () -> {
          ensureClassLoaded(JianYingGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:yuan_lao_gu_5_attack",
        "guzhenren:yuan_lao_gu_5_attack",
        "guzhenren:yuan_lao_gu_5",
        tags("输出"),
        "按消耗元石对大范围敌人造成伤害，量随消耗线性提升",
        "compat/guzhenren/item/yu_dao/behavior/YuanLaoGuFifthTierBehavior.java:40",
        () -> {
          ensureClassLoaded(YuanLaoGuFifthTierBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:tu_qiang_gu",
        "guzhenren:tu_qiang_gu",
        "guzhenren:tu_qiang_gu",
        tags("控制", "防御"),
        "针对目标生成土/玉监牢并为施术者充能防护屏障",
        "compat/guzhenren/item/tu_dao/behavior/TuQiangGuOrganBehavior.java:67",
        () -> {
          ensureClassLoaded(TuQiangGuOrganBehavior.INSTANCE);
        });

    register(
        "guzhenren:zi_li_geng_sheng_gu_3",
        "guzhenren:zi_li_geng_sheng_gu_3",
        "guzhenren:zi_li_geng_sheng_gu_3",
        tags("辅助", "治疗"),
        "消耗力道肌肉后持续再生 30 秒，结束附带虚弱",
        "compat/guzhenren/item/li_dao/behavior/ZiLiGengShengGuOrganBehavior.java:45",
        () -> {
          ensureClassLoaded(ZiLiGengShengGuOrganBehavior.INSTANCE);
        });

    register(
        "guzhenren:long_wan_qu_qu_gu",
        "guzhenren:long_wan_qu_qu_gu",
        "guzhenren:long_wan_qu_qu_gu",
        tags("防御", "机动"),
        "启动后获得 3 次短距闪避机会并提供短暂无敌窗口",
        "compat/guzhenren/item/li_dao/behavior/LongWanQuQuGuOrganBehavior.java:30",
        () -> {
          ensureClassLoaded(LongWanQuQuGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:zhi_zhuang_gu",
        "guzhenren:zhi_zhuang_gu",
        "guzhenren:zhi_zhuang_gu",
        tags("输出", "位移"),
        "直线冲锋命中刷新冷却并累积惯性层数，影道蛊虫加持下触发灵魂回声与连锁爆发",
        "compat/guzhenren/item/li_dao/behavior/ZhiZhuangGuOrganBehavior.java:120",
        () -> {
          ensureClassLoaded(ZhiZhuangGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:huang_luo_tian_niu_gu",
        "guzhenren:huang_luo_tian_niu_gu",
        "guzhenren:huang_luo_tian_niu_gu",
        tags("召唤", "辅助"),
        "召唤发疯天牛冲锋并给予 30 秒精力消耗减免",
        "compat/guzhenren/item/li_dao/behavior/HuangLuoTianNiuGuOrganBehavior.java:38",
        () -> {
          ensureClassLoaded(HuangLuoTianNiuGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("技能就绪", null));

    register(
        "guzhenren:xiong_hao_burst",
        "guzhenren:xiong_hao_burst",
        "guzhenren:xiong_hao_gu",
        tags("爆发", "近战"),
        "豪力爆发：消耗 300 基础真元，10 秒内每次近战命中额外 -6 精力并 +10/12 伤害",
        "compat/guzhenren/item/li_dao/behavior/XiongHaoGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(XiongHaoGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("豪力已回归！", null));

    register(
        "guzhenren:xiong_hao_slam",
        "guzhenren:xiong_hao_slam",
        "guzhenren:xiong_hao_gu",
        tags("范围", "击退"),
        "破城重锤：前方 4×3×4 范围重击，命中越近伤害越高，命中 3 体返 4 精力",
        "compat/guzhenren/item/li_dao/behavior/XiongHaoGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(XiongHaoGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("重锤就位", null));

    register(
        "guzhenren:xiong_hao_roar",
        "guzhenren:xiong_hao_roar",
        "guzhenren:xiong_hao_gu",
        tags("控制", "嘲讽"),
        "威压怒吼：消耗真元/念头/魂魄，嘲讽并减速 5 格内敌人 4 秒",
        "compat/guzhenren/item/li_dao/behavior/XiongHaoGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(XiongHaoGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("怒吼再起", null));

    register(
        "guzhenren:hua_shi_gu/charge",
        "guzhenren:hua_shi_gu/charge",
        "guzhenren:hua_shi_gu",
        tags("近战", "位移", "击退"),
        "野冲：消耗 80 基础真元与 6 精力，向前冲刺 4.5 格并击退首个命中目标",
        "compat/guzhenren/item/li_dao/behavior/HuaShiGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(HuaShiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("野冲就绪", "冲锋待命"));

    register(
        "guzhenren:hua_shi_gu/hoofquake",
        "guzhenren:hua_shi_gu/hoofquake",
        "guzhenren:hua_shi_gu",
        tags("近战", "控制", "范围"),
        "蹄震：消耗 120 基础真元与 8 精力，释放 3 格冲击波造成伤害与缓慢",
        "compat/guzhenren/item/li_dao/behavior/HuaShiGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(HuaShiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("蹄震就绪", "震击待命"));

    register(
        "guzhenren:hua_shi_gu/overload_burst",
        "guzhenren:hua_shi_gu/overload_burst",
        "guzhenren:hua_shi_gu",
        tags("近战", "蓄力", "爆发"),
        "负重爆发：消耗 150 基础真元与 10 精力进入 4 秒蓄势，累积层数后强化下一次普攻",
        "compat/guzhenren/item/li_dao/behavior/HuaShiGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(HuaShiGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("爆发就绪", "冲势充盈"));

    // 蛮力天牛蛊主动技：蛮牛激发与横冲直撞
    register(
        "guzhenren:man_li_tian_niu_gu/boost",
        "guzhenren:man_li_tian_niu_gu/boost",
        "guzhenren:man_li_tian_niu_gu",
        tags("强化", "近战"),
        "蛮牛激发：消耗约 200 基础真元获得 10 秒强筋，普攻追加精力换取真实伤害",
        "compat/guzhenren/item/li_dao/behavior/ManLiTianNiuGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(ManLiTianNiuGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan(
            "激发就绪", formatCooldownSeconds(ManLiTianNiuGuOrganBehavior.getBoostCooldownTicks())));

    register(
        "guzhenren:man_li_tian_niu_gu/rush",
        "guzhenren:man_li_tian_niu_gu/rush",
        "guzhenren:man_li_tian_niu_gu",
        tags("位移", "近战", "控制"),
        "横冲直撞：消耗约 80 真元与 10 精力突进 8 刻，首个目标受额外伤害与击退",
        "compat/guzhenren/item/li_dao/behavior/ManLiTianNiuGuOrganBehavior.java",
        () -> {
          ensureClassLoaded(ManLiTianNiuGuOrganBehavior.INSTANCE);
        },
        CooldownHint.useOrgan(
            "冲撞就绪", formatCooldownSeconds(ManLiTianNiuGuOrganBehavior.getRushCooldownTicks())));

    register(
        "guzhenren:shuang_xi_gu_frost_breath",
        "guzhenren:shuang_xi_gu_frost_breath",
        "guzhenren:shuang_xi_gu",
        tags("输出", "控制"),
        "锥形霜息附加寒冷与霜蚀 DoT，强度受冰雪道增益影响",
        "compat/guzhenren/item/bing_xue_dao/behavior/ShuangXiGuOrganBehavior.java:67",
        () -> {
          ensureClassLoaded(ShuangXiGuOrganBehavior.INSTANCE);
        });

    register(
        "guzhenren:bing_ji_gu_iceburst",
        "guzhenren:bing_ji_gu_iceburst",
        "guzhenren:bing_ji_gu",
        tags("输出", "控制"),
        "拥有玉骨时引爆寒冰冲击波，对范围敌人造成伤害与高额减速",
        "compat/guzhenren/item/bing_xue_dao/behavior/BingJiGuOrganBehavior.java:75",
        () -> {
          ensureClassLoaded(BingJiGuOrganBehavior.INSTANCE);
        });

    // 舍生取义（联动）：以生机叶图标展示，实际激活时要求具备舍利蛊 + 生机系器官
    register(
        "guzhenren:synergy/she_sheng_qu_yi",
        "guzhenren:synergy/she_sheng_qu_yi",
        "guzhenren:sheng_ji_xie",
        tags("联动", "道德", "誓约"),
        "舍利蛊 + 生机系联动：被动将道德转化为攻击力；主动消耗寿元换取大量道德并暂时降低上限",
        "compat/guzhenren/item/synergy/SheShengQuYiSynergyBehavior.java",
        () -> {
          ensureClassLoaded(SheShengQuYiSynergyBehavior.INSTANCE);
        },
        CooldownHint.useOrgan("誓约就绪", null));
  }

  private static Registration register(
      String skillId,
      String abilityId,
      String organId,
      List<String> tags,
      String description,
      String sourceHint,
      Runnable initializer) {
    return register(skillId, abilityId, organId, tags, description, sourceHint, initializer, null);
  }

  private static Registration register(
      String skillId,
      String abilityId,
      String organId,
      List<String> tags,
      String description,
      String sourceHint,
      Runnable initializer,
      CooldownHint cooldownHint) {
    initializer.run();
    ResourceLocation skill = ResourceLocation.parse(skillId);
    ResourceLocation ability = ResourceLocation.parse(abilityId);
    ResourceLocation organ = ResourceLocation.parse(organId);
    ActiveSkillEntry previous =
        ENTRIES.put(
            skill,
            new ActiveSkillEntry(
                skill, ability, organ, tags, description, sourceHint, cooldownHint));
    if (previous != null) {
      ChestCavity.LOGGER.warn(
          "[skill][registry] duplicate registration for {} (previous organ={})",
          skill,
          previous.organId());
    }
    return new Registration(skill);
  }

  private static List<String> tags(String... values) {
    List<String> list = new ArrayList<>(values.length);
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        list.add(value);
      }
    }
    return list;
  }

  private static String formatCooldownSeconds(long ticks) {
    if (ticks <= 0L) {
      return null;
    }
    long seconds = ticks / 20L;
    long remainder = ticks % 20L;
    if (remainder == 0L) {
      return seconds + " 秒冷却";
    }
    double preciseSeconds = ticks / 20.0D;
    return String.format(Locale.ROOT, "%.1f 秒冷却", preciseSeconds);
  }

  private static void ensureClassLoaded(Object instance) {
    // no-op: touching the instance ensures the class has run its static initializer.
    if (instance == null) {
      ChestCavity.LOGGER.debug(
          "[skill][registry] attempted to touch null instance during bootstrap");
    }
  }

  public static Optional<ActiveSkillEntry> get(ResourceLocation skillId) {
    bootstrap();
    return Optional.ofNullable(ENTRIES.get(skillId));
  }

  public static Collection<ActiveSkillEntry> entries() {
    bootstrap();
    return Collections.unmodifiableCollection(ENTRIES.values());
  }

  public static boolean isSkillRegistered(ResourceLocation skillId) {
    bootstrap();
    return ENTRIES.containsKey(skillId);
  }

  private static boolean hasOrganEquipped(ChestCavityInstance cc, ResourceLocation organId) {
    if (cc == null || organId == null) {
      return false;
    }
    Item item = BuiltInRegistries.ITEM.getOptional(organId).orElse(null);
    if (item == null) {
      // 无法校验具体物品时视为通过，避免阻塞热键
      return true;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == item) {
        return true;
      }
    }
    return false;
  }

  public static TriggerResult trigger(ServerPlayer player, ResourceLocation skillId) {
    bootstrap();
    ActiveSkillEntry entry = ENTRIES.get(skillId);
    if (entry == null) {
      return TriggerResult.NOT_REGISTERED;
    }
    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null) {
      return TriggerResult.NO_CHEST_CAVITY;
    }
    if (!hasOrganEquipped(cc, entry.organId())) {
      return TriggerResult.MISSING_ORGAN;
    }
    boolean activated = OrganActivationListeners.activate(entry.abilityId(), cc);
    return activated ? TriggerResult.SUCCESS : TriggerResult.ABILITY_NOT_REGISTERED;
  }

  public static void scheduleReadyToast(
      ServerPlayer player, ResourceLocation skillId, long readyAtTick, long nowTick) {
    if (player == null) {
      return;
    }
    bootstrap();
    ActiveSkillEntry entry = ENTRIES.get(skillId);
    if (entry == null) {
      return;
    }
    CooldownHint hint = entry.cooldownHint();
    if (hint == null) {
      return;
    }
    ResourceLocation iconId = hint.iconOverride() != null ? hint.iconOverride() : entry.organId();
    ItemStack iconStack = ItemStack.EMPTY;
    if (iconId != null) {
      Item item = BuiltInRegistries.ITEM.getOptional(iconId).orElse(null);
      if (item != null) {
        iconStack = new ItemStack(item);
      }
    }
    String title = hint.title();
    if (title == null || title.isBlank()) {
      title = "技能就绪";
    }
    String subtitle = hint.subtitle();
    if ((subtitle == null || subtitle.isBlank()) && !iconStack.isEmpty()) {
      subtitle = iconStack.getHoverName().getString();
    }
    ServerLevel level = player.serverLevel();
    CountdownOps.scheduleToastAt(level, player, readyAtTick, nowTick, iconStack, title, subtitle);
  }

  public static void pushToast(
      ServerPlayer player, ResourceLocation iconId, String title, String subtitle) {
    if (player == null) {
      return;
    }
    ServerLevel level = player.serverLevel();
    ItemStack iconStack = ItemStack.EMPTY;
    if (iconId != null) {
      Item item = BuiltInRegistries.ITEM.getOptional(iconId).orElse(null);
      if (item != null) {
        iconStack = new ItemStack(item);
      }
    }
    String resolvedTitle = (title == null || title.isBlank()) ? "技能就绪" : title;
    String resolvedSubtitle = subtitle;
    if ((resolvedSubtitle == null || resolvedSubtitle.isBlank()) && !iconStack.isEmpty()) {
      resolvedSubtitle = iconStack.getHoverName().getString();
    }
    CountdownOps.scheduleToast(level, player, 0, iconStack, resolvedTitle, resolvedSubtitle);
  }

  public record CooldownHint(ResourceLocation iconOverride, String title, String subtitle) {
    public static CooldownHint useOrgan(String title, String subtitle) {
      return new CooldownHint(null, title, subtitle);
    }
  }

  public record Registration(ResourceLocation skillId) {
    public Registration withCooldownToast(String title) {
      return withCooldownToast(title, null, null);
    }

    public Registration withCooldownToast(String title, String subtitle) {
      return withCooldownToast(title, null, subtitle);
    }

    public Registration withCooldownToast(
        String title, ResourceLocation iconOverride, String subtitle) {
      if (skillId == null) {
        return this;
      }
      ActiveSkillEntry entry = ENTRIES.get(skillId);
      if (entry == null) {
        return this;
      }
      CooldownHint hint = new CooldownHint(iconOverride, title, subtitle);
      ActiveSkillEntry updated =
          new ActiveSkillEntry(
              entry.skillId(),
              entry.abilityId(),
              entry.organId(),
              entry.tags(),
              entry.description(),
              entry.sourceHint(),
              hint);
      ENTRIES.put(skillId, updated);
      return this;
    }
  }
}
