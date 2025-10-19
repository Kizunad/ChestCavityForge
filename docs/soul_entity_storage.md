# 灵魂实体持久化概览

生成的 SoulPlayer（或其它基于 FakePlayer 的灵魂实体）在生命周期中会分别保存于内存和持久化存储：

- **在线内存快照**：`SoulFakePlayerSpawner` 维护了多张并发映射表，例如 `ACTIVE_SOUL_PLAYERS`、`OWNER_ACTIVE_SOUL`、`ENTITY_TO_SOUL` 等，用于跟踪当前已生成实体的 UUID、归属关系与实体 UUID 对应的灵魂档案。这些映射位于 `ConcurrentHashMap` 中，伴随服务器运行期常驻内存。【F:src/main/java/net/tigereye/chestcavity/soul/fakeplayer/SoulFakePlayerSpawner.java†L52-L85】
- **离线持久化**：当宿主玩家不在线或服务器准备卸载实体时，会调用 `SoulFakePlayerSpawner.saveSoulPlayerState`，若宿主离线则将快照写入 `SoulOfflineStore`（`SavedData` 派生类）。该存储位于主世界的数据仓中，并在玩家下次登录时由 `SoulPersistence.loadAll` 回填到其 `SoulContainer`。【F:src/main/java/net/tigereye/chestcavity/soul/fakeplayer/SoulFakePlayerSpawner.java†L497-L516】【F:src/main/java/net/tigereye/chestcavity/soul/util/SoulPersistence.java†L16-L42】
- **非玩家阵营持久化**：针对没有宿主玩家的灵魂实体（例如自定义敌对 BOSS），可以通过 `SoulEntityFactories.persist/peek/discard` 使用 `SoulEntityArchive`（`SavedData`）按实体 UUID 存取快照。这种模式与 vanilla 的 BOSS 事件存档类似，适用于 LLM/Gecko 驱动的通用实体扩展。【F:src/main/java/net/tigereye/chestcavity/soul/fakeplayer/SoulEntityFactories.java†L74-L86】【F:src/main/java/net/tigereye/chestcavity/soul/storage/SoulEntityArchive.java†L19-L84】
- **容器快照**：`SoulOfflineStore` 自身以 owner → soul → NBT 快照的形式落盘，提供 `put`、`consume` 等方法在停服/重启后恢复实体状态与上下文记忆。【F:src/main/java/net/tigereye/chestcavity/soul/storage/SoulOfflineStore.java†L17-L94】

因此，生成的灵魂实体不会仅依赖瞬时内存；它既有运行期的并发映射用于调度与所有权校验，也会在宿主离线或服务端保存时序列化到世界存档中，确保下次唤醒时能够恢复其模型、脑逻辑与上下文记忆。
