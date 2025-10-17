package net.tigereye.chestcavity.skill;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.BingJiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.ShuangXiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LeGuDunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LuoXuanGuQiangguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.behavior.ShanGuangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.GuiQiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.JianYingGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HuangLuoTianNiuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HuaShiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.LongWanQuQuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ZiLiGengShengGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.XiongHaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ZhiZhuangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.JiuYeShengJiCaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.LiandaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.ShengJiYeOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.synergy.SheShengQuYiSynergyBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.BaiYinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.ChiTieSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.HuangJinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.QingTongSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.ZaijinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.behavior.JiuChongOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior.TuQiangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.tian_dao.behavior.ShouGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieFeiguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XiediguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XueZhanGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoYiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.behavior.BaiYunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.behavior.YinYunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior.YuanLaoGuFifthTierBehavior;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.CountdownOps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry for Guzhenren active skills. Provides metadata and ability dispatch.
 */
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
            CooldownHint cooldownHint
    ) {
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

        register("guzhenren:jiu_chong", "guzhenren:jiu_chong", "guzhenren:jiu_chong",
                tags("输出"),
                "消耗酒精储备施放醉酒吐息，触发醉拳循环",
                "compat/guzhenren/item/shi_dao/behavior/JiuChongOrganBehavior.java:46",
                () -> { ensureClassLoaded(JiuChongOrganBehavior.INSTANCE); });

        register("guzhenren:gui_wu", "guzhenren:gui_wu", "guzhenren:guiqigu",
                tags("控制", "输出"),
                "启动鬼雾脚本，黑雾范围施加失明与缓慢",
                "compat/guzhenren/item/hun_dao/behavior/GuiQiGuOrganBehavior.java:57",
                () -> { ensureClassLoaded(GuiQiGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:le_gu_dun_gu", "guzhenren:le_gu_dun_gu", "guzhenren:le_gu_dun_gu",
                tags("防御"),
                "满\"不屈\"后消耗真元获得短暂无敌与高抗性",
                "compat/guzhenren/item/gu_dao/behavior/LeGuDunGuOrganBehavior.java:49",
                () -> { ensureClassLoaded(LeGuDunGuOrganBehavior.INSTANCE); });

        register("guzhenren:yin_yun_gu", "guzhenren:yin_yun_gu", "guzhenren:yin_yun_gu",
                tags("输出", "控制"),
                "消耗全部阴纹拉扯周围敌人并延迟引下多道雷狱，对范围敌人造成雷击并施加虚弱",
                "compat/guzhenren/item/yun_dao_cloud/behavior/YinYunGuOrganBehavior.java:166",
                () -> { ensureClassLoaded(YinYunGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:shou_gu", "guzhenren:shou_gu", "guzhenren:shou_gu",
                tags("防御", "续命"),
                "清空最多三层寿纹，6 秒内将 50% 伤害转为寿债并快速回血，结束时返还缓死冷却时间",
                "compat/guzhenren/item/tian_dao/behavior/ShouGuOrganBehavior.java",
                () -> { ensureClassLoaded(ShouGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:shi_nian_shou_gu", "guzhenren:shi_nian_shou_gu", "guzhenren:shi_nian_shou_gu",
                tags("防御", "续命"),
                "换命・续命进阶版：持续 6 秒转化 60% 伤害为寿债并强化治疗，缓死冷却额外回填",
                "compat/guzhenren/item/tian_dao/behavior/ShouGuOrganBehavior.java",
                () -> { ensureClassLoaded(ShouGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:bainianshougu", "guzhenren:bainianshougu", "guzhenren:bainianshougu",
                tags("防御", "续命"),
                "百年寿蛊版主动技：将 65% 伤害延后偿还并大幅自愈，风险更高但回报最大",
                "compat/guzhenren/item/tian_dao/behavior/ShouGuOrganBehavior.java",
                () -> { ensureClassLoaded(ShouGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:bai_yun_gu/cloud_step", "guzhenren:bai_yun_gu/cloud_step", "guzhenren:bai_yun_gu",
                tags("输出", "防御"),
                "引爆云堆造成范围伤害并赋予抗性，层数高时生成雾域",
                "compat/guzhenren/item/yun_dao_cloud/behavior/BaiYunGuOrganBehavior.java:69",
                () -> { ensureClassLoaded(BaiYunGuOrganBehavior.INSTANCE); });

        register("guzhenren:xie_di_gu_detonate", "guzhenren:xie_di_gu_detonate", "guzhenren:xie_di_gu",
                tags("输出", "辅助"),
                "引爆储存血滴造成真实伤害与流血，同时回复资源",
                "compat/guzhenren/item/xue_dao/behavior/XiediguOrganBehavior.java:63",
                () -> { ensureClassLoaded(XiediguOrganBehavior.INSTANCE); });

        register("guzhenren:xuezhangu", "guzhenren:xuezhangu", "guzhenren:xuezhangu",
                tags("输出", "增强"),
                "消耗生命与真元爆发血誓，斩击周围敌人并瞬时灌满战血，短时间内大幅提升吸血与攻击",
                "compat/guzhenren/item/xue_dao/behavior/XueZhanGuOrganBehavior.java:264",
                () -> { ensureClassLoaded(XueZhanGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:luo_xuan_gu_qiang_gu", "guzhenren:luo_xuan_gu_qiang_gu", "guzhenren:luo_xuan_gu_qiang_gu",
                tags("输出"),
                "消耗骨枪充能发射穿刺投射物，受骨道/力道增益影响",
                "compat/guzhenren/item/gu_dao/behavior/LuoXuanGuQiangguOrganBehavior.java:54",
                () -> { ensureClassLoaded(LuoXuanGuQiangguOrganBehavior.INSTANCE); });

        register("guzhenren:xie_fei_gu", "guzhenren:xie_fei_gu", "guzhenren:xie_fei_gu",
                tags("输出", "控制"),
                "付出生命与真元喷出血雾，造成持续伤害并施加失明/中毒",
                "compat/guzhenren/item/xue_dao/behavior/XieFeiguOrganBehavior.java:63",
                () -> { ensureClassLoaded(XieFeiguOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:liandaogu", "guzhenren:liandaogu", "guzhenren:liandaogu",
                tags("输出"),
                "蓄力释放长条刀光，对面前敌人造成高额斩击与击退",
                "compat/guzhenren/item/mu_dao/behavior/LiandaoGuOrganBehavior.java:49",
                () -> { ensureClassLoaded(LiandaoGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:sheng_ji_xie_burst", "guzhenren:sheng_ji_xie_burst", "guzhenren:sheng_ji_xie",
                tags("辅助", "治疗"),
                "催动生机叶，瞬发生机脉冲治疗自身与附近友方并赋予短时抗性/再生",
                "compat/guzhenren/item/mu_dao/behavior/ShengJiYeOrganBehavior.java",
                () -> { ensureClassLoaded(ShengJiYeOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:jiu_xie_sheng_ji_cao_cui_sheng", "guzhenren:jiu_xie_sheng_ji_cao_cui_sheng", "guzhenren:jiu_xie_sheng_ji_cao",
                tags("辅助", "治疗"),
                "催动九叶生机，治疗周围友方并赋予防御增益，阶段越高效果越强",
                "compat/guzhenren/item/mu_dao/behavior/JiuYeShengJiCaoOrganBehavior.java:0",
                () -> { ensureClassLoaded(JiuYeShengJiCaoOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:qing_tong_she_li_gu", "guzhenren:qing_tong_she_li_gu", "guzhenren:qing_tong_she_li_gu",
                tags("防御"),
                "入定 3 秒获得抗性 II，并重置冷却计时",
                "compat/guzhenren/item/ren_dao/behavior/QingTongSheLiGuOrganBehavior.java:26",
                () -> { ensureClassLoaded(QingTongSheLiGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:zi_jin_she_li_gu", "guzhenren:zi_jin_she_li_gu", "guzhenren:zi_jin_she_li_gu",
                tags("辅助", "治疗"),
                "燃烧 50% 生命与真元展开 15 秒领域，每秒恢复友方 10% 资源",
                "compat/guzhenren/item/ren_dao/behavior/ZaijinSheLiGuOrganBehavior.java:44",
                () -> { ensureClassLoaded(ZaijinSheLiGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:shan_guang_gu_flash", "guzhenren:shan_guang_gu_flash", "guzhenren:shan_guang_gu",
                tags("输出", "控制"),
                "闪现造成范围伤害并致盲减速，施放者获得短暂加速",
                "compat/guzhenren/item/guang_dao/behavior/ShanGuangGuOrganBehavior.java:61",
                () -> { ensureClassLoaded(ShanGuangGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:bai_yin_she_li_gu", "guzhenren:bai_yin_she_li_gu", "guzhenren:bai_yin_she_li_gu",
                tags("防御"),
                "激活十秒抗性 II，期间一次致命伤改判为 1 HP + 1 秒无敌",
                "compat/guzhenren/item/ren_dao/behavior/BaiYinSheLiGuOrganBehavior.java:34",
                () -> { ensureClassLoaded(BaiYinSheLiGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:chi_tie_she_li_gu", "guzhenren:chi_tie_she_li_gu", "guzhenren:chi_tie_she_li_gu",
                tags("辅助", "治疗"),
                "消耗真元与魂魄即刻回复 20%（上限 200）生命",
                "compat/guzhenren/item/ren_dao/behavior/ChiTieSheLiGuOrganBehavior.java:30",
                () -> { ensureClassLoaded(ChiTieSheLiGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:huang_jin_she_li_gu", "guzhenren:huang_jin_she_li_gu", "guzhenren:huang_jin_she_li_gu",
                tags("防御", "控制"),
                "6 秒抗性 III + 免击退，自身缓慢并对 8 格敌人施加缓慢 IV",
                "compat/guzhenren/item/ren_dao/behavior/HuangJinSheLiGuOrganBehavior.java:31",
                () -> { ensureClassLoaded(HuangJinSheLiGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:huo_gu", "guzhenren:huo_gu", "guzhenren:huo_gu",
                tags("输出", "控制"),
                "扣除真元与饥饿后激活灼烧光环，对敌灼烧并叠加缓慢",
                "compat/guzhenren/item/yan_dao/behavior/HuoYiGuOrganBehavior.java:50",
                () -> { ensureClassLoaded(HuoYiGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:jian_ying_fenshen", "guzhenren:jian_ying_fenshen", "guzhenren:jian_ying_gu",
                tags("输出", "召唤"),
                "支付真元与精力召唤剑影分身协同作战，随器官数量扩充",
                "compat/guzhenren/item/jian_dao/behavior/JianYingGuOrganBehavior.java:61",
                () -> { ensureClassLoaded(JianYingGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:yuan_lao_gu_5_attack", "guzhenren:yuan_lao_gu_5_attack", "guzhenren:yuan_lao_gu_5",
                tags("输出"),
                "按消耗元石对大范围敌人造成伤害，量随消耗线性提升",
                "compat/guzhenren/item/yu_dao/behavior/YuanLaoGuFifthTierBehavior.java:40",
                () -> { ensureClassLoaded(YuanLaoGuFifthTierBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:tu_qiang_gu", "guzhenren:tu_qiang_gu", "guzhenren:tu_qiang_gu",
                tags("控制", "防御"),
                "针对目标生成土/玉监牢并为施术者充能防护屏障",
                "compat/guzhenren/item/tu_dao/behavior/TuQiangGuOrganBehavior.java:67",
                () -> { ensureClassLoaded(TuQiangGuOrganBehavior.INSTANCE); });

        register("guzhenren:zi_li_geng_sheng_gu_3", "guzhenren:zi_li_geng_sheng_gu_3", "guzhenren:zi_li_geng_sheng_gu_3",
                tags("辅助", "治疗"),
                "消耗力道肌肉后持续再生 30 秒，结束附带虚弱",
                "compat/guzhenren/item/li_dao/behavior/ZiLiGengShengGuOrganBehavior.java:45",
                () -> { ensureClassLoaded(ZiLiGengShengGuOrganBehavior.INSTANCE); });

        register("guzhenren:long_wan_qu_qu_gu", "guzhenren:long_wan_qu_qu_gu", "guzhenren:long_wan_qu_qu_gu",
                tags("防御", "机动"),
                "启动后获得 3 次短距闪避机会并提供短暂无敌窗口",
                "compat/guzhenren/item/li_dao/behavior/LongWanQuQuGuOrganBehavior.java:30",
                () -> { ensureClassLoaded(LongWanQuQuGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:zhi_zhuang_gu", "guzhenren:zhi_zhuang_gu", "guzhenren:zhi_zhuang_gu",
                tags("输出", "位移"),
                "直线冲锋命中刷新冷却并累积惯性层数，魂道加持下触发灵魂回声与连锁爆发",
                "compat/guzhenren/item/li_dao/behavior/ZhiZhuangGuOrganBehavior.java:120",
                () -> { ensureClassLoaded(ZhiZhuangGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:huang_luo_tian_niu_gu", "guzhenren:huang_luo_tian_niu_gu", "guzhenren:huang_luo_tian_niu_gu",
                tags("召唤", "辅助"),
                "召唤发疯天牛冲锋并给予 30 秒精力消耗减免",
                "compat/guzhenren/item/li_dao/behavior/HuangLuoTianNiuGuOrganBehavior.java:38",
                () -> { ensureClassLoaded(HuangLuoTianNiuGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:xiong_hao_gu", "guzhenren:xiong_hao_gu", "guzhenren:xiong_hao_gu",
                tags("输出", "爆发"),
                "消耗 300 真元进入 10 秒激怒，每次近战命中额外消耗 6 精力并造成 +10 伤害",
                "compat/guzhenren/item/li_dao/behavior/XiongHaoGuOrganBehavior.java:28",
                () -> { ensureClassLoaded(XiongHaoGuOrganBehavior.INSTANCE); },
                CooldownHint.useOrgan("技能就绪", null));

        register("guzhenren:hua_shi_gu", "guzhenren:hua_shi_gu", "guzhenren:hua_shi_gu",
                tags("辅助", "增益"),
                "被动每 5 秒消耗 200 真元恢复 3 精力；主动消耗 300 真元获得 10 秒力量 III",
                "compat/guzhenren/item/li_dao/behavior/HuaShiGuOrganBehavior.java:24",
                () -> { ensureClassLoaded(HuaShiGuOrganBehavior.INSTANCE); });

        register("guzhenren:shuang_xi_gu_frost_breath", "guzhenren:shuang_xi_gu_frost_breath", "guzhenren:shuang_xi_gu",
                tags("输出", "控制"),
                "锥形霜息附加寒冷与霜蚀 DoT，强度受冰雪道增益影响",
                "compat/guzhenren/item/bing_xue_dao/behavior/ShuangXiGuOrganBehavior.java:67",
                () -> { ensureClassLoaded(ShuangXiGuOrganBehavior.INSTANCE); });

        register("guzhenren:bing_ji_gu_iceburst", "guzhenren:bing_ji_gu_iceburst", "guzhenren:bing_ji_gu",
                tags("输出", "控制"),
                "拥有玉骨时引爆寒冰冲击波，对范围敌人造成伤害与高额减速",
                "compat/guzhenren/item/bing_xue_dao/behavior/BingJiGuOrganBehavior.java:75",
                () -> { ensureClassLoaded(BingJiGuOrganBehavior.INSTANCE); });

        // 舍生取义（联动）：以生机叶图标展示，实际激活时要求具备舍利蛊 + 生机系器官
        register("guzhenren:synergy/she_sheng_qu_yi", "guzhenren:synergy/she_sheng_qu_yi", "guzhenren:sheng_ji_xie",
                tags("联动", "道德", "誓约"),
                "舍利蛊 + 生机系联动：被动将道德转化为攻击力；主动消耗寿元换取大量道德并暂时降低上限",
                "compat/guzhenren/item/synergy/SheShengQuYiSynergyBehavior.java",
                () -> { ensureClassLoaded(SheShengQuYiSynergyBehavior.INSTANCE); },
                CooldownHint.useOrgan("誓约就绪", null));
    }

    private static void register(String skillId,
                                 String abilityId,
                                 String organId,
                                 List<String> tags,
                                 String description,
                                 String sourceHint,
                                 Runnable initializer) {
        register(skillId, abilityId, organId, tags, description, sourceHint, initializer, null);
    }

    private static void register(String skillId,
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
        ActiveSkillEntry previous = ENTRIES.put(skill, new ActiveSkillEntry(skill, ability, organ, tags, description, sourceHint, cooldownHint));
        if (previous != null) {
            ChestCavity.LOGGER.warn("[skill][registry] duplicate registration for {} (previous organ={})", skill, previous.organId());
        }
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

    private static void ensureClassLoaded(Object instance) {
        // no-op: touching the instance ensures the class has run its static initializer.
        if (instance == null) {
            ChestCavity.LOGGER.debug("[skill][registry] attempted to touch null instance during bootstrap");
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
        ChestCavityInstance cc = ChestCavityEntity.of(player)
                .map(ChestCavityEntity::getChestCavityInstance)
                .orElse(null);
        if (cc == null) {
            return TriggerResult.NO_CHEST_CAVITY;
        }
        if (!hasOrganEquipped(cc, entry.organId())) {
            return TriggerResult.MISSING_ORGAN;
        }
        boolean activated = OrganActivationListeners.activate(entry.abilityId(), cc);
        return activated ? TriggerResult.SUCCESS : TriggerResult.ABILITY_NOT_REGISTERED;
    }

    public static void scheduleReadyToast(ServerPlayer player, ResourceLocation skillId, long readyAtTick, long nowTick) {
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

    public record CooldownHint(ResourceLocation iconOverride, String title, String subtitle) {
        public static CooldownHint useOrgan(String title, String subtitle) {
            return new CooldownHint(null, title, subtitle);
        }
    }
}
