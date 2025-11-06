# FxEngine 时间线系统 · 阶段执行指引

- 执行时同时参考：
  - 红线：docs/FxEngine/constraints.md
  - 规范：docs/FxEngine/standards.md
  - 总任务：docs/FxEngine/master-plan.md
  - 当前阶段：docs/FxEngine/stages/*.md

- 验证顺序：
  1) 单模块功能 → 2) 预算与合并 → 3) 门控 → 4) 与 Shockfield 集成

- 调试建议：
  - 默认关闭预算；在高载压场景开启并观察活跃数与丢弃率；
  - 打开 Debug 仅用于问题定位；避免线上常开。

