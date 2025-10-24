# GeckoFX 数据与自定义资源

灵魂系统与 GuScript 现在会通过 `data/<namespace>/geckofx/*.json` 读取 GeckoLib FX 定义。
开发者可以在资源包中新增 JSON 文件，再在代码或脚本中引用对应的 `ResourceLocation`，即可为特效、灵魂分身或其他流程调用提供一致的模型/贴图/动画三件套。

## JSON 结构

每个 JSON 需提供下列字段：

| 字段 | 说明 | 是否必填 |
| ---- | ---- | -------- |
| `model` | GeckoLib 模型资源（`.geo.json`） | 是 |
| `texture` | 贴图资源 | 是 |
| `animation` | 动画资源（`.animation.json`） | 是 |
| `default_animation` | 默认播放的动画片段名，可为空字符串 | 否 |
| `default_scale` | 基础缩放，默认 `1.0` | 否 |
| `default_tint` | 16 进制颜色（可写成 `#RRGGBB` 或 `0xRRGGBB`），默认 `#FFFFFF` | 否 |
| `default_alpha` | 基础透明度，默认 `1.0` | 否 |
| `blend` | 渲染模式，支持 `opaque` / `cutout` / `translucent` | 否，默认 `translucent` |

示例（项目已内置 `data/chestcavity/geckofx/example_ghost.json`）：

```json
{
  "model": "chestcavity:geo/ghost_tiger.geo.json",
  "texture": "minecraft:textures/entity/phantom.png",
  "animation": "chestcavity:animations/ghost_tiger.animation.json",
  "default_animation": "animation.chestcavity.ghost_tiger.idle",
  "default_scale": 1.0,
  "default_tint": "#FFFFFF",
  "default_alpha": 1.0,
  "blend": "translucent"
}
```

## 运行时覆写

`soul.fakeplayer.SoulEntitySpawnRequest` 新增了 `GeckoFx` 参数，可以在重生灵魂外壳时指定：

- 目标 `fxId`（与 JSON 文件名一致）。
- 可选的模型/贴图/动画覆写：当需要用相同骨架播放不同资源时，可在请求中直接提供新的 `ResourceLocation`，客户端会基于注册表定义合成最终渲染数据。
- 偏移、锚点（世界/实体/施法者）、缩放、透明度、持续时间、循环等参数；默认行为与 `FxFlowActions.emitGecko` 保持一致。

当这些信息随重生请求下发后，服务器会构造 `GeckoFxEventPayload` 并广播给客户端，客户端 `GeckoFxClient` 会根据 `GeckoFxRegistry` 查到的定义与覆写数据，在指定实体或世界坐标附着渲染。
