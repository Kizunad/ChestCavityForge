# Hun Dao Phase 2 实施计划

## 执行时间
- 开始时间：2025-11-17
- 预计完成时间：2025-11-17
- 前置阶段：Phase 1（已完成，提交 9dae6d9）

## 阶段目标
Phase 2 目标是建立魂魄/魂兽化状态管理与持久化系统，将运行时状态与存储逻辑从行为层分离。

## 任务清单

### 1. Runtime 核心组件设计与实现
**目标：** 建立统一的运行时上下文和状态管理机制

#### 1.1 HunDaoRuntimeContext
- [ ] 设计 `HunDaoRuntimeContext` 接口/类
  - 提供统一的资源操作访问 (`HunDaoResourceOps`)
  - 提供特效操作访问 (`HunDaoFxOps`)
  - 提供通知操作访问 (`HunDaoNotificationOps`)
  - 提供状态机访问 (`HunDaoStateMachine`)
  - 提供调度器访问 (`HunPoDrainScheduler`)
- [ ] 实现 `HunDaoRuntimeContext.Builder` 构建器模式
- [ ] 添加玩家/实体关联方法

**文件位置：** `runtime/HunDaoRuntimeContext.java`

#### 1.2 HunDaoStateMachine
- [ ] 设计状态枚举 `HunDaoState`
  - `NORMAL` - 正常状态
  - `SOUL_BEAST_ACTIVE` - 魂兽化激活
  - `SOUL_BEAST_PERMANENT` - 永久魂兽化
  - `DRAINING` - 魂魄泄露中
- [ ] 实现状态转换逻辑
  - 定义允许的状态转换路径
  - 实现状态转换验证
  - 触发状态改变事件
- [ ] 集成 `SoulBeastState` 作为底层状态存储
- [ ] 添加状态查询方法

**文件位置：** `runtime/HunDaoStateMachine.java`

#### 1.3 HunPoDrainScheduler
- [ ] 设计定时任务调度器
  - 每秒触发魂魄泄露检查
  - 支持暂停/恢复
  - 支持速率配置
- [ ] 实现泄露逻辑
  - 检查魂兽化状态
  - 调用 `HunDaoResourceOps.leakHunpoPerSecond()`
  - 魂魄耗尽时自动解除魂兽化
- [ ] 集成到服务器 tick 事件

**文件位置：** `runtime/HunPoDrainScheduler.java`

### 2. Storage 持久化层实现
**目标：** 建立专门的存储层，迁移 NBT 逻辑

#### 2.1 HunDaoAttachmentKeys
- [ ] 定义 NeoForge Attachment 键
  - `SOUL_STATE` - 魂魄状态数据
  - `SOUL_BEAST_STATE` - 魂兽化状态（复用现有 `SoulBeastState`）
  - `HUN_DAO_CONTEXT` - 运行时上下文缓存
- [ ] 注册到 `CCAttachments` 或创建专门注册类

**文件位置：** `storage/HunDaoAttachmentKeys.java`

#### 2.2 HunDaoSoulStateStore
- [ ] 设计 `HunDaoSoulState` 数据类
  - 魂魄相关的运行时状态（非资源值）
  - DOT 效果追踪
  - 魂兽化历史/统计
- [ ] 实现 NBT 序列化/反序列化
  - `save(CompoundTag)` 方法
  - `load(CompoundTag)` 方法
- [ ] 实现 Codec 序列化（用于 Attachment）
- [ ] 迁移现有 NBT 逻辑
  - 从 `SoulBeastState` 迁移相关字段
  - 从行为类中提取硬编码的状态存储

**文件位置：** `storage/HunDaoSoulStateStore.java`

### 3. 行为层集成
**目标：** 确保行为类通过 runtime 接口访问状态

