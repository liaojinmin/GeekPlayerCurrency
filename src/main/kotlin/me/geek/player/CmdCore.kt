package me.geek.player

import me.geek.player.api.Currency
import me.geek.player.api.basic.CurrencyBasicType.*
import me.geek.player.service.DataManager
import me.geek.player.service.DataManager.getDataCache
import me.geek.player.service.DataManager.getHaveCurrencyList
import me.geek.player.service.DataManager.saveToSql
import me.geek.player.menu.openMain
import me.geek.player.service.ServiceManager.addSaveTask
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.command.*
import taboolib.common.platform.function.submitAsync
import taboolib.platform.util.hasItem
import taboolib.platform.util.takeItem

/**
 * @作者: 老廖
 * @时间: 2023/6/14 2:34
 * @包: me.geek.player
 */
@CommandHeader(name = "GeekPlayerCurrency", aliases = ["currency", "gpc"], permissionDefault = PermissionDefault.TRUE)
object CmdCore {

    @CommandBody
    val manage = subCommand {
        execute<Player> { sender, _, _ ->
            val data = sender.getDataCache()
            if (data == null) {
                sender.sendMessage("你的数据获取异常")
            } else {
                sender.openMain(data)
            }
        }
        dynamic("动作") {
            suggest { listOf("add", "del") }
            dynamic("数量") {
                suggestUncheck { listOf("10","100","1000") }
                execute<Player> { sender, context, arg ->
                    val data = sender.getDataCache()
                    if (data == null) {
                        sender.sendMessage("你的数据获取异常")
                    } else {
                        if (data.ownerCurrency == null) {
                            sender.sendMessage("你还没有创建自己的货币")
                        } else {
                            try {
                                val amount = arg.toInt()
                                if (amount <= 0) {
                                    sender.sendMessage("不能输入负数")
                                } else {
                                    if (context["动作"] == "add") {
                                        when (data.ownerCurrency!!.type) {
                                            ITEMS_BASIC -> {
                                                if (sender.inventory.hasItem(amount) { it.type == data.ownerCurrency!!.bindItem }) {
                                                    sender.inventory.takeItem(amount) { it.type == data.ownerCurrency!!.bindItem }
                                                    data.ownerCurrency!!.addBank(amount)
                                                    sender.sendMessage("§7[§eG§7] §a添加成功...")
                                                } else {
                                                    sender.sendMessage("§7[§eG§7] §e你没有足够的物品对准备金扩张...")
                                                }
                                                return@execute
                                            }

                                            CURRENCY_BASIC -> {
                                                DataManager.getCurrencyInfo(data.ownerCurrency!!.bindCurrencyID)?.let {
                                                    it.currencyHolder[sender.uniqueId]?.let { currencyHolder ->
                                                        if (currencyHolder.get() >= amount) {
                                                            data.ownerCurrency!!.addBank(amount)
                                                            sender.sendMessage("§7[§eG§7] §a添加成功...")
                                                        } else {
                                                            sender.sendMessage("§7[§eG§7] §e你的本位绑定货币余额不足，无法完成对库存的增加...")
                                                        }
                                                    }
                                                }
                                                return@execute
                                            }

                                            CREDIT_BASIC -> {
                                                data.ownerCurrency!!.addBank(amount)
                                                return@execute
                                            }
                                        }
                                    } else if (context["动作"] == "del") {
                                        data.ownerCurrency!!.delBank(amount)
                                    }
                                    data.ownerCurrency?.addSaveTask()
                                }
                            } catch (_: Exception) {
                                sender.sendMessage("§7[§eG§7] §c你输入的不是数字...")
                            }
                        }
                    }
                }
            }
        }
    }
    @CommandBody
    val create = subCommand {
        dynamic("编号") {
            suggestUncheck { listOf("货币编号") }
            execute<Player> { sender, _, arg ->
                val data = sender.getDataCache()
                if (data == null) {
                    sender.sendMessage("你的数据获取异常")
                } else {
                    // 检测是否有同样名称的货币
                    if (DataManager.getCurrencyInfo(arg) != null) {
                        sender.sendMessage("§7[§eG§7] §e已经存在这个名称的货币...")
                        return@execute
                    }
                    if (data.isLock) {
                        sender.sendMessage("§7[§eG§7] §e你正在声明继承另外一个货币，暂时不能创建...")
                        return@execute
                    }

                    if (data.ownerCurrency == null) {
                        val currency = Currency(sender.uniqueId, arg)
                        data.ownerCurrency = currency
                        submitAsync { currency.saveToSql(true) }
                        sender.sendMessage("§7[§eG§7] §a货币创建成功")
                    } else {
                        sender.sendMessage("§7[§eG§7] §f你已经创建过货币了...")
                    }
                }
            }
        }
    }

