# 古真人物品“流派”扫描工具

## 背景

古真人模组的物品类（位于 `../decompile/10_6_decompile/net/guzhenren/item`）会在 `appendHoverText` 中直接写入“流派：××”等提示，没有统一的注解或枚举。为了后续迁移与能力映射，需要一个脚本快速罗列所有物品对应的“流派”提示。

## 工具说明

### Python 版本（快速导出）

- 入口脚本：`scripts/guzhenren_flow_scanner.py`
- 功能：遍历上述反编译目录的 `.java` 文件，抓取包含“流派”关键字的 tooltip 文本，输出 item 类名、所在文件以及提示内容。
- 输出格式：
  - `--format table`：Markdown 表格（默认）
  - `--format plain`：按流派分组的纯文本
  - `--format json`：分组后的 JSON 结构，便于进一步处理
- 目录可通过 `--decompile-root` 自定义（例如切换到不同的反编译版本）。

### Java 版本（仓库内工具类）

- 工具类：`net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowScanner`
- 功能：同样读取反编译的 item 目录，同时解析 `GuzhenrenModItems.java`，输出「物品 ID → 流派」映射，并列出缺乏注册或缺少流派提示的条目。
- 运行方式（在仓库根目录）：
  ```bash
  ./gradlew classes
  java -cp build/classes/java/main net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowScanner
  ```
- 支持参数：
  - `-i/--items <path>`：自定义 item 目录
  - `-r/--registry <path>`：自定义 `GuzhenrenModItems.java`
- 输出示例：
  ```
  guzhenren:ai_bie_chi                   -> 毒道        (class: AiBieChiItem)
  guzhenren:sheng_zhuan                  -> 无          (class: ShengZhuanItem)

  总计 2 个条目，其中 1 个缺少流派信息。
  ```
- 注意：运行时需使用与项目一致的 Java 版本（当前构建为 Java 21），否则可能出现 `UnsupportedClassVersionError`。

### Java 版本（运行时：Tooltip 解析）

- 工具类：`net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver`
- 功能：在游戏运行时读取任意 `ItemStack` 的 tooltip 文本，返回该物品声明的流派列表；若 tooltip 未标注流派，则返回空列表以示「无流派」。
- 典型用法：
  ```java
  var info = GuzhenrenFlowTooltipResolver.inspectWithLevel(stack, level, player, TooltipFlag.NORMAL);
  if (info.hasFlow()) {
      info.flows().forEach(flow -> LOGGER.info("检测到流派：{}", flow));
  } else {
      LOGGER.info("未检测到流派");
  }
  ```
- 适用场景：ModernUI 技能槽、快捷键绑定、调试命令等需要即时识别流派的功能模块。

## 快速使用

```bash
# 在 ChestCavityForge 仓库根目录执行
python3 scripts/guzhenren_flow_scanner.py --format table > docs/guzhenren_item_flows.md
```

或直接查看终端输出：

```bash
python3 scripts/guzhenren_flow_scanner.py --format plain
```

示例输出（节选）：

```
[云道]
  - BaiYunGuItem (BaiYunGuItem.java:30)
  - TengyunguItem (TengyunguItem.java:30)

[人道]
  - BaiYinSheLiGuItem (BaiYinSheLiGuItem.java:28)
  - ChiTieSheLiGuItem (ChiTieSheLiGuItem.java:28)
```

## 下一步建议

- 将 `docs/guzhenren_item_flows.md` 作为脚本产物定期更新，便于查阅全部物品与流派的对应关系。
- 若后续需要统计“本命蛊”或关联的 Procedure，可在脚本内扩展正则以同时抓取 `BenMingGu*Procedure` 的调用信息。
