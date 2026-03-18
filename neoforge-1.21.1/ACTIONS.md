# BlackBoxPro Actions 完整参考

所有 Action 通过 HTTP 统一调用：

```
POST http://127.0.0.1:25580/api/command
Content-Type: application/json

{"action": "action_id", "params": {...}}
```

参数类型说明：必填参数标记 `*`，可选参数标记默认值。

---

## 移动与位置（movement）

### player_move

通过输入注入移动到目标坐标，由 MC 物理引擎处理碰撞/重力。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Double | * | - | 目标 X |
| y | Double | * | - | 目标 Y |
| z | Double | * | - | 目标 Z |
| speed | Double | | 1.0 | 移动速度（0.1-2.0） |
| timeout | Int | | 200 | 超时 tick 数 |

### player_move_look

移动到目标坐标并调整视角（yaw 自动朝向目标）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Double | * | - | 目标 X |
| y | Double | * | - | 目标 Y |
| z | Double | * | - | 目标 Z |
| pitch | Double | * | - | 俯仰角 |
| speed | Double | | 1.0 | 移动速度（0.1-2.0） |
| timeout | Int | | 200 | 超时 tick 数 |

### player_look

设置玩家视角朝向。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| yaw | Double | * | - | 偏航角 |
| pitch | Double | * | - | 俯仰角 |
| onGround | Boolean | | true | 是否在地面 |

### player_on_ground

设置玩家地面状态。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| onGround | Boolean | * | - | 是否在地面 |

### confirm_teleportation

确认服务端传送。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| teleportId | Int | * | - | 传送 ID |

### move_vehicle

移动载具到指定位置。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Double | * | - | 载具 X |
| y | Double | * | - | 载具 Y |
| z | Double | * | - | 载具 Z |
| yaw | Double | * | - | 偏航角 |
| pitch | Double | * | - | 俯仰角 |

### paddle_boat

控制船桨划动。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| leftPaddling | Boolean | * | - | 左桨划动 |
| rightPaddling | Boolean | * | - | 右桨划动 |

### player_input

发送玩家移动输入包。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| forward | Boolean | | false | 前进 |
| backward | Boolean | | false | 后退 |
| left | Boolean | | false | 左移 |
| right | Boolean | | false | 右移 |
| jump | Boolean | | false | 跳跃 |
| sneak | Boolean | | false | 潜行 |

## 方块交互（block）

### dig_start

开始挖掘方块。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 方块 X |
| y | Int | * | - | 方块 Y |
| z | Int | * | - | 方块 Z |
| face | String | * | - | 方向（top/bottom/north/south/east/west） |
| sequence | Int | | 0 | 序列号 |

### dig_cancel

取消挖掘方块。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 方块 X |
| y | Int | * | - | 方块 Y |
| z | Int | * | - | 方块 Z |
| face | String | * | - | 方向 |
| sequence | Int | | 0 | 序列号 |

### dig_finish

完成挖掘方块。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 方块 X |
| y | Int | * | - | 方块 Y |
| z | Int | * | - | 方块 Z |
| face | String | * | - | 方向 |
| sequence | Int | | 0 | 序列号 |

### place_block

放置方块或与方块交互。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 方块 X |
| y | Int | * | - | 方块 Y |
| z | Int | * | - | 方块 Z |
| face | String | * | - | 方向 |
| hand | String | | main_hand | 手（main_hand/off_hand） |
| cursorX | Double | | 0.5 | 光标 X（0-1） |
| cursorY | Double | | 0.5 | 光标 Y（0-1） |
| cursorZ | Double | | 0.5 | 光标 Z（0-1） |
| insideBlock | Boolean | | false | 是否在方块内 |
| sequence | Int | | 0 | 序列号 |

### use_item

使用手中物品。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| hand | String | | main_hand | 手 |
| sequence | Int | | 0 | 序列号 |

## 实体交互（entity）

### attack_entity

攻击实体。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| entityId | Int | * | - | 实体 ID |
| sneaking | Boolean | | false | 是否潜行 |

### interact_entity

与实体交互。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| entityId | Int | * | - | 实体 ID |
| hand | String | | main_hand | 手 |
| sneaking | Boolean | | false | 是否潜行 |

### interact_entity_at

在指定位置与实体交互。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| entityId | Int | * | - | 实体 ID |
| targetX | Double | * | - | 交互点 X |
| targetY | Double | * | - | 交互点 Y |
| targetZ | Double | * | - | 交互点 Z |
| hand | String | | main_hand | 手 |
| sneaking | Boolean | | false | 是否潜行 |

### swing_arm

挥动手臂。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| hand | String | | main_hand | 手 |

## 容器/GUI（container）

### click_slot

