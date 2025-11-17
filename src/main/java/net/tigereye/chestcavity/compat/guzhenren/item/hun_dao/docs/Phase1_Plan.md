# Hun Dao Phase 1 实施计划

## 计划制定时间
2025-11-17

## 阶段目标
搭建未来模块化所需的目录与接口层，解耦中间件。

## 任务清单

### 1. 创建目录结构
**负责人：** Claude
**预计完成时间：** 2025-11-17
**状态：** ✅ 已完成

创建以下目录以对齐 `jian_dao` 架构：
- [ ] `runtime/` - 运行时状态与操作接口
- [ ] `calculator/` - 计算逻辑（占位符）
- [ ] `tuning/` - 平衡与调参常量
- [ ] `fx/` - 特效管理（已存在，验证）
- [ ] `client/` - 客户端代码（占位符）
- [ ] `events/` - 事件处理器（占位符）
- [ ] `ui/` - UI 组件（占位符）
- [ ] `storage/` - 数据持久化（占位符）

### 2. 定义接口层
**负责人：** Claude
**预计完成时间：** 2025-11-17
**状态：** ✅ 已完成

#### 2.1 HunDaoResourceOps 接口
创建资源操作接口，包含以下方法：
- `leakHunpoPerSecond(Player, double)` - 魂魄泄露
- `consumeHunpo(Player, double)` - 魂魄消耗
- `openHandle(Player)` - 打开资源句柄
- `readHunpo(Player)` - 读取当前魂魄值
- `readMaxHunpo(Player)` - 读取最大魂魄值
- `adjustDouble(Player, String, double, boolean, String)` - 调整双精度资源字段

#### 2.2 HunDaoFxOps 接口
创建特效操作接口，包含以下方法：
- `applySoulFlame(Player, LivingEntity, double, int)` - 应用魂焰 DoT

#### 2.3 HunDaoNotificationOps 接口
创建通知操作接口，包含以下方法：
- `handlePlayer(Player)` - 玩家维护
- `handleNonPlayer(LivingEntity)` - 非玩家实体维护

### 3. 实现适配器
**负责人：** Claude
**预计完成时间：** 2025-11-17
**状态：** ✅ 已完成

创建 `HunDaoOpsAdapter` 实现所有接口，委托给现有的 `HunDaoMiddleware` 和 `ResourceOps`。

**设计考虑：**
- 单例模式
- 临时过渡层
- 完全向后兼容

### 4. 迁移调参系统
**负责人：** Claude
**预计完成时间：** 2025-11-17
**状态：** ✅ 已完成

#### 4.1 创建 HunDaoTuning
将 `HunDaoBalance` 迁移到 `tuning/HunDaoTuning`，按功能分组：
- `SoulBeast` - 魂兽化机制
- `SoulFlame` - 魂焰 DoT 机制
- `XiaoHunGu` - 小魂蛊机制
- `DaHunGu` - 大魂蛊机制
- `Effects` - 通用效果

#### 4.2 标记旧类为废弃
将 `HunDaoBalance` 标记为 `@Deprecated`，保留以保持临时兼容性。

#### 4.3 更新行为类引用
更新所有行为类使用 `HunDaoTuning` 而非 `HunDaoBalance` 或硬编码常量。

### 5. 重构行为类
**负责人：** Claude
**预计完成时间：** 2025-11-17
**状态：** ✅ 已完成

#### 5.1 HunDaoSoulBeastBehavior
- 注入接口依赖（resourceOps, fxOps, notificationOps）
- 替换所有直接的 `GuzhenrenResourceBridge` 和 `ResourceOps` 调用为接口方法调用
- 使用 `HunDaoTuning` 常量
- 移除不必要的导入

**关键重构点：**
- `onSlowTick()` - 使用 `resourceOps.leakHunpoPerSecond()` 和 `notificationOps.handlePlayer()`
- `onHit()` - 使用 `resourceOps.readHunpo()`, `resourceOps.adjustDouble()`, `resourceOps.readMaxHunpo()`, `fxOps.applySoulFlame()`

### 6. 验证与测试
**负责人：** Claude
**预计完成时间：** 2025-11-17
**状态：** ✅ 已完成

- 验证所有文件编译通过
- 确认目录结构完整
- 检查接口调用路径
- 确认没有直接的中间件依赖（除适配器外）

### 7. 文档输出
**负责人：** Claude
**预计完成时间：** 2025-11-17
**状态：** ✅ 已完成

- 创建 `Phase1_Report.md` 记录完成情况
- 列出所有修改的文件
- 记录关键代码变更
- 说明验收标准满足情况

## 技术约束
- 保持向后兼容性
- 遵循 KISS、YAGNI、DIP 原则
- 所有新代码必须有 Javadoc
- 接口设计必须与 `jian_dao` 对齐

## 风险管理
- **接口设计不完整**：通过参考 `jian_dao` 和现有代码确保接口覆盖所有需求
- **行为类遗漏**：系统性搜索所有引用，确保全部迁移
- **常量双重维护**：强制所有代码使用 `HunDaoTuning`，废弃旧类

## 验收标准
1. ✅ 目录结构与 `jian_dao` 对齐
2. ✅ 接口层完整定义并实现
3. ✅ 行为类通过接口访问资源/FX/通知
4. ✅ 所有调参常量在 `HunDaoTuning` 中统一管理
5. ✅ 编译通过
6. ✅ 无直接中间件依赖（除适配器外）

## 下一步（Phase 2 预览）
Phase 2 将实现：
- `HunDaoRuntimeContext` 和 `HunDaoStateMachine`
- `storage/HunDaoSoulStateStore` 和持久化逻辑
- 状态机覆盖魂兽化、DoT、魂魄泄露等流程
