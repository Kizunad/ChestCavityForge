为了可玩性，设定 " 三转开始才能稳定替代 原生器官 "

"item.guzhenren.gu_zhu_gu": "骨竹蛊", 骨竹 - 生长 - 强化 - 持续性 - 加快/(或者x转前唯一充能方法?)其他骨道蛊虫充能 - 使用 真元 (被动增长) / 食用骨粉(催化) (主动)

联动触发器设计草案
- 主动/被动双通道：SlowTick 等被动触发负责“被动增长”，Activation/ItemUse 负责“主动催化”。
- LinkageChannel 作为集中状态容器，骨竹蛊在 SlowTick 中读取 channel 值并按指数或线性曲线调整自身/他者充能。
- 性能约束：若 channel 值、参与蛊数量较大，应缓存上一tick结果并设置最小触发间隔（如 4~10 tick）以避免每tick全量遍历；对无骨道器官的胸腔直接短路返回。
- 建议提供 decay/cooldown 机制，持续降低 channel，提高扩展蛊虫时的稳定性。





















> 结论先说：松骨蛊触发后不是直接“秒杀”，而是给范围内生物施加自定义药水效果“松骨”(songgu)，再由该效果每刻结算“固定伤
  害+属性削减”，从而快速致死目标。

  - 触发入口
      - 文件：net/guzhenren/item/SongGuGuItem.java:use
      - 调用：net/guzhenren/procedures/SongGuGuYouJiKongQiShiShiTiDeWeiZhiProcedure.execute
      - 关键行为：
          - 播放音效/粒子，扣耐久/消耗副手素材（若持有守痕护蛊）
          - 对施法者周围半径25格的活物，施加自定义药水“松骨”效果
              - 代码：net/guzhenren/procedures/SongGuGuYouJiKongQiShiShiTiDeWeiZhiProcedure.java:146
              - 效果ID：GuzhenrenModMobEffects.SONGGU（注册名 songgu）
  - 杀伤机制（核心）
      - 药水类：net/guzhenren/potion/SongguMobEffect
          - 每tick都会触发效果逻辑（shouldApplyEffectTickThisTick 返回 true）
          - 调 SongguXiaoGuoChiXuShiMeiKeFaShengProcedure.execute
      - 周期伤害与削弱：net/guzhenren/procedures/SongguXiaoGuoChiXuShiMeiKeFaShengProcedure.java
          - 每tick对目标造成 100 点通用伤害：entity.hurt(DamageTypes.GENERIC, 100f)
          - 施加移动减速：MobEffects.MOVEMENT_SLOWDOWN，持续60tick，放大10
          - 每20tick（1秒）额外结算一次“骨骼松化”效果：
              - 玩家：通过记分板 smz 每秒减少 100 * 放大等级 分
              - 非玩家生物：直接降低其“最大生命”属性 MAX_HEALTH 每秒100点
          - 这意味着——松骨不是一次性“秒杀”，而是在短时间内通过高额DOT与上限削减迅速致死
  - 小结
      - 实现方式：范围施加“自定义药水效果”→ 药水每tick造成高额伤害并附带减速→ 每秒进一步降低生命上限/记分板，最终导
  致目标被“磨死/压死”
      - 非原版“中毒”路线，核心是自定义 MobEffect songgu + 周期 Procedure 的数值伤害与属性操作

  如果你要在游戏里验证：

  - 用松骨蛊对怪物释放后，data get entity <目标> ActiveEffects 可看到 guzhenren:songgu
  - 观察其生命会持续快速掉并出现减速，同时非玩家目标的最大生命值会被持续降低
