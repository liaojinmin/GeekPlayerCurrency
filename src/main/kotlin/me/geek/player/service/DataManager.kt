package me.geek.player.service

import com.google.gson.GsonBuilder
import me.geek.player.GeekPlayerCurrency
import me.geek.player.SetTings
import me.geek.player.api.Currency
import me.geek.player.api.CurrencyHolder
import me.geek.player.api.PlayerData
import me.geek.player.service.ServiceManager.addTask
import me.geek.player.service.sql.*
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.platform.event.SubscribeEvent
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * @作者: 老廖
 * @时间: 2023/4/26 22:01
 * @包: me.geek.vault.api
 */
object DataManager {

    private val dataSub by lazy {
        if (SetTings.configSql.use_type.equals("mysql", ignoreCase = true)) {
            return@lazy Mysql(SetTings.configSql)
        } else return@lazy Sqlite(SetTings.configSql)
    }

    private val sqlImpl: SQLImpl by lazy { SQLImpl() }


    /**
     * 玩家数据缓存
     */
    private val playerCache: MutableMap<UUID, PlayerData> = ConcurrentHashMap()

    /**
     * 货币缓存
     */
    private val currencyCache: MutableMap<String, Currency> = ConcurrentHashMap()



    fun getDataCache(uuid: UUID): PlayerData? {
        return playerCache[uuid]
    }


    fun Player.getDataCache(): PlayerData? {
        return getDataCache(this.uniqueId)
    }


    fun getCurrencyInfo(id: String): Currency? {
        return currencyCache[id]
    }

    fun Player.getHaveCurrencyList(): List<Currency> {
        val data = getDataCache() ?: return emptyList()
        return data.currency.values.toList()
    }


    fun CurrencyHolder.getCurrency(): Currency {
        return currencyCache[this.currencyName] ?: error("异常，未找到对应货币缓存 by ${this::class.java}")
    }


    fun Currency.saveToSql(isNew: Boolean = false) {
        GeekPlayerCurrency.debug("将 ${this.uniqueString} 保存到数据库")
        if (isNew) {
            sqlImpl.insertByCurrency(this)
        } else {
            sqlImpl.updateByCurrency(this)
        }
    }


    fun CurrencyHolder.saveToSql(isNew: Boolean = false) {
        if (isNew) {
            sqlImpl.insertByHolder(this)
        } else {
            sqlImpl.updateByHolder(this)
        }
    }

    fun CurrencyHolder.delToSql() = sqlImpl.deleteByHolder(this)

    fun Currency.delToSql() {
        this.currencyHolder.forEach { (t, value) ->
            // 找到持有人，并清空
            getDataCache(value.playerUUID)?.let {
                if (it.ownerCurrency?.uniqueString == this.uniqueString) {
                    it.ownerCurrency = null
                }
                it.currency.remove(this.uniqueString)
                sqlImpl.deleteByHolder(value)
            }
        }
        this.currencyHolder.clear()
        // 删除缓存
        currencyCache.remove(this.uniqueString)
        // 删除数据库
        sqlImpl.deleteByCurrency(this)
    }


    @SubscribeEvent
    fun join(event: PlayerJoinEvent) {
        val player = event.player
        if (playerCache.containsKey(player.uniqueId)) {
            return
        } else {
            val data = PlayerData(player)
            GeekPlayerCurrency.debug("size: ${currencyCache.size}")
            currencyCache.forEach { (key, value) ->

                GeekPlayerCurrency.debug("value size: ${value.currencyHolder.size}")

                value.currencyHolder.forEach { (_, value2) ->

                    GeekPlayerCurrency.debug(" value.currencyHolder.forEach is null")

                    if (value2.playerUUID == player.uniqueId) {
                        data.currency[key] = value
                    }
                }
                if (value.owner == player.uniqueId) {
                    data.ownerCurrency = value
                }
            }
            playerCache[player.uniqueId] = data
        }
    }