点击容器槽位。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| windowId | Int | * | - | 窗口 ID |
| stateId | Int | * | - | 状态 ID |
| slot | Int | * | - | 槽位 |
| button | Int | * | - | 按钮（0=左键/1=右键） |
| mode | Int | * | - | 模式（0=PICKUP/1=QUICK_MOVE/2=SWAP/3=CLONE/4=THROW/5=QUICK_CRAFT/6=PICKUP_ALL） |

### click_button

点击容器按钮。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| windowId | Int | * | - | 窗口 ID |
| buttonId | Int | * | - | 按钮 ID |

### close_container

关闭容器。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| windowId | Int | | 当前容器 | 窗口 ID |

### open_inventory

打开玩家背包界面。无参数。

### set_carried_item

切换快捷栏选中槽位。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| slot | Int | * | - | 槽位（0-8） |

### creative_set_slot

创造模式设置槽位物品。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| slot | Int | * | - | 槽位 |

### pick_item

中键拾取物品。无参数。

### pick_entity

中键拾取实体。无参数。

### pick_item_from_block

从方块中键拾取物品。无参数。

### pick_item_from_entity

从实体中键拾取物品。无参数。

### slot_state_change

切换槽位状态（如合成书配方锁定）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| windowId | Int | * | - | 窗口 ID |
| slotId | Int | * | - | 槽位 ID |
| newState | Boolean | * | - | 新状态 |

## 玩家状态（player）

### sneak_start / sneak_stop

开始/停止潜行。无参数。

### sprint_start / sprint_stop

开始/停止冲刺。无参数。

### jump

跳跃。无参数。

### drop_item

丢弃单个物品。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| sequence | Int | | 0 | 序列号 |

### drop_item_stack

丢弃整组物品。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| sequence | Int | | 0 | 序列号 |

### swap_hands

交换主副手物品。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| sequence | Int | | 0 | 序列号 |

### finish_using

完成使用物品（如吃食物）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| sequence | Int | | 0 | 序列号 |

### leave_bed

离开床。无参数。

### elytra_start

开始鞘翅飞行。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| entityId | Int | | 玩家 ID | 实体 ID |
| jumpBoost | Int | | 100 | 跳跃助力 |

### horse_jump_start

开始马跳跃。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| entityId | Int | | 玩家 ID | 实体 ID |
| jumpBoost | Int | | 100 | 跳跃助力 |

### horse_jump_stop

停止马跳跃。无参数。

### open_horse_inventory

打开马背包。无参数。

### perform_respawn

执行重生。无参数。

### spectator_teleport

旁观者模式传送到目标玩家。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| targetUuid | String | * | - | 目标玩家 UUID |

## 聊天命令（chat）

### chat_message

发送聊天消息。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| message | String | * | - | 消息内容（≤256 字符） |

### chat_command

执行命令（不含 `/` 前缀）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| command | String | * | - | 命令内容 |

## 客户端设置（client）

### client_information

发送客户端设置信息。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| locale | String | | en_us | 语言代码 |
| viewDistance | Int | | 12 | 视距 |
| chatMode | Int | | 0 | 聊天模式 |
| chatColors | Boolean | | true | 聊天颜色 |
| skinParts | Int | | 127 | 皮肤部分 |
| mainHand | Int | | 1 | 主手（0=左/1=右） |
| textFiltering | Boolean | | false | 文本过滤 |
| allowServerListings | Boolean | | true | 允许服务器列表 |

### player_abilities

设置玩家能力（飞行）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| flying | Boolean | * | - | 是否飞行 |

### resource_pack_response

响应资源包请求。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| uuid | String | * | - | 资源包 UUID |
| result | String | * | - | 结果（accepted/declined/failed/loaded） |

### screenshot

截取客户端屏幕。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| playerName | String | | 当前玩家名 | 玩家名（用于目录） |
| testId | String | | default | 测试 ID（用于子目录） |
| prefix | String | | - | 文件名前缀 |

## 世界管理（client/world）

### create_world

创建新的单机世界并加入（自动开启作弊）。异步 action。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| name | String | * | - | 世界名称 |
| timeout | Int | | 1200 | 超时 tick 数（默认 60s） |

### join_world

加入已有的单机世界。异步 action。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| levelName | String | * | - | saves/ 下的世界文件夹名 |
| timeout | Int | | 600 | 超时 tick 数（默认 30s） |

### leave_world

退出当前世界回到主菜单。异步 action。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| timeout | Int | | 600 | 超时 tick 数（默认 30s） |

## 进阶交互（advanced）

### edit_book

编辑书与笔的页面内容。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| slot | Int | * | - | 物品栏槽位 |
| pages | JsonArray | * | - | 页面内容数组 |
| title | String | | - | 书籍标题 |

### sign_book

签名书与笔。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| slot | Int | * | - | 物品栏槽位 |
| title | String | * | - | 书籍标题 |

### update_sign

