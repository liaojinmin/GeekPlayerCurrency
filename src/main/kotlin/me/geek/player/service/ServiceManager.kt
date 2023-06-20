package me.geek.player.service



import me.geek.player.api.Currency
import me.geek.player.api.CurrencyClearingSystem
import me.geek.player.api.CurrencyHolder
import me.geek.player.service.DataManager.saveToSql
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.function.submitAsync

import taboolib.common.platform.service.PlatformExecutor
import java.util.*

/**
 * @作者: 老廖
 * @时间: 2023/4/28 1:46
 * @包: me.geek.vault.service
 */
@PlatformSide([Platform.BUKKIT])
object ServiceManager {



    private val sqlPack: MutableList<Any> = mutableListOf()
    private var sqlSaveTask: PlatformExecutor.PlatformTask? = null

    private val clearingPack: MutableList<CurrencyClearingSystem> = mutableListOf()
    private var clearingTask: PlatformExecutor.PlatformTask? = null



    fun CurrencyClearingSystem.addTask() = clearingPack.add(this)
    fun CurrencyClearingSystem.delTask() = clearingPack.remove(this)

    fun Currency.addSaveTask() {
        sqlPack.add(this)
    }


    fun CurrencyHolder.addSaveTask() {
        sqlPack.add(this)
    }

    @Awake(LifeCycle.ACTIVE)
    fun start() {
        sqlSaveTask?.cancel()
        sqlSaveTask = submitAsync(delay = 20, period = 20) {
            try {
                val a = sqlPack.listIterator()
                while (a.hasNext()) {
                    when (val c = a.next()) {
                        is Currency -> {
                            c.saveToSql()
                        }
                        is CurrencyHolder -> {
                            c.saveToSql()
                        }
                    }
                    a.remove()
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
        startClearingTask()
    }

    @Awake(LifeCycle.DISABLE)
    fun close() {
        sqlSaveTask?.cancel()
        sqlPack.clear()
    }

    fun startClearingTask() {
        clearingTask?.cancel()
        clearingTask = submitAsync(delay = 20, period = 20) {
            try {
                val a = clearingPack.listIterator()
                while (a.hasNext()) {
                    val b = a.next()
                    if (b.postUpdate()) {
                        a.remove()
                    }
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }



}