#!/usr/bin/env python3
"""
Combo 技能代码规范检查工具

以 bian_hua 族为标准，检查 combo 技能目录结构和代码规范。

使用方法：
python scripts/check_combo_compliance.py [目标目录]
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Set

class ComboComplianceChecker:
    def __init__(self, target_dir: str = None):
        self.project_root = Path.cwd()

        # 默认检查 combo 目录
        if target_dir:
            self.target_dir = Path(target_dir).resolve()
        else:
            self.target_dir = self.project_root / "src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/combo"

        self.issues = []
        self.warnings = []
        self.passed = []

        # 必需的子目录（参考 yin_yang 标准结构）
        self.required_dirs = ["behavior", "calculator", "tuning", "messages", "fx"]

        # 可选的子目录
        self.optional_dirs = ["state", "runtime"]

        # 文件后缀规范
        self.file_suffixes = {
            "behavior": "Behavior",
            "calculator": "Calculator",
            "tuning": "Tuning",
            "messages": "Messages",
            "fx": "Fx",
            "runtime": "Runtime",
            "state": "State"
        }

    def check_all(self) -> Dict[str, int]:
        """执行所有检查"""
        print("🔍 Combo 技能代码规范检查")
        print(f"📂 检查目录: {self.target_dir}")
        print("=" * 60)

        if not self.target_dir.exists():
            print(f"❌ 目录不存在: {self.target_dir}")
            return {"issues": 1, "warnings": 0, "passed": 0}

        # 查找所有技能目录
        skill_dirs = self.find_skill_directories()

        if not skill_dirs:
            print("⚠️  未找到技能目录")
            return {"issues": 0, "warnings": 1, "passed": 0}

        print(f"📊 找到 {len(skill_dirs)} 个技能目录\n")

        # 检查每个技能目录
        for skill_dir in skill_dirs:
            self.check_skill_directory(skill_dir)

        # 生成报告
        return self.generate_report()

    def find_skill_directories(self) -> List[Path]:
        """查找所有技能目录

        技能目录特征：
        - 在族文件夹下（如 bian_hua/yin_yang/transfer）
        - 包含 behavior, calculator 等子目录或对应的 Java 文件
        """
        skill_dirs = []

        # 遍历目标目录，寻找技能文件夹
        for family_dir in self.target_dir.iterdir():
            if not family_dir.is_dir() or family_dir.name.startswith('.'):
                continue

            # 家族下的子类别（如 yin_yang）
            for category_dir in family_dir.iterdir():
                if not category_dir.is_dir() or category_dir.name.startswith('.'):
                    continue

                # 技能目录（如 transfer, tai_ji_swap）
                for potential_skill_dir in category_dir.iterdir():
                    if not potential_skill_dir.is_dir() or potential_skill_dir.name.startswith('.'):
                        continue

                    # 判断是否为技能目录
                    if self.is_skill_directory(potential_skill_dir):
                        skill_dirs.append(potential_skill_dir)

                # 也检查 category_dir 本身（如 yu_qun, yu_shi 的扁平结构）
                if self.is_skill_directory(category_dir):
                    skill_dirs.append(category_dir)

        return sorted(skill_dirs)

    def is_skill_directory(self, dir_path: Path) -> bool:
        """判断是否为技能目录

        技能目录应该：
        1. 包含至少2个必需子目录（behavior, calculator等）
        2. 或者包含至少3个规范命名的Java文件（扁平结构）

        排除：单个子目录（它们是组件目录，不是技能目录）
        """
        # 统计包含的必需子目录数量
        subdirs_count = sum(1 for subdir in self.required_dirs if (dir_path / subdir).exists())

        # 统计符合规范的 Java 文件数量
        files_count = sum(1 for f in dir_path.iterdir()
                         if f.is_file() and any(f.name.endswith(suffix + ".java")
                                               for suffix in self.file_suffixes.values()))

        # 必须有至少2个子目录，或者至少3个规范文件
        return subdirs_count >= 2 or files_count >= 3

    def check_skill_directory(self, skill_dir: Path):
        """检查单个技能目录"""
        skill_name = skill_dir.name
        try:
            relative_path = skill_dir.relative_to(self.project_root)
        except ValueError:
            # 如果无法计算相对路径，使用相对于 target_dir 的路径
            relative_path = skill_dir.relative_to(self.target_dir)

        print(f"\n🎯 检查技能: {relative_path}")

        # 1. 检查目录结构
        self.check_directory_structure(skill_dir, skill_name)

        # 2. 检查文件命名
        self.check_file_naming(skill_dir, skill_name)

        # 3. 检查代码规范
        self.check_code_quality(skill_dir, skill_name)

    def check_directory_structure(self, skill_dir: Path, skill_name: str):
        """检查目录结构"""
        print(f"  📁 目录结构检查...")

        # 检查必需目录
        for req_dir in self.required_dirs:
            dir_path = skill_dir / req_dir
            if dir_path.exists() and dir_path.is_dir():
                self.passed.append(f"✅ {skill_name}: 存在目录 {req_dir}/")
            else:
                # 检查是否使用扁平结构（文件直接在技能目录下）
                flat_file = self.find_flat_file(skill_dir, req_dir)
                if flat_file:
                    self.passed.append(f"✅ {skill_name}: 使用扁平结构 {flat_file.name}")
                else:
                    self.issues.append(f"❌ {skill_name}: 缺少目录或文件 {req_dir}/")

    def find_flat_file(self, skill_dir: Path, dir_type: str) -> Path:
        """查找扁平结构中的文件"""
        suffix = self.file_suffixes.get(dir_type, "")
        if not suffix:
            return None

        # 查找以该后缀结尾的文件
        for file in skill_dir.iterdir():
            if file.is_file() and file.name.endswith(suffix + ".java"):
                return file
        return None

    def check_file_naming(self, skill_dir: Path, skill_name: str):
        """检查文件命名规范"""
        print(f"  📝 文件命名检查...")

        java_files = list(skill_dir.rglob("*.java"))

        for java_file in java_files:
            file_name = java_file.name

            # 检查文件名是否符合规范后缀
            has_valid_suffix = any(file_name.endswith(suffix + ".java")
                                  for suffix in self.file_suffixes.values())

            if has_valid_suffix:
                self.passed.append(f"✅ {skill_name}: 文件命名规范 {file_name}")
            elif "Util" not in file_name and "common" not in str(java_file):
                self.warnings.append(f"⚠️  {skill_name}: 文件命名可能不规范 {file_name}")

    def check_code_quality(self, skill_dir: Path, skill_name: str):
        """检查代码质量"""
        print(f"  🔍 代码规范检查...")

        # 检查 Calculator 文件（应该是纯函数，全部静态方法）
        calculator_files = list(skill_dir.rglob("*Calculator.java"))

        for calc_file in calculator_files:
            try:
                with open(calc_file, 'r', encoding='utf-8') as f:
                    content = f.read()

                # 检查是否有非静态的公共方法（排除 record, class, interface, enum）
                non_static_pattern = r"(?:public|protected)\s+(?!static\s+)(?!class\s+)(?!record\s+)(?!interface\s+)(?!enum\s+)\w+.*?\s+\w+\s*\("
                matches = re.findall(non_static_pattern, content)

                if matches and "Calculator" in calc_file.name:
                    # 过滤掉构造函数
                    non_constructors = [m for m in matches if calc_file.stem not in m]
                    if non_constructors:
                        self.issues.append(f"❌ {skill_name}: Calculator 包含非静态方法 {calc_file.name}")
                    else:
                        self.passed.append(f"✅ {skill_name}: Calculator 符合纯函数规范")
                else:
                    self.passed.append(f"✅ {skill_name}: Calculator 符合纯函数规范")

            except Exception as e:
                self.warnings.append(f"⚠️  {skill_name}: 无法读取文件 {calc_file.name} - {e}")

        # 检查 Behavior 文件（应该有 initialize 方法）
        behavior_files = list(skill_dir.rglob("*Behavior.java"))

        for behavior_file in behavior_files:
            try:
                with open(behavior_file, 'r', encoding='utf-8') as f:
                    content = f.read()

                if 'initialize()' in content or 'initialize(' in content:
                    self.passed.append(f"✅ {skill_name}: Behavior 包含 initialize 方法")
                else:
                    self.warnings.append(f"⚠️  {skill_name}: Behavior 可能缺少 initialize 方法 {behavior_file.name}")

            except Exception as e:
                self.warnings.append(f"⚠️  {skill_name}: 无法读取文件 {behavior_file.name} - {e}")

    def generate_report(self) -> Dict[str, int]:
        """生成检查报告"""
        print("\n" + "=" * 60)
        print("📊 检查报告")
        print("=" * 60)

        issues_count = len(self.issues)
        warnings_count = len(self.warnings)
        passed_count = len(self.passed)

        print(f"\n✅ 通过: {passed_count}")
        print(f"⚠️  警告: {warnings_count}")
        print(f"❌ 问题: {issues_count}")

        if self.issues:
            print(f"\n❌ 问题详情:")
            for issue in self.issues:
                print(f"  {issue}")

        if self.warnings:
            print(f"\n⚠️  警告详情:")
            for warning in self.warnings:
                print(f"  {warning}")

        # 总结
        if issues_count == 0 and warnings_count == 0:
            print(f"\n🎉 所有检查通过！")
            status = "PASS"
        elif issues_count == 0:
            print(f"\n✅ 通过检查，有少量警告")
            status = "PASS"
        else:
            print(f"\n❌ 发现 {issues_count} 个问题，需要修复")
            status = "FAIL"

        return {
            "issues": issues_count,
            "warnings": warnings_count,
            "passed": passed_count,
            "status": status
        }


def main():
    """主函数"""
    import argparse

    parser = argparse.ArgumentParser(
        description="Combo 技能代码规范检查工具 - 以 bian_hua 族为标准检查目录结构和代码规范",
        epilog="""
示例用法:
  # 检查整个 bian_hua 族
  python scripts/check_combo_compliance.py src/main/java/.../combo/bian_hua

  # 检查特定技能
  python scripts/check_combo_compliance.py src/main/java/.../combo/bian_hua/yin_yang/transfer

  # 检查所有 combo（在项目根目录运行）
  python scripts/check_combo_compliance.py

规范说明:
  ✅ 必需目录: behavior/, calculator/, tuning/, messages/, fx/
  ✅ 可选目录: state/, runtime/
  ✅ Calculator 文件应使用纯静态方法
  ✅ Behavior 文件应包含 initialize() 方法
        """,
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("target_dir", nargs='?',
                       help="检查目标目录（默认检查所有 combo）")

    args = parser.parse_args()

    checker = ComboComplianceChecker(args.target_dir)
    result = checker.check_all()

    # 设置退出码
    sys.exit(0 if result["status"] == "PASS" else 1)


if __name__ == "__main__":
    main()
