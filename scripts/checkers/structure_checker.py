#!/usr/bin/env python3
"""
基础结构检查器

检查 Combo 技能的目录结构和文件命名规范。
"""

import re
from pathlib import Path
from .base_checker import BaseChecker, Severity


class StructureChecker(BaseChecker):
    """基础结构检查器 - 检查目录结构和文件命名"""

    def __init__(self, project_root: Path):
        super().__init__(project_root)
        self.required_dirs = ["behavior", "calculator", "tuning", "messages", "fx"]
        self.optional_dirs = ["state", "runtime"]
        self.file_suffixes = {
            "behavior": "Behavior",
            "calculator": "Calculator",
            "tuning": "Tuning",
            "messages": "Messages",
            "fx": "Fx",
            "runtime": "Runtime",
            "state": "State"
        }

    @property
    def description(self) -> str:
        return "检查目录结构和文件命名规范"

    def check(self, target_dir: Path) -> 'CheckReport':
        """执行结构检查"""
        if not target_dir.exists():
            self.error(f"目标目录不存在: {target_dir}", category="目录")
            return self.report

        skill_dirs = self.find_skill_directories(target_dir)

        if not skill_dirs:
            self.warning("未找到技能目录", category="目录")
            return self.report

        self.info(f"找到 {len(skill_dirs)} 个技能目录", category="统计")

        for skill_dir in skill_dirs:
            self.check_skill_structure(skill_dir)
            self.check_file_naming(skill_dir)
            self.check_calculator_purity(skill_dir)

        return self.report

    def check_skill_structure(self, skill_dir: Path):
        """检查技能目录结构"""
        skill_name = skill_dir.name

        # 检查必需目录
        for req_dir in self.required_dirs:
            dir_path = skill_dir / req_dir
            if dir_path.exists() and dir_path.is_dir():
                self.passed(f"{skill_name}: 存在目录 {req_dir}/", category="目录结构")
            else:
                # 检查扁平结构
                flat_file = self.find_flat_file(skill_dir, req_dir)
                if flat_file:
                    self.passed(f"{skill_name}: 使用扁平结构 {flat_file.name}",
                              category="目录结构")
                else:
                    self.error(f"{skill_name}: 缺少目录或文件 {req_dir}/",
                             file_path=skill_dir,
                             category="目录结构")

    def find_flat_file(self, skill_dir: Path, dir_type: str) -> Path:
        """查找扁平结构中的文件"""
        suffix = self.file_suffixes.get(dir_type, "")
        if not suffix:
            return None

        for file in skill_dir.iterdir():
            if file.is_file() and file.name.endswith(suffix + ".java"):
                return file
        return None

    def check_file_naming(self, skill_dir: Path):
        """检查文件命名规范"""
        skill_name = skill_dir.name
        java_files = list(skill_dir.rglob("*.java"))

        for java_file in java_files:
            file_name = java_file.name

            # 检查是否符合规范后缀
            has_valid_suffix = any(file_name.endswith(suffix + ".java")
                                  for suffix in self.file_suffixes.values())

            if has_valid_suffix:
                self.passed(f"{skill_name}: 文件命名规范 {file_name}",
                          category="文件命名")
            elif "Util" not in file_name and "common" not in str(java_file):
                self.warning(f"{skill_name}: 文件命名可能不规范 {file_name}",
                           file_path=java_file,
                           category="文件命名")

    def check_calculator_purity(self, skill_dir: Path):
        """检查 Calculator 文件是否为纯函数"""
        skill_name = skill_dir.name
        calculator_files = list(skill_dir.rglob("*Calculator.java"))

        for calc_file in calculator_files:
            try:
                with open(calc_file, 'r', encoding='utf-8') as f:
                    content = f.read()

                # 检查非静态方法（排除 record, class, interface, enum）
                non_static_pattern = r"(?:public|protected)\s+(?!static\s+)(?!class\s+)(?!record\s+)(?!interface\s+)(?!enum\s+)\w+.*?\s+\w+\s*\("
                matches = re.findall(non_static_pattern, content)

                if matches:
                    # 过滤掉构造函数
                    non_constructors = [m for m in matches if calc_file.stem not in m]
                    if non_constructors:
                        self.error(f"{skill_name}: Calculator 包含非静态方法",
                                 file_path=calc_file,
                                 category="代码质量")
                    else:
                        self.passed(f"{skill_name}: Calculator 符合纯函数规范",
                                  category="代码质量")
                else:
                    self.passed(f"{skill_name}: Calculator 符合纯函数规范",
                              category="代码质量")

            except Exception as e:
                self.warning(f"{skill_name}: 无法读取 Calculator 文件 - {e}",
                           file_path=calc_file,
                           category="文件访问")
