#!/usr/bin/env python3
"""
检查器基类

所有专门检查器的基础类，提供统一的接口和通用功能。
"""

from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Dict, Optional
from enum import Enum


class Severity(Enum):
    """检查结果严重程度"""
    PASS = "pass"
    INFO = "info"
    WARNING = "warning"
    ERROR = "error"


@dataclass
class CheckResult:
    """单个检查项的结果"""
    severity: Severity
    message: str
    file_path: Optional[Path] = None
    line_number: Optional[int] = None
    category: str = "general"

    def __str__(self):
        icon = {
            Severity.PASS: "✅",
            Severity.INFO: "ℹ️",
            Severity.WARNING: "⚠️",
            Severity.ERROR: "❌"
        }[self.severity]

        location = ""
        if self.file_path:
            location = f" [{self.file_path}"
            if self.line_number:
                location += f":{self.line_number}"
            location += "]"

        return f"{icon} {self.message}{location}"


@dataclass
class CheckReport:
    """检查报告汇总"""
    checker_name: str
    results: List[CheckResult] = field(default_factory=list)

    @property
    def error_count(self) -> int:
        return sum(1 for r in self.results if r.severity == Severity.ERROR)

    @property
    def warning_count(self) -> int:
        return sum(1 for r in self.results if r.severity == Severity.WARNING)

    @property
    def pass_count(self) -> int:
        return sum(1 for r in self.results if r.severity == Severity.PASS)

    @property
    def status(self) -> str:
        if self.error_count > 0:
            return "FAIL"
        elif self.warning_count > 0:
            return "WARN"
        else:
            return "PASS"


class BaseChecker:
    """检查器基类"""

    def __init__(self, project_root: Path):
        self.project_root = project_root
        self.java_source_dir = project_root / "src" / "main" / "java"
        self.test_source_dir = project_root / "src" / "test" / "java"
        self.resources_dir = project_root / "src" / "main" / "resources"
        self.report = CheckReport(checker_name=self.name)

    @property
    def name(self) -> str:
        """检查器名称"""
        return self.__class__.__name__

    @property
    def description(self) -> str:
        """检查器描述"""
        return self.__doc__ or "无描述"

    def check(self, target_dir: Path) -> CheckReport:
        """执行检查（子类实现）"""
        raise NotImplementedError("子类必须实现 check() 方法")

    def add_result(self, severity: Severity, message: str,
                   file_path: Optional[Path] = None,
                   line_number: Optional[int] = None,
                   category: str = "general"):
        """添加检查结果"""
        self.report.results.append(CheckResult(
            severity=severity,
            message=message,
            file_path=file_path,
            line_number=line_number,
            category=category
        ))

    def error(self, message: str, **kwargs):
        """添加错误"""
        self.add_result(Severity.ERROR, message, **kwargs)

    def warning(self, message: str, **kwargs):
        """添加警告"""
        self.add_result(Severity.WARNING, message, **kwargs)

    def info(self, message: str, **kwargs):
        """添加信息"""
        self.add_result(Severity.INFO, message, **kwargs)

    def passed(self, message: str, **kwargs):
        """添加通过项"""
        self.add_result(Severity.PASS, message, **kwargs)

    def find_skill_directories(self, target_dir: Path) -> List[Path]:
        """查找技能目录（通用逻辑）"""
        skill_dirs = []

        for family_dir in target_dir.iterdir():
            if not family_dir.is_dir() or family_dir.name.startswith('.'):
                continue

            for category_dir in family_dir.iterdir():
                if not category_dir.is_dir() or category_dir.name.startswith('.'):
                    continue

                for potential_skill_dir in category_dir.iterdir():
                    if not potential_skill_dir.is_dir() or potential_skill_dir.name.startswith('.'):
                        continue

                    if self.is_skill_directory(potential_skill_dir):
                        skill_dirs.append(potential_skill_dir)

                if self.is_skill_directory(category_dir):
                    skill_dirs.append(category_dir)

        return sorted(skill_dirs)

    def is_skill_directory(self, dir_path: Path) -> bool:
        """判断是否为技能目录（子类可覆盖）"""
        required_dirs = ["behavior", "calculator", "tuning"]
        subdirs_count = sum(1 for d in required_dirs if (dir_path / d).exists())

        suffixes = ["Behavior.java", "Calculator.java", "Tuning.java"]
        files_count = sum(1 for f in dir_path.iterdir()
                         if f.is_file() and any(f.name.endswith(s) for s in suffixes))

        return subdirs_count >= 2 or files_count >= 2

    def print_report(self, verbose: bool = True):
        """打印检查报告"""
        print(f"\n{'='*60}")
        print(f"📊 {self.name} - {self.description}")
        print(f"{'='*60}")

        print(f"\n✅ 通过: {self.report.pass_count}")
        print(f"⚠️  警告: {self.report.warning_count}")
        print(f"❌ 错误: {self.report.error_count}")

        if verbose:
            # 按类别分组显示
            categories = {}
            for result in self.report.results:
                if result.severity in (Severity.ERROR, Severity.WARNING):
                    cat = result.category
                    if cat not in categories:
                        categories[cat] = []
                    categories[cat].append(result)

            for cat, results in categories.items():
                if results:
                    print(f"\n[{cat}]")
                    for result in results[:20]:  # 每类最多显示20个
                        print(f"  {result}")
                    if len(results) > 20:
                        print(f"  ... 还有 {len(results) - 20} 个")

        status_msg = {
            "PASS": "🎉 所有检查通过！",
            "WARN": "⚠️  存在警告，建议关注",
            "FAIL": "❌ 发现错误，需要修复"
        }
        print(f"\n{status_msg[self.report.status]}")