更新告示牌文本。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 告示牌 X |
| y | Int | * | - | 告示牌 Y |
| z | Int | * | - | 告示牌 Z |
| isFrontText | Boolean | | true | 是否正面 |
| lines | JsonArray | * | - | 4 行文本 |

### update_command_block

更新命令方块。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 命令方块 X |
| y | Int | * | - | 命令方块 Y |
| z | Int | * | - | 命令方块 Z |
| command | String | * | - | 命令 |
| mode | Int | | 2 | 模式（0=SEQUENCE/1=AUTO/2=REDSTONE） |
| trackOutput | Boolean | | true | 追踪输出 |
| conditional | Boolean | | false | 条件执行 |
| alwaysActive | Boolean | | false | 始终活跃 |

### update_command_block_minecart

更新命令方块矿车。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| entityId | Int | * | - | 实体 ID |
| command | String | * | - | 命令 |
| trackOutput | Boolean | | true | 追踪输出 |

### update_structure_block

更新结构方块。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 结构方块 X |
| y | Int | * | - | 结构方块 Y |
| z | Int | * | - | 结构方块 Z |
| action | Int | * | - | 动作 |
| mode | String | * | - | 模式 |
| name | String | * | - | 名称 |
| offsetX | Int | | 0 | 偏移 X |
| offsetY | Int | | 0 | 偏移 Y |
| offsetZ | Int | | 0 | 偏移 Z |
| sizeX | Int | | 0 | 大小 X |
| sizeY | Int | | 0 | 大小 Y |
| sizeZ | Int | | 0 | 大小 Z |
| mirror | String | | none | 镜像 |
| rotation | String | | none | 旋转 |
| metadata | String | | - | 元数据 |
| integrity | Float | | 1.0 | 完整性 |
| seed | Long | | 0 | 种子 |
| flags | Int | | 0 | 标志 |

### update_jigsaw_block

更新拼图方块。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 拼图方块 X |
| y | Int | * | - | 拼图方块 Y |
| z | Int | * | - | 拼图方块 Z |
| name | String | * | - | 名称 |
| target | String | * | - | 目标 |
| pool | String | * | - | 池 |
| finalState | String | | - | 最终状态 |
| jointType | String | | rollable | 关节类型 |
| selectionPriority | Int | | 0 | 选择优先级 |
| placementPriority | Int | | 0 | 放置优先级 |

### select_recipe

选择配方。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| recipeId | String | * | - | 配方 ID |

### recipe_book_toggle

切换配方书状态。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| category | String | * | - | 分类 |
| open | Boolean | * | - | 是否打开 |
| filtering | Boolean | * | - | 是否过滤 |

### recipe_book_seen

标记配方已查看。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| recipeId | String | * | - | 配方 ID |

### select_trade

选择村民交易。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| selectedSlot | Int | * | - | 交易槽位 |

### set_beacon_effect

设置信标效果。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| primaryEffect | Int | | -1 | 主效果 ID |
| secondaryEffect | Int | | -1 | 副效果 ID |

### rename_item

铁砧重命名物品。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| name | String | * | - | 新名称 |

### lock_difficulty

锁定难度。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| locked | Boolean | * | - | 是否锁定 |

### advancement_tab

打开/关闭进度标签页。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| action | String | * | - | 动作（open/close） |
| tabId | String | | - | 标签 ID（open 时必填） |

### query_entity_nbt

查询实体 NBT 数据。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| transactionId | Int | * | - | 事务 ID |
| entityId | Int | * | - | 实体 ID |

### query_block_nbt

查询方块 NBT 数据。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| transactionId | Int | * | - | 事务 ID |
| x | Int | * | - | 方块 X |
| y | Int | * | - | 方块 Y |
| z | Int | * | - | 方块 Z |

## 调试（debug）

### keep_alive

发送 Keep Alive 包。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | Long | * | - | Keep Alive ID |

### pong

发送 Pong 包。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| parameter | Int | * | - | Pong 参数 |

### custom_payload

发送自定义 Payload 包。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| channel | String | * | - | 通道名 |
| data | String | * | - | 数据（Base64 编码） |

### tab_complete

请求 Tab 补全。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| transactionId | Int | * | - | 事务 ID |
| text | String | * | - | 补全文本 |

### debug_sample_subscription

订阅调试采样。无参数。

### chunk_batch_received

确认区块批次接收。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| desiredChunksPerTick | Float | | 7.0 | 每 tick 期望区块数 |

## 复合行为（composite）

### look_at

看向指定坐标。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Double | * | - | 目标 X |
| y | Double | * | - | 目标 Y |
| z | Double | * | - | 目标 Z |

### look_at_entity

看向指定实体。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| entityId | Int | * | - | 实体 ID |

### look_at_block

