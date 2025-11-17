# Hun Dao Phase 4 实施计划

## 制定时间
- 2025-11-18

## 阶段目标
围绕“Combat & Calculator”主题，将魂道所有战斗/数值算法从行为层和 `HunDaoDamageUtil` 中剥离，形成可测试、可调参的 Calculator + Tuning 体系，并补齐最小化测试矩阵：
1. 建立与 `jian_dao` 对齐的 `calculator/` 子包结构（common/damage/resource/skill）；
2. 将 DOT、真实伤害、魂兽化泄露/回流计算迁移到 Calculator 层，通过接口注入至行为；
3. 引入集中配置的 `tuning/` 子包，对战斗常量、软硬上限、概率参数进行数据驱动管理；
4. 使用单元测试 + smoke 测试验证关键计算路径，确保功能等价。

## 任务清单

### 任务 1：Calculator 架构落地
- [ ] 在 `hun_dao/calculator/` 下建立分层结构：
  - `common/`：`HunDaoCalcContext`、`CalcMath` 等通用工具；
  - `damage/`：`HunDaoDamageCalculator`、`HunDaoDotCalculator`；
  - `resource/`：`HunPoDrainCalculator`、`HunPoRecoveryCalculator`；
  - `skill/`：`GuiWuCalculator`（处理命中/范围/持续）；
- [ ] 设计 `HunDaoCombatCalculator` 接口（或门面）供行为层调用，内部组合以上 Calculator；
- [ ] 输出 `calculator/README.md` 描述调用顺序、扩展点、示例；
- [ ] 与 `HunDaoRuntimeContext` 集成：在 context 中暴露 `getCalculator()` 或 `getCombatOps()`，保持依赖倒置。

### 任务 2：战斗入口重构
- [ ] 将 `HunDaoDamageUtil`、`HunDaoMiddleware` 中的伤害、魂焰 DOT、魂兽攻击加成逻辑，迁移到新的 Calculator；
- [ ] 创建 `combat/HunDaoCombatHooks`（或 `combat/` 目录），封装行为与 Calculator 的桥接，例如 `applySoulFlame`、`applySoulBeastStrike`；
- [ ] 将 `HunDaoBehaviorContextHelper`、`HunDaoSoulBeastBehavior`、`GuiQiGuOrganBehavior`、`TiPoGuOrganBehavior` 等类改为调用新 Hooks/Calculator，而非直接计算；
- [ ] 补齐 `events/GuiQiGuEvents`、`runtime/HunPoDrainScheduler` 等位置的 Calculator 接入，保证唯一计算来源；
- [ ] 在 `middleware/HunDaoMiddleware` 中仅保留资源/FX 转发逻辑，移除所有数值计算，避免双写。

### 任务 3：Tuning 数据驱动
- [ ] 新建 `tuning/HunDaoCombatTuning.java`、`tuning/HunDaoSkillTuning.java`，集中管理：
  - DOT 系数、持续时间上限、触发概率；
  - GUI_WU 范围、冷却、魂魄消耗；
  - 魂兽化增伤系数、泄露速率、护盾刷新倍率；
- [ ] 为 tuning 常量提供读取 API（含注释/默认值），禁止外部硬编码；
- [ ] 若需配置化，预留 `json`/`toml` 读取接口，但遵循 YAGNI：先锁死代码常量，再在 Phase 5+ 考虑外部化；
- [ ] 复查 `Phase0` Smoke 脚本中的数值断言，更新为引用 tuning 常量，保证计划内自检一致。

### 任务 4：测试与文档
- [ ] 编写 `HunDaoDamageCalculatorTest`、`HunPoDrainCalculatorTest`（Junit/Mockito），覆盖：真实伤害计算、魂焰 DOT 合成、泄露阈值计算；
- [ ] 更新 `smoke_test_script.md`：增加“魂焰 DOT 叠加”、“Gui Wu 范围” 观察点，并串联调参后的预期值；
- [ ] 完成 `Phase4_Report.md`，列出迁移文件、Calculator 接入矩阵、测试结果；
- [ ] 如有新的公共工具（CalcContext/Math），在 `behavior/common` 或 `calculator/common` 增加 package-info/README。

## 关键要点
1. **功能等价**：Phase 4 只做重构和可测试化，不改变外部数值；所有 Calculator 须引用当前常量。
2. **单一来源**：Calc 层成为唯一的数值入口，行为/事件/调度器严禁再写公式；通过代码扫描（`rg -n "0.03D"` 等）确认无散落常量。
3. **接口注入**：Calculator 通过 `HunDaoRuntimeContext` 或新的 `HunDaoCombatOps` 提供，保持 SOLID 中的 DIP。
4. **测试先行**：对于新增 Calculator，先写接口 & 测试桩，再迁移逻辑，避免一次性大改难以验证。
5. **Checkstyle**：只需保证修改文件通过 Checkstyle；迁移过程中若需 reformat，拆分 commit/文件方便审查。

## 自检清单
- [ ] `rg -n "HunDaoDamageUtil" src/main/java/.../hun_dao` 仅剩 `combat/` 内部引用；
- [ ] `rg -n "0\.03D" src/main/java/.../hun_dao` 等关键常量命中均来自 `tuning/`；
- [ ] `rg -n "new HunDaoDamageCalculator"` 只在 `runtime` 构建或测试中出现，行为层全部注入式获取；
- [ ] `./gradlew :ChestCavityForge:test --tests "*HunDao*Calculator*"` 通过；
- [ ] Smoke 测试：按 `smoke_test_script.md` 执行魂兽化/鬼雾/魂焰场景，记录数值与预期一致。

## 交付物
- `calculator/` 全量目录与实现、`combat/HunDaoCombatHooks.java`；
- `tuning/HunDaoCombatTuning.java`、`HunDaoSkillTuning.java` + 相关 README；
- 更新后的行为/事件/调度器引用；
- `calculator/README.md`、`tuning/README.md`、`Phase4_Report.md`、`smoke_test_script.md`；
- 新增单元测试文件与执行记录。
