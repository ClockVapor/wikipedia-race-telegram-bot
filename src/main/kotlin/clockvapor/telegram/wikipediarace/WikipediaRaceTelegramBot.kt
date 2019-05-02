package clockvapor.telegram.wikipediarace

import clockvapor.telegram.trySuccessful
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import me.ivmg.telegram.Bot
import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.entities.ChatAction
import me.ivmg.telegram.entities.Update
import okhttp3.logging.HttpLoggingInterceptor

class WikipediaRaceTelegramBot(private val token: String) {
    private val games = hashMapOf<Long, Game>()

    fun run() {
        bot {
            token = this@WikipediaRaceTelegramBot.token
            logLevel = HttpLoggingInterceptor.Level.NONE
            dispatch {
                command("newgame") { bot, update -> tryCommand(bot, update, ::doNewGameCommand) }
                command("stopgame") { bot, update -> tryCommand(bot, update, ::doStopGameCommand) }
                command("next") { bot, update -> tryCommand(bot, update, ::doNextPageOfLinksCommand) }
                command("prev") { bot, update -> tryCommand(bot, update, ::doPreviousPageOfLinksCommand) }
                command("pick") { bot, update -> tryCommand(bot, update, ::doPickCommand) }
            }
        }.startPolling()
    }

    private fun tryCommand(bot: Bot, update: Update, action: (Bot, Update) -> Unit) {
        val message = update.message!!
        bot.sendChatAction(message.chat.id, ChatAction.TYPING)
        if (!trySuccessful(reportException = true) { action(bot, update) }) {
            bot.sendMessage(message.chat.id, "<an error occurred>", replyToMessageId = message.messageId)
        }
    }

    private fun doNewGameCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        if (chatId in games) {
            bot.sendMessage(chatId, GAME_IN_PROGRESS, replyToMessageId = message.messageId)
            return
        }

        val command = message.entities!![0]
        val query = message.text!!.substring(command.offset + command.length).trim()
        if (query.isBlank()) {
            bot.sendMessage(chatId, "Please give a destination page title.", replyToMessageId = message.messageId)
            return
        }

        val targetPageTitle = Wikipedia.fetchPageTitle(query)
        if (targetPageTitle == null) {
            bot.sendMessage(chatId, "I couldn't find a page with that title.",
                replyToMessageId = message.messageId)
            return
        }

        val startPageTitle = Wikipedia.fetchRandomPageTitle()
        val game = Game(startPageTitle, targetPageTitle)
        games[chatId] = game
        handleGame(bot, chatId, message.messageId, game)
    }

    private fun doStopGameCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        val game = games.remove(chatId)
        val replyText =
            if (game == null) NO_GAME_IN_PROGRESS
            else "Okay, the game is cancelled."
        bot.sendMessage(chatId, replyText, replyToMessageId = message.messageId)
    }

    private fun doNextPageOfLinksCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        val game = games[chatId]
        if (game == null) {
            bot.sendMessage(chatId, NO_GAME_IN_PROGRESS, replyToMessageId = message.messageId)
            return
        }

        game.getNextPageOfLinks()
        bot.sendMessage(chatId, game.toString(), replyToMessageId = message.messageId)
    }

    private fun doPreviousPageOfLinksCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        val game = games[chatId]
        if (game == null) {
            bot.sendMessage(chatId, NO_GAME_IN_PROGRESS, replyToMessageId = message.messageId)
            return
        }

        game.getPreviousPageOfLinks()
        bot.sendMessage(chatId, game.toString(), replyToMessageId = message.messageId)
    }

    private fun doPickCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        val game = games[chatId]
        if (game == null) {
            bot.sendMessage(chatId, NO_GAME_IN_PROGRESS, replyToMessageId = message.messageId)
            return
        }

        val command = message.entities!![0]
        val i = message.text!!.substring(command.offset + command.length).trim().toIntOrNull()
        if (i == null || !game.update(i)) {
            bot.sendMessage(chatId, "Please select a link number from the list.", replyToMessageId = message.messageId)
            return
        }

        handleGame(bot, chatId, message.messageId, game)
    }

    private fun handleGame(bot: Bot, chatId: Long, messageId: Long, game: Game) {
        bot.sendMessage(chatId, game.toString(), replyToMessageId = messageId)
        if (game.state == Game.State.WIN) {
            games -= chatId
        }
    }

    companion object {
        private const val GAME_IN_PROGRESS = "There's already a game in progress."
        private const val NO_GAME_IN_PROGRESS = "There's no game in progress."

        @JvmStatic
        fun main(a: Array<String>) = mainBody {
            val args = ArgParser(a).parseInto(::Args)
            WikipediaRaceTelegramBot(args.telegramBotToken).run()
        }
    }

    class Args(parser: ArgParser) {
        val telegramBotToken: String by parser.storing("-t", "--token", help = "Telegram bot token")
    }
}
