package me.geek.player.api

import com.google.gson.annotations.Expose
import me.geek.player.service.DataManager
import me.geek.player.service.DataManager.delToSql
import me.geek.player.utils.ExpIryBuilder
import taboolib.common.platform.function.submitAsync
import java.util.UUID

/**
 * @作者: 老廖
 * @时间: 2023/6/14 4:41
 * @包: me.geek.player.api
 *
 * 货币清算处理系统
 */
class CurrencyClearingSystem(

    /**
     * 结束日期
     */
    val endTime: ExpIryBuilder,


    /**
     * 被清算的货币
     */
    @Expose
    private val removedCurrency: Currency
) {

    /**
     * 清算编号
     */
    val uniqueId: UUID = UUID.randomUUID()

    /**
     * 愿意继承货币的玩家 UUID
     */
    private val inherit: MutableList<CurrencyHolder> = mutableListOf()

    /**
     * 玩家尝试声明继承该货币
     */
    fun inheritCurrency(playerData: PlayerData): Boolean {
        return if (playerData.ownerCurrency != null) {
            false
        } else {
            playerData.isLock = true
            playerData.currency[removedCurrency.uniqueString]?.let {
                it.currencyHolder[playerData.player.uniqueId]?.let { out -> inherit.add(out) }
            }
            true
        }
    }
    fun postUpdate(): Boolean {
        if (!endTime.autoUpdate()) {
            // 货币清算过期
            if (inherit.isEmpty()) {
                // 摧毁实例
                removedCurrency.clearingSystem = null
                submitAsync { removedCurrency.delToSql() }
            } else {
                inherit.sortByDescending { it.get() }
                val newOwner = inherit[0]

                removedCurrency.currencyHolder.remove(newOwner.playerUUID)
                removedCurrency.currencyHolder.remove(removedCurrency.owner)

                removedCurrency.owner = newOwner.playerUUID
                DataManager.getDataCache(removedCurrency.owner)?.let {
                    it.ownerCurrency = null
                }
                DataManager.getDataCache(newOwner.playerUUID)?.let {
                    it.ownerCurrency = removedCurrency
                }
                removedCurrency.clearingSystem = null
            }
            return true
        } else return false
    }

    /**
     * 汇率计算
     */
    private fun calculateExchangeRate(currencyA: Currency, currencyB: Currency): Double {
        val powerRatio = currencyB.power / currencyA.power
        val activationRatio = currencyB.activation.amount.toDouble() / currencyA.activation.amount.toDouble()
        val acceptanceRatio = currencyB.acceptance.toDouble() / currencyA.acceptance.toDouble()

        return (powerRatio + activationRatio + acceptanceRatio) / 3
    }

    fun mergeCurrencies(sourceCurrency: Currency) {
        val exchangeRate = calculateExchangeRate(sourceCurrency, removedCurrency)
        val mergedAmount = (removedCurrency.allAmount * exchangeRate).toInt()
        // 继承持有人列表
        removedCurrency.currencyHolder.forEach { (key, value) ->
            sourceCurrency.currencyHolder[key] = value.also {
                // 修改货币信息
                it.currencyName = sourceCurrency.uniqueString
                it.uniqueId = sourceCurrency.uniqueId
                // 根据汇率兑换
                it.add((it.get() * calculateExchangeRate(sourceCurrency, removedCurrency)).toInt())
            }
        }
        sourceCurrency.currencyHolder.putAll(removedCurrency.currencyHolder)
        // 继承活跃度
        sourceCurrency.activation.amount += removedCurrency.activation.amount
        // 将货币添加至目标货币库存
        sourceCurrency.addBank(mergedAmount)
    }
}