    fun start() {
        if (dataSub.isActive) return
        dataSub.onStart()
        if (dataSub.isActive) {
            dataSub.createTab {
                getConnection().use {
                    createStatement().action { statement ->
                        if (dataSub is Mysql) {
                            statement.addBatch(SqlTab.MYSQL_1.tab)
                            statement.addBatch(SqlTab.MYSQL_2.tab)
                        } else {
                            statement.addBatch("PRAGMA foreign_keys = ON;")
                            statement.addBatch("PRAGMA encoding = 'UTF-8';")
                            statement.addBatch(SqlTab.SQLITE_1.tab)
                            statement.addBatch(SqlTab.SQLITE_2.tab)
                        }
                        statement.executeBatch()
                    }
                }
            }
            val data = sqlImpl.selectByCurrency()
            GeekPlayerCurrency.debug("查询到 ${data.size} 个货币")
            val holder = sqlImpl.selectByHolder()
            GeekPlayerCurrency.debug("查询到 ${holder.size} 个持有信息")
            data.forEach {
                it.inti()
                holder.forEach { currencyHolder ->
                    if (currencyHolder.uniqueId == it.uniqueId) {
                        it.currencyHolder[currencyHolder.playerUUID] = currencyHolder
                    }
                }
                currencyCache[it.uniqueString] = it
                it.clearingSystem?.addTask()
            }
        }

    }

    fun close() {
        dataSub.onClose()
    }


    private fun getConnection(): Connection {
        return dataSub.getConnection()
    }



    /**
     * 数据库实现类
     * 所以数据库操作私有
     */
    private class SQLImpl {
        fun insertByCurrency(data: Currency) {
            if (dataSub.isActive) {
                getConnection().use {
                    prepareStatement(
                        "INSERT INTO currency_data(`unique_id`,`currency_data`,`time`,`outdated`) VALUES(?,?,?,?);"
                    ).actions {
                        it.setString(1, data.uniqueId.toString())
                        it.setBytes(2, data.toByteArray())
                        it.setLong(3, System.currentTimeMillis())
                        it.setBoolean(4, data.outdated)
                        it.executeUpdate()
                    }
                }
            }
        }
        fun deleteByCurrency(data: Currency) {
            if (dataSub.isActive) {
                if (data.currencyHolder.isNotEmpty()) {
                    GeekPlayerCurrency.say("&c警告: 改货币类型并未提供正常途径删除，此次数据库擦除操作终止。")
                    return
                }
                getConnection().use {
                    prepareStatement(
                        "DELETE FROM `currency_data` WHERE `unique_id`=?"
                    ).actions {
                        it.setString(1, data.uniqueId.toString())
                        it.executeUpdate()
                    }
                }
            }
        }

        fun updateByCurrency(data: Currency) {
            if (dataSub.isActive) {
                getConnection().use {
                    prepareStatement(
                        "UPDATE `currency_data` SET `currency_data`=?,`time`=?,`outdated`=? WHERE `unique_id`=?;"
                    ).actions {
                        it.setBytes(1, data.toByteArray())
                        it.setLong(2, System.currentTimeMillis())
                        it.setBoolean(3, data.outdated)
                        it.setString(4, data.uniqueId.toString())
                        it.executeUpdate()
                    }
                }
            }
        }
        fun selectByCurrency(): MutableList<Currency> {
            val data = mutableListOf<Currency>()
            if (dataSub.isActive) {
                getConnection().use {
                    prepareStatement(
                        "SELECT * FROM `currency_data`;"
                    ).actions {
                        val res = it.executeQuery()
                        while (res.next()) {
                            val out = GsonBuilder()
                                .setExclusionStrategies(Exclude())
                                .create()
                                .fromJson(res.getBytes("currency_data").toString(StandardCharsets.UTF_8), Currency::class.java)
                            data.add(out)
                            if (out == null) {
                                GeekPlayerCurrency.debug("反序列化异常 ${res.getString("unique_id")}")
                            }
                        }
                    }
                }
            }
            return data
        }


        /**
         * 货币持有操作
         */

