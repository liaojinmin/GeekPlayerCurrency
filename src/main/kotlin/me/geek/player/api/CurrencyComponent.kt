package me.geek.player.api

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import me.geek.player.api.basic.CurrencyBasic
import me.geek.player.service.sql.Exclude
import java.util.*

/**
 * @作者: 老廖
 * @时间: 2023/6/14 2:30
 * @包: me.geek.player.api
 */
abstract class CurrencyComponent: CurrencyBasic() {

    var displayName: String = "未命名"

    var info: String = "这家伙很懒什么都没写..."

    /**
     * 唯一内部ID
     */
    val uniqueId: UUID = UUID.randomUUID()

    /**
     * 是否处于非活跃期
     */
    var outdated: Boolean = false

    /**
     * 被标记非活跃的时间
     */
    var outdatedTime: Long = -1

    /**
     * 清算程序
     */
    var clearingSystem: CurrencyClearingSystem? = null

    /**
     * 货币的活跃度（30天内账户变化次数）
     */
    var activation: CurrencyActivation = CurrencyActivation()

    /**
     * 货币持有者列表
     * key = 玩家UUID
     * value = CurrencyHolder
     */
    @Expose
    var currencyHolder: MutableMap<UUID, CurrencyHolder> = mutableMapOf()
        private set

    fun inti() {
        currencyHolder = mutableMapOf()
    }


}
