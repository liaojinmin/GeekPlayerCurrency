package me.geek.player.menu

import me.geek.player.SetTings
import me.geek.player.api.CurrencyClearingSystem
import me.geek.player.api.PlayerData
import me.geek.player.api.basic.CurrencyBasicType
import me.geek.player.service.DataManager
import me.geek.player.utils.ExpIryBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Basic
import taboolib.platform.util.buildItem
import taboolib.platform.util.cancelNextChat
import taboolib.platform.util.nextChatInTick

/**
 * @作者: 老廖
 * @时间: 2023/6/14 15:39
 * @包: me.geek.player.menu
 */
fun Player.openManager(playerData: PlayerData) {
    openMenu<Basic>("§0货币管理") {

        rows(3)

        //9 11 13 15 17
        set(9, buildItem(XMaterial.PAPER) {
            name = "§e设置展示名称"
        }) {
            closeInventory()
            sendMessage("§7[§eG§7] §f请在 30 秒内输入你要设置的展示名称, 取消输入 Cancel")
            clicker.nextChatInTick(601L, {
                if (!it.equals("Cancel", ignoreCase = true)) {
                    playerData.ownerCurrency!!.displayName = it.replace("&", "§")
                    submit { openManager(playerData) }
                } else this.clicker.cancelNextChat(false)
            })
        }

        set(11, buildItem(XMaterial.PAPER) {
            name = "§e设置简介"
        }) {
            closeInventory()
            sendMessage("§7[§eG§7] §f请在 30 秒内输入你要设置的简介, 取消输入 Cancel")
            clicker.nextChatInTick(600L, {
                if (!it.equals("Cancel", ignoreCase = true)) {
                    playerData.ownerCurrency!!.info = it.replace("&", "§")
                    submit { openManager(playerData) }
                } else this.clicker.cancelNextChat(false)
            })
        }

        set(13, buildItem(Material.DIAMOND) {
            name = "§e你创建的货币信息"
            parseCurrencyInfo(playerData.ownerCurrency!!, lore)
        })

        set(15, buildItem(Material.APPLE) {
            name = "§e设置本位信息"
            lore.add("")
            lore.add("§7[§a左键设置本位种类§7]")
            lore.add("§7[§aShift_点击设置兑换物§7]")
            lore.add("§7[§a右键设置本位兑换比§7]")
            lore.add("")
        }) {
            val ev = this.clickEvent()
            if (ev.isShiftClick) {
                if (playerData.ownerCurrency!!.isLock) {
                    sendMessage("§7[§eG§7] §c请先设置本位种类")
                } else {
                    closeInventory()
                    sendMessage("")
                    sendMessage("§7[§eG§7] §f请在 40 秒内输入, 取消输入 Cancel")
                    if (playerData.ownerCurrency!!.type == CurrencyBasicType.ITEMS_BASIC) {
                        sendMessage("§7[§eG§7] §f请手持要设置的原版物品，并输入 确认")
                    } else if (playerData.ownerCurrency!!.type == CurrencyBasicType.CURRENCY_BASIC)
                    sendMessage("§7[§eG§7] §f请输入要设置目标货币名称")
                    sendMessage("")
                    clicker.nextChatInTick(800L, {
                        if (!it.equals("Cancel", ignoreCase = true)) {
                            if (playerData.ownerCurrency!!.type == CurrencyBasicType.ITEMS_BASIC && it == "确认") {
                                playerData.ownerCurrency!!.bindItem = this.clicker.inventory.itemInMainHand.type
                                sendMessage("§7[§eG§7] §a成功")
                            } else {
                                val target = DataManager.getCurrencyInfo(it)
                                if (target != null) {
                                    playerData.ownerCurrency!!.bindCurrencyID = target.uniqueString
                                    sendMessage("§7[§eG§7] §a成功")
                                } else {
                                    sendMessage("§7[§eG§7] §c不存在这个货币名称 $it")
                                }
                            }
                        }
                    })
                }
            } else if (ev.isLeftClick) {
                closeInventory()
                sendMessage("")
                sendMessage("§7[§eG§7] §f请在 40 秒内输入你要设置的本位, 取消输入 Cancel")
                sendMessage("§7[§eG§7] §f可用本位: §e物品本位、货币本位、信用本位、")
                sendMessage("§7[§eG§7] §f选择上面的其中一个本位之类、")
                sendMessage("")
                clicker.nextChatInTick(800L, {
                    if (!it.equals("Cancel", ignoreCase = true)) {
                        when (it) {
                            "物品本位" -> {
                                playerData.ownerCurrency?.let { c ->
                                    c.isLock = false
                                    c.type = CurrencyBasicType.ITEMS_BASIC
                                }
                            }

                            "货币本位" -> {
                                playerData.ownerCurrency?.let { c ->
                                    c.isLock = false
                                    c.type = CurrencyBasicType.CURRENCY_BASIC
                                }
                            }

                            "信用本位" -> {
                                playerData.ownerCurrency?.let { c ->
                                    c.isLock = false
                                    c.type = CurrencyBasicType.CREDIT_BASIC
                                }
                            }

                            else -> {
                                sendMessage("§7[§eG§7] §c未知的本位种类")
                            }
                        }
                        submit { openManager(playerData) }
                    } else this.clicker.cancelNextChat(false)
                })
            } else {
                if (playerData.ownerCurrency!!.type == CurrencyBasicType.CREDIT_BASIC) {
                    sendMessage("§7[§eG§7] §e你的货币是信用本来，无法设置比例...")
                } else if (playerData.ownerCurrency!!.isLock) {
                    sendMessage("§7[§eG§7] §c你还没有设置本位种类...")
                } else {
                    closeInventory()
                    val curr = playerData.ownerCurrency!!
                    val bind = if (curr.type == CurrencyBasicType.ITEMS_BASIC) curr.bindItem.name else if (curr.type == CurrencyBasicType.CURRENCY_BASIC) curr.uniqueString else "信用"
                    sendMessage("")
                    sendMessage("§7[§eG§7] §f请在 40 秒内输入你要设置的兑换比, 取消输入 Cancel")
                    sendMessage("§7[§eG§7] §f当前种类: ${curr.type.dis}")
                    sendMessage("§7[§eG§7] §f当前绑定: $bind")
                    sendMessage("§7[§eG§7] §f兑换比: §e${curr.currencyBasicRatio.currencyRatio}§f货币§7/§e${curr.currencyBasicRatio.valueRatio}§f${curr.type.dis}")
                    sendMessage("§7[§eG§7] §e格式: §f货币/本位物品 §e列如: §f100/1")
                    sendMessage("")
                    clicker.nextChatInTick(800L, {
                        if (!it.equals("Cancel", ignoreCase = true)) {
                            try {
                                val ac = it.split("/")
                                if (ac.size >= 2) {
                                    curr.currencyBasicRatio.currencyRatio = ac[0].toInt()
                                    curr.currencyBasicRatio.valueRatio = ac[1].toInt()
                                    submit { openManager(playerData) }
                                } else {
                                    sendMessage("§7[§eG§7] §c你输入的 $it 错误...")
                                }
                            } catch (ex: Exception) {
                                sendMessage("§7[§eG§7] §c你的输入错误...")
                            }
                        } else this.clicker.cancelNextChat(false)
                    })
                }
            }
        }




        set(26, buildItem(XMaterial.BARRIER) {
            name = "§c放弃管理权"
            lore.add("")
            lore.add("  §e这会导致货币进入锁定状态，并启动清算流程")
        }) {
            playerData.ownerCurrency?.let {
                if (it.clearingSystem != null) {
                    sendMessage("§7[§eG§7] §f你的货币正在清算阶段....")
                } else {
                    val exp = ExpIryBuilder(SetTings.clearingTime)
                    it.clearingSystem = CurrencyClearingSystem(exp, it)
                    sendMessage("§7[§eG§7] §e放弃成功，货币已进入清算程序...")
                }
            }
        }
    }
}
