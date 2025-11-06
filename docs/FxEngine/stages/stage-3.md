# 阶段三（Stage 3）· 工具与示例

## 阶段目标

- 提供常用插值/形状/采样工具；撰写 Shockfield 集成示例（文档级）。

## 具体任务

- Interpolators：线性/指数/弹性；Shapes：圆环/扇形/路径；Samplers：采样 Wave 半径/振幅。
- 示例：
  - onWaveCreate 启动“环纹扩张” FX（tickInterval=2、TTL=5s）；
  - onSubwaveCreate 启动“次级扩散” FX（TTL=2s）；
  - onExtinguish 启动“收束消散” FX。

## 依赖关系

- Stage 2 完成。

## 验收标准

- 文档示例清晰；工具可复用；
- 运行时在预算关闭/开启两种模式下均可稳定运行。

