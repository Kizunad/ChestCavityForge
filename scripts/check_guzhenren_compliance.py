#!/usr/bin/env python3
"""
古真人器官系统代码规范检查工具

检查项目根目录下的古真人器官代码是否符合规范，包括：
1. Combo 目录结构规范
2. 文件命名规范
3. 代码结构规范
4. 注册规范
5. 资源操作规范

支持指定检查范围：
- 特定目录检查
- 包名过滤
- 家族过滤

使用方法：
python scripts/check_guzhenren_compliance.py [目标目录] --target <过滤模式>
"""

import os
import re
import sys
import json
import fnmatch
from pathlib import Path
from typing import List, Dict, Set, Tuple, Optional

class GuzhenrenComplianceChecker:
    def __init__(self, project_root: str, target_filter: Optional[str] = None):
        self.project_root = Path(project_root)
        self.java_source_dir = self.project_root / "src" / "main" / "java"
        self.target_filter = target_filter
        self.issues = []
        self.warnings = []
        self.passed_checks = []
        self.files_checked = []
        self.files_filtered = []
        
        # 规范定义
        self.required_combo_dirs = [
            "behavior", "calculator", "tuning", "messages", "fx"
        ]
        self.optional_combo_dirs = ["state", "runtime"]
        
        self.required_file_suffixes = [
            "Calculator", "Tuning", "Messages", "Fx", "Behavior"
        ]
        
        self.required_imports = {
            "LedgerOps": "util/behavior/LedgerOps.java",
            "ResourceOps": "util/behavior/ResourceOps.java", 
            "MultiCooldown": "util/behavior/MultiCooldown.java",
            "AbsorptionHelper": "util/behavior/AbsorptionHelper.java",
            "DoTEngine": "util/dot/DoTEngine.java",
            "DoTTypes": "util/dot/DoTTypes.java",
            "ReactionRegistry": "util/reaction/ReactionRegistry.java",
        }
        
        self.banned_patterns = [
            r"LinkageManager\s*\.",
            r"new\s+DamageSource",
            r"\.hurt\(",
            r"DirectExecutor\.INSTANCE",
            r"FutureTask",
        ]
        
        self.organ_activation_pattern = r"OrganActivationListeners\s*\.\s*register"
        self.combo_skill_registry_pattern = r"ComboSkillRegistry\s*\.\s*register"

    def check_all(self) -> Dict[str, int]:
        """执行所有检查"""
        print("🔍 开始检查古真人器官系统代码规范...")
        print(f"📂 项目根目录: {self.project_root}")
        if self.target_filter:
            print(f"🎯 检查范围: {self.target_filter}")
        
        # 根据过滤条件获取文件
        target_files = self.get_target_files()
        if not target_files:
            print("⚠️  未找到符合检查条件的文件")
            return {"issues": 0, "warnings": 0, "passed": 0, "status": "PASS"}
        
        print(f"📊 找到 {len(target_files)} 个文件需要检查")
        
        # 检查 Combo 目录
        self.check_combo_directories()
        
        # 检查文件命名规范
        self.check_file_naming()
        
        # 检查代码规范
        self.check_code_compliance()
        
        # 检查注册规范
        self.check_registration_compliance()
        
        # 检查资源操作规范
        self.check_resource_operations()
        
        # 生成报告
        return self.generate_report()

    def get_target_files(self) -> List[Path]:
        """根据过滤条件获取目标文件"""
        if not self.target_filter:
            # 没有过滤条件，返回所有 Java 文件
            return list(self.java_source_dir.rglob("*.java"))
        
        # 解析过滤模式
        all_files = list(self.java_source_dir.rglob("*.java"))
        target_files = []
        
        # 支持多种过滤模式
        filter_patterns = self.parse_filter_pattern(self.target_filter)
        
        for file_path in all_files:
            relative_path = file_path.relative_to(self.java_source_dir)
            path_str = str(relative_path)
            
            # 检查是否符合任何过滤模式
            for pattern in filter_patterns:
                if self.matches_filter(path_str, pattern):
                    target_files.append(file_path)
                    self.files_filtered.append(f"包含: {path_str}")
                    break
        
        return target_files

    def parse_filter_pattern(self, target_filter: str) -> List[str]:
        """解析过滤模式"""
        patterns = []
        
        # 逗号分隔的多个模式
        if ',' in target_filter:
            patterns.extend(p.strip() for p in target_filter.split(','))
        else:
            patterns.append(target_filter.strip())
        
        # 标准化模式
        normalized_patterns = []
        for pattern in patterns:
            if pattern:
                # 如果不是路径分隔符模式，自动添加路径前缀
                if '/' not in pattern and '\\' not in pattern:
                    pattern = f"**/{pattern}/**"
                normalized_patterns.append(pattern)
        
        return normalized_patterns

    def matches_filter(self, path_str: str, pattern: str) -> bool:
        """检查路径是否匹配过滤模式"""
        try:
            # 处理通配符匹配
            if '*' in pattern or '?' in pattern:
                return fnmatch.fnmatch(path_str, pattern)
            else:
                # 字符串包含检查
                return pattern.lower() in path_str.lower()
        except Exception:
            return False

    def check_combo_directories(self):
        """检查 Combo 目录结构"""
        print("📁 检查 Combo 目录结构...")
        
        # 获取目标 Combo 目录
        if self.target_filter:
            combo_dirs = []
            for java_file in self.get_target_files():
                if 'combo' in str(java_file).lower():
                    combo_dirs.extend(java_file.parent.parents)
        else:
            combo_dirs = list(self.java_source_dir.glob("**/combo"))
        
        for combo_dir in set(combo_dirs):  # 去重
            if combo_dir.exists():
                self.check_single_combo_structure(combo_dir)

    def check_single_combo_structure(self, combo_dir: Path):
        """检查单个 Combo 目录结构"""
        family_dirs = [d for d in combo_dir.iterdir() if d.is_dir() and not d.name.startswith('.')]
        
        for family_dir in family_dirs:
            skill_dirs = [d for d in family_dir.iterdir() if d.is_dir()]
            
            for skill_dir in skill_dirs:
                self.check_skill_directory_structure(skill_dir)

    def check_skill_directory_structure(self, skill_dir: Path):
        """检查技能目录结构"""
        skill_name = skill_dir.name
        relative_path = skill_dir.relative_to(self.java_source_dir)
        
        # 跳过不在检查范围内的目录
        if self.target_filter and not any(self.matches_filter(str(relative_path), pattern) 
                                        for pattern in self.parse_filter_pattern(self.target_filter)):
            return
        
        # 检查必需目录
        for required_dir in self.required_combo_dirs:
            dir_path = skill_dir / required_dir
            if not dir_path.exists():
                self.issues.append(f"❌ 缺少必需目录: {dir_path}")
            else:
                self.passed_checks.append(f"✅ 目录存在: {dir_path}")
        
        # 检查可选目录
        for optional_dir in self.optional_combo_dirs:
            dir_path = skill_dir / optional_dir
            if dir_path.exists():
                self.passed_checks.append(f"✅ 可选目录存在: {dir_path}")

    def check_file_naming(self):
        """检查文件命名规范"""
        print("📝 检查文件命名规范...")
        
        target_files = self.get_target_files()
        for java_file in target_files:
            if 'combo' in str(java_file).lower():
                self.check_single_file_naming(java_file)

    def check_single_file_naming(self, file_path: Path):
        """检查单个文件的命名规范"""
        if file_path.is_file():
            file_name = file_path.name
            relative_path = file_path.relative_to(self.java_source_dir)
            
            self.files_checked.append(f"📄 {relative_path}")
            
            # 检查是否包含必需的后缀
            has_required_suffix = any(suffix in file_name for suffix in self.required_file_suffixes)
            
            # 检查 Calculator 文件是否为纯函数
            if "Calculator" in file_name:
                self.check_calculator_pure_functions(file_path)
            
            # 检查 Behavior 文件
            if "Behavior" in file_name:
                self.check_behavior_structure(file_path)
            
            if not has_required_suffix and "test" not in str(file_path).lower():
                self.warnings.append(f"⚠️  文件命名可能不符合规范: {file_path}")

    def check_calculator_pure_functions(self, file_path: Path):
        """检查 Calculator 文件是否为纯函数"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 检查是否有非静态方法（非私有）
            method_pattern = r"(?:public|protected)\s+(?!static\s+)(?:[\w<>,\s])+\s+(\w+)\s*\("
            non_static_methods = re.findall(method_pattern, content)
            
            if non_static_methods:
                self.issues.append(f"❌ Calculator 文件包含非静态方法: {file_path}, 方法: {non_static_methods}")
            else:
                self.passed_checks.append(f"✅ Calculator 文件符合纯函数规范: {file_path}")
                
        except Exception as e:
            self.warnings.append(f"⚠️  无法读取文件: {file_path}, 错误: {e}")

    def check_behavior_structure(self, file_path: Path):
        """检查 Behavior 文件结构"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 检查是否有 initialize 方法
            if 'initialize()' not in content:
                self.issues.append(f"❌ Behavior 文件缺少 initialize() 方法: {file_path}")
            else:
                self.passed_checks.append(f"✅ Behavior 文件包含 initialize() 方法: {file_path}")
            
            # 检查是否有 activate 方法
            if 'activate' not in content:
                self.warnings.append(f"⚠️  Behavior 文件可能缺少 activate 方法: {file_path}")
                
        except Exception as e:
            self.warnings.append(f"⚠️  无法读取文件: {file_path}, 错误: {e}")

    def check_code_compliance(self):
        """检查代码规范"""
        print("🔍 检查代码规范...")
        
        target_files = self.get_target_files()
        for java_file in target_files:
            self.check_single_file_compliance(java_file)

    def check_single_file_compliance(self, file_path: Path):
        """检查单个文件的代码规范"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 检查是否包含禁止的模式
            for pattern in self.banned_patterns:
                if re.search(pattern, content):
                    self.issues.append(f"❌ 发现禁止的模式 '{pattern}': {file_path}")
            
            # 检查 DoT 规范
            self.check_dot_compliance(file_path, content)
            
            # 检查资源操作规范
            self.check_resource_usage(file_path, content)
            
        except Exception as e:
            self.warnings.append(f"⚠️  无法读取文件: {file_path}, 错误: {e}")

    def check_dot_compliance(self, file_path: Path, content: str):
        """检查 DoT 规范"""
        # 检查 DoTEngine 调用
        dot_engine_calls = re.findall(r"DoTEngine\.schedulePerSecond\([^)]*\)", content)
        
        for call in dot_engine_calls:
            # 检查是否包含 typeId
            if 'typeId=' not in call and 'DoTTypes' not in call:
                self.issues.append(f"❌ DoT 调用缺少 typeId: {file_path}, 调用: {call}")
        
        # 检查是否有不带 typeId 的 DoTEngine 调用
        if 'DoTEngine.schedulePerSecond(' in content and 'typeId=' not in content:
            self.issues.append(f"❌ 发现缺少 typeId 的 DoTEngine 调用: {file_path}")

    def check_resource_usage(self, file_path: Path, content: str):
        """检查资源使用规范"""
        # 检查直接使用 LinkageManager
        if 'LinkageManager.' in content:
            self.issues.append(f"❌ 直接使用 LinkageManager: {file_path}")
        
        # 检查真元消耗规范
        if 'tryConsumeScaledZhenyuan' in content:
            # 检查参数数量，避免传入 baseCost
            calls = re.findall(r"tryConsumeScaledZhenyuan\s*\([^)]+\)", content)
            for call in calls:
                param_count = call.count(',') + 1
                if param_count == 2:  # 只有两个参数，可能是 baseCost 调用
                    self.warnings.append(f"⚠️  可能使用了 baseCost 调用方式: {file_path}, 调用: {call}")

    def check_registration_compliance(self):
        """检查注册规范"""
        print("📋 检查注册规范...")
        
        target_files = self.get_target_files()
        for java_file in target_files:
            self.check_single_registration_compliance(java_file)

    def check_single_registration_compliance(self, file_path: Path):
        """检查单个文件的注册规范"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 检查 ComboSkillRegistry 注册
            if 'combo' in str(file_path).lower():
                if not re.search(self.combo_skill_registry_pattern, content):
                    self.warnings.append(f"⚠️  Combo 文件可能未在 ComboSkillRegistry 中注册: {file_path}")
                
                # 检查 Behavior 是否有 initialize 方法
                if 'Behavior' in file_path.name and 'initialize()' not in content:
                    self.issues.append(f"❌ Behavior 文件缺少 initialize() 方法: {file_path}")
            
            # 检查器官激活注册
            if re.search(self.organ_activation_pattern, content):
                if 'enum' not in content:
                    self.warnings.append(f"⚠️  器官激活注册可能不在 enum 单例中: {file_path}")
                
        except Exception as e:
            self.warnings.append(f"⚠️  无法读取文件: {file_path}, 错误: {e}")

    def check_resource_operations(self):
        """检查资源操作规范"""
        print("⚡ 检查资源操作规范...")
        
        # 根据目标过滤决定检查范围
        if self.target_filter:
            ops_files = []
            for java_file in self.get_target_files():
                if 'util' in str(java_file) and 'behavior' in str(java_file):
                    ops_files.append(java_file)
        else:
            ops_files = list(self.java_source_dir.rglob("**/util/behavior/*.java"))
        
        for ops_file in ops_files:
            self.check_single_ops_file(ops_file)

    def check_single_ops_file(self, file_path: Path):
        """检查单个 Ops 文件"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 检查是否正确使用了助手
            file_name = file_path.name
            
            if 'LedgerOps' in file_name:
                # 检查是否直接操作 LinkageChannel
                if 'LinkageChannel' in content and 'adjust' not in content:
                    self.issues.append(f"❌ LedgerOps 文件可能直接操作 LinkageChannel: {file_path}")
            
            elif 'MultiCooldown' in file_name:
                # 检查是否使用时间戳而非 MultiCooldown
                timestamp_patterns = [
                    r"System\.currentTimeMillis",
                    r"gameTime\s*[\+\-]",
                    r"setReadyAt"
                ]
                for pattern in timestamp_patterns:
                    if re.search(pattern, content) and 'MultiCooldown' not in content:
                        self.issues.append(f"❌ MultiCooldown 文件可能使用手动时间戳: {file_path}")
                        
        except Exception as e:
            self.warnings.append(f"⚠️  无法读取文件: {file_path}, 错误: {e}")

    def generate_report(self) -> Dict[str, int]:
        """生成检查报告"""
        print("\n" + "="*60)
        print("📊 古真人器官系统代码规范检查报告")
        print("="*60)
        
        # 显示检查范围
        if self.target_filter:
            print(f"🎯 检查范围: {self.target_filter}")
        else:
            print("🎯 检查范围: 全项目")
        
        print(f"📁 文件统计:")
        print(f"   检查文件数: {len(self.files_checked)}")
        if self.files_filtered:
            print(f"   过滤详情:")
            for filtered_info in self.files_filtered[:10]:  # 只显示前10个
                print(f"     {filtered_info}")
            if len(self.files_filtered) > 10:
                print(f"     ... 还有 {len(self.files_filtered) - 10} 个")
        
        issues_count = len(self.issues)
        warnings_count = len(self.warnings)
        passed_count = len(self.passed_checks)
        
        print(f"\n📈 检查结果:")
        print(f"✅ 通过检查: {passed_count}")
        print(f"⚠️  警告: {warnings_count}")
        print(f"❌ 问题: {issues_count}")
        
        if self.issues:
            print(f"\n❌ 问题详情 ({issues_count}):")
            for issue in self.issues:
                print(f"  {issue}")
        
        if self.warnings:
            print(f"\n⚠️  警告详情 ({warnings_count}):")
            for warning in self.warnings[:20]:  # 只显示前20个警告
                print(f"  {warning}")
            if len(self.warnings) > 20:
                print(f"  ... 还有 {len(self.warnings) - 20} 个警告")
        
        if self.passed_checks and issues_count == 0:
            print(f"\n✅ 优秀实践示例:")
            for check in self.passed_checks[:10]:  # 只显示前10个
                print(f"  {check}")
        
        # 状态总结
        if issues_count == 0:
            print(f"\n🎉 恭喜！所有检查都通过了！")
            status = "PASS"
        elif issues_count < 5:
            print(f"\n⚠️  存在少量问题，建议修复")
            status = "WARN"
        else:
            print(f"\n❌ 存在较多问题，需要重点关注")
            status = "FAIL"
        
        return {
            "issues": issues_count,
            "warnings": warnings_count,
            "passed": passed_count,
            "status": status
        }

    def export_report(self, output_file: str):
        """导出详细报告到文件"""
        report = {
            "timestamp": str(Path().cwd()),
            "project_root": str(self.project_root),
            "target_filter": self.target_filter,
            "issues": self.issues,
            "warnings": self.warnings,
            "passed_checks": self.passed_checks,
            "files_checked": self.files_checked,
            "files_filtered": self.files_filtered,
            "summary": {
                "issues_count": len(self.issues),
                "warnings_count": len(self.warnings),
                "passed_count": len(self.passed_checks),
                "files_checked_count": len(self.files_checked),
                "files_filtered_count": len(self.files_filtered)
            }
        }
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        print(f"\n📄 详细报告已导出到: {output_file}")


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description="古真人器官系统代码规范检查工具")
    parser.add_argument("project_root", nargs='?', default=".", 
                       help="项目根目录路径（默认当前目录）")
    parser.add_argument("--target", "-t", 
                       help="检查目标过滤模式，支持：\n"
                           "  家族名称 (如 'bian_hua', 'bing_xue')\n"
                           "  特定路径 (如 'combo/bian_hua')\n"
                           "  包名 (如 '*yan_dao*')\n"
                           "  多个模式 (逗号分隔，如 'bian_hua,bing_xue')")
    parser.add_argument("--output", "-o", help="输出报告文件路径")
    parser.add_argument("--quiet", "-q", action="store_true", help="静默模式，只显示摘要")
    
    args = parser.parse_args()
    
    # 检查项目根目录
    project_root = Path(args.project_root)
    if not project_root.exists():
        print(f"❌ 项目根目录不存在: {project_root}")
        sys.exit(1)
    
    # 检查是否存在 Java 源码目录
    java_source_dir = project_root / "src" / "main" / "java"
    if not java_source_dir.exists():
        print(f"❌ 未找到 Java 源码目录: {java_source_dir}")
        sys.exit(1)
    
    # 创建检查器并运行检查
    checker = GuzhenrenComplianceChecker(project_root, args.target)
    result = checker.check_all()
    
    # 导出报告
    if args.output:
        checker.export_report(args.output)
    
    # 设置退出码
    if result["status"] == "FAIL":
        sys.exit(1)
    elif result["status"] == "WARN":
        sys.exit(2)
    else:
        sys.exit(0)


if __name__ == "__main__":
    main()
