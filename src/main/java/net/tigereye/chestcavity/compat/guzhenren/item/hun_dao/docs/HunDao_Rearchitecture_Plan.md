# HunDao 架构重构工程计划

## 1. 背景现状
- 现有 `hun_dao` 仅有 `behavior/`, `middleware/`, `combat/`, `storage/` 雏形目录，逻辑集中在若干巨型类（例如 `HunDaoMiddleware`, `HunDaoDamageUtil`），导致行为、数值、渲染耦合严重。
- 数据存储与运行状态缺失专门的 `runtime`/`storage` 实现，魂兽化流程与魂魄资源管理散落在行为类与中间件中。
- 客户端表现（FX/音效/UI）仍绑定在 `HunDaoClientAbilities` 与中间件中，没有 `fx/`, `client/`, `ui/` 分层。
- 缺乏统一的数值/调参目录，`HunDaoBalance` 和常量写死在行为逻辑里，难以测试与调优。

## 2. 重构目标
1. 将 `hun_dao` 包结构对齐 `jian_dao` 成熟架构，形成独立的 `behavior/active|passive|organ|skills`, `runtime/`, `calculator/`, `tuning/`, `fx/`, `client/`, `events/`, `ui/`, `storage/` 等子模块。
2. 引入 `HunDaoRuntimeContext` 与 `HunDaoResourceOps` 等抽象，落实依赖倒置（DIP），行为模块仅依赖接口。
3. 建立统一数值/调参体系和 FX/客户端体系，确保 KISS/YAGNI：只迁移现有魂道功能，不做超前实现。
4. 构建可重复执行的测试与验收流程，每阶段必须具备 smoke 测试与代码审查准则（0 容忍）。

## 3. 分阶段计划

### Phase 0：基线&观测
- **目标**：锁定当前功能基线，确保重构期间可快速验证。
- **关键输出**
  - `docs/Phase0_Report.md`：现状依赖图、魂魄流转路径、烟测脚本说明。
  - 简易 smoke 测试脚本（命令序列或自动化脚本），验证魂兽化、魂魄扣减、魂焰 DOT。
- **验收标准**
  - 能复现现有魂道核心功能且记录成功/失败指标。
  - 建立 “计划-报告-验收” 模板。

### Phase 1：包结构与接口骨架
- **目标**：搭建未来模块化所需的目录与接口层，解耦中间件。
- **范围**
  - 新建 `runtime/`, `calculator/`, `tuning/`, `fx/`, `client/`, `events/`, `ui/`, `storage/` 等目录及空实现。
  - 将 `HunDaoMiddleware` 拆分为 `HunDaoResourceOps`, `HunDaoFxOps`, `HunDaoNotificationOps` 接口，并提供临时实现。
  - 将 `HunDaoBalance` 迁至 `tuning/HunDaoTuning`，按功能分组常量。
- **验收标准**
  - 目录与接口结构与 `jian_dao` 对齐，编译通过。
  - 所有行为类通过接口访问资源/FX，不再直接依赖 `HunDaoMiddleware` 静态方法。

### Phase 2：Runtime & Storage
- **目标**：建立魂魄/魂兽化状态管理与持久化。
- **范围**
  - 设计 `HunDaoRuntimeContext`, `HunDaoStateMachine`, `HunPoDrainScheduler`。
  - 实现 `storage/HunDaoSoulStateStore`, `HunDaoAttachmentKeys`，并迁移现有 NBT 逻辑。
  - 行为层获取状态统一通过 `runtime` 接口，禁止直接访问玩家 capability。
- **验收标准**
  - 状态机覆盖魂兽化、DOT、魂魄泄露等流程。
  - 运行/存档兼容验证通过（老存档迁移方案确认）。

### Phase 3：行为模块化
- **目标**：按 organ/active/passive/skills/command 重新划分行为。
- **范围**
  - 仿照 `jian_dao/behavior/organ/*` 等进行拆分；构建 `behavior/common` 提供共享上下文与守卫。
  - 将事件监听类迁至 `events/`，行为层仅输出意图。
- **验收标准**
  - 每个蛊的逻辑位于独立类，单职责清晰。
  - 行为依赖 `runtime` 与 `calculator` 接口，无重复代码。

### Phase 4：Combat & Calculator
- **目标**：数值计算与战斗入口结构化。
- **范围**
  - 拆分 `HunDaoDamageUtil` 为 `calculator/HunDaoDamageCalc`, `calculator/HunPoScalingCalc`, `combat/HunDaoCombatHooks`。
  - 建立 `tuning/HunDaoCombatTuning`，参数化 DOT、穿透、持续时间等。
  - 引入基础单元测试（与 `jian_dao` 共享测试夹具）。
- **验收标准**
  - 战斗计算路径 “输入→Calc→输出” 清晰可测试。
  - 新增测试覆盖核心数值函数。

### Phase 5：FX/Client/UI 与中间件解耦
- **目标**：客户端/特效/提示逻辑模块化。
- **范围**
  - 新建 `fx/HunDaoFxRouter`, `fx/HunDaoFxTuning`, `client/HunDaoClientRegistries`, `ui/HunDaoSoulHud`。
  - `HunDaoMiddleware` 中所有渲染/提示逻辑迁移至对应模块。
- **验收标准**
  - 客户端能力注册与 FX/音效/提示各自独立，服务器中间件仅保留运算逻辑。

### Phase 6：收尾与验收
- **目标**：全链路回归、文档与交付。
- **范围**
  - 制定 `docs/Phase6_Acceptance.md`，记录测试矩阵与结果。
  - 运行 `./gradlew test` + 关键场景手测，输出验收报告。
- **验收标准**
  - 所有阶段报告齐全，代码审查零问题。
  - 验证脚本/手测全部通过，文档同步更新。

## 4. 交付物与报告机制
- 每阶段需在 `docs/PhaseX_Plan.md` 中列出任务清单、责任人、预期完成时间。
- 阶段结束由你提供完成报告（含修改文件列表、关键代码 diff、测试结果），我进行逐行审查并在 `docs/PhaseX_Review.md` 填写验收结论；若发现问题，创建 `PhaseX.1_Plan.md` 继续迭代。
- 所有计划/报告遵循 “计划→实施→完成报告→审查→验收” 流程，确保可追溯。

## 5. 风险与控制
- **结构漂移风险**：以 `jian_dao` 目录为蓝本，阶段性对比结构，避免偏离。
- **回归风险**：Phase 0 建立 smoke 测试，后续每阶段都需复跑；关键数值函数必须引入单测。
- **范围膨胀**：遵循 YAGNI，只迁移当前魂道机制；新增功能需单独立项。
- **审查压力**：0 容忍策略要求每次 PR 粒度可控，建议阶段内多次小提交，方便 review。

## 6. 下一步
1. 按此计划在 `docs/Phase0_Plan.md` 填写详细任务与时间表。
2. 盘点现状，输出 Phase 0 完成报告模板供后续使用。

---  
如需调整阶段范围或优先级，请在执行前确认，我将同步更新计划文档。
