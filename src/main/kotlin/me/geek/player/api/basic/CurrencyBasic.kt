package me.geek.player.api.basic

import org.bukkit.Material

/**
 * @作者: 老廖
 * @时间: 2023/6/14 18:14
 * @包: me.geek.player.api.basic
 */
abstract class CurrencyBasic(
    var type: CurrencyBasicType = CurrencyBasicType.CREDIT_BASIC
) {

    var isLock: Boolean = true

    /**
     * 物品绑定
     */
    var bindItem: Material = Material.DIAMOND

    /**
     * 货币绑定
     */
    var bindCurrencyID: String = ""

    /**
     * 价值比
     * 列如 1个绑定物品 = 1
     */
    var currencyBasicRatio: CurrencyBasicRatio = CurrencyBasicRatio()


    /**
     * 库存数量
     */
    var bankAmount: Int = 0
        private set


    fun addBank(amount: Int): Boolean {
        return if (amount > 0) {
            this.bankAmount += amount
            true
        } else false
    }

    fun delBank(amount: Int): Boolean {
        if (amount < 0) return false
        if (amount > bankAmount) {
            bankAmount = 0
        } else {
            bankAmount-=amount
        }
        return true
    }

}