# Combo 代码规范检查器系统

## 概述

模块化的 Combo 技能代码规范检查器系统，每个检查器专注于特定方面的规范验证。

### 系统架构

```
scripts/
├── check_combo.py              # 主入口：协调所有检查器
├── check_combo_compliance.py   # 旧版简化检查器（保留用于快速检查）
└── checkers/                   # 检查器模块
    ├── __init__.py
    ├── base_checker.py         # 基础类
    ├── structure_checker.py    # 结构检查器
    ├── documentation_checker.py # 文档检查器
    ├── test_checker.py         # 测试检查器
    ├── registration_checker.py # 注册检查器
    └── runtime_checker.py      # 运行时检查器
```

## 快速开始

### 1. 列出所有可用检查器

```bash
python scripts/check_combo.py --list
```

### 2. 运行所有检查器

```bash
# 检查整个 bian_hua 族
python scripts/check_combo.py src/main/java/.../combo/bian_hua

# 检查特定技能
python scripts/check_combo.py src/main/java/.../combo/bian_hua/yin_yang/transfer
```

### 3. 运行特定检查器

```bash
# 只检查结构和测试
python scripts/check_combo.py path/to/combo --checkers structure,test

# 只检查注册和运行时规范
python scripts/check_combo.py path/to/combo --checkers registration,runtime
```

### 4. 静默模式（只显示摘要）

```bash
python scripts/check_combo.py path/to/combo --quiet
```

## 检查器详解

### 1. 结构检查器 (structure)

**检查内容：**
- ✅ 目录结构完整性
  - 必需目录：behavior/, calculator/, tuning/, messages/, fx/
  - 可选目录：state/, runtime/
- ✅ 文件命名规范
  - 文件后缀：*Behavior, *Calculator, *Tuning 等
- ✅ Calculator 纯函数规范
  - 检查是否只包含静态方法
  - 排除 record, class, interface, enum

**示例输出：**
```
[目录结构]
  ❌ fascia_latch: 缺少目录或文件 messages/
  ✅ transfer: 存在目录 behavior/

[代码质量]
  ✅ transfer: Calculator 符合纯函数规范
```

**适用场景：**
- 新建技能后的基础验证
- 重构技能目录结构时

---

### 2. 文档检查器 (documentation)

**检查内容：**
- ✅ JSON 文档文件存在性
  - 位置：`src/main/resources/assets/guzhenren/docs/combo/`
- ✅ 必需字段完整性
  - id, title, summary, details
- ✅ 推荐字段
  - icon, iconTexture
- ✅ 字段格式规范
  - iconTexture 应为 PNG 路径
  - 应以 "guzhenren:" 开头

**示例输出：**
```
[文档存在性]
  ❌ transfer: 缺少文档文件 yin_yang_transfer.json

[文档完整性]
  ✅ tai_ji_swap: 包含字段 'title'
  ⚠️ dual_strike: 建议添加字段 'iconTexture' (图标纹理路径)
```

**适用场景：**
- 技能开发完成后的文档验证
- 确保玩家可见信息完整

---

### 3. 测试检查器 (test)

**检查内容：**
- ✅ Calculator 测试文件存在性
  - 位置：`src/test/java/` 对应路径
- ✅ 测试质量
  - 使用 @Test 注解
  - 测试用例数量（建议 ≥3）
  - 使用 AssertJ 断言
  - 包含边界条件测试

**示例输出：**
```
[测试存在性]
  ❌ fascia_latch: 缺少测试文件 ShouPiFasciaLatchCalculatorTest.java

[测试质量]
  ✅ transfer: 有 5 个测试用例
  ✅ transfer: 使用 AssertJ 断言
  ⚠️ roll: 测试用例较少 (2 个)，建议增加边界测试
```

**适用场景：**
- TDD 开发流程中的持续验证
- 代码审查前的质量检查
- CI/CD 集成

---

### 4. 注册检查器 (registration)

**检查内容：**
- ✅ Behavior.initialize() 方法存在
- ✅ ComboSkillRegistry 注册状态
- ✅ 注册详情完整性
  - iconLocation
  - displayName
  - description

**示例输出：**
```
[Behavior 实现]
  ✅ transfer: Behavior 有 initialize() 方法

[注册状态]
  ✅ transfer: 已在 ComboSkillRegistry 中注册
  ❌ new_skill: 未在 ComboSkillRegistry 中注册

[注册详情]
  ⚠️ transfer: 注册可能缺少 iconLocation
```

**适用场景：**
- 新技能集成前的验证
- 确保技能可被游戏加载

---

### 5. 运行时检查器 (runtime)

**检查内容：**
- ✅ 资源消耗规范
  - 使用 ComboSkillUtil.tryPayCost()
- ✅ 冷却管理规范
  - 使用 MultiCooldown
  - 避免手动时间戳管理
- ✅ Toast 提示
  - 调用 scheduleReadyToast()
- ✅ activate() 方法结构
  - 前置检查
  - 调用 Calculator
  - 播放效果

