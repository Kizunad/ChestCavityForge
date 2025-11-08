第一步：生成需求方案文档

    创建一个 Markdown 格式的需求方案文档
    文档中必须使用 Mermaid 图表（可配合 VSCode 的 “Markdown Preview Mermaid Support” 插件预览）
    文档内容应包含：
        功能列表和功能描述
        用例图（Use Case Diagram）
        用户故事（User Stories）
        业务流程图（使用 Mermaid flowchart 或 sequence diagram）
    重要：尽量减少代码示例，专注于需求描述和流程设计

第二步：人工审核与迭代讨论

    生成初稿后，我会进行人工审核
    在这个阶段，请从以下两个角度反复思考和优化方案：
        产品经理视角：关注用户需求、业务价值、功能完整性
        工程师视角：关注技术可行性、实现复杂度、潜在风险
    这个步骤可能会跨越多个会话，每次讨论后请根据反馈修改方案
    注意：如果使用 sub-agent 进行多角度讨论，响应时间会较慢，请提前告知

第三步：敲定技术框架方案

    根据最终确定的需求方案，设计技术框架
    核心原则：
        遵循 KISS 原则（Keep It Simple, Stupid）
        遵循轻量化原则，避免过度设计
        不引入不必要的技术栈和依赖
    输出一份简洁的技术框架方案文档（Markdown 格式）

第四步：生成约束与规范文档

生成两份关键文档：
4.1 红线文档（Constraints Document）

    明确规定项目禁止做的事情，例如：
        禁止的技术选型
        禁止的架构模式
        禁止的代码实践
        安全红线
        性能红线

4.2 规范文档（Standards Document）

    明确规定项目应该遵循的规范，例如：
        代码风格规范
        命名约定
        目录结构规范
        Git 提交规范
        文档编写规范

第五步：生成实施文档

    综合以下四份文档生成详细的实施文档：
        需求方案
        技术框架方案
        红线文档
        规范文档
    注意：由于需要参考多份文档，可能会导致上下文不足，请在生成后进行多轮审核和打磨
    建议使用多个 sub-agent 分别审核不同部分，以减少单次上下文消耗

第六步：任务拆解

将实施文档拆解为：
6.1 总任务文档（Master Plan）

    单独一个 Markdown 文档
    包含项目整体目标、里程碑、关键交付物

6.2 阶段任务文档（Stage Plans）

    每个阶段一个独立的 Markdown 文档
    每个文档包含：
        阶段目标
        具体任务列表
        依赖关系
        验收标准
    使用 sub-agent 并行处理多个阶段文档的生成
    完成后进行人工审核，添加批注
    用红线文档和规范文档对每个阶段文档进行最终校验

第七步：执行各阶段任务

    按阶段顺序执行任务
    每个阶段执行时，必须同时参考：
        红线文档
        规范文档
        总任务文档
        当前阶段任务文档
    配合自主调试工具（如浏览器 MCP、测试工具等）进行验证

第八步：测试覆盖（如果框架支持）

    生成测试用例文档（Test Case Document）
    为每个功能模块编写单元测试
    逐个执行并确保测试通过

——

本次主题：新增“OnHit 聚合接口与伤害桶（Damage Buckets）”方案

文档产出已分组存放于 `docs/damage-buckets/`，按上述步骤对应如下：

- 01 需求方案：`docs/damage-buckets/01-requirements.md`
- 02 技术框架：`docs/damage-buckets/02-architecture.md`
- 03 红线文档：`docs/damage-buckets/03-constraints.md`
- 04 规范文档：`docs/damage-buckets/04-standards.md`
- 05 实施文档：`docs/damage-buckets/05-implementation.md`
- 06 总任务：`docs/damage-buckets/06-master-plan.md`
- 06 阶段任务（样例）：
  - `docs/damage-buckets/06-stage-1.md`
  - `docs/damage-buckets/06-stage-2.md`
- 08 测试用例：`docs/damage-buckets/08-test-cases.md`

说明：本轮仅产出“聚合接口与桶”的方案，不包含具体器官/行为的迁移实现，迁移留待重构阶段按需推进。
