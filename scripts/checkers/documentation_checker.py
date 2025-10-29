#!/usr/bin/env python3
"""
文档规范检查器

检查技能文档 JSON 文件的存在性和完整性。
"""

import json
import re
from pathlib import Path
from .base_checker import BaseChecker, Severity


class DocumentationChecker(BaseChecker):
    """文档规范检查器 - 检查 JSON 文档完整性"""

    @property
    def description(self) -> str:
        return "检查技能文档 JSON 的存在性和完整性"

    def check(self, target_dir: Path) -> 'CheckReport':
        """执行文档检查"""
        skill_dirs = self.find_skill_directories(target_dir)
        self.doc_index = self._build_document_index()

        if not skill_dirs:
            self.warning("未找到技能目录", category="目录")
            return self.report

        for skill_dir in skill_dirs:
            self.check_skill_documentation(skill_dir)

        return self.report

    def check_skill_documentation(self, skill_dir: Path):
        """检查单个技能的文档"""
        skill_name = skill_dir.name

        resolved_resource = self._resolve_skill_resource(skill_dir)
        if resolved_resource:
            namespace, resource_path = resolved_resource
        else:
            namespace, resource_path = self._infer_skill_resource_from_path(skill_dir)

        expected_doc_id = f"{namespace}:{resource_path}"
        expected_doc_name = resource_path

        # 查找文档文件
        doc_path = (
            self.resources_dir
            / "assets"
            / namespace
            / "docs"
            / "combo"
            / f"{expected_doc_name}.json"
        )
        naming_issue_logged = False

        if not doc_path.exists():
            alternate_path = self.doc_index.get(expected_doc_id)
            if alternate_path:
                self.error(
                    f"{skill_name}: 文档文件命名不规范，应使用 {expected_doc_name}.json",
                    file_path=alternate_path,
                    category="文档命名")
                doc_path = alternate_path
                naming_issue_logged = True
            else:
                self.error(f"{skill_name}: 缺少文档文件 {expected_doc_name}.json",
                         file_path=doc_path,
                         category="文档存在性")
                return

        # 检查文档内容
        try:
            with open(doc_path, 'r', encoding='utf-8') as f:
                doc = json.load(f)

            self.check_documentation_fields(
                skill_name,
                doc,
                doc_path,
                expected_doc_id,
                expected_doc_name,
                naming_issue_logged
            )

        except json.JSONDecodeError as e:
            self.error(f"{skill_name}: JSON 格式错误 - {e}",
                     file_path=doc_path,
                     category="文档格式")
        except Exception as e:
            self.error(f"{skill_name}: 无法读取文档 - {e}",
                     file_path=doc_path,
                     category="文件访问")

    def check_documentation_fields(self, skill_name: str, doc: dict, doc_path: Path,
                                   expected_doc_id: str, expected_doc_name: str,
                                   naming_issue_logged: bool):
        """检查文档字段完整性"""
        required_fields = {
            "id": "技能ID",
            "title": "技能标题",
            "summary": "简要描述",
            "details": "详细说明"
        }

        recommended_fields = {
            "icon": "物品图标ID",
            "iconTexture": "图标纹理路径"
        }

        # 检查必需字段
        for field, desc in required_fields.items():
            if field not in doc:
                self.error(f"{skill_name}: 缺少必需字段 '{field}' ({desc})",
                         file_path=doc_path,
                         category="文档完整性")
            elif not doc[field] or (isinstance(doc[field], str) and not doc[field].strip()):
                self.warning(f"{skill_name}: 字段 '{field}' 为空",
                           file_path=doc_path,
                           category="文档完整性")
            else:
                self.passed(f"{skill_name}: 包含字段 '{field}'",
                          category="文档完整性")

        # 检查推荐字段
        for field, desc in recommended_fields.items():
            if field not in doc:
                self.warning(f"{skill_name}: 建议添加字段 '{field}' ({desc})",
                           file_path=doc_path,
                           category="文档完整性")
            else:
                self.passed(f"{skill_name}: 包含字段 '{field}'",
                          category="文档完整性")

        # 检查 iconTexture 路径格式
        if "iconTexture" in doc and doc["iconTexture"]:
            texture_path = doc["iconTexture"]
            if not texture_path.endswith(".png"):
                self.warning(f"{skill_name}: iconTexture 应为 PNG 文件路径",
                           file_path=doc_path,
                           category="文档格式")
            if not texture_path.startswith("guzhenren:"):
                self.warning(f"{skill_name}: iconTexture 应以 'guzhenren:' 开头",
                           file_path=doc_path,
                           category="文档格式")

        doc_id = doc.get("id")
        if doc_id:
            if doc_id != expected_doc_id:
                self.error(
                    f"{skill_name}: 文档 ID 不匹配，期望 {expected_doc_id} 实际 {doc_id}",
                    file_path=doc_path,
                    category="文档命名")
            else:
                self.passed(f"{skill_name}: 文档 ID 与技能匹配",
                            category="文档命名")

        if doc_path.stem != expected_doc_name and not naming_issue_logged:
            self.error(
                f"{skill_name}: 文档文件名与技能不一致，应为 {expected_doc_name}.json",
                file_path=doc_path,
                category="文档命名")

    def _resolve_skill_resource(self, skill_dir: Path):
        """尝试从行为类中解析技能的 ResourceLocation"""
        behavior_dir = skill_dir / "behavior"
        candidates = []

        if behavior_dir.exists():
            candidates.extend(behavior_dir.glob("*.java"))
        else:
            candidates.extend(skill_dir.glob("*Behavior.java"))

        pattern = re.compile(
            r"ResourceLocation\s+(?:ABILITY_ID|SKILL_ID)\s*="
            r"\s*ResourceLocation\.fromNamespaceAndPath\(\"([^\"]+)\"\s*,\s*\"([^\"]+)\"\)"
        )

        for java_file in candidates:
            try:
                content = java_file.read_text(encoding="utf-8")
            except Exception as exc:
                self.warning(
                    f"无法读取行为类 {java_file.name} - {exc}",
                    file_path=java_file,
                    category="文件访问")
                continue

            match = pattern.search(content)
            if match:
                namespace, path_value = match.groups()
                return namespace, path_value

        return None

    def _infer_skill_resource_from_path(self, skill_dir: Path):
        """基于目录结构推断技能资源路径（回退方案）"""
        default_namespace = "guzhenren"
        skill_name = skill_dir.name

        try:
            relative = skill_dir.relative_to(self.java_source_dir)
            parts = relative.parts
            combo_idx = parts.index("combo")
            family = parts[combo_idx + 1] if len(parts) > combo_idx + 1 else None
            category = parts[combo_idx + 2] if len(parts) > combo_idx + 2 else None
            skill = parts[combo_idx + 3] if len(parts) > combo_idx + 3 else None

            if category and skill:
                resource_path = f"{category}_{skill}"
            elif family:
                resource_path = family
            else:
                resource_path = skill_name
        except (ValueError, IndexError):
            resource_path = skill_name

        return default_namespace, resource_path

    def _build_document_index(self):
        """预先构建文档 ID 到文件路径的索引，便于检测命名问题"""
        index = {}
        assets_dir = self.resources_dir / "assets"

        if not assets_dir.exists():
            self.warning("未找到资源目录 assets/", category="目录")
            return index

        for namespace_dir in assets_dir.iterdir():
            if not namespace_dir.is_dir():
                continue

            docs_dir = namespace_dir / "docs" / "combo"
            if not docs_dir.exists():
                continue

            for doc_file in docs_dir.glob("*.json"):
                try:
                    with open(doc_file, 'r', encoding='utf-8') as f:
                        doc = json.load(f)
                    doc_id = doc.get("id")
                    if not doc_id:
                        self.warning(
                            f"文档 {doc_file.name} 缺少字段 'id'，无法建立命名索引",
                            file_path=doc_file,
                            category="文档命名")
                        continue
                    if doc_id in index:
                        self.warning(
                            f"检测到重复的技能 ID '{doc_id}'，文件 {doc_file.name} 将覆盖 {index[doc_id].name}",
                            file_path=doc_file,
                            category="文档命名")
                    index[doc_id] = doc_file
                except json.JSONDecodeError as e:
                    self.error(
                        f"文档 {doc_file.name} JSON 格式错误，无法用于命名索引 - {e}",
                        file_path=doc_file,
                        category="文档格式")
                except Exception as e:
                    self.error(
                        f"读取文档 {doc_file.name} 失败 - {e}",
                        file_path=doc_file,
                        category="文件访问")

        return index
