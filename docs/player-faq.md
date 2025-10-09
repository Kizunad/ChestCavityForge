# ChestCavityForge 玩家常见问题（FAQ）— 实用版（游戏内与依赖）

本篇聚焦玩家角度：如何安装、如何操作、常见依赖与兼容问题、游戏内机制与排查。不涉及源码或实现细节。

## 1) 安装与依赖
- 运行环境：NeoForge 1.21.1（客户端与服务端需一致）。
- 必需前置（客户端）：Modern UI（`icyllis.modernui:ModernUI-NeoForge:1.21.1-3.12.0.2`）。未安装会导致胸腔界面、配置界面、GuScript 笔记本无法打开。
- 兼容前置（可选）：Guzhenren（蛊真人）。安装后解锁真元/精力/魂魄/念头与多种器官联动；未安装则相关功能自动停用。
- 多人游戏：
  - ChestCavityForge 必须安装在服务器与所有客户端。
  - Guzhenren/Modern UI：客户端也必须安装，版本与服务端相同（或兼容）。
  - 数据包/资源包：若使用自定义器官配置或 FX，需要同步到服务器与所有客户端。

常见错误与处理：
- Missing Mod/Modern UI：给客户端安装对应版本的 Modern UI。
- Mod Version Mismatch：确保 NeoForge、ChestCavityForge、Guzhenren 三者均为 1.21.1 对应版本。
- 加载即崩：先只放必需模组测试，再逐个加入其他模组定位冲突。

## 2) 操作与界面
- 打开胸腔：使用 `chestcavity:chest_opener` 对实体（含玩家）使用，进入胸腔 GUI 更换器官。
- 主动技能：在“按键设置”中绑定 ChestCavity 能力热键（通用激活键与特定技能键，如“龙骨爆弹”）。
- Modern UI：部分界面使用 Modern UI 呈现，卡住时可先退出世界再重进。指令 `/testmodernUI config`（调试）可打开配置示例界面。

## 3) 器官与效果（简述）
- 缺失惩罚：缺心脏=持续掉血；缺肺=更快缺氧；缺肾=周期性中毒；缺脾/胃/肠影响进食收益。
- 主要分数：
  - 呼吸类：`breath_capacity`（耐氧），`water_breath`（水下呼吸）。
  - 生存类：`health`（最大生命），`defense`（减伤），`arrow_dodging`（闪避投射物）。
  - 食性/代谢：`nutrition`、`digestion`、`metabolism` 调整吃饱速度与饱和度。
- 排异：非通用器官会排异。通用器官/原生器官/特殊掉落可避免排异。

## 4) Guzhenren 资源（安装蛊真人后）
- 资源种类：
  - 真元（zhenyuan）：多数技能主耗；精力（jingli）：副耗；魂魄（hunpo）：魂兽/分魂相关；念头（niantou）：智道多用。
- 念头上限：
  - `niantou_zhida`（短期恢复上限）：念头恢复只会抬到此阈值。
  - `niantou_rongliang`（容量上限）：念头总量硬上限，超出会被裁掉。
- 非玩家生物：多数情况下不使用真元/精力，技能表现按生命值折算或跳过资源扣除。

## 5) 魂兽与分魂（玩家向）
- 开启魂兽：集齐魂道器官后，通过技能或配置界面触发魂兽化；服务器也可开启“魂兽门”后按条件自动转换。
- 退出魂兽：移除关键器官或由管理员/命令关闭。若为“永久魂兽”，需要特殊道具/脚本或管理指令解除。
- 魂兽效果：近战/投射命中时附加魂焰 DoT（消耗魂魄），玩家自动维持饥饿/饱和；每秒消耗 3 点魂魄，不足时会致死。
- 分魂界面（Modern UI）：在配置界面“分魂”页查看分魂列表、切换附身、改名等（需服务器支持并安装前置）。

## 6) GuScript（玩家能看到的）
- 位置：`data/chestcavity/guscript/`（玩家不需要编辑）。
- 作用：为器官技能提供“流程化”与“条件化”的释放逻辑（如蓄力→释放→冷却），并统一播放粒子/音效。
- 例子：`flows/thoughts_remote_burst.json`（念头远程爆发）——蓄力足够且念头≥180时自动释放，对目标造成冲击并播放特效；目标无效时不扣资源。
- 页内物品数量：Notebook 会将物品堆叠数计入匹配条件（例如需要“骨道×2”，单槽 2 个也可满足）。

