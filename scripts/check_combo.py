#!/usr/bin/env python3
"""
Combo 技能代码规范检查主工具

协调多个专门检查器，提供统一的检查入口。
"""

import sys
import argparse
from pathlib import Path
from typing import List

# 导入所有检查器
from checkers.structure_checker import StructureChecker
from checkers.documentation_checker import DocumentationChecker
from checkers.test_checker import TestChecker
from checkers.registration_checker import RegistrationChecker
from checkers.runtime_checker import RuntimeChecker
from checkers.base_checker import BaseChecker


class ComboCheckCoordinator:
    """检查器协调器"""

    def __init__(self, project_root: Path):
        self.project_root = project_root
        self.checkers = {}
        self.register_checkers()

    def register_checkers(self):
        """注册所有检查器"""
        self.checkers = {
            'structure': StructureChecker(self.project_root),
            'documentation': DocumentationChecker(self.project_root),
            'test': TestChecker(self.project_root),
            'registration': RegistrationChecker(self.project_root),
            'runtime': RuntimeChecker(self.project_root),
        }

    def run_checks(self, target_dir: Path, selected_checkers: List[str] = None,
                   verbose: bool = True) -> dict:
        """运行检查"""
        if selected_checkers is None:
            selected_checkers = list(self.checkers.keys())

        print(f"🔍 Combo 技能代码规范检查")
        print(f"📂 目标目录: {target_dir}")
        print(f"📋 启用检查器: {', '.join(selected_checkers)}")
        print("=" * 60)

        results = {}
        total_errors = 0
        total_warnings = 0
        total_passed = 0

        # 运行每个选定的检查器
        for checker_name in selected_checkers:
            if checker_name not in self.checkers:
                print(f"⚠️  未知检查器: {checker_name}")
                continue

            checker = self.checkers[checker_name]
            print(f"\n▶ 运行 {checker.name}...")

            report = checker.check(target_dir)
            results[checker_name] = report

            if verbose:
                checker.print_report(verbose=True)

            total_errors += report.error_count
            total_warnings += report.warning_count
            total_passed += report.pass_count

        # 总体报告
        print(f"\n{'='*60}")
        print(f"📊 总体检查报告")
        print(f"{'='*60}")
        print(f"\n✅ 总通过: {total_passed}")
        print(f"⚠️  总警告: {total_warnings}")
        print(f"❌ 总错误: {total_errors}")

        # 状态判断
        if total_errors == 0 and total_warnings == 0:
            print(f"\n🎉 所有检查完美通过！")
            status = "PASS"
        elif total_errors == 0:
            print(f"\n✅ 检查通过，有 {total_warnings} 个警告建议关注")
            status = "WARN"
        else:
            print(f"\n❌ 发现 {total_errors} 个错误，需要修复")
            status = "FAIL"

        return {
            "status": status,
            "errors": total_errors,
            "warnings": total_warnings,
            "passed": total_passed,
            "results": results
        }

    def list_checkers(self):
        """列出所有可用的检查器"""
        print("📋 可用的检查器：\n")
        for name, checker in self.checkers.items():
            print(f"  {name:15} - {checker.description}")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(
        description="Combo 技能代码规范检查工具 - 模块化检查器系统",
        epilog="""
示例用法:
  # 运行所有检查器
  python scripts/check_combo.py src/main/java/.../combo/bian_hua

  # 只运行特定检查器
  python scripts/check_combo.py path/to/combo --checkers structure,test

  # 列出所有可用检查器
  python scripts/check_combo.py --list

检查器说明:
  structure      - 基础结构：目录结构、文件命名、Calculator纯函数
  documentation  - 文档规范：JSON文档存在性和完整性
  test          - 测试检查：单元测试存在性和质量
  registration  - 注册检查：ComboSkillRegistry注册状态
  runtime       - 运行时规范：资源消耗、冷却管理、Toast提示
        """,
        formatter_class=argparse.RawDescriptionHelpFormatter
    )

    parser.add_argument("target_dir", nargs='?',
                       help="检查目标目录（默认检查整个 combo 目录）")
    parser.add_argument("--checkers", "-c",
                       help="指定要运行的检查器，逗号分隔（如: structure,test）")
    parser.add_argument("--list", "-l", action="store_true",
                       help="列出所有可用的检查器")
    parser.add_argument("--quiet", "-q", action="store_true",
                       help="静默模式，只显示总结")

    args = parser.parse_args()

    # 确定项目根目录
    project_root = Path.cwd()

    # 创建协调器
    coordinator = ComboCheckCoordinator(project_root)

    # 列出检查器
    if args.list:
        coordinator.list_checkers()
        sys.exit(0)

    # 确定目标目录
    if args.target_dir:
        target_dir = Path(args.target_dir).resolve()
    else:
        target_dir = project_root / "src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/combo"

    if not target_dir.exists():
        print(f"❌ 目标目录不存在: {target_dir}")
        sys.exit(1)

    # 确定要运行的检查器
    selected_checkers = None
    if args.checkers:
        selected_checkers = [c.strip() for c in args.checkers.split(',')]

    # 运行检查
    results = coordinator.run_checks(
        target_dir,
        selected_checkers=selected_checkers,
        verbose=not args.quiet
    )

    # 设置退出码
    exit_code = 0 if results["status"] == "PASS" else 1
    sys.exit(exit_code)


if __name__ == "__main__":
    main()
