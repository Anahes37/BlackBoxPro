# BlackBoxPro Action 完整目录

按功能分类，共 108 个 Action。每个表格列出 action ID、参数和简要说明。

> 这是便于检索的参考快照；若与运行时代码不一致，以 `common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt` 与各端 `ActionRegistry.kt` 为准。

## 移动与位置（8）

| Action | 参数 | 说明 |
|--------|------|------|
| `player_move` | `x`, `y`, `z`, `speed`, `timeout` | 移动到目标坐标（物理引擎驱动） |
| `player_move_look` | `x`, `y`, `z`, `pitch`, `speed`, `timeout` | 移动并面向目标（yaw 自动计算） |
| `player_look` | `yaw`, `pitch`, `onGround` | 设置视角朝向 |
| `player_on_ground` | `onGround` | 发送地面状态包 |
| `confirm_teleportation` | `teleportId` | 确认服务端传送 |
| `move_vehicle` | `x`, `y`, `z`, `yaw`, `pitch` | 移动载具 |
| `paddle_boat` | `leftPaddling`, `rightPaddling` | 划船 |
| `player_input` | `forward`, `backward`, `left`, `right`, `jump`, `sneak`, `sprint` | 注入键盘输入 |

## 方块交互（5）

| Action | 参数 | 说明 |
|--------|------|------|
| `dig_start` | `x`, `y`, `z`, `face`, `sequence` | 开始挖掘方块 |
| `dig_cancel` | `x`, `y`, `z`, `face`, `sequence` | 取消挖掘 |
| `dig_finish` | `x`, `y`, `z`, `face`, `sequence` | 完成挖掘 |
| `place_block` | `x`, `y`, `z`, `face`, `hand`, `cursorX`, `cursorY`, `cursorZ`, `insideBlock`, `sequence` | 放置方块 |
| `use_item` | `hand`, `sequence` | 使用手持物品 |

## 实体交互（4）

| Action | 参数 | 说明 |
|--------|------|------|
| `attack_entity` | `entityId`, `sneaking` | 攻击实体 |
| `interact_entity` | `entityId`, `hand`, `sneaking` | 与实体交互 |
| `interact_entity_at` | `entityId`, `targetX`, `targetY`, `targetZ`, `hand`, `sneaking` | 在指定位置与实体交互 |
| `swing_arm` | `hand` | 挥手动画 |

## 容器/GUI 操作（11）

| Action | 参数 | 说明 |
|--------|------|------|
| `click_slot` | `windowId`, `stateId`, `slot`, `button`, `mode` | 点击容器槽位 |
| `click_button` | `windowId`, `buttonId` | 点击容器按钮 |
| `close_container` | `windowId` | 关闭容器 |
| `set_carried_item` | `slot` | 切换快捷栏选中槽位 |
| `creative_set_slot` | `slot` | 创造模式设置槽位 |
| `pick_item` | `x`, `y`, `z`, `includeData` | 拾取方块物品（鼠标中键） |
| `pick_entity` | `entityId`, `includeData` | 拾取实体物品 |
| `pick_item_from_block` | `x`, `y`, `z`, `includeData` | 从方块拾取 |
| `pick_item_from_entity` | `entityId`, `includeData` | 从实体拾取 |
| `bundle_selected_slot` | `slotId`, `selectedIndex` | 收纳袋槽位选择 |
| `slot_state_change` | `windowId`, `slotId`, `state` | 槽位状态变更 |

## 玩家状态与动作（16）

| Action | 参数 | 说明 |
|--------|------|------|
| `sneak_start` | 无 | 开始潜行 |
| `sneak_stop` | 无 | 停止潜行 |
| `sprint_start` | 无 | 开始冲刺 |
| `sprint_stop` | 无 | 停止冲刺 |
| `leave_bed` | 无 | 离开床 |
| `horse_jump_start` | `jumpBoost` | 马匹跳跃开始 |
| `horse_jump_stop` | 无 | 马匹跳跃停止 |
| `open_horse_inventory` | 无 | 打开马匹背包 |
| `elytra_start` | 无 | 开始鞘翅飞行 |
| `drop_item` | 无 | 丢弃单个物品 |
| `drop_item_stack` | 无 | 丢弃整组物品 |
| `finish_using` | `sequence` | 结束物品使用（如吃食物） |
| `swap_hands` | 无 | 主副手交换 |
| `perform_respawn` | 无 | 执行重生 |
| `spectator_teleport` | `targetUuid` | 旁观者传送 |
| `jump` | 无 | 跳跃 |

## 聊天与命令（2）

| Action | 参数 | 说明 |
|--------|------|------|
| `chat_message` | `message` | 发送聊天消息 |
| `chat_command` | `command` | 执行聊天命令（不含 `/`） |

## 客户端会话与设置（9）

| Action | 参数 | 说明 |
|--------|------|------|
| `client_information` | `locale`, `viewDistance`, `chatMode`, `chatColors`, `skinParts`, `mainHand`, `textFiltering`, `allowServerListings` | 发送客户端设置 |
| `player_abilities` | `flying` | 设置飞行状态 |
| `resource_pack_response` | `uuid`, `result` | 资源包响应 |
| `screenshot` | `testId`, `prefix`, `playerName` | 截图保存到磁盘 |
| `connect_to_server` | `ip`, `port` | 连接到多人服务器 |
| `close_screen` | 无 | 关闭当前 GUI / 初始化界面 |
| `create_world` | `worldName`(必填), `gameMode`, `difficulty`, `allowCommands`, `generateStructures`, `bonusChest`, `seed` | 创建单人世界 |
| `join_world` | `worldName`(必填) | 加入已有单人世界 |
| `leave_world` | 无 | 离开当前世界并回到主菜单 |

