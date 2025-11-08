# 剑脉蛊音效资源

此目录用于存放剑脉蛊（Jianmai Gu）的音效文件。

## 所需音效文件

### 1. heartbeat.ogg
- **用途**: 被动效果心跳音
- **描述**: 低沉的心跳声，频率约 60 BPM（每秒一次跳动）
- **时长**: 0.5 秒
- **音量**: 设计为较小音量（代码中会乘以 0.3 系数）
- **格式**: OGG Vorbis
- **建议来源**:
  - 录制真实心跳声
  - 使用音效库（如 freesound.org）
  - 使用 Audacity 合成低频脉冲

### 2. activate.ogg
- **用途**: 主动技能激活音效
- **描述**: "嗡~~~" 的电流激活声
- **结构**:
  - 初始 (0-0.2秒): 快速上升的电流声
  - 中期 (0.2-0.6秒): 持续的嗡鸣声
  - 结束 (0.6-0.8秒): 快速下降的电流衰减
- **时长**: 0.8 秒
- **音量**: 设计为正常音量（代码中会乘以 1.0 系数）
- **格式**: OGG Vorbis
- **建议来源**:
  - 使用合成器生成电流声
  - 音效库中的"信标激活"或"能量充能"音效
  - Minecraft 中的 BEACON_ACTIVATE 作为参考

## 临时占位符

当前代码使用 Minecraft 原版音效作为临时占位符：
- `heartbeat.ogg` → `SoundEvents.BEACON_AMBIENT`
- `activate.ogg` → `SoundEvents.BEACON_ACTIVATE`

## 音效制作建议

### 心跳音效
```bash
# 使用 sox 工具生成示例心跳音
sox -n heartbeat.ogg synth 0.5 sine 50 fade 0.1 0.5 0.2 vol 0.5
```

### 激活音效
```bash
# 使用 sox 工具生成示例电流音
sox -n activate.ogg synth 0.8 sine 200-400 fade 0.2 0.8 0.2 vol 0.7
```

## 文件规范

- **格式**: OGG Vorbis
- **采样率**: 44100 Hz 或 48000 Hz
- **比特率**: 128 kbps 或更高
- **声道**: 单声道（Mono）或立体声（Stereo）

## 集成说明

音效文件放置在此目录后，需要确保：
1. 文件名与 `sounds.json` 中的配置匹配
2. 文件格式为 `.ogg`
3. 在游戏中测试音效的音量和音高
4. 根据需要调整 `JianmaiTuning.java` 中的音量参数

## 相关代码

- 音效配置: `src/main/resources/assets/guzhenren/sounds.json`
- 音效调用: `src/main/java/.../jian_dao/fx/JianmaiAudioEffects.java`
- 音量参数: `src/main/java/.../jian_dao/tuning/JianmaiTuning.java`
