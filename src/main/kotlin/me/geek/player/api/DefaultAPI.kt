package me.geek.player.api

import me.geek.player.service.DataManager
import me.geek.player.service.DataManager.getDataCache
import org.bukkit.entity.Player

/**
 * @作者: 老廖
 * @时间: 2023/6/14 17:56
 * @包: me.geek.player.api
 */
object DefaultAPI {

    /**
     * 扣除指定玩家，指定货币数量
     */
    fun takePlayer(player: Player, currencyID: String, amount: Int): Boolean {
        val data = player.getDataCache() ?: return false
        // 如果是自己的货币
        if (data.ownerCurrency?.uniqueString == currencyID) return false
        data.currency

        val currency = data.currency[currencyID] ?: return false
        currency.currencyHolder[player.uniqueId]?.del(amount)
        return false
    }

}