## 7) 物品与掉落
- 生物掉落：BOSS 类型更高几率掉稀有器官；普通生物有较低概率掉通用器官。
- 胸腔随机生成：兼容生物的胸腔可能包含随机部件（权重定义在 `types/compatibility/*`）。

## 8) 常见问题与排查
- 进入界面崩溃/打不开：客户端缺少 Modern UI 或版本不匹配；请安装/更换与服务器一致的版本。
- 能力按键无效：确认在控制设置绑定按键，且对应器官已装配、无冷却、资源充足。
- 粒子/音效缺失：检查资源包与声音设置；使用 `/reload` 或重进世界。
- 吸收护盾始终为 0：请使用本模组最新版本（1.21.1 线），服务器需要支持玩家的 `MAX_ABSORPTION` 属性；旧服可尝试重启刷新。
- 多人不同步：确认所有人安装了同版本 ChestCavityForge/Modern UI/Guzhenren，并同步相同数据包。
- 与着色器/性能模组冲突：若界面渲染异常，可暂时关闭着色器/Iris/Embeddium/Rubidium 等进行排查。

## 9) 调试与实用指令（服主管理常用）
- `/cc ability`：查看/触发胸腔能力（调试）。
- `/testmodernUI config|container`：打开示例界面（客户端/联机调试）。
- 魂兽状态（玩家本体）：
  - `/soulbeast enable <true|false>`：开启/关闭魂兽门（是否启用）
  - `/soulbeast permanent <true|false>`：设置是否永久魂兽
  - `/soulbeast info`：查看当前启用/永久/激活状态
- 分魂系统（需服务器启用）：
  - `/soul enable`：启用分魂系统（一次性开关）
  - `/soul order follow|guard|forcefight|idle <idOrName|@a>`：下达分魂行为指令（可对 @a 作用于全部自有分魂）
  - `/soul name set <idOrName> <newName>`、`/soul name apply <idOrName>`：设置并应用分魂显示名
  - `/soul skin set <idOrName> <mojangName>`、`/soul skin apply <idOrName>`：设置并应用分魂皮肤
  - `/soul control owner`、`/soul control <idOrName>`：切回本体/切换附身分魂
  - `/soul autospawn on|off <idOrName>`：登录/维度变更时是否自动生成该分魂
  - 测试工具：`/soul test SoulPlayerList`、`/soul test SoulPlayerSwitch owner|<idOrName>`、`/soul test SoulPlayerRemove <idOrName>`、`/soul test spawnFakePlayer`、`/soul test CreateSoulDefault`、`/soul test CreateSoulAt <x> <y> <z>`、`/soul test saveAll`

# 社区精华与扩展资料索引

本页仅作“导航”，指向社区群精华中整理的资料（效果说明、玩法示例、后续计划、版本公告等）。由于群精华可能随时间更新，请以最新精华为准。

## 快速入口
- 效果/器官合集（群精华）：请在群聊的“精华消息”中搜索关键词“效果”“器官”“数值”。
- 后续计划/路线图：搜索“计划”“Roadmap”“版本公告”。
- 常见问题速查：搜索“FAQ”“崩溃”“依赖”“Modern UI”。
- 玩法示例与配装：搜索“配装”“联动”“GuScript 示例”。

若需要离线版本或无法访问群精华，请在仓库的 `docs/` 中查阅：
- `player-faq.md`（实用版 FAQ：安装依赖、玩法操作与排查）
- `organ-score-effects.md`（器官分数与效果总览）
- `design/guscript-module.md`（GuScript 基本原理与示例）
- `migration-neoforge.md`（1.21.1 迁移记录）

## 贡献与补充
- 如发现群精华与仓库文档存在差异，请以服务器公告与最新精华为准，并在反馈时附带截图与时间戳，方便同步到仓库。
- 欢迎将高质量的教程/示例整理为 Markdown。

## 备注
- 本页不保存具体数值或步骤，仅提供“去哪里看”的线索。
- 群精华中的方案可能包含实验性内容；正式服以服务器侧配置与公告为准。
- 若你在精华中找不到所需主题，请在群内 @管理 或提交问题清单，我们会补充指引。
- Github源码仓库: https://github.com/Kizunad/ChestCavityForge/tree/feature/guzhenren-migration

## 10) 反馈建议
- 反馈请附：`latest.log`、已安装模组列表、NeoForge 与前置版本、问题发生步骤与截图/视频。
- 若为多人问题，请同步提供“服务器端日志+任一客户端日志”。

—— 希望这份 FAQ 能帮助你顺利开膛练蛊，玩得开心！
