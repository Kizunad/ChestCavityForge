# Combo 检查器快速开始

## 30 秒快速开始

```bash
# 1. 列出所有检查器
python scripts/check_combo.py --list

# 2. 检查整个家族
python scripts/check_combo.py src/main/java/.../combo/bian_hua

# 3. 只检查结构
python scripts/check_combo.py path/to/combo --checkers structure

# 4. 检查多个方面
python scripts/check_combo.py path/to/combo --checkers structure,test,registration
```

## 检查器选择指南

### 🚀 开发新技能

```bash
# 阶段 1: 创建目录后
check_combo.py new_skill --checkers structure

# 阶段 2: 编写代码后
check_combo.py new_skill --checkers structure,test

# 阶段 3: 集成前
check_combo.py new_skill  # 运行全部检查
```

### 🔍 代码审查

```bash
# 快速检查
check_combo.py changed_dir --checkers structure,runtime

# 全面检查
check_combo.py changed_dir
```

### 🛠️ 重构旧代码

```bash
# 重点检查运行时规范
check_combo.py old_skill --checkers runtime,registration
```

## 5个检查器说明

| 检查器 | 用途 | 何时使用 |
|-------|------|---------|
| **structure** | 目录、文件、Calculator | 🟢 开发初期，每次修改结构 |
| **documentation** | JSON文档 | 🟡 开发后期，准备发布 |
| **test** | 单元测试 | 🟢 TDD流程，持续验证 |
| **registration** | 注册状态 | 🟡 集成前，确保可加载 |
| **runtime** | 资源、冷却、Toast | 🟢 代码审查，规范检查 |

🟢 = 频繁使用
🟡 = 阶段性使用

## 常见问题

**Q: 哪个检查器最重要？**
A: `structure` - 基础结构正确是其他一切的前提

**Q: 检查器报错但我认为代码没问题？**
A: 检查器基于 AGENTS.md 规范，如果规范需要调整，请更新文档

**Q: 如何集成到 Git Hook？**
A: 创建 `.git/hooks/pre-commit`:
```bash
#!/bin/bash
python scripts/check_combo.py src/.../combo/ --quiet
```

**Q: 运行太慢？**
A: 使用 `--checkers` 只运行需要的检查器

## 详细文档

完整文档：[CHECKERS_README.md](./CHECKERS_README.md)

AGENTS.md 规范：[src/.../combo/AGENTS.md](../src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/combo/AGENTS.md)
