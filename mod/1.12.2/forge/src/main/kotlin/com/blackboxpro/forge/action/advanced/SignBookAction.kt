package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import io.netty.buffer.Unpooled

class SignBookAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val title = params.requireString("title")
        val pagesArray = params.getAsJsonArray("pages")
            ?: return ActionResult.fail("Missing required field: pages")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val mc = Minecraft.getMinecraft()
        val playerName = mc.player?.gameProfile?.name ?: "Unknown"

        val book = ItemStack(Items.WRITABLE_BOOK)
        val tag = NBTTagCompound()
        val pagesList = NBTTagList()
        for (i in 0 until pagesArray.size()) {
            pagesList.appendTag(NBTTagString(pagesArray[i].asString))
        }
        tag.setTag("pages", pagesList)
        tag.setString("title", title)
        tag.setString("author", playerName)
        book.tagCompound = tag

        val buf = PacketBuffer(Unpooled.buffer())
        buf.writeItemStack(book)
        connection.sendPacket(CPacketCustomPayload("MC|BSign", buf))

        return ActionResult.ok("Signed book '$title' with ${pagesArray.size()} pages")
    }
}
