
ChestCavity (NeoForge 1.21.1) — Guzhenren 扩展与机制概览
===========================================================

Contributors / 贡献者
---------------------
- David Trover — 原作作者（Chest Cavity）
- BoonelDanForever — 维护与迁移（NeoForge 1.21.1）
- 社区贡献者 — Bug 修复、行为扩展与本地化

- 数据驱动映射
  - 实体分配：`src/main/resources/data/chestcavity/entity_assignment/guzhenren/` 下提供与蛊真人生态相关的实体到胸腔类型映射。
  - 胸腔类型：`src/main/resources/data/chestcavity/types/compatibility/guzhenren/` 下提供对应的胸腔类型定义与器官配置。
  - 器官定义：`src/main/resources/data/chestcavity/organs/guzhenren/human/` 记录蛊修专属器官（如空窍）。

- 记分板驱动升级（简称“cc”）
  - 基于 scoreboard 条件为玩家的 cc 注入固定物品/器官。
  - 入口：`ScoreboardUpgradeManager`（流程编排）与 `registration/CCScoreboardUpgrades`（升级清单与生成逻辑）。

- 蛊真人联动（开窍展示）
  - 通过蛊真人的“开窍”条件（scoreboard 目标 `kaiqiao`）达成后，向 cc 指定槽位注入物品：`guzhenren:gucaikongqiao`。
  - 该物品将附带 NBT 字段：`Owner`，值为玩家的计分板名，用于标识归属。

- 工具类
  - 新增 `NBTWriter`（`src/main/java/net/tigereye/chestcavity/util/NBTWriter.java`），用于简化 `CustomData` 写入流程：`NBTWriter.updateCustomData(stack, tag -> { ... })`。


Source installation information for modders
-------------------------------------------
This code follows the Minecraft Forge installation methodology. It will apply
some small patches to the vanilla MCP source code, giving you and it access 
to some of the data and functions you need to build a successful mod.

Note also that the patches are built against "un-renamed" MCP source code (aka
SRG Names) - this means that you will not be able to read them directly against
normal code.

Setup Process:
==============================

Step 1: Open your command-line and browse to the folder where you extracted the zip file.

Step 2: You're left with a choice.
If you prefer to use Eclipse:
1. Run the following command: `gradlew genEclipseRuns` (`./gradlew genEclipseRuns` if you are on Mac/Linux)
2. Open Eclipse, Import > Existing Gradle Project > Select Folder 
   or run `gradlew eclipse` to generate the project.

If you prefer to use IntelliJ:
1. Open IDEA, and import project.
2. Select your build.gradle file and have it import.
3. Run the following command: `gradlew genIntellijRuns` (`./gradlew genIntellijRuns` if you are on Mac/Linux)
4. Refresh the Gradle Project in IDEA if required.

If at any point you are missing libraries in your IDE, or you've run into problems you can 
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
=============================
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license, if you do not agree with it you can change your mapping names to other crowdsourced names in your 
build.gradle. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/MinecraftForge/MCPConfig/blob/master/Mojang.md

Additional Resources: 
=========================
Community Documentation: http://mcforge.readthedocs.io/en/latest/gettingstarted/  
LexManos' Install Video: https://www.youtube.com/watch?v=8VEdtQLuLO0  
Forge Forum: https://forums.minecraftforge.net/  
Forge Discord: https://discord.gg/UvedJ9m  
