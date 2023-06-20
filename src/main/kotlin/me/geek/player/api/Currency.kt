package me.geek.player.api

import com.google.gson.GsonBuilder
import me.geek.player.service.DataManager
import me.geek.player.service.DataManager.saveToSql
import me.geek.player.service.ServiceManager.addSaveTask
import me.geek.player.service.sql.Exclude
import taboolib.common.platform.function.submitAsync
import java.util.UUID

/**
 * @作者: 老廖
 * @时间: 2023/6/11 16:22
 * @包: me.geek.player.api
 */
class Currency(
    var owner: UUID,
    /**
     * 唯一识别编号
     */
    val uniqueString: String,
): CurrencyComponent() {

    /**
     * 货币的接纳度（持有人数）
     */
    val acceptance: Int
        get() = currencyHolder.size

    /**
     * 流通货币总数量
     */
    val allAmount: Int
        get() = getAmount()

    /**
     * 货币影响力指数
     */
    val power: Double
        get() = getCPower()



    @Synchronized
    fun pay(source: UUID, target: UUID, targetName: String, amount: Int, createEmpty: Boolean = false): CurrencyCallBack {

        if (amount <= 0) return CurrencyCallBack(false, "数量不能小于 0")
        if (isLock) return CurrencyCallBack(false, "未设置本位绑定，货币处于锁定状态。")


        var b = currencyHolder[target]

        if (b == null && createEmpty) {
            b = createHolder(target, targetName)
            DataManager.getDataCache(target)?.currency?.putIfAbsent(uniqueString, this)
            currencyHolder[target] = b
        }

        if (source == owner && b != null) {
            // 库存不足
            return if (bankAmount < amount) {
                CurrencyCallBack(false, "货币可用准备金不足，无法完成转账.")
            } else {
                b.add(amount)
                delBank(amount)
                this.addSaveTask()
                b.addSaveTask()
                CurrencyCallBack(false, "转账成功")
            }
        } else {
            val a = currencyHolder[source] ?: return CurrencyCallBack(false, "无法获取的转账账户，异常.")
            return if (b != null && a.get() > 0) {
                a.del(amount)
                b.add(amount)
                a.addSaveTask()
                b.addSaveTask()
                CurrencyCallBack(false, "转账成功")
            } else if (a.get() > 0) {
                CurrencyCallBack(false, "货币不足，无法转账")
            } else {
                CurrencyCallBack(false, "转账异常...")
            }
        }
    }

    fun createHolder(target: UUID, targetName: String): CurrencyHolder {
        val data = CurrencyHolder(uniqueId, uniqueString, targetName, target, 0)
        submitAsync {
            data.saveToSql(true)
        }
        return data
    }

    private fun getAmount(): Int {
        var out = 0
        currencyHolder.forEach { (_, value) ->
            if (value.playerUUID != owner) {
                out += value.get()
            }
        }
        return out
    }

    private fun getCPower(): Double {
        // 接纳度
        val acceptanceWeight = 0.7
        // 活跃度权重
        val activationWeight = 0.3
        return acceptance.toDouble() * acceptanceWeight + activation.amount.toDouble() * activationWeight
    }

    fun toByteArray(): ByteArray = toJson().toByteArray(Charsets.UTF_8)

    private fun toJson(): String {
        return GsonBuilder()
            .setExclusionStrategies(Exclude())
            .create()
            .toJson(this)
    }
}