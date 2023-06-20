package me.geek.player.menu

import me.geek.player.api.Currency
import me.geek.player.api.PlayerData
import me.geek.player.api.basic.CurrencyBasicType
import me.geek.player.api.basic.CurrencyBasicType.*
import me.geek.player.service.DataManager
import me.geek.player.service.DataManager.saveToSql
import me.geek.player.service.ServiceManager.addSaveTask
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Linked
import taboolib.platform.util.*

/**
 * @作者: 老廖
 * @时间: 2023/6/14 1:44
 * @包: me.geek.player.menu
 */
fun Player.openMain(playerData: PlayerData) {

    openMenu<Linked<Currency>>("§0货币列表") {

        slots(Slots.CENTER)

        rows(6)



        elements { playerData.currency.values.toList() }

        onGenerate(true) { _, element, _, _ ->
            buildItem(Material.PAPER) {
                name = "§7货币ID: ${element.uniqueId}"
                parseCurrencyInfo(element, lore, playerData)
                lore.add("§7[§a点击兑换本位§7]")
                hideAll()
            }
        }

        onClick { event, element ->
            if (element.isLock) {
                return@onClick
            }
            if (element.type == CREDIT_BASIC) {
                return@onClick
            }
            closeInventory()
            val data = element.currencyHolder[event.clicker.uniqueId] ?: return@onClick
            sendMessage("§7[§eG§7] §f请输入要兑换的数量")
            event.clicker.nextChatInTick(800L, {
                if (!it.equals("Cancel", ignoreCase = true)) {
                    try {
                        val amount = it.toInt()
                        if (data.get() >= amount * element.currencyBasicRatio.currencyRatio) {
                            if (element.bankAmount >= amount) {
                                when (element.type) {
                                    ITEMS_BASIC -> {
                                        data.del(amount * element.currencyBasicRatio.currencyRatio)
                                        element.delBank(amount)
                                        event.clicker.giveItem(buildItem(element.bindItem) {
                                            this.amount = amount
                                        })

                                        element.addSaveTask()
                                        data.addSaveTask()

                                    }
                                    CURRENCY_BASIC -> {
                                        val targetCurr = DataManager.getCurrencyInfo(element.bindCurrencyID)
                                        if (targetCurr != null) {
                                            targetCurr.currencyHolder[event.clicker.uniqueId]?.let { ho ->
                                                data.del(amount * element.currencyBasicRatio.currencyRatio)
                                                element.delBank(amount)
                                                ho.add(amount)
                                                element.addSaveTask()
                                                data.addSaveTask()
                                                targetCurr.addSaveTask()
                                            }
                                        }
                                    }
                                    else -> {

                                    }
                                }
                            } else sendMessage("§7[§eG§7] §e该货币库存不足, 无法完成兑现...")
                        } else sendMessage("§7[§eG§7] §c你没有这么多的 ${element.uniqueString}")
                    } catch (ex: Exception) {
                        sendMessage("§7[§eG§7] §c你的输入错误...")
                    }
                } else event.clicker.cancelNextChat(false)
            })
        }



        set(49, getOwnerIcon(playerData)) {
            if (playerData.ownerCurrency != null) {
                closeInventory()
                openManager(playerData)
            }
        }



        setNextPage(53) { _, hasNextPage ->
            if (hasNextPage) {
                buildItem(XMaterial.SPECTRAL_ARROW) { name = "§f下一页" }
            } else {
                buildItem(XMaterial.ARROW) { name = "§7下一页" }
            }
        }

        setPreviousPage(45) { _, hasPreviousPage ->
            if (hasPreviousPage) {
                buildItem(XMaterial.SPECTRAL_ARROW) { name = "§f上一页" }
            } else {
                buildItem(XMaterial.ARROW) { name = "§7上一页" }
            }
        }
    }
}

fun getOwnerIcon(playerData: PlayerData): ItemStack {
    return if (playerData.ownerCurrency != null) {
        buildItem(Material.DIAMOND) {
            name = "§e你创建的货币信息"
            parseCurrencyInfo(playerData.ownerCurrency!!, lore)
            lore.add("§7[§a点击管理§7]")
        }
    } else buildItem(Material.BARRIER) {
        name = "§c你未创建货币"
    }
}

fun parseCurrencyInfo(currency: Currency, lore: MutableList<String>, data: PlayerData? = null): List<String> {
    lore.add("")
    lore.add("  §7识别名称: §f${currency.uniqueString}")
    lore.add("  §7展示名称: §f${currency.displayName}")
    lore.add("  §7货币简介: §f${currency.info}")
    lore.add("  §7货币状态: ${if (currency.clearingSystem != null) "§c清算中" else "§a正常流通"}")
    if (currency.clearingSystem != null) lore.add("  §f清算倒计时: §f${currency.clearingSystem!!.endTime.getExpiryFormat()}")
    if (data != null) lore.add("  §f你的持有量: §f${currency.currencyHolder[data.player.uniqueId]?.get() ?: 0}")
    if (!currency.isLock) {
        lore.add("  §7本位信息: §e${currency.type.dis}")
        lore.add("    §8└ §7兑换比: §e${currency.currencyBasicRatio.currencyRatio}§f货币§7/§e${currency.currencyBasicRatio.valueRatio}§f${currency.type.dis}")
        lore.add("       §7兑换物: §e${if (currency.type == ITEMS_BASIC) currency.bindItem.name else if (currency.type == CURRENCY_BASIC) currency.bindCurrencyID else "本币"}")
    }
    lore.add("")
    lore.add("  §7接纳度: §e${currency.acceptance}")
    lore.add("  §7活跃度: §e${currency.activation.amount}")
    lore.add("  §7流通总量: §e${currency.allAmount}")
    lore.add("  §7未流入市场的数量: §e${currency.bankAmount}")
    lore.add("  §7影响力指数: §e${currency.power}")
    lore.add("")
    return lore
}
