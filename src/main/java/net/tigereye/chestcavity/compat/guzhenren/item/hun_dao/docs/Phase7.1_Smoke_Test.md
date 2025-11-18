# Hun Dao Phase 7.1 烟雾测试脚本 — Modern UI Panel

## 测试环境准备

### 前置条件
1. Minecraft 客户端已加载 ChestCavity mod
2. 玩家已进入游戏世界（单人或多人）
3. 玩家具有命令执行权限（创造模式或管理员）

### 测试数据准备
**可选**: 使用以下命令设置测试数据（如有相关命令）：
```
# 激活魂魄系统（示例命令，需根据实际实现调整）
/hun_dao activate

# 设置测试属性（示例）
/hun_dao set_level 5
/hun_dao set_rarity RARE
/hun_dao add_attribute strength 10
```

---

## 测试场景 1: 基础面板打开与关闭

### 测试步骤
1. 在聊天框输入命令: `/hundaopanel`
2. 按下 `Enter` 键

### 预期结果
- ✅ Modern UI 面板在屏幕中央打开
- ✅ 显示标题 "Hun Dao Modern Panel (Phase 7)"
- ✅ 顶部显示 3 个 Tab 按钮:
  - "Soul Overview" (启用，默认选中)
  - "Reserved" (禁用，灰色)
  - "Reserved" (禁用，灰色)
- ✅ 内容区域显示 Soul Overview 内容（非空白）
- ✅ 底部显示 "Close Panel" 按钮

### 关闭测试
**步骤**: 点击 "Close Panel" 按钮

**预期结果**:
- ✅ 面板关闭，返回游戏主界面
- ✅ 无错误日志输出

---

## 测试场景 2: Soul Overview Tab - 系统激活状态

### 前置条件
魂魄系统已激活（通过命令或游戏进程）

### 测试步骤
1. 打开面板: `/hundaopanel`
2. 确认默认显示 "Soul Overview" Tab

### 预期内容显示
```
Soul Overview

Soul State: [ACTIVE/REST/其他状态]
Soul Level: [数字或--]
Soul Rarity: [COMMON/RARE/EPIC/LEGENDARY/Unidentified]
Soul Max: [数字或--]

Attributes:
  - [属性名1]: [值1]
  - [属性名2]: [值2]
  - [属性名3]: [值3]
  (或显示 "Attribute 1: --" 等占位符)
```

### 检查点
- ✅ 标题使用白色字体
- ✅ 字段标签使用灰色字体
- ✅ 数据值正确显示（非 "[Tab content renders here]"）
- ✅ 属性列表至少显示 3 行（含占位符）
- ✅ 无 "Soul System is Inactive" 警告

---

## 测试场景 3: Soul Overview Tab - 系统未激活状态

### 前置条件
魂魄系统未激活（新建角色或使用命令重置）

### 测试步骤
1. 打开面板: `/hundaopanel`
2. 观察内容区域

### 预期内容显示
```
Soul Overview

━━━━━━━━━━━━━━━━━━━━━━━━━━
⚠ Soul System is Inactive
━━━━━━━━━━━━━━━━━━━━━━━━━━

Soul State: Unknown
Soul Level: --
Soul Rarity: Unidentified
Soul Max: --

Attributes:
  - Attribute 1: --
  - Attribute 2: --
  - Attribute 3: --
```

### 检查点
- ✅ 顶部显示黄色警告区块
- ✅ 警告文本包含 "⚠" 符号
- ✅ 所有数据字段显示占位符（Unknown、--、Unidentified）
- ✅ 属性列表显示 3 个占位符
- ✅ 无崩溃或空指针异常

---

## 测试场景 4: Tab 切换功能

### 测试步骤
1. 打开面板: `/hundaopanel`
2. 观察 "Soul Overview" 内容
3. 点击 "Reserved" Tab 按钮（第一个）

### 预期结果
**Reserved Tab 按钮状态**:
- ⚠ **如果按钮禁用（灰色）**: 无法点击，内容保持不变
- ✅ **如果按钮启用**: 点击后内容区域立即切换

**Reserved Tab 内容**:
```
Coming Soon

Reserved for Future Use

This tab will be implemented
in a future phase.
```

### 检查点
- ✅ Tab 切换响应正常（无延迟）
- ✅ 内容区域完全刷新（不是追加）
- ✅ Reserved 内容使用白色/灰色字体
- ✅ 可以通过点击 "Soul Overview" Tab 返回

---

## 测试场景 5: 多次 Tab 切换稳定性

### 测试步骤
1. 打开面板: `/hundaopanel`
2. 快速连续点击以下 Tab（如果启用）:
   - Soul Overview → Reserved → Soul Overview → Reserved
3. 重复 5-10 次

### 预期结果
- ✅ 每次切换内容立即更新
- ✅ 无内容重叠或残留
- ✅ 无客户端卡顿或日志错误
- ✅ 内存占用稳定（使用 F3 调试界面监控）

---

## 测试场景 6: 重复打开/关闭稳定性

