package me.geek.player.api

import org.bukkit.entity.Player

/**
 * @作者: 老廖
 * @时间: 2023/6/12 14:17
 * @包: me.geek.player.api
 */
class PlayerData(
    val player: Player,
    /**
     * 属于玩家的货币
     */
    var ownerCurrency: Currency? = null,

    /**
     * 玩家拥有的货币
     * key = 货币唯一ID
     */
    val currency: MutableMap<String, Currency> = mutableMapOf()

) {
    /**
     * 当玩家参与货币清算的情况下将锁定
     */
    var isLock: Boolean = false
}
