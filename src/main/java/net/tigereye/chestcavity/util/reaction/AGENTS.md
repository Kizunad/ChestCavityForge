Reactions — 运行时标签与血道规则速查

范围
- 本说明覆盖 `net.tigereye.chestcavity.util.reaction` 目录及其子目录（引擎/标签/规则）。
- 作用：为后续开发者快速定位反应系统（Reaction）入口、通用标签与新增的血道联动规则。

新增通用标签（ReactionTagKeys）
- `reaction/blood_mark`：血印（目标身上的血道标记）。
- `reaction/hemorrhage`：失血（强流血通用标记，独立于药水效果）。
- `reaction/blood_trail`：血迹（可追猎痕迹，由血眼蛊命中赋予）。
- `reaction/blood_residue`：血雾/血泊（预留：地面残留体）。
- `reaction/blood_rage`：血怒态（施法者身上，血战蛊自动激活）。
- `reaction/blood_oath`：血誓态（施法者身上，血战蛊主动技）。
- `reaction/blood_ritual`：血祭窗（铁血蛊吸血成功后的短时窗口）。
- `reaction/blood_flow`：血气流转（血气蛊维持态，短时续期）。
- `reaction/light_daze`：光晕眩目（闪光蛊被动/爆闪赋予的光道标记）。
- `reaction/soul_scar`：灵痕（鬼气蛊等魂道真伤命中后留下的灵魂创痕）。
- `reaction/sword_scar`：剑痕（剑影蛊及其分身命中时刻印的剑道伤痕）。
- `reaction/lightning_charge`：雷痕（电流蛊命中后短时间内携带的电荷）。
- `reaction/lightning_immune`：雷痕免疫窗（避免同一窗口反复触发雷系反应）。
- `reaction/wood_growth`：木灵赐福（木道护佑、催生系主动技赋予的生机标记）。
- `reaction/human_aegis`：人道守心（舍利蛊的护体金刚状态）。

血道联动规则（ReactionRegistry.registerBloodDefaults）
- 沸血（火衣 × 血印/失血）：在火衣 DoT 命中时，对有血印/失血的目标追加小额伤害，清除血印并短暂火系免疫；血怒/血誓存在时有小幅加成。
- 凝血碎裂（霜痕 × 血印）：在霜痕 DoT 命中时，对有血印的目标追加伤害 + 短减速，清除血印并短暂冰系免疫。
- 渗魂回声（魂焰 × 血印）：在魂焰 DoT 命中时，对有血印的目标追加一次延迟伤害，清除血印并短暂魂系免疫。
- 败血激增（腐蚀 × 血印）：在腐蚀 DoT 命中时，对有血印的目标追加伤害 + 虚弱，并在地面生成轻度腐蚀残留，清除血印并短暂腐蚀免疫。
- 所有规则默认通过系统消息提示（使用 i18n 文案键，缺失时显示键名）。

骨道联动规则（ReactionRegistry.registerBoneDefaults）
- 骨蚀裂（腐蚀 × 骨印）：追加轻微伤害与挖掘疲劳，清理骨印并赋予短暂骨免疫，防止在同一窗口重复触发。
- 骨煅灼（火衣 × 骨印）：额外灼烧伤害并点燃目标，生成小范围火焰冲击波；清除骨印并记录骨免疫。
- 骨棘霜爆（霜痕 × 骨棘场）：裂骨碎冰，施加减速与虚弱并刷新冰霜残留领域；清除骨棘场并施加冰系+骨系免疫。
- 骨魂共鸣（魂焰 × 骨印）：在短延迟后再度造成魂伤，附带灵光标记与小范围魂波伤害；清除骨印并施加魂免疫与骨免疫。

光/魂/剑道联动规则（ReactionRegistry.registerLightSoulSwordDefaults）
- 光魂共振（魂焰 × 光晕眩目）：魂焰额外灼烧并施加致盲，短暂赋予魂系免疫。
- 剑魂裂击（魂焰 × 剑痕）：强化魂焰直伤并施加虚弱/迟缓，随后进入魂系免疫窗口。
- 灵魂炽燃（火衣 × 灵痕）：火衣点燃残留灵痕，追加灼烧伤害并引燃目标，同时应用火系免疫。

雷/木/人道联动规则（ReactionRegistry.registerLeiMuRenDefaults）
- 雷霜锁链（霜痕 × 雷痕）：额外魔法伤害并施加强控，清除雷痕并进入短暂雷免疫。
- 雷炎爆轰（火衣 × 雷痕）：在目标周围触发小范围爆轰，清除雷痕并进入雷免疫。
- 木灵护熄（火衣 × 木灵）：取消火衣伤害并转化为治疗，赋予短时火免疫。
- 木灵护体（腐蚀 × 木灵）：将腐蚀伤害转化为再生与吸收护盾，赋予短时腐蚀免疫。
- 金刚护心（魂焰 × 人道守心）：抵消魂焰并反噬施法者少量伤害，赋予魂系免疫。
- 金刚护体（腐蚀 × 人道守心）：抵消腐蚀并赋予短时抗性/再生，进入腐蚀免疫窗口。

器官行为注入点（已落地）
- 血滴蛊（XiediguOrganBehavior.activateAbility）：引爆命中后，为每个目标添加 `blood_mark(100t)` 与 `hemorrhage(>=60t)`。
- 血眼蛊（XieyanguOrganBehavior.onHit）：命中时添加 `blood_trail(200t)`（玩家与非玩家路径均已添加）。
- 铁血蛊（TiexueguOrganBehavior.triggerEffect）：吸血成功后，给自身添加 `blood_ritual(60t)`。
- 血战蛊：
  - activateBloodRage：激活血怒时给自身添加 `blood_rage(持续时长)`。
  - activateAbility（血誓）：激活血誓时给自身添加 `blood_oath(持续时长)`。
- 血气蛊（XueqiguOrganBehavior.onSlowTick）：每慢 Tick 续期 `blood_flow(60t)`。

开发建议
- 若要新增“血雾/血泊”地面残留，优先扩展 ReactionEngine（参考 frost/corrosion 的 residue 实现）；当前规则暂以 AoE/AreaEffectCloud 轻量替代。
- 需要更细的数值/特效可在 CCConfig.ReactionConfig 扩展可调参数，再将规则伤害/半径等接入配置。
- 若在其它模块需要判断血道标签，请统一调用 `ReactionTagOps.has/add/clear`。

验证
- 血滴引爆→紧随其后用火衣/霜息/魂焰/腐蚀攻击，应该看到对应的反应提示与效果；
- 血眼命中后再次攻击，目标应带 `blood_trail` 标记；
- 铁血触发吸血后短时间内触发的血道反应应略有增益（`blood_ritual` 窗口）。
