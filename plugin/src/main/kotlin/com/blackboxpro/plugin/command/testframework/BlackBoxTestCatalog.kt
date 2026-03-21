package com.blackboxpro.plugin.command.testframework

import com.blackboxpro.common.action.ActionCatalog
import com.blackboxpro.common.action.ActionDefinition
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.Locale
import java.util.concurrent.CompletableFuture

object BlackBoxTestCatalog {

    private val unsupportedOn1122 = setOf(
        "bundle_selected_slot",
        "chunk_batch_received",
        "debug_sample_subscription",
        "pick_entity",
        "pick_item_from_block",
        "pick_item_from_entity",
        "pong",
        "query_block_nbt",
        "query_entity_nbt",
        "slot_state_change",
        "update_jigsaw_block"
    )

    private val pendingFixtureActions = setOf(
        "confirm_teleportation",
        "move_vehicle",
        "paddle_boat",
        "dig_start",
        "dig_cancel",
        "dig_finish",
        "place_block",
        "use_item",
        "attack_entity",
        "interact_entity",
        "interact_entity_at",
        "click_slot",
        "click_button",
        "close_container",
        "creative_set_slot",
        "pick_item",
        "pick_entity",
        "pick_item_from_block",
        "pick_item_from_entity",
        "bundle_selected_slot",
        "slot_state_change",
        "leave_bed",
        "horse_jump_start",
        "horse_jump_stop",
        "open_horse_inventory",
        "elytra_start",
        "drop_item",
        "drop_item_stack",
        "finish_using",
        "perform_respawn",
        "spectator_teleport",
        "edit_book",
        "sign_book",
        "update_sign",
        "update_command_block",
        "update_command_block_minecart",
        "update_structure_block",
        "update_jigsaw_block",
        "select_recipe",
        "recipe_book_toggle",
        "recipe_book_seen",
        "query_entity_nbt",
        "query_block_nbt",
        "set_beacon_effect",
        "rename_item",
        "select_trade",
        "custom_payload",
        "tab_complete",
        "pong",
        "debug_sample_subscription",
        "chunk_batch_received",
        "look_at_entity",
        "break_block",
        "place_block_at",
        "attack",
        "use",
        "open_container",
        "container_transfer",
        "drop_inventory",
        "pathfind_to",
        "respawn",
        "craft_recipe",
        "look_at_block",
        "navigate_to"
    )

    private val longRunningActions = setOf(
        "player_move",
        "player_move_look",
        "pathfind_to",
        "navigate_to",
        "break_block",
        "screenshot"
    )

    fun getCategories(): List<String> =
        ActionCatalog.getDefinitions().map { categoryOf(it.id) }.distinct().sorted()

    fun findAction(actionId: String): BlackBoxActionTestCase? =
        ActionCatalog.getDefinitions().firstOrNull { it.id == actionId }?.let(::buildCase)

    fun cases(profile: BlackBoxTestProfile, category: String? = null): List<BlackBoxActionTestCase> {
        val all = when (profile) {
            BlackBoxTestProfile.SMOKE -> smokeCases()
            BlackBoxTestProfile.FULL -> ActionCatalog.getDefinitions().map(::buildCase)
        }
        return if (category == null) all else all.filter { it.category.equals(category, ignoreCase = true) }
    }

    fun smokeCases(): List<BlackBoxActionTestCase> {
        val smokeIds = listOf(
            "player_on_ground",
            "player_move",
            "player_move_look",
            "player_look",
            "sneak_start",
            "sneak_stop",
            "sprint_start",
            "sprint_stop",
            "jump",
            "swap_hands",
            "swing_arm",
            "chat_message",
            "chat_command",
            "set_carried_item",
            "query_player_state",
            "query_held_item",
            "query_nearby_entities",
            "query_chat_history",
            "query_active_effects",
            "wait",
            "batch",
            "screenshot"
        )
        return smokeIds.mapNotNull(::findAction)
    }

