#!/usr/bin/env python3
"""
注册检查器

检查技能是否在 ComboSkillRegistry 中正确注册。
"""

import re
from pathlib import Path
from .base_checker import BaseChecker, Severity


class RegistrationChecker(BaseChecker):
    """注册检查器 - 检查 ComboSkillRegistry 注册"""

    @property
    def description(self) -> str:
        return "检查技能在 ComboSkillRegistry 中的注册"

    def check(self, target_dir: Path) -> 'CheckReport':
        """执行注册检查"""
        skill_dirs = self.find_skill_directories(target_dir)

        if not skill_dirs:
            self.warning("未找到技能目录", category="目录")
            return self.report

        # 读取 ComboSkillRegistry
        registry_file = self.find_registry_file()
        if not registry_file:
            self.error("未找到 ComboSkillRegistry.java",
                     category="注册检查")
            return self.report

        registry_content = self.read_registry(registry_file)
        if not registry_content:
            return self.report

        # 检查每个技能
        for skill_dir in skill_dirs:
            self.check_skill_registration(skill_dir, registry_content, registry_file)

        return self.report

    def find_registry_file(self) -> Path:
        """查找 ComboSkillRegistry 文件"""
        # 搜索整个源码目录
        candidates = list(self.java_source_dir.rglob("ComboSkillRegistry.java"))
        return candidates[0] if candidates else None

    def read_registry(self, registry_file: Path) -> str:
        """读取注册表文件"""
        try:
            with open(registry_file, 'r', encoding='utf-8') as f:
                return f.read()
        except Exception as e:
            self.error(f"无法读取 ComboSkillRegistry - {e}",
                     file_path=registry_file,
                     category="文件访问")
            return ""

    def check_skill_registration(self, skill_dir: Path, registry_content: str, registry_file: Path):
        """检查单个技能的注册"""
        skill_name = skill_dir.name

        # 查找 Behavior 文件
        behavior_files = list(skill_dir.rglob("*Behavior.java"))
        if not behavior_files:
            return

        behavior_file = behavior_files[0]

        # 检查 Behavior 是否有 initialize() 方法
        try:
            with open(behavior_file, 'r', encoding='utf-8') as f:
                behavior_content = f.read()

            if 'initialize()' not in behavior_content and 'initialize(' not in behavior_content:
                self.error(f"{skill_name}: Behavior 缺少 initialize() 方法",
                         file_path=behavior_file,
                         category="Behavior 实现")
                return
            else:
                self.passed(f"{skill_name}: Behavior 有 initialize() 方法",
                          category="Behavior 实现")

        except Exception as e:
            self.warning(f"{skill_name}: 无法读取 Behavior 文件 - {e}",
                       file_path=behavior_file,
                       category="文件访问")
            return

        # 检查是否在 Registry 中注册
        behavior_class_name = behavior_file.stem
        if f"{behavior_class_name}::initialize" in registry_content:
            self.passed(f"{skill_name}: 已在 ComboSkillRegistry 中注册",
                      category="注册状态")
        else:
            self.error(f"{skill_name}: 未在 ComboSkillRegistry 中注册",
                     file_path=behavior_file,
                     category="注册状态")

        # 检查注册是否包含必需信息
        self.check_registration_details(skill_name, behavior_class_name,
                                       registry_content, registry_file)

    def check_registration_details(self, skill_name: str, behavior_class: str,
                                  registry_content: str, registry_file: Path):
        """检查注册详情"""
        # 查找该技能的 register 调用
        # 匹配模式: register(..., BehaviorClass::initialize, ...)
        pattern = rf'register\([^)]*{behavior_class}::initialize[^)]*\)'
        matches = re.findall(pattern, registry_content, re.DOTALL)

        if not matches:
            return

        registration_call = matches[0]

        # 检查是否有 iconLocation
        if 'iconLocation' in registration_call or '.icon(' in registration_call:
            self.passed(f"{skill_name}: 注册包含图标信息",
                      category="注册详情")
        else:
            self.warning(f"{skill_name}: 注册可能缺少 iconLocation",
                       file_path=registry_file,
                       category="注册详情")

        # 检查是否有显示名称
        if '.displayName(' in registration_call or 'displayName:' in registration_call:
            self.passed(f"{skill_name}: 注册包含显示名称",
                      category="注册详情")
        else:
            self.warning(f"{skill_name}: 注册可能缺少 displayName",
                       file_path=registry_file,
                       category="注册详情")

        # 检查是否有描述
        if '.description(' in registration_call or 'description:' in registration_call:
            self.passed(f"{skill_name}: 注册包含描述",
                      category="注册详情")
        else:
            self.warning(f"{skill_name}: 注册可能缺少 description",
                       file_path=registry_file,
                       category="注册详情")
