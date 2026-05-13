package com.blackboxpro.fabric.action.client

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.getBooleanOrDefault
import com.blackboxpro.fabric.util.getStringOrNull
import com.blackboxpro.fabric.util.requireString
import com.blackboxpro.runtime.action.AsyncClientActionSupport
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.world.CreateWorldScreen
import net.minecraft.client.gui.screen.world.WorldCreator
import net.minecraft.world.Difficulty
import net.minecraft.world.GameMode
import org.tabooproject.reflex.Reflex.Companion.invokeMethod
import java.nio.file.Files
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

class CreateWorldAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("CreateWorldAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val client = MinecraftClient.getInstance()
        if (client.world != null || client.player != null) {
            return ActionResult.fail("Already in a world, leave the current world first")
        }

        val requestedWorldName = params.requireString("worldName").trim()
        validateWorldName(requestedWorldName)?.let { return ActionResult.fail(it) }

        if (client.levelStorage.levelExists(requestedWorldName)) {
            return ActionResult.fail("World already exists: $requestedWorldName")
        }

        val mode = parseMode(params.getStringOrNull("gameMode"))
        val difficulty = parseDifficulty(params.getStringOrNull("difficulty"))
        val allowCommands = params.getBooleanOrDefault("allowCommands", mode.gameMode == GameMode.CREATIVE)
        val generateStructures = params.getBooleanOrDefault("generateStructures", true)
        val bonusChest = params.getBooleanOrDefault("bonusChest", false)
        val seedText = params.getStringOrNull("seed")?.trim()?.takeIf { it.isNotEmpty() }
        val terminal = AtomicReference<ActionResult?>(null)

        return AsyncClientActionSupport.startPolling(
            commandId = commandId,
            executeOnMainThread = { task -> client.execute(task) },
            timeoutMs = 90_000L,
            timeoutMessage = "Timed out waiting for world creation: $requestedWorldName",
            startAction = {
                try {
                    // Build the LevelInfo and GeneratorOptionsHolder using reflection
                    // since the exact Yarn class/method names vary by version
                    val levelInfo = buildLevelInfo(requestedWorldName, mode, difficulty, allowCommands)
                    val holder = buildGeneratorOptionsHolder(generateStructures, bonusChest, seedText)
                    val tempDir = Files.createTempDirectory("bbp-create-world-")

                    // Use reflection to call CreateWorldScreen.create with the
                    // dynamically-resolved parameter types
                    val createWorldScreenClass = Class.forName("net.minecraft.client.gui.screen.world.CreateWorldScreen")
                    val levelInfoClass = Class.forName("net.minecraft.world.level.LevelInfo")
                    val holderClass = Class.forName("net.minecraft.world.level.levelgen.GeneratorOptionsHolder")
                    createWorldScreenClass.getMethod("create", levelInfoClass, holderClass, java.nio.file.Path::class.java)
                        .invoke(null, levelInfo, holder, tempDir)
                } catch (t: Throwable) {
                    terminal.set(ActionResult.fail("Failed to open create world screen: ${t.message}"))
                }

                client.execute {
                    try {
                        val screen = client.currentScreen as? CreateWorldScreen
                            ?: throw IllegalStateException("Create world screen did not open")
                        val creator = screen.worldCreator
                        creator.setWorldName(requestedWorldName)
                        creator.setGameMode(mode.worldCreatorMode)
                        creator.setDifficulty(difficulty)
                        creator.setCheatsEnabled(allowCommands)
                        creator.setGenerateStructures(generateStructures)
                        creator.setBonusChestEnabled(bonusChest)
                        if (seedText != null) {
                            creator.setSeed(seedText)
                        }

                        screen.invokeMethod<Any?>("createLevel")
                    } catch (t: Throwable) {
                        terminal.set(ActionResult.fail("Failed to create world: ${t.message}"))
                    }
                }
            },
            poll = {
                terminal.get()?.let { return@startPolling it }
                if (client.world != null && client.player != null) {
                    ActionResult.ok(
                        "Created and entered world: $requestedWorldName",
                        JsonObject().apply {
                            addProperty("worldName", requestedWorldName)
                            addProperty("state", "in_world")
                        }
                    )
                } else {
                    null
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildLevelInfo(
        worldName: String,
        mode: WorldMode,
        difficulty: Difficulty,
        allowCommands: Boolean
    ): Any {
        val levelInfoClass = Class.forName("net.minecraft.world.level.LevelInfo")
        val gameRulesClass = Class.forName("net.minecraft.world.GameRules")
        val gameRules = gameRulesClass.getDeclaredConstructor().also { it.isAccessible = true }.newInstance()

        val worldDataConfigClass = try {
            Class.forName("net.minecraft.world.WorldDataConfiguration")
        } catch (_: ClassNotFoundException) {
            Class.forName("net.minecraft.world.level.WorldDataConfiguration")
        }
        val defaultDataConfig = worldDataConfigClass.getDeclaredField("DEFAULT").get(null)

        val constructor = levelInfoClass.constructors.firstOrNull { c ->
            c.parameterCount == 7
        } ?: throw IllegalStateException("Cannot find LevelInfo constructor with 7 params")

        return constructor.newInstance(
            worldName,
            mode.gameMode,
            mode.hardcore,
            difficulty,
            allowCommands,
            gameRules,
            defaultDataConfig
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildGeneratorOptionsHolder(
        generateStructures: Boolean,
        bonusChest: Boolean,
        seedText: String?
    ): Any {
        val worldOptionsClass = Class.forName("net.minecraft.world.level.levelgen.WorldOptions")

        val seed = if (seedText != null) {
            try {
                val parseSeedMethod = worldOptionsClass.getMethod("parseSeed", String::class.java)
                val optionalLong = parseSeedMethod.invoke(null, seedText)
                val isPresent = optionalLong.javaClass.getMethod("isPresent").invoke(optionalLong) as Boolean
                if (isPresent) optionalLong.javaClass.getMethod("asLong").invoke(optionalLong) as Long
                else worldOptionsClass.getMethod("randomSeed").invoke(null) as Long
            } catch (_: NoSuchMethodException) {
                seedText.toLongOrNull() ?: seedText.hashCode().toLong()
            }
        } else {
            worldOptionsClass.getMethod("randomSeed").invoke(null) as Long
        }

        val worldOptions = worldOptionsClass.getConstructor(
            Long::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        ).newInstance(seed, generateStructures, bonusChest)

        val dynamicRegistryClass = Class.forName("net.minecraft.registry.DynamicRegistryManager")
        val simpleRegistry = dynamicRegistryClass.getMethod("createSimple").invoke(null)

        val holderClass = Class.forName("net.minecraft.world.level.levelgen.GeneratorOptionsHolder")

        return try {
            holderClass.getMethod("withDefault", dynamicRegistryClass, worldOptionsClass)
                .invoke(null, simpleRegistry, worldOptions)
        } catch (_: NoSuchMethodException) {
            try {
                holderClass.getConstructor(dynamicRegistryClass, worldOptionsClass)
                    .newInstance(simpleRegistry, worldOptions)
            } catch (_: NoSuchMethodException) {
                // Last resort: create via its create method if it has one
                holderClass.getMethod("create", dynamicRegistryClass, worldOptionsClass)
                    .invoke(null, simpleRegistry, worldOptions)
            }
        }
    }

    private fun parseMode(raw: String?): WorldMode =
        when ((raw ?: "survival").trim().lowercase(Locale.ROOT)) {
            "survival" -> WorldMode(GameMode.SURVIVAL, WorldCreator.Mode.SURVIVAL)
            "creative" -> WorldMode(GameMode.CREATIVE, WorldCreator.Mode.CREATIVE)
            "hardcore" -> WorldMode(GameMode.SURVIVAL, WorldCreator.Mode.HARDCORE)
            else -> throw IllegalArgumentException("Invalid gameMode: $raw")
        }

    private fun parseDifficulty(raw: String?): Difficulty =
        when ((raw ?: "normal").trim().lowercase(Locale.ROOT)) {
            "peaceful" -> Difficulty.PEACEFUL
            "easy" -> Difficulty.EASY
            "normal" -> Difficulty.NORMAL
            "hard" -> Difficulty.HARD
            else -> throw IllegalArgumentException("Invalid difficulty: $raw")
        }

    private fun validateWorldName(worldName: String): String? = when {
        worldName.isBlank() -> "worldName cannot be blank"
        INVALID_WORLD_NAME.any(worldName::contains) -> "worldName contains invalid filesystem characters"
        worldName.contains("..") -> "worldName cannot contain '..'"
        else -> null
    }

    private data class WorldMode(
        val gameMode: GameMode,
        val worldCreatorMode: WorldCreator.Mode
    ) {
        val hardcore: Boolean get() = worldCreatorMode == WorldCreator.Mode.HARDCORE
    }

    companion object {
        private val INVALID_WORLD_NAME = charArrayOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
    }
}
