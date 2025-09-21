
ChestCavity (NeoForge 1.21.1)
=============================

简介 / Introduction
-------------------
Chest Cavity 是一个允许实体拥有“胸腔”与“器官分数”的玩法模组。本分支在 1.21.1 + NeoForge 上进行维护，并扩展了与“蛊真人”玩法的联动：新增多种器官的慢速监听（SlowTick）与事件回调、数值折算、以及富有手感的视听表现（音效 + 粒子）。

亮点特性 / Highlights
- 慢速监听与兼容桥：统一的 SlowTick 与 IncomingDamage 监听管线；通过 GuzhenrenResourceBridge 以反射方式安全读写玩家真元/精力等数据。
- 木肝蛊（Mugangu）：每秒按修为缩放回复真元；若未集齐其余四蛊，先扣精力再按 0.8 折扣回复。
- 金肺蛊（Jinfeigu）：满饥饿且当前吸收未达阈值时，消耗饥饿换取 60s 吸收之心，强度随堆叠扩展。
- 水肾蛊（Shuishengu）：在水中消耗真元充能护盾；受击时按指数函数平滑消耗护盾并减伤，护盾枯竭或未枯竭分别播放破盾/水花音效；粒子与音效随堆叠线性放大。
- 土脾蛊（Tupigu）：慢速触发真元→精力转换，并提供短效跳跃增益。

演出与可读性 / FX & UX
- 充能阶段：海泡/上升泡柱/泡破粒子于脚边缓慢上升，伴随轻微“咕噜”音；施加短时导管之力作为淡蓝色视觉提示。
- 充能完成：信标选能量音效 + 环绕粒子环（END_ROD/SOUL 点缀）。
- 护盾减伤：溅水 + 泡破粒子；护盾枯竭时额外播放护盾破裂音。

Contributors / 贡献者
---------------------
- David Trover — 原作作者（Chest Cavity）
- BoonelDanForever — 维护与迁移
- 社区贡献者 — Bug 修复、行为扩展与本地化
- 哈基芊 社区测试者
- （· _ ·） 社区测试者

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
