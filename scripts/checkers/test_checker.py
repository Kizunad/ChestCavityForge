#!/usr/bin/env python3
"""
测试检查器

检查技能是否有对应的单元测试，特别是 Calculator 的测试。
"""

import re
from pathlib import Path
from .base_checker import BaseChecker, Severity


class TestChecker(BaseChecker):
    """测试检查器 - 检查单元测试的存在性和质量"""

    @property
    def description(self) -> str:
        return "检查单元测试的存在性和质量"

    def check(self, target_dir: Path) -> 'CheckReport':
        """执行测试检查"""
        skill_dirs = self.find_skill_directories(target_dir)

        if not skill_dirs:
            self.warning("未找到技能目录", category="目录")
            return self.report

        for skill_dir in skill_dirs:
            self.check_skill_tests(skill_dir)

        return self.report

    def check_skill_tests(self, skill_dir: Path):
        """检查单个技能的测试"""
        skill_name = skill_dir.name

        # 查找所有 Calculator 文件
        calculator_files = list(skill_dir.rglob("*Calculator.java"))

        if not calculator_files:
            # 没有 Calculator，不需要测试
            return

        # 检查每个 Calculator 是否有对应的测试
        for calc_file in calculator_files:
            self.check_calculator_test(skill_name, calc_file)

    def check_calculator_test(self, skill_name: str, calc_file: Path):
        """检查 Calculator 是否有对应的测试文件"""
        # 计算测试文件路径
        relative_path = calc_file.relative_to(self.java_source_dir)
        test_file = self.test_source_dir / relative_path.parent / f"{calc_file.stem}Test.java"

        if not test_file.exists():
            self.error(f"{skill_name}: 缺少测试文件 {test_file.name}",
                     file_path=calc_file,
                     category="测试存在性")
            return

        self.passed(f"{skill_name}: 存在测试文件 {test_file.name}",
                  category="测试存在性")

        # 检查测试质量
        self.check_test_quality(skill_name, test_file)

    def check_test_quality(self, skill_name: str, test_file: Path):
        """检查测试文件的质量"""
        try:
            with open(test_file, 'r', encoding='utf-8') as f:
                content = f.read()

            # 检查是否使用 JUnit 5
            if '@Test' not in content:
                self.error(f"{skill_name}: 测试文件缺少 @Test 注解",
                         file_path=test_file,
                         category="测试质量")
                return

            # 统计测试方法数量
            test_methods = re.findall(r'@Test\s+(?:public\s+)?void\s+(\w+)\s*\(', content)
            test_count = len(test_methods)

            if test_count == 0:
                self.error(f"{skill_name}: 测试文件没有测试方法",
                         file_path=test_file,
                         category="测试质量")
            elif test_count < 3:
                self.warning(f"{skill_name}: 测试用例较少 ({test_count} 个)，建议增加边界测试",
                           file_path=test_file,
                           category="测试质量")
            else:
                self.passed(f"{skill_name}: 有 {test_count} 个测试用例",
                          category="测试质量")

            # 检查是否使用 AssertJ
            if 'assertThat' in content:
                self.passed(f"{skill_name}: 使用 AssertJ 断言",
                          category="测试质量")
            elif 'assertEquals' not in content and 'assertTrue' not in content:
                self.warning(f"{skill_name}: 测试缺少断言",
                           file_path=test_file,
                           category="测试质量")

            # 检查是否有边界测试
            boundary_keywords = ['zero', '0', 'negative', 'max', 'min', 'boundary', 'edge']
            has_boundary_tests = any(keyword in content.lower() for keyword in boundary_keywords)

            if has_boundary_tests:
                self.passed(f"{skill_name}: 包含边界条件测试",
                          category="测试质量")
            else:
                self.warning(f"{skill_name}: 建议添加边界条件测试（如0、负值、最大值）",
                           file_path=test_file,
                           category="测试质量")

        except Exception as e:
            self.warning(f"{skill_name}: 无法读取测试文件 - {e}",
                       file_path=test_file,
                       category="文件访问")