    /**
     * /currency status (id)
     */
    @CommandBody
    val status = subCommand {
        dynamic("编号") {
            suggestion<Player> { sender, _ ->
                sender.getHaveCurrencyList().map { it.uniqueString }
            }
            execute<Player> { sender, _, arg ->
                val currency = DataManager.getCurrencyInfo(arg)
                if (currency != null) {
                    sender.sendMessage("")
                    sender.sendMessage("  §7识别名称: §f${currency.uniqueString}")
                    sender.sendMessage("  §7展示名称: §f${currency.displayName}")
                    sender.sendMessage("  §7货币简介: §f${currency.info}")
                    if (currency.currencyHolder.containsKey(sender.uniqueId)) {
                        sender.sendMessage("  §f你的持有量: §f${currency.currencyHolder[sender.uniqueId]?.get() ?: 0}")
                    }
                    sender.sendMessage("")
                    sender.sendMessage("  §7接纳度: §e${currency.acceptance}")
                    sender.sendMessage("  §7活跃度: §e${currency.activation}")
                    sender.sendMessage("  §7流通总量: §e${currency.allAmount}")
                    sender.sendMessage("  §7未流入市场的数量: §e${currency.bankAmount}")
                    sender.sendMessage("  §7影响力指数: §e${currency.power}")
                    sender.sendMessage("")
                }
            }
        }
    }
    /**
     * /currency balance
     */
    @CommandBody
    val balance = subCommand {

    }

    /**
     * /currency pay (id) (player) (amount)
     */
    @CommandBody
    val pay = subCommand {
        dynamic("编号") {
            suggestion<Player> { sender, _ ->
                val data = mutableListOf<String>()
                data.addAll(sender.getHaveCurrencyList().map { it.uniqueString })
                sender.getDataCache()?.ownerCurrency?.uniqueString?.let {
                    data.add(it)
                }
                data
            }
            dynamic("玩家") {
                suggestion<Player> { sender, _ ->
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it != sender.name }
                }
                dynamic("数量") {
                    suggestUncheck {
                        listOf("10","100","1000")
                    }
                    execute<Player> { sender, context, _ ->
                        val data = sender.getDataCache()
                        if (data != null) {
                            // 取得这个货币信息对象
                            val currency = if (sender.uniqueId == data.ownerCurrency?.owner && context["编号"] == data.ownerCurrency?.uniqueString) {
                                data.ownerCurrency
                            } else {
                                data.currency[context["编号"]]
                            }
                            if (currency != null) {
                                try {
                                    val amount = context["数量"].toInt()
                                    if (amount > 0) {
                                        val targetPlayer = Bukkit.getPlayer(context["玩家"])
                                        if (targetPlayer == null) {
                                            sender.sendMessage("目标玩家不存在...")
                                            return@execute
                                        }
                                        if (sender.uniqueId != currency.owner) {
                                            val have = currency.currencyHolder[sender.uniqueId] ?: error("发生异常，找到玩家拥有的货币，但在货币信息中未找到持有者数据。")

                                            if (have.get() >= amount) {
                                                sender.sendMessage(currency.pay(
                                                    sender.uniqueId,
                                                    targetPlayer.uniqueId,
                                                    targetPlayer.name,
                                                    amount,
                                                    true
                                                ).message)
                                            } else sender.sendMessage("§7[§eG§7] §c你的余额不足...")
                                        } else {
                                            sender.sendMessage(currency.pay(
                                                sender.uniqueId,
                                                targetPlayer.uniqueId,
                                                targetPlayer.name,
                                                amount,
                                                true
                                            ).message)
                                        }
                                    } else sender.sendMessage("§7[§eG§7] §c你不能输入比 0 还小的数字...")
                                } catch (e: Exception) {
                                    if (SetTings.deBug) e.printStackTrace()
                                    sender.sendMessage("§7[§eG§7] §c你输入的不是数字...")
                                }
                            } else sender.sendMessage("§7[§eG§7] §c无法找到这个货币信息，请联系管理员...")
                        } else sender.sendMessage("§7[§eG§7] §c你的数据获取异常")
                    }
                }
            }

        }
    }
}