## 进阶交互（17）

| Action | 参数 | 说明 |
|--------|------|------|
| `edit_book` | `slot`, `pages` | 编辑书与笔 |
| `sign_book` | `slot`, `title`, `pages` | 署名书 |
| `update_sign` | `x`, `y`, `z`, `isFrontText`, `lines` | 编辑告示牌 |
| `update_command_block` | `x`, `y`, `z`, `command`, `mode`, `trackOutput`, `conditional`, `alwaysActive` | 更新命令方块 |
| `update_command_block_minecart` | `entityId`, `command`, `trackOutput` | 更新命令方块矿车 |
| `update_structure_block` | `x`, `y`, `z`, `action`, `mode`, `name`, `offsetX/Y/Z`, `sizeX/Y/Z`, `mirror`, `rotation`, `metadata`, `integrity`, `seed`, `flags` | 更新结构方块 |
| `update_jigsaw_block` | `x`, `y`, `z`, `name`, `target`, `pool`, `finalState`, `jointType`, `selectionPriority`, `placementPriority` | 更新拼图方块 |
| `select_recipe` | `windowId`, `recipeIndex`, `makeAll` | 选择配方 |
| `recipe_book_toggle` | `category`, `open`, `filtering` | 切换配方书 |
| `recipe_book_seen` | `recipeIndex` | 标记配方已查看 |
| `query_entity_nbt` | `transactionId`, `entityId` | 查询实体 NBT |
| `query_block_nbt` | `transactionId`, `x`, `y`, `z` | 查询方块 NBT |
| `set_beacon_effect` | `primaryEffect`, `secondaryEffect` | 设置信标效果 |
| `rename_item` | `name` | 铁砧重命名 |
| `select_trade` | `selectedSlot` | 选择村民交易 |
| `lock_difficulty` | `locked` | 锁定难度 |
| `advancement_tab` | `action`, `tabId` | 成就页签操作 |

## 调试与特殊（6）

| Action | 参数 | 说明 |
|--------|------|------|
| `custom_payload` | `channel`, `data` | 发送自定义数据包 |
| `tab_complete` | `transactionId`, `text` | Tab 补全请求 |
| `keep_alive` | `id` | 心跳包 |
| `pong` | `parameter` | Pong 响应 |
| `debug_sample_subscription` | `type` | 调试采样订阅 |
| `chunk_batch_received` | `desiredChunksPerTick` | 区块批次确认 |

## 复合行为（16）

| Action | 参数 | 说明 |
|--------|------|------|
| `look_at` | `x`, `y`, `z` | 看向坐标 |
| `look_at_entity` | `entityId` | 看向实体 |
| `look_at_block` | `x`, `y`, `z`, `face` | 看向方块面 |
| `break_block` | `x`, `y`, `z` | 挖掘方块（完整流程） |
| `place_block_at` | `x`, `y`, `z`, `face`, `hand` | 在坐标放置方块 |
| `attack` | `entityId` | 攻击（含朝向） |
| `use` | `hand` | 使用（含朝向） |
| `open_container` | `x`, `y`, `z` | 打开指定坐标的容器 |
| `container_transfer` | `windowId`, `stateId`, `slot` | 容器物品转移 |
| `drop_inventory` | `slot` | 丢弃指定槽位 |
| `pathfind_to` | `x`, `y`, `z`, `speed` | A* 寻路到坐标 |
| `navigate_to` | `x`, `y`, `z`, `speed`, `timeout`, `allowJump` | 高级导航 |
| `batch` | `actions[]` | 批量顺序执行多个 action |
| `wait` | `ticks` | 等待指定 tick 数 |
| `respawn` | 无 | 重生（含等待） |
| `craft_recipe` | `windowId`, `recipeIndex`, `makeAll` | 自动合成 |

## 查询行为（14）

| Action | 参数 | 说明 |
|--------|------|------|
| `query_player_state` | 无 | 玩家完整状态（位置/生命/饥饿/装甲/维度等） |
| `query_held_item` | `hand` | 手持物品详情 |
| `query_inventory_slot` | `slot` | 指定槽位物品 |
| `query_chat_history` | `count`, `filter`, `since` | 聊天历史 |
| `query_nearby_entities` | `radius`, `type`, `limit` | 附近实体列表 |
| `query_container_state` | 无 | 当前容器状态 |
| `query_container_slots` | `windowId`, `slots` | 容器槽位详情 |
| `query_active_effects` | 无 | 当前药水效果 |
| `query_block_state` | `x`, `y`, `z` | 方块状态与属性 |
| `query_world_state` | 无 | 世界时间/天气/难度等 |
| `query_tab_list` | `limit` | Tab 列表玩家信息 |
| `query_scoreboard` | `objective` | 记分板状态 |
| `query_screen_state` | 无 | 当前屏幕/GUI 信息 |
| `query_boss_bar` | 无 | Boss 血条信息 |

## 1.12.2 不支持的 Action（11）

以下 Action 仅在 1.21.x 可用：

`bundle_selected_slot`, `chunk_batch_received`, `debug_sample_subscription`, `pick_entity`, `pick_item_from_block`, `pick_item_from_entity`, `pong`, `query_block_nbt`, `query_entity_nbt`, `slot_state_change`, `update_jigsaw_block`

## 仅 Plugin 端 Action（2）

以下 Action 只能通过 Plugin HTTP (`:38080`) 调用：

| Action | 参数 | 说明 |
|--------|------|------|
| `run_test` | `player`(必填), `scope`(`smoke`/`full`), `category` | 执行自动化测试套件 |
| `stop_server` | 无 | 安全停止服务端 |
