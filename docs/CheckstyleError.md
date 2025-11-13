# Checkstyle 修复计划

最新一次 `./gradlew build` 仅因 Checkstyle 阻塞，编译阶段已通过。命令输出显示仍存在的违规文件集中在飞剑/冰雪刀相关测试用例，主要问题为行长超过 100 字符与方法块缩进不符合 2 空格规范。现制定如下处理计划：

## 1. 明确目标文件
仅处理日志中真实报错的文件：
- `src/test/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/ai/ward/InterceptPlannerTest.java`
- `src/test/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/calculator/FlyingSwordCalculatorTest.java`
- `src/test/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/motion/SteeringOpsTest.java`
- `src/test/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/client/orientation/OrientationOpsTest.java`
- `src/test/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/integration/resource/UpkeepOpsTest.java`
- `src/test/java/net/tigereye/chestcavity/compat/guzhenren/item/bing_xue_dao/calculator/CooldownOpsTest.java`

## 2. 修复策略
1. **行长控制**：
   - 对断言、字符串、构造器参数等超过 100 字符的行进行换行。
   - 必要时引入局部变量（例如拆分长 `assertTrue` 条件），确保语义清晰。

2. **缩进统一**：
   - 全部使用 2 空格缩进，移除混用的 4 空格或制表符。
   - 检查注解、`@Nested` 类、lambda 体等位置的缩进，保持与主项目风格一致。

3. **逐步验证**：
   - 每完成一至两个文件后执行 `./gradlew checkstyleTest`，确保没有新警告。
   - 若出现新的文件被列出，追加到“目标文件”并继续迭代。

## 3. 完成标准
- `./gradlew checkstyleTest` 无错误。
- 随后执行 `./gradlew build`，确认整个流水线（含 Shadow/测试）通过。
- 仅涉及格式调整，无逻辑改动；必要时在提交信息中注明“Checkstyle fix only”。

按该计划推进，可最小化 diff 并快速恢复 CI 绿灯。