#### 3.1 更新行为类
- [ ] 重构 `HunDaoSoulBeastBehavior` 使用 `HunDaoRuntimeContext`
- [ ] 重构 `XiaoHunGuBehavior` 使用 `HunDaoRuntimeContext`
- [ ] 重构 `DaHunGuBehavior` 使用 `HunDaoRuntimeContext`
- [ ] 重构 `GuiQiGuOrganBehavior` 使用 `HunDaoRuntimeContext`
- [ ] 重构 `TiPoGuOrganBehavior` 使用 `HunDaoRuntimeContext`
- [ ] 禁止直接访问玩家 capability（通过 Checkstyle 或代码审查确保）

#### 3.2 移除直接状态访问
- [ ] 替换 `SoulBeastStateManager` 直接调用为通过 `HunDaoStateMachine`
- [ ] 替换直接 NBT 操作为通过 `HunDaoSoulStateStore`

### 4. 存档兼容性
**目标：** 确保老存档可以正常迁移

#### 4.1 迁移策略
- [ ] 设计迁移逻辑
  - 从旧 NBT 格式读取数据
  - 转换为新的 Attachment 格式
  - 保留向后兼容性
- [ ] 实现迁移代码（如需要）
- [ ] 文档化迁移行为

#### 4.2 验证
- [ ] 创建测试存档（包含魂兽化玩家）
- [ ] 验证加载后状态正确
- [ ] 验证保存后格式正确

### 5. 事件集成
**目标：** 确保状态机正确触发和响应事件

#### 5.1 状态变更事件
- [ ] 复用 `SoulBeastStateChangedEvent`
- [ ] 在状态机中正确触发事件
- [ ] 确保事件监听器正常工作

#### 5.2 定时任务注册
- [ ] 注册 `HunPoDrainScheduler` 到服务器 tick 事件
- [ ] 确保仅在服务器端执行
- [ ] 添加性能监控日志（如需要）

### 6. 文档与测试
**目标：** 完善文档和验收材料

#### 6.1 文档
- [ ] 更新 `storage/README.md` 说明实际实现
- [ ] 更新 `runtime/README.md` 说明新增组件
- [ ] 创建 `Phase2_Report.md` 完成报告

#### 6.2 验收测试
- [ ] 复现 Phase 0 smoke 测试场景
  - 魂兽化激活/解除
  - 魂魄泄露
  - 魂焰 DOT
- [ ] 验证存档加载/保存
- [ ] 验证状态同步到客户端

## 验收标准

### ✅ 状态机覆盖核心流程
- [ ] 魂兽化激活/解除状态转换正确
- [ ] DOT 效果状态追踪正常
- [ ] 魂魄泄露流程完整

### ✅ 运行时/存档兼容性
- [ ] 新存档正常保存/加载
- [ ] 老存档迁移成功（如适用）
- [ ] 状态持久化无丢失

### ✅ 行为层解耦
- [ ] 行为类通过 `HunDaoRuntimeContext` 访问状态
- [ ] 禁止直接访问 capability
- [ ] 所有状态操作通过 runtime 接口

## 编码规范
- 遵循 Checkstyle 规则（仅针对新增/修改文件）
- 所有公开 API 需要 Javadoc
- 遵循 KISS、YAGNI、DIP 原则
- 与 `jian_dao` 结构对齐

## 风险与缓解
- **复杂度风险：** 状态机设计过于复杂 → 保持简单，仅覆盖现有功能
- **兼容性风险：** 破坏现有存档 → 实现迁移逻辑，充分测试
- **性能风险：** 定时任务影响性能 → 仅在必要时执行，添加监控

## 依赖关系
- 依赖 Phase 1 完成的接口层（`HunDaoResourceOps` 等）
- 复用现有 `SoulBeastState` 和 `SoulBeastStateManager`
- 集成到现有的 `CCAttachments` 系统

## 下一步（Phase 3 预览）
Phase 2 完成后，Phase 3 将进行行为模块化：
- 按 organ/active/passive/skills/command 重新划分
- 构建 `behavior/common` 提供共享上下文
- 将事件监听类迁移至 `events/`

---
**审查要求：** 完成后提交 `Phase2_Report.md`，记录所有修改文件和关键变更。
