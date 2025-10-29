# Combo 代码规范检查工具

## 🆕 新版模块化检查器系统

**推荐使用：** `check_combo.py` - 功能更全面的模块化检查器系统

详细文档请查看：**[CHECKERS_README.md](./CHECKERS_README.md)**

### 快速对比

| 特性 | check_combo.py (新版) | check_combo_compliance.py (旧版) |
|-----|----------------------|--------------------------------|
| 模块化 | ✅ 5个独立检查器 | ❌ 单体结构 |
| 选择性检查 | ✅ 可选择检查器 | ❌ 全部检查 |
| 文档检查 | ✅ | ❌ |
| 测试检查 | ✅ | ❌ |
| 注册检查 | ✅ | ❌ |
| 运行时检查 | ✅ | ❌ |
| 详细报告 | ✅ 分类展示 | ⚠️ 基础报告 |

**推荐：** 新项目使用 `check_combo.py`，快速检查可继续使用 `check_combo_compliance.py`

---

## 旧版简化检查器

`check_combo_compliance.py` 是一个简化的代码规范检查工具，用于检查古真人器官 Combo 技能的基础代码结构。

以 `bian_hua` 族（特别是 `yin_yang` 系列）为标准参考。

## 快速使用

```bash
# 检查整个 bian_hua 族
python scripts/check_combo_compliance.py src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/combo/bian_hua

# 检查特定家族（如 bing_xue）
python scripts/check_combo_compliance.py src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/combo/bing_xue

# 检查单个技能
python scripts/check_combo_compliance.py src/main/java/.../combo/bian_hua/yin_yang/transfer
```

## 检查规范

### 1. 目录结构规范

每个技能目录应包含以下子目录：

**必需目录：**
- `behavior/` - 行为逻辑
- `calculator/` - 纯计算逻辑
- `tuning/` - 调优参数
- `messages/` - 消息文本
- `fx/` - 视觉效果

**可选目录：**
- `state/` - 状态数据
- `runtime/` - 运行时数据

### 2. 文件命名规范

文件应使用以下后缀：
- `*Behavior.java` - 行为文件
- `*Calculator.java` - 计算器文件
- `*Tuning.java` - 调优文件
- `*Messages.java` - 消息文件
- `*Fx.java` - 效果文件
- `*Runtime.java` - 运行时文件
- `*State.java` - 状态文件

### 3. 代码质量规范

**Calculator 规范：**
- 应该使用纯静态方法（纯函数）
- 不应包含实例方法
- 可以包含 `record` 类型作为返回值

**Behavior 规范：**
- 应该包含 `initialize()` 方法
- 用于注册和初始化行为

## 输出说明

检查完成后会显示：

```
📊 检查报告
============================================================

✅ 通过: 96      # 通过的检查项
⚠️  警告: 10     # 警告（不严重）
❌ 问题: 8       # 需要修复的问题

❌ 问题详情:
  ❌ fascia_latch: 缺少目录或文件 messages/
  ...

⚠️  警告详情:
  ⚠️  gui_bian: 文件命名可能不规范 WuxingGuiBianCostService.java
  ...
```

- **通过（✅）**：符合规范的检查项
- **警告（⚠️）**：可能不规范但不严重
- **问题（❌）**：需要修复的问题

## 相比原版的改进

原版 `check_guzhenren_compliance.py` 功能过于复杂，包含了很多检查项和过滤选项。

简化版 `check_combo_compliance.py` 的优势：

1. **更专注**：只检查核心的目录结构和代码规范
2. **更简单**：去除了复杂的过滤逻辑
3. **更快速**：减少了不必要的检查项
4. **更易用**：只需指定目录即可检查
5. **更准确**：修复了原版的一些误报（如 record 类型被识别为非静态方法）

## 开发说明

如果需要自定义检查规则，可以修改 `ComboComplianceChecker` 类中的以下变量：

```python
# 必需的子目录
self.required_dirs = ["behavior", "calculator", "tuning", "messages", "fx"]

# 可选的子目录
self.optional_dirs = ["state", "runtime"]

# 文件后缀规范
self.file_suffixes = {
    "behavior": "Behavior",
    "calculator": "Calculator",
    ...
}
```
