package me.geek.player.api

import java.util.UUID

/**
 * @作者: 老廖
 * @时间: 2023/6/11 17:01
 * @包: me.geek.player.api
 */
data class CurrencyHolder(
    /**
     * 货币唯一 UUID
     */
    var uniqueId: UUID,
    /**
     * 货币唯一 ID
     */
    var currencyName: String,

    val playerName: String,

    val playerUUID: UUID,

    private var amount: Int
) {
    fun get(): Int = amount

    fun add(amount: Int): Boolean {
        return if (amount >= 0) {
            this.amount+=amount
            return true
        } else false
    }

    fun del(amount: Int): Boolean {
        return if (amount >= 0) {
            this.amount-=amount
            return true
        } else false
    }
}
