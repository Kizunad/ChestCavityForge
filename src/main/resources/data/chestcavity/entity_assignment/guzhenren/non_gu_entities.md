# ChestCavityForge 非蛊虫实体清单

整理自 `guzhenren` 模组，聚焦常规野兽类实体（MobCategory.MONSTER）且不属于蛊虫体系，可直接在 ChestCavityForge 扩展中引用。

## 狼类
- `guzhenren:dian_lang` — 电狼（`net/guzhenren/init/GuzhenrenModEntities.java:425`）
- `guzhenren:hao_dian_lang` — 嚎电狼（`net/guzhenren/init/GuzhenrenModEntities.java:426`）
- `guzhenren:lei_dian_lang` — 雷电狼（`net/guzhenren/init/GuzhenrenModEntities.java:427`）
- `guzhenren:lei_guan_tou_lang` — 雷冠头狼（`net/guzhenren/init/GuzhenrenModEntities.java:532`）
- `guzhenren:tu_lang` — 土狼（`net/guzhenren/init/GuzhenrenModEntities.java:428`）
- `guzhenren:hui_lang` — 灰狼（`net/guzhenren/init/GuzhenrenModEntities.java:429`）

## 熊类
- `guzhenren:xiong` — 褐熊（`net/guzhenren/init/GuzhenrenModEntities.java:430`）
- `guzhenren:hong_xiong` — 红熊（`net/guzhenren/init/GuzhenrenModEntities.java:431`）
- `guzhenren:hui_xiong` — 灰熊（`net/guzhenren/init/GuzhenrenModEntities.java:432`）
- `guzhenren:dian_xiong` — 电熊（`net/guzhenren/init/GuzhenrenModEntities.java:433`）
- `guzhenren:huoyanxiong` — 火焰熊（`net/guzhenren/init/GuzhenrenModEntities.java:558`）
- `guzhenren:lieyanxiong` — 烈焰熊（`net/guzhenren/init/GuzhenrenModEntities.java:559`）

## 虎类
- `guzhenren:hu` — 虎（`net/guzhenren/init/GuzhenrenModEntities.java:614`）
- `guzhenren:jinfenghu` — 金风虎（`net/guzhenren/init/GuzhenrenModEntities.java:615`）
- `guzhenren:jinrenwanghu` — 金刃王虎（`net/guzhenren/init/GuzhenrenModEntities.java:616`）
- `guzhenren:xiongshabaihu` — 熊煞白虎（`net/guzhenren/init/GuzhenrenModEntities.java:618`）

## 犬类
- `guzhenren:tu_quan` — 土犬（`net/guzhenren/init/GuzhenrenModEntities.java:663`）
- `guzhenren:yan_xu_quan` — 烟墟犬（`net/guzhenren/init/GuzhenrenModEntities.java:664`）
- `guzhenren:yao_ya_quan` — 咬牙犬（`net/guzhenren/init/GuzhenrenModEntities.java:665`）
- `guzhenren:yue_ao_yan_quan` — 月傲焰犬（`net/guzhenren/init/GuzhenrenModEntities.java:666`）
- `guzhenren:shan_mai_di_yan_quan` — 山脉地焰犬（`net/guzhenren/init/GuzhenrenModEntities.java:667`）

> 后续建议：为每类创建对应的 Chest Cavity type JSON，再在 `data/chestcavity/entity_assignment/` 下引用这些实体 ID，并在游戏内通过 `/summon <id>` 复核。