    private fun buildCase(definition: ActionDefinition): BlackBoxActionTestCase {
        val actionId = definition.id
        val supportedProfiles = BlackBoxLoaderProfile.entries.filterTo(linkedSetOf()) { profile ->
            profile != BlackBoxLoaderProfile.MC_1122 || actionId !in unsupportedOn1122
        }

        return BlackBoxActionTestCase(
            actionId = actionId,
            displayName = prettify(actionId),
            category = categoryOf(actionId),
            supportedProfiles = supportedProfiles,
            timeoutMs = if (actionId in longRunningActions) 20000L else 5000L,
            prepare = { ctx ->
                when {
                    ctx.loaderProfile !in supportedProfiles ->
                        CompletableFuture.completedFuture(BlackBoxPrepareResult("当前版本不支持该 action"))
                    actionId in pendingFixtureActions ->
                        CompletableFuture.completedFuture(BlackBoxPrepareResult("该 action 需要专用夹具，当前框架已建模但夹具尚未补齐"))
                    else -> CompletableFuture.completedFuture(BlackBoxPrepareResult())
                }
            },
            execute = { ctx ->
                val params = defaultParams(actionId, ctx)
                ctx.sendAction(actionId, params, timeoutMs = if (actionId in setOf("player_move", "player_move_look")) 15000L else if (actionId == "screenshot") 20000L else 5000L)
            },
            verify = { _, response -> verify(actionId, response) }
        )
    }

    private fun verify(actionId: String, response: com.blackboxpro.plugin.channel.ResponseMessage): String? {
        if (!response.isSuccess) {
            return response.message ?: response.status
        }
        val data = response.data
        return when (actionId) {
            "screenshot" -> if (data?.has("filePath") == true) null else "截图响应缺少 filePath"
            "query_player_state",
            "query_held_item",
            "query_inventory_slot",
            "query_chat_history",
            "query_nearby_entities",
            "query_container_state",
            "query_container_slots",
            "query_active_effects",
            "query_block_state",
            "query_world_state",
            "query_tab_list",
            "query_scoreboard",
            "query_screen_state",
            "query_boss_bar" -> if (data != null && data.size() > 0) null else "查询响应没有返回数据"
            else -> null
        }
    }

    private fun defaultParams(actionId: String, ctx: BlackBoxTestContext): JsonObject = when (actionId) {
        "player_move" -> JsonObject().apply {
            val target = ctx.fixtureManager.relative(2.0, 0.0, 0.0)
            addProperty("x", target.x)
            addProperty("y", target.y)
            addProperty("z", target.z)
            addProperty("speed", 1.0)
            addProperty("timeout", 80)
        }
        "player_move_look" -> JsonObject().apply {
            val target = ctx.fixtureManager.relative(-2.0, 0.0, 0.0)
            addProperty("x", target.x)
            addProperty("y", target.y)
            addProperty("z", target.z)
            addProperty("pitch", 0.0f)
            addProperty("speed", 1.0)
            addProperty("timeout", 80)
        }
        "player_look" -> JsonObject().apply {
            addProperty("yaw", 90.0f)
            addProperty("pitch", -15.0f)
            addProperty("onGround", true)
        }
        "player_on_ground" -> JsonObject().apply { addProperty("onGround", true) }
        "player_input" -> JsonObject().apply {
            addProperty("forward", true)
            addProperty("backward", false)
            addProperty("left", false)
            addProperty("right", false)
            addProperty("jump", false)
            addProperty("sneak", false)
            addProperty("sprint", false)
        }
        "sneak_start", "sneak_stop", "sprint_start", "sprint_stop", "jump", "swap_hands", "leave_bed", "drop_item", "drop_item_stack", "perform_respawn" -> JsonObject()
        "chat_message" -> JsonObject().apply { addProperty("message", "[BlackBoxPro] full action test") }
        "chat_command" -> JsonObject().apply { addProperty("command", "me BlackBoxPro full test") }
        "set_carried_item", "creative_set_slot" -> JsonObject().apply { addProperty("slot", 0) }
        "client_information" -> JsonObject().apply {
            addProperty("locale", "zh_cn")
            addProperty("viewDistance", 8)
            addProperty("chatMode", 0)
            addProperty("chatColors", true)
            addProperty("skinParts", 127)
            addProperty("mainHand", 0)
            addProperty("textFiltering", false)
            addProperty("allowServerListings", true)
        }
        "player_abilities" -> JsonObject().apply { addProperty("flying", false) }
        "resource_pack_response" -> JsonObject().apply {
            addProperty("uuid", "00000000-0000-0000-0000-000000000000")
            addProperty("result", "accepted")
        }
        "keep_alive" -> JsonObject().apply { addProperty("id", 1L) }
        "look_at" -> JsonObject().apply {
            val target = ctx.fixtureManager.relative(0.0, 1.0, 4.0)
            addProperty("x", target.x)
            addProperty("y", target.y)
            addProperty("z", target.z)
        }
        "wait" -> JsonObject().apply { addProperty("ticks", 10) }
        "batch" -> JsonObject().apply {
            add("actions", JsonArray().apply {
                add(JsonObject().apply { addProperty("action", "sneak_start"); add("params", JsonObject()) })
                add(JsonObject().apply { addProperty("action", "wait"); add("params", JsonObject().apply { addProperty("ticks", 5) }) })
                add(JsonObject().apply { addProperty("action", "sneak_stop"); add("params", JsonObject()) })
            })
        }
        "query_held_item" -> JsonObject().apply { addProperty("hand", "main_hand") }
        "query_inventory_slot" -> JsonObject().apply { addProperty("slot", 0) }
        "query_chat_history" -> JsonObject().apply { addProperty("count", 5) }
        "query_nearby_entities" -> JsonObject().apply {
            addProperty("radius", 16.0)
            addProperty("limit", 20)
        }
        "query_container_slots" -> JsonObject().apply { addProperty("windowId", 0) }
        "query_block_state" -> JsonObject().apply {
            val target = ctx.fixtureManager.block(0, -1, 0)
            addProperty("x", target.blockX)
            addProperty("y", target.blockY)
            addProperty("z", target.blockZ)
        }
        "query_tab_list" -> JsonObject().apply { addProperty("limit", 10) }
        "query_scoreboard" -> JsonObject()
        "screenshot" -> JsonObject().apply {
            addProperty("testId", ctx.testId)
            addProperty("prefix", "catalog_${actionId}")
            addProperty("playerName", ctx.player.name)
        }
        else -> JsonObject()
    }

