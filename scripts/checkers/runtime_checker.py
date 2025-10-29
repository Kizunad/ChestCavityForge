#!/usr/bin/env python3
"""
运行时规范检查器

检查技能代码是否遵循运行时规范，如资源消耗、冷却管理等。
"""

import re
from pathlib import Path
from .base_checker import BaseChecker, Severity


class RuntimeChecker(BaseChecker):
    """运行时规范检查器 - 检查资源、冷却等运行时规范"""

    @property
    def description(self) -> str:
        return "检查资源消耗、冷却管理等运行时规范"

    def check(self, target_dir: Path) -> 'CheckReport':
        """执行运行时规范检查"""
        skill_dirs = self.find_skill_directories(target_dir)

        if not skill_dirs:
            self.warning("未找到技能目录", category="目录")
            return self.report

        for skill_dir in skill_dirs:
            self.check_skill_runtime(skill_dir)

        return self.report

    def check_skill_runtime(self, skill_dir: Path):
        """检查单个技能的运行时规范"""
        skill_name = skill_dir.name

        # 查找 Behavior 文件
        behavior_files = list(skill_dir.rglob("*Behavior.java"))
        if not behavior_files:
            return

        for behavior_file in behavior_files:
            self.check_behavior_runtime(skill_name, behavior_file)

    def check_behavior_runtime(self, skill_name: str, behavior_file: Path):
        """检查 Behavior 文件的运行时规范"""
        try:
            with open(behavior_file, 'r', encoding='utf-8') as f:
                content = f.read()

            # 检查资源消耗规范
            self.check_resource_consumption(skill_name, content, behavior_file)

            # 检查冷却管理规范
            self.check_cooldown_management(skill_name, content, behavior_file)

            # 检查 Toast 提示
            self.check_toast_notification(skill_name, content, behavior_file)

            # 检查 activate 方法
            self.check_activate_method(skill_name, content, behavior_file)

        except Exception as e:
            self.warning(f"{skill_name}: 无法读取 Behavior 文件 - {e}",
                       file_path=behavior_file,
                       category="文件访问")

    def check_resource_consumption(self, skill_name: str, content: str, file_path: Path):
        """检查资源消耗规范"""
        # 检查是否使用 ComboSkillUtil.tryPayCost
        if 'tryPayCost' in content:
            self.passed(f"{skill_name}: 使用 ComboSkillUtil.tryPayCost() 处理资源",
                      category="资源消耗")
        elif any(keyword in content for keyword in ['consume', 'cost', 'pay', '消耗']):
            # 有资源消耗相关代码但未使用标准方法
            self.warning(f"{skill_name}: 建议使用 ComboSkillUtil.tryPayCost() 统一处理资源",
                       file_path=file_path,
                       category="资源消耗")

    def check_cooldown_management(self, skill_name: str, content: str, file_path: Path):
        """检查冷却管理规范"""
        # 检查是否使用 MultiCooldown
        if 'MultiCooldown' in content:
            self.passed(f"{skill_name}: 使用 MultiCooldown 管理冷却",
                      category="冷却管理")

            # 检查是否正确设置冷却
            if 'setCooldown' in content or 'setReady' in content:
                self.passed(f"{skill_name}: 正确设置冷却时间",
                          category="冷却管理")
        else:
            # 检查是否有手动时间戳管理
            manual_patterns = [
                r'System\.currentTimeMillis',
                r'gameTime\s*[+\-]',
                r'setReadyAt'
            ]
            has_manual_cooldown = any(re.search(pattern, content) for pattern in manual_patterns)

            if has_manual_cooldown:
                self.error(f"{skill_name}: 使用手动时间戳管理冷却，应改用 MultiCooldown",
                         file_path=file_path,
                         category="冷却管理")
            elif any(keyword in content for keyword in ['cooldown', '冷却']):
                self.warning(f"{skill_name}: 建议使用 MultiCooldown 统一管理冷却",
                           file_path=file_path,
                           category="冷却管理")

    def check_toast_notification(self, skill_name: str, content: str, file_path: Path):
        """检查 Toast 提示"""
        # 检查是否调用 scheduleReadyToast
        if 'scheduleReadyToast' in content:
            self.passed(f"{skill_name}: 调用 scheduleReadyToast() 提示冷却就绪",
                      category="用户反馈")
        elif 'MultiCooldown' in content:
            # 使用了冷却但没有 Toast 提示
            self.warning(f"{skill_name}: 建议调用 scheduleReadyToast() 提示玩家冷却就绪",
                       file_path=file_path,
                       category="用户反馈")

    def check_activate_method(self, skill_name: str, content: str, file_path: Path):
        """检查 activate 方法"""
        # 查找 activate 方法
        activate_pattern = r'(?:private|public)\s+static\s+\w+\s+activate\s*\('

        if re.search(activate_pattern, content):
            self.passed(f"{skill_name}: 包含 activate() 方法作为入口",
                      category="代码结构")

            # 检查 activate 方法结构
            self.check_activate_structure(skill_name, content, file_path)
        else:
            self.info(f"{skill_name}: 未找到标准 activate() 方法",
                    category="代码结构")

    def check_activate_structure(self, skill_name: str, content: str, file_path: Path):
        """检查 activate 方法的结构"""
        # 提取 activate 方法体（简化处理）
        activate_match = re.search(
            r'activate\s*\([^)]*\)\s*\{',
            content
        )

        if not activate_match:
            return

        # 从 activate 开始的内容
        start_pos = activate_match.start()
        remaining_content = content[start_pos:start_pos + 2000]  # 检查前2000字符

        # 检查是否有前置检查（冷却、资源）
        has_precondition = any(keyword in remaining_content
                              for keyword in ['if', 'check', 'can', 'isReady'])

        if has_precondition:
            self.passed(f"{skill_name}: activate() 包含前置检查",
                      category="代码结构")
        else:
            self.warning(f"{skill_name}: activate() 可能缺少前置检查",
                       file_path=file_path,
                       category="代码结构")

        # 检查是否调用 Calculator
        if 'Calculator.' in remaining_content or 'calculator.' in remaining_content:
            self.passed(f"{skill_name}: activate() 调用 Calculator 计算",
                      category="代码结构")

        # 检查是否播放效果
        if 'Fx.' in remaining_content or 'fx.' in remaining_content or 'playSound' in remaining_content:
            self.passed(f"{skill_name}: activate() 播放效果",
                      category="代码结构")