### 测试步骤
1. 执行 `/hundaopanel` → 关闭 → 重复 10 次
2. 每次打开后观察内容

### 预期结果
- ✅ 每次打开都显示最新数据
- ✅ 无内存泄漏（使用 JVM 内存监控）
- ✅ Fragment 正确创建/销毁
- ✅ Tab 状态重置为 "Soul Overview"

---

## 测试场景 7: 边界条件测试

### 测试 7.1: 属性列表为空
**模拟方法**: `HunDaoClientState.getSoulAttributes()` 返回空 Map
**预期结果**:
```
Attributes:
  - Attribute 1: --
  - Attribute 2: --
  - Attribute 3: --
```

### 测试 7.2: 属性列表仅 1 项
**模拟方法**: 添加单个属性 `strength: 10`
**预期结果**:
```
Attributes:
  - strength: 10
  - Attribute 2: --
  - Attribute 3: --
```

### 测试 7.3: 属性列表超过 3 项
**模拟方法**: 添加 5 个属性
**预期结果**: 显示所有 5 个属性（无截断或错误）

---

## 代码验证清单

### 1. 编译测试
**命令**:
```bash
./gradlew compileJava
```

**预期结果**: ✅ 编译成功，无错误

### 2. TODO 检查
**命令**:
```bash
rg -n "TODO Phase 7" src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/hun_dao/client/modernui
rg -n "TODO" src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/hun_dao/client/modernui
```

**预期结果**: ✅ 0 命中

### 3. 日志验证（开发者模式）
启用调试日志后打开面板并切换 Tab，检查日志包含：
```
[DEBUG] CanvasContentView: setActiveTab called
[DEBUG] CanvasContentView: onDraw executed
[DEBUG] SoulOverviewTab: renderContent executed
```

---

## 回归测试快速清单

| 检查项 | 命令/操作 | 通过标准 |
|--------|----------|---------|
| 面板打开 | `/hundaopanel` | 显示 Modern UI 面板 |
| 默认 Tab | 打开后观察 | "Soul Overview" 已选中 |
| 内容渲染 | 检查内容区域 | 显示真实数据（非占位文本） |
| Tab 切换 | 点击其他 Tab | 内容立即刷新 |
| 未激活警告 | 新角色测试 | 显示黄色警告区块 |
| Reserved Tab | 点击（如启用） | 显示 "Coming Soon" |
| 关闭面板 | 点击 Close | 面板关闭 |
| 日志检查 | 查看 `latest.log` | 无 ERROR/WARN |

---

## 性能基准测试

### 测试方法
使用 F3 调试界面监控：
1. 打开面板前记录 FPS
2. 打开面板后记录 FPS
3. 快速切换 Tab 10 次，记录平均 FPS

### 通过标准
- ✅ FPS 下降 < 10%（在 60 FPS 基准下）
- ✅ 无卡顿或掉帧
- ✅ 内存增长 < 50MB

---

## 测试完成报告模板

```
测试日期: ____________________
测试人员: ____________________
Minecraft 版本: _______________
Mod 版本: _____________________

| 测试场景 | 状态 | 备注 |
|---------|------|------|
| 场景 1: 基础打开/关闭 | ✅/❌ |  |
| 场景 2: 激活状态显示 | ✅/❌ |  |
| 场景 3: 未激活状态显示 | ✅/❌ |  |
| 场景 4: Tab 切换 | ✅/❌ |  |
| 场景 5: 多次切换稳定性 | ✅/❌ |  |
| 场景 6: 重复打开/关闭 | ✅/❌ |  |
| 场景 7: 边界条件 | ✅/❌ |  |

代码验证:
- [ ] ./gradlew compileJava 通过
- [ ] TODO 检查通过（0 命中）
- [ ] 日志验证通过

总体评估: ✅ 通过 / ❌ 失败
关键问题: ____________________
签名: _____________ @ 2025-11-18
```

---

## 故障排除指南

### 问题 1: 面板显示空白内容
**可能原因**:
- `CanvasContentView` 未初始化
- `renderContent()` 抛出异常但被捕获

**检查步骤**:
1. 查看日志是否有异常
2. 确认 `HunDaoClientState.instance()` 不为 null
3. 在 `renderContent()` 开头添加调试日志

### 问题 2: Tab 切换无响应
**可能原因**:
- `switchTab()` 未调用 `setActiveTab()`
- `invalidate()` 未触发重绘

**检查步骤**:
1. 在 `switchTab()` 中添加日志
2. 确认 `contentView` 不为 null
3. 检查 `CanvasContentView.onDraw()` 是否被调用

### 问题 3: 显示 "[Tab content renders here]"
**可能原因**:
- Fragment 仍使用旧的 TextView 实现
- CanvasContentView 未正确替换

**检查步骤**:
1. 确认代码更新已编译
2. 重启 Minecraft 客户端
3. 检查 Fragment 源代码第 107-117 行

---

**通过标准**: 所有场景 100% ✅，代码验证全通过。
**阻塞问题**: 记录到 Phase7.1_Report.md。