    private fun categoryOf(actionId: String): String = when {
        actionId.startsWith("query_") -> "query"
        actionId in setOf("look_at", "look_at_entity", "look_at_block", "pathfind_to", "navigate_to", "break_block", "place_block_at", "attack", "use", "open_container", "container_transfer", "drop_inventory", "wait", "batch", "respawn", "craft_recipe") -> "composite"
        actionId in setOf("chat_message", "chat_command") -> "chat"
        actionId in setOf("client_information", "player_abilities", "resource_pack_response", "screenshot") -> "client"
        actionId in setOf("custom_payload", "tab_complete", "keep_alive", "pong", "debug_sample_subscription", "chunk_batch_received") -> "debug"
        actionId in setOf("player_move", "player_move_look", "player_look", "player_on_ground", "confirm_teleportation", "move_vehicle", "paddle_boat", "player_input") -> "movement"
        actionId in setOf("dig_start", "dig_cancel", "dig_finish", "place_block", "use_item") -> "block"
        actionId in setOf("attack_entity", "interact_entity", "interact_entity_at", "swing_arm") -> "entity"
        actionId in setOf("click_slot", "click_button", "close_container", "set_carried_item", "creative_set_slot", "pick_item", "pick_entity", "pick_item_from_block", "pick_item_from_entity", "bundle_selected_slot", "slot_state_change") -> "container"
        actionId in setOf("sneak_start", "sneak_stop", "sprint_start", "sprint_stop", "leave_bed", "horse_jump_start", "horse_jump_stop", "open_horse_inventory", "elytra_start", "drop_item", "drop_item_stack", "finish_using", "swap_hands", "perform_respawn", "spectator_teleport", "jump") -> "player"
        actionId in setOf("edit_book", "sign_book", "update_sign", "update_command_block", "update_command_block_minecart", "update_structure_block", "update_jigsaw_block", "select_recipe", "recipe_book_toggle", "recipe_book_seen", "query_entity_nbt", "query_block_nbt", "set_beacon_effect", "rename_item", "select_trade", "lock_difficulty", "advancement_tab") -> "advanced"
        else -> "misc"
    }

    private fun prettify(actionId: String): String =
        actionId.split('_').joinToString(" ") { part ->
            part.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString() }
        }
}
