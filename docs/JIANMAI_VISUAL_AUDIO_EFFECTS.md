# 剑脉蛊（Jianmai Gu）视觉与音频特效计划

> **文档版本**: 1.0
> **创建日期**: 2025-11-09
> **状态**: 需求阶段
> **优先级**: 中

## 目录

1. [概述](#概述)
2. [需求分析](#需求分析)
3. [被动效果（飞剑脉络）](#被动效果飞剑脉络)
4. [主动效果（雷电信标）](#主动效果雷电信标)
5. [技术实现方案](#技术实现方案)
6. [资源清单](#资源清单)
7. [开发步骤](#开发步骤)
8. [验收标准](#验收标准)

---

## 概述

### 目标

增强剑脉蛊（Jianmai Gu）的玩家感知体验，通过视觉粒子效果和音频反馈，让玩家能够清晰地感受到：

1. **被动效果**：周围飞剑形成的脉络能量
2. **主动效果**：激活技能时的强大雷电连接

### 设计理念

- **被动（低调）**: 微弱、持续、环境融合
- **主动（突出）**: 强烈、瞬间、视听冲击

---

## 需求分析

### 用户故事

#### 故事 1: 被动脉络感知

```
作为一个剑脉蛊使用者
我想在周围有飞剑时看到微弱的能量脉络
这样我能够直观感受到飞剑的存在和距离
```

**接受标准**:
- ✅ 每把飞剑到玩家之间显示能量线
- ✅ 线条微弱，不遮挡视线
- ✅ 距离越远，线条越淡
- ✅ 线条上均匀分布粒子效果

#### 故事 2: 被动音频反馈

```
作为一个剑脉蛊使用者
我想听到微弱的心跳声音
这样我能够在不看屏幕时也感知到被动效果的激活
```

**接受标准**:
- ✅ 每 10 秒播放一次心跳声
- ✅ 音量很小（不打扰其他音效）
- ✅ 只在有飞剑时播放
- ✅ 玩家或 MOD 可关闭

#### 故事 3: 主动技能激活视觉

```
作为一个剑脉蛊使用者
我想在激活主动技能时看到强烈的雷电效果
这样能够清晰地表达"我正在进行强力攻击"
```

**接受标准**:
- ✅ 雷电从玩家延伸到所有飞剑
- ✅ 效果持续约 2-3 秒
- ✅ 雷电亮度随飞剑数量增加而增加
- ✅ 与被动脉络视觉层次分明

#### 故事 4: 主动技能激活音频

```
作为一个剑脉蛊使用者
我想听到"激活信标"或"雷电激活"的清晰声音
这样我能够确认技能已成功激活
```

**接受标准**:
- ✅ 激活时立即播放声音
- ✅ 声音清晰，音量适中
- ✅ 根据消耗量调整音效强度

---

## 被动效果（飞剑脉络）

### 视觉设计

#### 能量线（Energy Lines）

```
玩家
  │
  ├─────→ 飞剑 1 (距离 5m)
  ├──────→ 飞剑 2 (距离 10m)
  └───────→ 飞剑 3 (距离 15m)

线条特性:
- 颜色: 淡蓝色 (~0.5 透明度)
- 宽度: 0.3 像素（极细）
- 闪烁: 轻微闪烁（0.5 秒周期）
- 衰减: 距离越远越淡
```

#### 粒子效果

```
位置: 每隔 1m 沿能量线放置一个粒子
间隔: 1 米
粒子类型: 微粒子（Particle Type）
  - 粒子类型: CLOUD（或 ENCHANT）
  - 颜色: 淡蓝色 RGB(100, 150, 255)
  - 大小: 0.3 - 0.5
  - 速度: 0 (静止，随玩家视角更新)
  - 生命周期: 1 秒 (持续更新)
  - 数量: min(飞剑数量 * (距离 / 1m), 50) 个
```

**效果示例**:
```
距离 12m 的飞剑，每秒放 12 个粒子
距离 6m 的飞剑，每秒放 6 个粒子
总共最多 50 个粒子（防止过载）
```

### 音频设计

#### 心跳声（Heartbeat）

| 属性 | 值 | 说明 |
|------|-----|------|
| 触发间隔 | 10 秒 | 足够低调，但能感知 |
| 音量 | 0.3 | 很小声，不打扰 |
| 音效源 | 玩家位置 | 跟随玩家 |
| 衰减距离 | 20m | 只有附近玩家听到 |
| 触发条件 | 有飞剑 && JME > 0 | 只在系统活跃时 |
| 音效文件 | `guzhenren:jianmai.heartbeat` | 新增资源包 |

**音效描述**:
```
"扑通" "扑通" 的低沉心跳声
频率: ~60 BPM（1 秒一次跳动）
持续时间: 0.5 秒
尾音: 逐渐淡出
```

---

## 主动效果（雷电信标）

### 视觉设计

#### 雷电链（Lightning Chain）

```
玩家 ━━⚡━━⚡━━ 飞剑 1
    ━━⚡━━⚡━━ 飞剑 2
    ━━⚡━━⚡━━ 飞剑 3

链特性:
- 颜色: 鲜亮蓝色/紫色 (~1.0 透明度)
- 宽度: 1-2 像素（粗）
- 闪烁: 快速闪烁（0.1 秒周期）
- 范围: 所有在线范围内的飞剑
- 持续时间: 2-3 秒（逐渐淡出）
```

#### 电击粒子

```
位置:
  1. 玩家身上（激活点）
  2. 每条链上均匀分布（每 0.5m 一个）
  3. 目标飞剑上（激击点）

粒子:
  - 类型: ELECTRIC 或 REDSTONE
  - 颜色: 纯蓝色 RGB(0, 100, 255)
  - 大小: 0.5 - 1.0
  - 生命周期: 0.3 秒
  - 发射频率: 100 ms 一次
  - 运动: 沿链移动（视觉效果）

激活点粒子:
  - 类型: FIREWORK 爆炸（可选）
  - 大小: 中等
  - 持续时间: 0.5 秒
```

#### 屏幕效果（可选）

```
激活时:
- 屏幕轻微白闪（0.2 秒）
- 或者屏幕边缘蓝光脉冲

目的: 强化"强力激活"的感觉
```

### 音频设计

#### 激活音效（Activation Sound）

| 属性 | 值 | 说明 |
|------|-----|------|
| 触发时机 | 技能激活时 | 立即播放 |
| 音量 | 1.0 | 清晰可听 |
| 音效类型 | 信标激活 / 电流 | 独特识别 |
| 持续时间 | 0.8 秒 | 足够短促 |
| 衰减 | 40m | 附近玩家都能听到 |
| 音效文件 | `guzhenren:jianmai.activate` | 新增资源包 |

**音效描述**:
```
"嗡~~~" 的电流激活声
包含:
  1. 初始：快速上升的电流声（0.2 秒）
  2. 中期：持续的嗡鸣声（0.4 秒）
  3. 结束：快速下降的电流衰减（0.2 秒）
```

#### 连接音效（Optional）

```
如果激活多条链：
  - 对于每把飞剑：播放微弱的"击中"音效
  - 音量: 0.5
  - 频率: 50 ms 延迟
  - 作用: 强化"多链连接"的感觉
```

---

## 技术实现方案

### 架构设计

```
玩家 Tick 事件
    │
    ├─→ [被动效果处理]
    │    ├─→ 扫描飞剑
    │    ├─→ 生成能量线粒子（每 Tick）
    │    └─→ 触发心跳音效（每 10 秒）
    │
    └─→ [主动效果处理]
         ├─→ 检测技能激活（从 applyActiveBuff）
         ├─→ 生成雷电链粒子（0.3 秒/次）
         ├─→ 播放激活音效
         └─→ 自动淡出（3 秒后停止）
```

### 核心类设计

#### 1. JianmaiVisualEffects（新增）

```java
public final class JianmaiVisualEffects {

    // 被动效果
    public static void renderPassiveLines(ServerPlayer player, long now) {
        // 扫描飞剑
        // 为每把飞剑绘制能量线
        // 沿线放置粒子
    }

    // 主动效果
    public static void renderActiveLightning(ServerPlayer player,
                                             double deltaAmount,
                                             long now) {
        // 获取所有目标飞剑
        // 绘制雷电链
        // 放置电击粒子
    }

    // 辅助方法
    private static void spawnLineParticles(ServerLevel level, Vec3 from, Vec3 to) { }
    private static void spawnElectricParticles(ServerLevel level, Vec3 pos) { }
}
```

#### 2. JianmaiAudioEffects（新增）

```java
public final class JianmaiAudioEffects {

    // 被动音效
    public static void playHeartbeat(ServerPlayer player) {
        // 播放心跳声
        // 音量 0.3
    }

    // 主动音效
    public static void playActivation(ServerPlayer player, double strength) {
        // 播放激活音效
        // 音量根据 strength 调整
    }

    // 连接音效（可选）
    public static void playHitEffect(ServerPlayer player, Vec3 pos) {
        // 播放击中音效
    }
}
```

#### 3. 集成到现有代码

**在 JianmaiPlayerTickEvents.onPlayerTick() 中**:
```java
// 被动粒子效果（每 Tick）
JianmaiVisualEffects.renderPassiveLines(player, now);

// 被动音效（10 秒一次）
if (lastHeartbeat + 200 < now) {  // 200 ticks = 10 秒
    JianmaiAudioEffects.playHeartbeat(player);
    lastHeartbeat = now;
}
```

**在 JianmaiAmpOps.applyActiveBuff() 中**:
```java
// 主动视觉效果（立即播放，逐渐淡出）
JianmaiVisualEffects.renderActiveLightning(player, deltaAmount, now);

// 主动音效（立即播放）
JianmaiAudioEffects.playActivation(player, deltaAmount);
```

### 性能考虑

| 操作 | Tick 频率 | 性能影响 | 优化措施 |
|------|----------|---------|---------|
| 被动粒子 | 每 Tick | 中等 | 最多 50 个粒子；距离衰减 |
| 被动音效 | 10 秒 1 次 | 极低 | 无需优化 |
| 主动粒子 | 0.3 秒 1 次 | 高（短期） | 持续 3 秒后停止 |
| 主动音效 | 激活 1 次 | 极低 | 无需优化 |

**优化策略**:
1. 使用粒子批处理
2. 根据距离启用/禁用效果
3. 可配置的特效强度
4. 离屏优化（超视距时禁用）

---

## 资源清单

### 新增资源文件

#### 音效资源

```
src/main/resources/assets/guzhenren/sounds/
├── jianmai/
│   ├── heartbeat.ogg              # 心跳声
│   ├── activate.ogg               # 激活音效
│   └── hit.ogg                    # 连接音效（可选）

src/main/resources/assets/guzhenren/sounds.json
{
  "guzhenren.jianmai.heartbeat": {
    "sounds": ["guzhenren:jianmai/heartbeat"],
    "subtitle": "subtitle.guzhenren.jianmai.heartbeat"
  },
  "guzhenren.jianmai.activate": {
    "sounds": ["guzhenren:jianmai/activate"],
    "subtitle": "subtitle.guzhenren.jianmai.activate"
  }
}
```

#### 粒子效果（Minecraft 内置）

使用现有粒子类型，无需新增资源：
- `minecraft:cloud` - 被动脉络粒子
- `minecraft:electric_spark` - 主动雷电粒子
- 或使用 Guzhenren 的自定义粒子系统

### 配置参数（JianmaiTuning.java）

```java
// ========== 被动效果参数 ==========

/** 脉络扫描间隔（Tick）*/
public static final int VEIN_RENDER_INTERVAL = 1;  // 每 Tick 更新

/** 脉络最大粒子数 */
public static final int VEIN_MAX_PARTICLES = 50;

/** 脉络粒子间隔（方块） */
public static final double VEIN_PARTICLE_SPACING = 1.0;

/** 脉络透明度（0-1） */
public static final float VEIN_ALPHA = 0.5f;

/** 脉络颜色（RGB） */
public static final int[] VEIN_COLOR = {100, 150, 255};

/** 心跳声触发间隔（Tick） */
public static final int HEARTBEAT_INTERVAL = 200;  // 10 秒

/** 心跳声音量 */
public static final float HEARTBEAT_VOLUME = 0.3f;

// ========== 主动效果参数 ==========

/** 雷电链持续时间（Tick） */
public static final int LIGHTNING_DURATION = 60;  // 3 秒

/** 雷电链粒子生成间隔（Tick） */
public static final int LIGHTNING_PARTICLE_INTERVAL = 6;  // 0.3 秒

/** 雷电链颜色（RGB） */
public static final int[] LIGHTNING_COLOR = {0, 100, 255};

/** 激活音效音量 */
public static final float ACTIVATION_VOLUME = 1.0f;

/** 屏幕白闪强度（0-1） */
public static final float SCREEN_FLASH_INTENSITY = 0.3f;
```

---

## 开发步骤

### 阶段 1: 被动脉络效果（1-2 天）

**任务**:
1. [ ] 创建 `JianmaiVisualEffects.java`
   - 实现 `renderPassiveLines()` 方法
   - 能量线绘制算法
   - 粒子放置逻辑

2. [ ] 集成到 `JianmaiPlayerTickEvents.java`
   - 在 `onPlayerTick()` 中调用被动效果

3. [ ] 参数调优
   - 测试粒子数量、间隔、颜色
   - 验证性能影响

**验收标准**:
- ✅ 飞剑脉络可见
- ✅ 粒子均匀分布
- ✅ 性能无明显下降（< 5% FPS）

### 阶段 2: 被动心跳音效（0.5 天）

**任务**:
1. [ ] 创建 `JianmaiAudioEffects.java`
   - 实现 `playHeartbeat()` 方法
   - 音量和距离衰减处理

2. [ ] 音效资源准备
   - 获取或录制心跳声音
   - 注册到 `sounds.json`

3. [ ] 集成到 `JianmaiPlayerTickEvents.java`
   - 每 10 秒触发一次

**验收标准**:
- ✅ 心跳声能听到
- ✅ 音量适中（不打扰）
- ✅ 触发频率正确

### 阶段 3: 主动雷电效果（1-2 天）

**任务**:
1. [ ] 实现 `JianmaiVisualEffects.renderActiveLightning()`
   - 雷电链绘制
   - 粒子生成和衰减
   - 屏幕闪光效果（可选）

2. [ ] 集成到 `JianmaiAmpOps.applyActiveBuff()`
   - 修改 `applyActiveBuff()` 调用视觉效果
   - 记录激活时间用于效果衰减

3. [ ] 参数调优
   - 链数量、粒子密度、持续时间

**验收标准**:
- ✅ 激活时看到雷电链
- ✅ 链连接所有飞剑
- ✅ 效果 3 秒后自动消失
- ✅ 性能可接受（激活时 < 10% FPS 下降）

### 阶段 4: 主动激活音效（0.5 天）

**任务**:
1. [ ] 实现 `JianmaiAudioEffects.playActivation()`
   - 根据消耗量调整音效

2. [ ] 音效资源准备
   - 获取或录制激活音效
   - 注册到 `sounds.json`

3. [ ] 集成到 `JianmaiAmpOps.applyActiveBuff()`
   - 激活时立即播放

**验收标准**:
- ✅ 激活时听到声音
- ✅ 音效清晰有力
- ✅ 音量适中

### 阶段 5: 集成测试与优化（1 天）

**任务**:
1. [ ] 完整的被动 + 主动测试
2. [ ] 性能分析和优化
3. [ ] 参数微调
4. [ ] 文档更新

**验收标准**:
- ✅ 所有效果正常工作
- ✅ 无 Bug 或异常
- ✅ 性能稳定（60 FPS）
- ✅ 视听体验协调

---

## 验收标准

### 功能验收

#### 被动效果

```
□ 飞剑脉络可见
  ├─ 玩家附近有飞剑时显示能量线
  ├─ 线条微弱，不遮挡视线
  ├─ 距离越远线条越淡
  └─ 实时更新

□ 粒子效果
  ├─ 沿能量线均匀分布
  ├─ 粒子间隔约 1 米
  ├─ 总数不超过 50 个
  └─ 无明显卡顿

□ 心跳音效
  ├─ 每 10 秒播放一次
  ├─ 音量很小（不打扰）
  ├─ 只在有飞剑时播放
  └─ 可在设置中关闭
```

#### 主动效果

```
□ 雷电链可见
  ├─ 激活时立即显示
  ├─ 连接所有在线飞剑
  ├─ 线条粗亮，视觉冲击强
  └─ 3 秒后自动消失

□ 电击粒子
  ├─ 密集均匀
  ├─ 沿链移动
  └─ 粒子效果协调

□ 激活音效
  ├─ 立即播放（无延迟）
  ├─ 清晰有力
  ├─ 音量适中
  └─ 持续时间 < 1 秒
```

### 性能验收

```
□ FPS 影响
  ├─ 被动效果：< 5% 下降
  ├─ 主动效果（激活时）：< 10% 下降
  └─ 总体保持 60+ FPS

□ 内存占用
  ├─ 无内存泄漏
  └─ 效果结束后正常释放

□ 网络性能（多人）
  ├─ 效果同步延迟 < 100ms
  └─ 无网络拥塞
```

### 用户体验验收

```
□ 感知清晰
  ├─ 被动：感受到飞剑存在和距离
  └─ 主动：感受到技能激活的力量

□ 沉浸感
  ├─ 视听效果协调
  ├─ 无违和感
  └─ 增强游戏体验

□ 可定制性
  ├─ 可在设置中调整效果强度
  ├─ 可选择关闭特定效果
  └─ 配置参数易理解
```

---

## 后续改进方向

### 短期（1-2 周）

- [ ] 支持配置特效强度（高/中/低）
- [ ] 性能模式（禁用部分特效）
- [ ] 音效本地化（多语言字幕）

### 中期（1-2 月）

- [ ] 自定义粒子效果
- [ ] 色彩主题支持（不同皮肤对应不同颜色）
- [ ] 与其他器官效果的和谐设计

### 长期（2-3 月）

- [ ] 高级视觉效果（着色器集成）
- [ ] 3D 音效（空间音频）
- [ ] 效果录制和分享系统

---

## 相关文档

- [剑脉蛊主动技能开发指南](./JIANMAI_ACTIVE_SKILL_GUIDE.md)
- [飞剑系统架构](./FLYINGSWORD_TECH_FRAMEWORK.md)
- [音效资源管理](./resources/guzhenren/sounds/)

---

**下一步**: 待用户反馈后，将进行需求细化和技术设计文档编写。
