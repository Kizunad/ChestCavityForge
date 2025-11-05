# 飞剑模块重构｜测试用例文档（Test Cases）

> 策略：只测纯逻辑/可抽象模块；Minecraft 核心类不 mock。

## 1. 覆盖范围
- calculator/FlyingSwordCalculator：速度上限、伤害、经验、维持消耗
- integration/resource/UpkeepOps：区间消耗计算与扣减策略
- motion/SteeringOps & KinematicsOps：速度约束、插值、命令合成
- systems（可抽象）：Combat（伤害合成）、Upkeep（策略分支）
- util：ItemAffinityUtil（继承属性计算）

## 2. 测试用例清单（示例）
- CALC-001：速度²伤害随速度单调递增，0 速度为 0
- CALC-002：经验累加触发多级升级，余量正确
- CALC-003：有效速度上限受属性与上下文影响，边界裁剪正确
- UPKEEP-001：维持消耗在 interval 窗口内线性缩放
- UPKEEP-002：玩家疾跑/破块/高速度时倍率提升
- UPKEEP-003：资源不足触发失败策略（停滞/减速/召回）
- MOTION-001：SteeringOps 速度不超过上限；方向插值平滑
- SYSTEM-001：Combat 混入 OnHitEntity 修改伤害后仍保持非负
- UTIL-001：物品亲和度映射在未知条目时回退默认

## 3. 断言规范
- 纯函数：输入-输出精确断言；边界/异常覆盖
- 近似计算：允许误差范围（如 1e-6）

## 4. 运行方式
```bash
./gradlew test
# 或
./scripts/run-tests.sh
```

## 5. 结果记录
- 将关键指标（覆盖率/失败用例/回归项）附加至 `docs/TESTING_SUMMARY.md`

