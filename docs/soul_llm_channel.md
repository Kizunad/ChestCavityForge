# 灵魂 LLM 指令通道快速上手

本文说明如何通过 `SoulLLMInstructionChannel` 将外部 LLM / 脚本下发指令给分魂，以及如何读取执行结果与环境摘要。

## 1. 推送指令

```java
UUID soulId = /* 分魂实体 UUID */;
// 例如要求分魂进入战斗模式，持续 10 秒（200 tick）
SoulLLMInstructionChannel.Instruction instruction = SoulLLMInstructionChannel.submit(
        soulId,
        "intent:combat",
        Map.of(
                "style", "force_fight",
                "ttl", "200"
        )
);
System.out.println("指令已提交: " + instruction.id());
```

常用指令：

| 指令名 | 说明 | 关键参数 |
| --- | --- | --- |
| `intent:combat` | 推送战斗意图 | `style` (`force_fight` / `guard`)、`ttl`、`target` (可选 UUID) |
| `intent:follow` | 跟随主人或指定实体 | `target` (`owner` / UUID)、`distance`、`ttl` |
| `intent:hold` | 固守当前位置或指定坐标 | `x/y/z`、`ttl` |
| `intent:clear` | 清空当前显式意图 | 无 |
| `action:start` | 直接启动注册的 Action | `action` (ResourceLocation) |
| `action:cancel` | 取消指定 Action | `action` (ResourceLocation) |

## 2. 查询执行结果

处理器消费指令后，会将执行情况写入结果通道。可按指令 ID 查询，也可轮询魂魄的结果队列：

```java
SoulLLMInstructionChannel.getResult(instruction.id())
        .ifPresent(result -> {
            System.out.println("状态: " + result.status());
            System.out.println("信息: " + result.message());
            System.out.println("元数据: " + result.metadata());
        });

// 或者：轮询未读结果（读取即移除）
SoulLLMInstructionChannel.pollResult(soulId)
        .ifPresent(result -> {
            switch (result.status()) {
                case SUCCESS -> System.out.println("完成: " + result.message());
                case IGNORED -> System.out.println("忽略: " + result.message());
                case ERROR -> System.err.println("异常: " + result.message());
            }
        });
```

## 3. 获取环境摘要

`soulPlayer.tick()` 会在服务端每 tick 更新一次环境快照，可供提示词或决策模块参考：

```java
SoulLLMInstructionChannel.getEnvironment(soulId)
        .ifPresent(snapshot -> {
            System.out.println("位置: " + snapshot.x() + ", " + snapshot.y() + ", " + snapshot.z());
            System.out.println("血量: " + snapshot.health() + " / " + snapshot.maxHealth());
            System.out.println("脑模式: " + snapshot.metadata().get("brain_mode"));
            System.out.println("JSON: " + snapshot.toJson());
        });
```

## 4. 接入思路

1. 服务端启动阶段调用 `SoulRuntimeHandlers.bootstrap()`，系统会自动注册 `SoulLLMControlHandler`。
2. 在网络层或脚本引擎中，根据 LLM 输出组装 `command + parameters`，调用 `submit` 推送。
3. 后台线程按需轮询 `pollResult` 或 `getResult`，并回写到对话上下文中。
4. 使用 `getEnvironment` 提供的摘要（或 `toJson()`）构建下一轮提示词，形成闭环控制。

> 提示：通道为无界队列，请根据业务自行做节流；若指令不再需要，可在消费侧忽略或自定义清理逻辑。