        fun insertByHolder(data: CurrencyHolder) {
            if (dataSub.isActive) {
                getConnection().use {
                    prepareStatement(
                        "INSERT INTO currency_holder(`unique_id`,`currency_name`,`player_uuid`,`player_name`,`amount`) VALUES(?,?,?,?,?);"
                    ).actions {
                        it.setString(1, data.uniqueId.toString())
                        it.setString(2, data.currencyName)
                        it.setString(3, data.playerUUID.toString())
                        it.setString(4, data.playerName)
                        it.setInt(5, data.get())
                        it.executeUpdate()
                    }
                }
            }
        }

        fun deleteByHolder(data: CurrencyHolder) {
            if (dataSub.isActive) {
                getConnection().use {
                    prepareStatement(
                        "DELETE FROM `currency_holder` WHERE `unique_id`=? AND `player_uuid`=?"
                    ).actions {
                        it.setString(1, data.uniqueId.toString())
                        it.setString(2, data.playerUUID.toString())
                        it.executeUpdate()
                    }
                }
            }
        }

        fun updateByHolder(data: CurrencyHolder) {
            if (dataSub.isActive) {
                getConnection().use {
                    prepareStatement(
                        "UPDATE `currency_holder` SET `currency_name`=?,`player_name`=?,`amount`=? WHERE `unique_id`=? AND `player_uuid`=?;"
                    ).actions {
                        it.setString(1, data.currencyName)
                        it.setString(2, data.playerName)
                        it.setInt(3, data.get())
                        it.setString(4, data.uniqueId.toString())
                        it.setString(5, data.playerUUID.toString())
                        it.executeUpdate()
                    }
                }
            }
        }

        fun selectByHolder(): MutableList<CurrencyHolder> {
            val data = mutableListOf<CurrencyHolder>()
            if (dataSub.isActive) {
                getConnection().use {
                    prepareStatement(
                      "SELECT * FROM `currency_holder`;"
                    ).actions {
                        val res = it.executeQuery()
                        while (res.next()) {
                            val uid = UUID.fromString(res.getString("unique_id"))
                            val name = res.getString("currency_name")
                            val playerName = res.getString("player_name")
                            val playerUuid = UUID.fromString(res.getString("player_uuid"))
                            val amount = res.getInt("amount")
                            data.add(CurrencyHolder(uid, name, playerName, playerUuid, amount))
                        }
                    }
                }
            }
            return data
        }
    }

    private enum class SqlTab(val tab: String) {
        SQLITE_1(
            "CREATE TABLE IF NOT EXISTS `currency_holder` (" +
                    " `unique_id` CHAR(36) NOT NULL, " +
                    " `currency_name` varchar(36) NOT NULL," +
                    " `player_name` varchar(36) NOT NULL," +
                    " `player_uuid` CHAR(36) NOT NULL," +
                    " `amount` BIGINT(36) NOT NULL" +
                    ");"),
        SQLITE_2(
            "CREATE TABLE IF NOT EXISTS `currency_data` (" +
                    " `unique_id` CHAR(36) NOT NULL UNIQUE PRIMARY KEY," +
                    " `currency_data` longblob NOT NULL," +
                    " `time` BIGINT(20) NOT NULL," +
                    " `outdated` BOOLEAN NOT NULL" +
                    ");"
        ),

        MYSQL_1(
            "CREATE TABLE IF NOT EXISTS `currency_holder` (" +
                    " `unique_id` CHAR(36) NOT NULL," +
                    " `currency_name` varchar(36) NOT NULL," +
                    " `player_name` varchar(36) NOT NULL," +
                    " `player_uuid` CHAR(36) NOT NULL," +
                    " `amount` BIGINT(36) NOT NULL," +
                    "PRIMARY KEY (`unique_id`)" +
                    ");"
        ),
        MYSQL_2(
            "CREATE TABLE IF NOT EXISTS `currency_data` (" +
                    " `unique_id` CHAR(36) NOT NULL UNIQUE," +
                    " `currency_data` longblob NOT NULL," +
                    " `time` BIGINT(20) NOT NULL," +
                    " `outdated` BOOLEAN NOT NULL," +
                    "PRIMARY KEY (`unique_id`)" +
                    ");"
        ),
    }

}