看向方块指定面的中心。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 方块 X |
| y | Int | * | - | 方块 Y |
| z | Int | * | - | 方块 Z |
| face | String | | top | 方向 |

### break_block

完整破坏方块（look_at + dig_start + 等待 + dig_finish）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 方块 X |
| y | Int | * | - | 方块 Y |
| z | Int | * | - | 方块 Z |

### place_block_at

看向方块并放置（look_at + place_block）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 方块 X |
| y | Int | * | - | 方块 Y |
| z | Int | * | - | 方块 Z |
| face | String | | top | 方向 |
| hand | String | | main_hand | 手 |

### attack

攻击实体（look_at_entity + attack_entity + swing_arm）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| entityId | Int | * | - | 实体 ID |

### use

使用物品（可选切换槽位）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| slot | Int | | -1 | 物品栏槽位（-1 不切换） |
| hand | String | | main_hand | 手 |

### open_container

看向方块容器并打开（look_at + place_block 交互）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 容器 X |
| y | Int | * | - | 容器 Y |
| z | Int | * | - | 容器 Z |
| hand | String | | main_hand | 手 |

### container_transfer

Shift+左键快速转移物品。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| windowId | Int | * | - | 窗口 ID |
| stateId | Int | * | - | 状态 ID |
| slot | Int | * | - | 槽位 |

### drop_inventory

丢弃指定槽位物品。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| slot | Int | * | - | 槽位 |

### pathfind_to

A* 寻路移动到目标坐标。异步 action。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Double | * | - | 目标 X |
| y | Double | * | - | 目标 Y |
| z | Double | * | - | 目标 Z |
| speed | Double | | 1.0 | 移动速度 |

### navigate_to

导航移动到目标坐标（支持跳跃）。异步 action。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Double | * | - | 目标 X |
| y | Double | * | - | 目标 Y |
| z | Double | * | - | 目标 Z |
| speed | Double | | 1.0 | 移动速度 |
| timeout | Int | | 配置值 | 超时 tick 数 |
| allowJump | Boolean | | true | 允许跳跃 |

### craft_recipe

在工作台/背包中合成配方。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| windowId | Int | * | - | 窗口 ID |
| recipeIndex | Int | * | - | 配方索引 |
| makeAll | Boolean | | false | 全部制作 |

### batch

批量执行多个 action。异步 action。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| actions | JsonArray | * | - | 动作数组，每项 `{"action":"id","params":{}}` |

### wait

等待指定 tick 数。异步 action。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| ticks | Int | | 0 | 等待 tick 数 |

### respawn

重生（perform_respawn 的复合封装）。无参数。

## 查询行为（query）

### query_player_state

查询玩家完整状态。无参数。

返回 data 字段：`x`, `y`, `z`, `yaw`, `pitch`, `health`, `maxHealth`, `food`, `saturation`, `gameMode`, `onGround`, `sneaking`, `sprinting`, `flying`, `dead`, `selectedSlot`, `experienceLevel`, `experienceProgress`, `absorption`, `armorValue`, `airSupply`, `maxAirSupply`, `isSwimming`, `isUsingItem`, `isFallFlying`, `fallDistance`, `vehicleId`, `dimension`, `biome`, `mainHandItem`

### query_held_item

查询手持物品信息。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| hand | String | | main_hand | 手 |

### query_inventory_slot

查询背包槽位物品。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| slot | Int | | -1 | 槽位（-1=当前选中） |

### query_container_state

查询当前打开的容器状态。无参数。

### query_container_slots

查询容器所有槽位。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| windowId | Int | | 当前容器 | 窗口 ID |

### query_block_state

查询指定坐标方块状态。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| x | Int | * | - | 方块 X |
| y | Int | * | - | 方块 Y |
| z | Int | * | - | 方块 Z |

### query_world_state

查询世界全局状态。无参数。

返回 data 字段：`timeOfDay`, `worldTime`, `raining`, `thundering`, `dimension`, `hasSkyLight`, `hasCeiling`, `difficulty`, `seaLevel`

### query_tab_list

查询 Tab 列表玩家。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | Int | | 100 | 最大玩家数 |

### query_scoreboard

查询记分板状态。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| objective | String | | - | 记分板目标（空则返回所有） |

### query_screen_state

查询当前屏幕/GUI 状态。无参数。

### query_boss_bar

查询 Boss Bar 状态。无参数。

### query_nearby_entities

查询附近实体。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| radius | Double | | 10.0 | 搜索半径 |
| type | String | | - | 实体类型过滤 |
| limit | Int | | 20 | 最大实体数 |

### query_chat_history

查询聊天历史。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| count | Int | | 10 | 消息数量 |
| filter | String | | - | 过滤关键词 |
| since | Long | | - | 时间戳过滤（毫秒） |

### query_active_effects

查询玩家当前活跃药水效果。无参数。
