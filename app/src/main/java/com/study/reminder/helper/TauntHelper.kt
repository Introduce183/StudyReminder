package com.study.reminder.helper

/**
 * 本地随机挑衅语句生成（不依赖AI和无障碍）
 */
object TauntHelper {

    private val taunts = listOf(
        Pair("还在玩？快去学习！", "😤"),
        Pair("学习时间到了，放下手机！", "📚"),
        Pair("你以为我不知道你在偷懒？", "👀"),
        Pair("这点时间都不想学？", "💢"),
        Pair("手机先放一边，书本拿起来！", "📖"),
        Pair("别装了，赶紧去学！", "🔥"),
        Pair("你的对手正在努力，你在干嘛？", "😠"),
        Pair("又在摸鱼？！给我学习！", "🐟"),
        Pair("时间不等人，快去学习！", "⏰"),
        Pair("现在不学，以后哭都来不及！", "😭"),
        Pair("放下手机！现在！马上！", "🚨"),
        Pair("就差这点努力，你要放弃吗？", "💪"),
        Pair("少玩一会儿手机又不会死！", "🙄"),
        Pair("我都替你着急了，快去学！", "😩"),
        Pair("学习时间！不许偷懒！", "✏️"),
    )

    fun random(): Pair<String, String> = taunts.random()
}
