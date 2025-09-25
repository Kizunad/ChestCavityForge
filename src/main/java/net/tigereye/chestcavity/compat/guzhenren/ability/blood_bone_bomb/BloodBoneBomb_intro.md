
把 **铁血蛊 + 血滴蛊 + 血眼蛊 + 骨竹蛊 + 螺旋骨枪蛊** 结合成一个完整的 **器官杀招** 设计

---

# ⚔️ 杀招设定：血骨爆弹 (Bloodbone Bomb)

📦 **触发方式**

* 快捷键：`[key.chestcavity.dragon_bombs]`
* 玩家进入 **持续蓄力状态**（10s，不能移动）。

---

## ⚡ 蓄力机制

* 每秒消耗：

  * 2 点生命值
  * 20 点真元
  * 10 点体力

* 粒子：血腥粒子不断向内收紧，类似血色漩涡。

* 音效：逐渐增强的低沉心跳声 + 骨骼摩擦爆裂声。

❌ **失败判定**

* 若在蓄力过程中，生命值 / 真元 / 体力 **任何一项提前耗尽**：
  → 玩家 **立即爆炸**：

  * 产生范围性爆炸（破坏方块）
  * 扣除 **50 点生命值**（无视护甲）
  * 音效：爆裂骨肉 + 撕裂尖啸

---

## 💥 蓄力完成效果

* 蓄力成功后：

  * 释放爆炸音效（血肉 + 骨骼崩裂声）
  * 生成一个 **高速 Projectile（血骨螺旋弹）**

### Projectile 特效

* 外观：由骨骼 + 血液凝聚的螺旋体
* 粒子：旋转的血色骨屑 & 血滴飞散
* 速度：极快，带有尾迹血雾
* 命中时：产生血色小型爆炸

---

## ☠️ 命中效果

* 造成 **80 点基础生命值伤害**

* 附加药水效果：

  * `effect.guzhenren.liuxue` → 流血 (持续掉血)
  * 缓慢 (Slowness II, 5s)
  * 虚弱 (Weakness I, 5s)
  并且 × (1 + li_dao_increase_effect) × (1 + xue_dao_increase_effect) × (1 + gu_dao_increase_effect) 取整 成最终等级
* 伤害与效果倍率：

```java
伤害最终值 = 80 × (1 + li_dao_increase_effect) × (1 + xue_dao_increase_effect) × (1 + gu_dao_increase_effect)
```

---

# 🔑 提示词 (专业写法)

```
器官杀招：血骨爆弹 (Bloodbone Bomb)

触发：
- 快捷键 [key.chestcavity.dragon_bombs]
- 玩家进入持续 10s 蓄力，无法移动。

蓄力过程：
- 每秒扣除：2 生命值 + 20 真元 + 10 体力
- 粒子：血腥粒子向内收紧（血色漩涡效果）
- 音效：心跳声 + 骨骼摩擦声
- 若生命值/真元/体力任一提前耗尽 → 玩家立即爆炸
  - 产生范围性爆炸（破坏方块）
  - 扣除 50 点生命值（无视护甲）
  - 音效：血肉爆裂+尖啸

蓄力完成：
- 产生爆炸音效，释放高速 Projectile
- Projectile：
  - 外观：血骨螺旋体
  - 粒子：旋转血色骨屑 + 血滴尾迹
  - 音效：骨肉飞散爆破音
  - 碰撞时小型爆炸

命中效果：
- 基础伤害 80
- 附加效果：
  - 流血 (effect.guzhenren.liuxue)
  - 缓慢 (Slowness II, 5s)
  - 虚弱 (Weakness I, 5s)
- 伤害倍率：
  - 80 × (1 + li_dao_increase_effect) × (1 + xue_dao_increase_effect) × (1 + gu_dao_increase_effect)
```
