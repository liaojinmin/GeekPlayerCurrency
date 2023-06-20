package me.geek.player.api.basic

/**
 * @作者: 老廖
 * @时间: 2023/6/14 17:48
 * @包: me.geek.player.api.standard
 */
enum class CurrencyBasicType(val dis: String) {

    /**
     * 物品本位
     */
    ITEMS_BASIC("物品本位"),

    /**
     * 货币本位
     */
    CURRENCY_BASIC("货币本位"),

    /**
     * 信用本位
     */
    CREDIT_BASIC("信用本位")
}