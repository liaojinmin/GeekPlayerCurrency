package me.geek.player

import me.geek.player.service.DataManager
import org.bukkit.Bukkit
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.console
import taboolib.platform.BukkitPlugin

/**
 * @作者: 老廖
 * @时间: 2023/6/11 14:57
 * @包: me.geek.player
 */
@RuntimeDependencies(
    RuntimeDependency(value = "!com.zaxxer:HikariCP:4.0.3",
        relocate = ["!com.zaxxer.hikari", "!com.zaxxer.hikari_4_0_3_player"]),
)
@PlatformSide([Platform.BUKKIT])
object GeekPlayerCurrency: Plugin() {

    val instance by lazy { BukkitPlugin.getInstance() }

    const val VERSION = 1.0

    const val pluginName = "GeekPlayerCurrency"


    override fun onLoad() {
        console().sendMessage("")
        console().sendMessage("正在加载 §3§l$pluginName §f...  §8" + Bukkit.getVersion())
        console().sendMessage("")
    }

    override fun onEnable() {
        console().sendMessage("")
        console().sendMessage("       §a$pluginName§8-§6Plus  §bv$VERSION §7by §awww.geekcraft.ink")
        console().sendMessage("       §8适用于Bukkit: §71.16.5-1.19.4 §8当前: §7 ${Bukkit.getServer().version}")
        console().sendMessage("")
        DataManager.start()
    }

    override fun onDisable() {
        DataManager.close()
    }

    @JvmStatic
    fun say(msg: String) {
        console().sendMessage("§8[§a$pluginName§8] ${msg.replace("&", "§")}")
    }


    @JvmStatic
    fun debug(msg: String) {
        if(SetTings.deBug) {
            console().sendMessage("§8[§a$pluginName§8]§8[§cDeBug§8]§7 ${msg.replace("&", "§")}")
        }
    }

}