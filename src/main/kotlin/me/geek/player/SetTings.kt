package me.geek.player

import me.geek.player.service.sql.ConfigSql
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.configuration.Configuration.Companion.getObject
import taboolib.platform.BukkitPlugin
import kotlin.system.measureTimeMillis

/**
 * 作者: 老廖
 * 时间: 2023/4/26
 *
 **/
@PlatformSide([Platform.BUKKIT])
object SetTings {

    @Config(value = "settings.yml", autoReload = true)
    lateinit var config: ConfigFile
        private set

    @Awake(LifeCycle.ENABLE)
    fun init() {
        config.onReload { onLoadSetTings() }
        onLoadSetTings()
    }

    lateinit var configSql: ConfigSql


    var deBug: Boolean = false

    var clearingTime: String = "15d"

    private fun onLoadSetTings() {
        measureTimeMillis {
            deBug = config.getBoolean("debug", false)
            configSql = config.getObject("data_storage", false)
         //   redisConfig = config.getObject("Redis", false)
            configSql.sqlite = BukkitPlugin.getInstance().dataFolder

            // set
            clearingTime = config.getString("set.clearingTime") ?: "15d"
        }
    }

}