**示例输出：**
```
[资源消耗]
  ✅ transfer: 使用 ComboSkillUtil.tryPayCost() 处理资源

[冷却管理]
  ✅ transfer: 使用 MultiCooldown 管理冷却
  ❌ old_skill: 使用手动时间戳管理冷却，应改用 MultiCooldown

[用户反馈]
  ⚠️ transfer: 建议调用 scheduleReadyToast() 提示玩家冷却就绪

[代码结构]
  ✅ transfer: 包含 activate() 方法作为入口
  ✅ transfer: activate() 包含前置检查
```

**适用场景：**
- 确保运行时行为符合规范
- 代码审查时的规范检查
- 重构旧代码时的规范验证

---

## 使用场景示例

### 场景 1：开发新技能

**步骤 1：创建基础结构后**
```bash
python scripts/check_combo.py path/to/new_skill --checkers structure
```

**步骤 2：编写 Calculator 和测试后**
```bash
python scripts/check_combo.py path/to/new_skill --checkers structure,test
```

**步骤 3：完成集成前的全面检查**
```bash
python scripts/check_combo.py path/to/new_skill
```

---

### 场景 2：代码审查

```bash
# 检查整个 Pull Request 涉及的家族
python scripts/check_combo.py src/main/java/.../combo/bing_xue

# 只关注测试和运行时规范
python scripts/check_combo.py path/to/changed_skills --checkers test,runtime
```

---

### 场景 3：重构旧代码

```bash
# 先检查当前状态
python scripts/check_combo.py path/to/old_skill > before.txt

# 重构后对比
python scripts/check_combo.py path/to/old_skill > after.txt
diff before.txt after.txt
```

---

### 场景 4：CI/CD 集成

```bash
# 在 CI 流程中运行全部检查
python scripts/check_combo.py src/main/java/.../combo/

# 非零退出码表示检查失败
if [ $? -ne 0 ]; then
    echo "代码规范检查失败"
    exit 1
fi
```

---

## 与 AGENTS.md 规范对照

### 覆盖的规范

| AGENTS.md 步骤 | 对应检查器 | 覆盖程度 |
|---------------|----------|---------|
| 步骤 1: 目录结构 | structure | 100% |
| 步骤 2: Calculator & 测试 | structure, test | 90% |
| 步骤 3: 参数、文案、效果 | structure | 80% |
| 步骤 4: 行为逻辑 | registration, runtime | 70% |
| 步骤 5: 注册与文档 | registration, documentation | 85% |
| 步骤 6: 验证 | test | 60% |

### 未覆盖的检查（需要手动验证）

- ❌ `./gradlew build` 编译成功
- ❌ `./gradlew test` 测试通过
- ❌ Checkstyle 警告
- ❌ 图标纹理文件实际存在性
- ❌ 游戏内运行时功能验证

**建议：** 将检查器集成到 CI/CD 流程中，配合 Gradle 构建和测试任务。

---

## 扩展开发

### 添加新检查器

1. 创建新的检查器类：

```python
# scripts/checkers/my_checker.py
from .base_checker import BaseChecker, Severity

class MyChecker(BaseChecker):
    """我的自定义检查器"""

    @property
    def description(self) -> str:
        return "检查自定义规范"

    def check(self, target_dir: Path) -> 'CheckReport':
        # 实现检查逻辑
        self.passed("检查通过", category="自定义")
        return self.report
```

2. 在 `__init__.py` 中注册：

```python
from .my_checker import MyChecker
__all__ = [..., 'MyChecker']
```

3. 在 `check_combo.py` 中添加：

```python
self.checkers = {
    ...,
    'my_check': MyChecker(self.project_root),
}
```

---

## 故障排除

### 问题：找不到技能目录

**症状：** "未找到技能目录"

**原因：** 目录结构不符合预期（缺少至少2个必需子目录）

**解决：** 确保技能目录包含至少 behavior/ 和 calculator/ 子目录

---

### 问题：无法读取文件

**症状：** "无法读取文件" 警告

**原因：** 文件编码问题或权限不足

**解决：**
```bash
# 检查文件编码
file -i path/to/file.java

# 确保文件为 UTF-8
iconv -f GBK -t UTF-8 file.java > file_utf8.java
```

---

### 问题：检查器误报

**症状：** Calculator 被误报为包含非静态方法

**原因：** 使用了 record、inner class 等结构

**解决：** 检查器已考虑 record, class, interface, enum，如仍有误报请报告。

---

## 性能优化

- 大型项目检查可能需要数秒
- 使用 `--checkers` 参数只运行必要的检查器
- 使用 `--quiet` 模式减少输出

---

## 版本历史

- **v2.0** (2024-10-29): 模块化检查器系统
  - 5个专门检查器
  - 可选择性运行
  - 更详细的报告

- **v1.0** (2024-10-29): 简化单体检查器
  - 基础结构检查
  - 文件命名检查

---

## 贡献指南

欢迎提交 Issue 和 Pull Request！

建议改进：
- [ ] 添加性能分析检查器
- [ ] 添加安全规范检查器
- [ ] 支持 JSON 格式输出
- [ ] 支持配置文件自定义规则
- [ ] 集成到 pre-commit hook
