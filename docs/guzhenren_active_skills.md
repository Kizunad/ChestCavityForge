# Guzhenren 主动技清单

> 来源：`src/main/java/net/tigereye/chestcavity/skill/ActiveSkillRegistry.java`（自动整理，含最新迁移进度）

| SkillId | OrganId | 标签 | 描述 | 源文件 |
| --- | --- | --- | --- | --- |
| `guzhenren:bai_yin_she_li_gu` | `guzhenren:bai_yin_she_li_gu` | 防御 | 激活十秒抗性 II，期间一次致命伤改判为 1 HP + 1 秒无敌 | `compat/guzhenren/item/ren_dao/behavior/BaiYinSheLiGuOrganBehavior.java:34` |
| `guzhenren:bai_yun_gu/cloud_step` | `guzhenren:bai_yun_gu` | 输出, 防御 | 引爆云堆造成范围伤害并赋予抗性，层数高时生成雾域 | `compat/guzhenren/item/yun_dao_cloud/behavior/BaiYunGuOrganBehavior.java:69` |
| `guzhenren:bainianshougu` | `guzhenren:bainianshougu` | 防御, 续命 | 百年寿蛊版主动技：将 65% 伤害延后偿还并大幅自愈，风险更高但回报最大 | `compat/guzhenren/item/tian_dao/behavior/ShouGuOrganBehavior.java` |
| `guzhenren:bing_ji_gu_iceburst` | `guzhenren:bing_ji_gu` | 输出, 控制 | 拥有玉骨时引爆寒冰冲击波，对范围敌人造成伤害与高额减速 | `compat/guzhenren/item/bing_xue_dao/behavior/BingJiGuOrganBehavior.java:75` |
| `guzhenren:chi_tie_she_li_gu` | `guzhenren:chi_tie_she_li_gu` | 辅助, 治疗 | 消耗真元与魂魄即刻回复 20%（上限 200）生命 | `compat/guzhenren/item/ren_dao/behavior/ChiTieSheLiGuOrganBehavior.java:30` |
| `guzhenren:gui_wu` | `guzhenren:guiqigu` | 控制, 输出 | 启动鬼雾脚本，黑雾范围施加失明与缓慢 | `compat/guzhenren/item/hun_dao/behavior/GuiQiGuOrganBehavior.java:57` |
| `guzhenren:hua_shi_gu` | `guzhenren:hua_shi_gu` | 辅助, 增益 | 被动每 5 秒消耗 200 真元恢复 3 精力；主动消耗 300 真元获得 10 秒力量 III | `compat/guzhenren/item/li_dao/behavior/HuaShiGuOrganBehavior.java:24` |
| `guzhenren:huang_jin_she_li_gu` | `guzhenren:huang_jin_she_li_gu` | 防御, 控制 | 6 秒抗性 III + 免击退，自身缓慢并对 8 格敌人施加缓慢 IV | `compat/guzhenren/item/ren_dao/behavior/HuangJinSheLiGuOrganBehavior.java:31` |
| `guzhenren:huang_luo_tian_niu_gu` | `guzhenren:huang_luo_tian_niu_gu` | 召唤, 辅助 | 召唤发疯天牛冲锋并给予 30 秒精力消耗减免 | `compat/guzhenren/item/li_dao/behavior/HuangLuoTianNiuGuOrganBehavior.java:38` |
| `guzhenren:huo_gu` | `guzhenren:huo_gu` | 输出, 控制 | 扣除真元与饥饿后激活灼烧光环，对敌灼烧并叠加缓慢 | `compat/guzhenren/item/yan_dao/behavior/HuoYiGuOrganBehavior.java:50` |
| `guzhenren:huo_you_gu` | `guzhenren:huo_you_gu` | 输出, 反应 | 消耗全部燃油喷射火焰波；命中油涂层且火衣激活时爆炸并短暂禁用火衣 | `compat/guzhenren/item/yan_dao/behavior/HuoYouGuOrganBehavior.java` |
| `guzhenren:jian_ying_fenshen` | `guzhenren:jian_ying_gu` | 输出, 召唤 | 支付真元与精力召唤剑影分身协同作战，随器官数量扩充 | `compat/guzhenren/item/jian_dao/behavior/JianYingGuOrganBehavior.java:61` |
| `guzhenren:jiu_chong` | `guzhenren:jiu_chong` | 输出 | 消耗酒精储备施放醉酒吐息，触发醉拳循环 | `compat/guzhenren/item/shi_dao/behavior/JiuChongOrganBehavior.java:46` |
| `guzhenren:le_gu_dun_gu` | `guzhenren:le_gu_dun_gu` | 防御 | 满"不屈"后消耗真元获得短暂无敌与高抗性 | `compat/guzhenren/item/gu_dao/behavior/LeGuDunGuOrganBehavior.java:49` |
| `guzhenren:liandaogu` | `guzhenren:liandaogu` | 输出 | 蓄力释放长条刀光，对面前敌人造成高额斩击与击退 | `compat/guzhenren/item/mu_dao/behavior/LiandaoGuOrganBehavior.java:49` |
| `guzhenren:sheng_ji_xie_burst` | `guzhenren:sheng_ji_xie` | 辅助, 治疗 | 催动生机叶释放生机脉冲，治疗自身与友方并赋予短时间抗性/再生 | `compat/guzhenren/item/mu_dao/behavior/ShengJiYeOrganBehavior.java` |
| `guzhenren:jiu_xie_sheng_ji_cao_cui_sheng` | `guzhenren:jiu_xie_sheng_ji_cao` | 辅助, 治疗 | 催动九叶生机治疗自身与友方，并按阶段赋予抗性/吸收护盾 | `compat/guzhenren/item/mu_dao/behavior/JiuYeShengJiCaoOrganBehavior.java` |
| `guzhenren:long_wan_qu_qu_gu` | `guzhenren:long_wan_qu_qu_gu` | 防御, 机动 | 启动后获得 3 次短距闪避机会并提供短暂无敌窗口 | `compat/guzhenren/item/li_dao/behavior/LongWanQuQuGuOrganBehavior.java:30` |
| `guzhenren:luo_xuan_gu_qiang_gu` | `guzhenren:luo_xuan_gu_qiang_gu` | 输出 | 消耗骨枪充能发射穿刺投射物，受骨道/力道增益影响 | `compat/guzhenren/item/gu_dao/behavior/LuoXuanGuQiangguOrganBehavior.java:54` |
| `guzhenren:qing_tong_she_li_gu` | `guzhenren:qing_tong_she_li_gu` | 防御 | 入定 3 秒获得抗性 II，并重置冷却计时 | `compat/guzhenren/item/ren_dao/behavior/QingTongSheLiGuOrganBehavior.java:26` |
| `guzhenren:shan_guang_gu_flash` | `guzhenren:shan_guang_gu` | 输出, 控制 | 闪现造成范围伤害并致盲减速，施放者获得短暂加速 | `compat/guzhenren/item/guang_dao/behavior/ShanGuangGuOrganBehavior.java:61` |
| `guzhenren:shi_nian_shou_gu` | `guzhenren:shi_nian_shou_gu` | 防御, 续命 | 换命・续命进阶版：持续 6 秒转化 60% 伤害为寿债并强化治疗，缓死冷却额外回填 | `compat/guzhenren/item/tian_dao/behavior/ShouGuOrganBehavior.java` |
| `guzhenren:shou_gu` | `guzhenren:shou_gu` | 防御, 续命 | 清空最多三层寿纹，6 秒内将 50% 伤害转为寿债并快速回血，结束时返还缓死冷却时间 | `compat/guzhenren/item/tian_dao/behavior/ShouGuOrganBehavior.java` |
| `guzhenren:shuang_xi_gu_frost_breath` | `guzhenren:shuang_xi_gu` | 输出, 控制 | 锥形霜息附加寒冷与霜蚀 DoT，强度受冰雪道增益影响 | `compat/guzhenren/item/bing_xue_dao/behavior/ShuangXiGuOrganBehavior.java:67` |
| `guzhenren:tu_qiang_gu` | `guzhenren:tu_qiang_gu` | 控制, 防御 | 针对目标生成土/玉监牢并为施术者充能防护屏障 | `compat/guzhenren/item/tu_dao/behavior/TuQiangGuOrganBehavior.java:67` |
| `guzhenren:xie_di_gu_detonate` | `guzhenren:xie_di_gu` | 输出, 辅助 | 引爆储存血滴造成真实伤害与流血，同时回复资源 | `compat/guzhenren/item/xue_dao/behavior/XiediguOrganBehavior.java:63` |
| `guzhenren:xie_fei_gu` | `guzhenren:xie_fei_gu` | 输出, 控制 | 付出生命与真元喷出血雾，造成持续伤害并施加失明/中毒 | `compat/guzhenren/item/xue_dao/behavior/XieFeiguOrganBehavior.java:63` |
| `guzhenren:xiong_hao_gu` | `guzhenren:xiong_hao_gu` | 输出, 爆发 | 消耗 300 真元进入 10 秒激怒，每次近战命中额外消耗 6 精力并造成 +10 伤害 | `compat/guzhenren/item/li_dao/behavior/XiongHaoGuOrganBehavior.java:28` |
| `guzhenren:xuezhangu` | `guzhenren:xuezhangu` | 输出, 增强 | 消耗生命与真元爆发血誓，斩击周围敌人并瞬时灌满战血，短时间内大幅提升吸血与攻击 | `compat/guzhenren/item/xue_dao/behavior/XueZhanGuOrganBehavior.java:264` |
| `guzhenren:yin_yun_gu` | `guzhenren:yin_yun_gu` | 输出, 控制 | 消耗全部阴纹拉扯周围敌人并延迟引下多道雷狱，对范围敌人造成雷击并施加虚弱 | `compat/guzhenren/item/yun_dao_cloud/behavior/YinYunGuOrganBehavior.java:166` |
| `guzhenren:yuan_lao_gu_5_attack` | `guzhenren:yuan_lao_gu_5` | 输出 | 按消耗元石对大范围敌人造成伤害，量随消耗线性提升 | `compat/guzhenren/item/yu_dao/behavior/YuanLaoGuFifthTierBehavior.java:40` |
| `guzhenren:zhi_zhuang_gu` | `guzhenren:zhi_zhuang_gu` | 输出, 位移 | 直线冲锋命中刷新冷却并累积惯性层数，魂道加持下触发灵魂回声与连锁爆发 | `compat/guzhenren/item/li_dao/behavior/ZhiZhuangGuOrganBehavior.java:120` |
| `guzhenren:zi_jin_she_li_gu` | `guzhenren:zi_jin_she_li_gu` | 辅助, 治疗 | 燃烧 50% 生命与真元展开 15 秒领域，每秒恢复友方 10% 资源 | `compat/guzhenren/item/ren_dao/behavior/ZaijinSheLiGuOrganBehavior.java:44` |
| `guzhenren:zi_li_geng_sheng_gu_3` | `guzhenren:zi_li_geng_sheng_gu_3` | 辅助, 治疗 | 消耗力道肌肉后持续再生 30 秒，结束附带虚弱 | `compat/guzhenren/item/li_dao/behavior/ZiLiGengShengGuOrganBehavior.java:45` |
