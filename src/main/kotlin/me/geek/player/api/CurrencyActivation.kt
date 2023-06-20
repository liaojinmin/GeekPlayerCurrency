package me.geek.player.api

/**
 * @作者: 老廖
 * @时间: 2023/6/14 4:29
 * @包: me.geek.player.api
 */
data class CurrencyActivation(
    /**
     * 货币的活跃度（30天内账户变化次数）
     */
    var amount: Int = 0,

    /**
     * 记录起始日期
     */
    var startTime: Long = System.currentTimeMillis()
)