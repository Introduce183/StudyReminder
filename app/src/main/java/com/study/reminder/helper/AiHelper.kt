package com.study.reminder.helper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiHelper {

    // 小米MiMo API（OpenAI兼容格式）
    private const val MIMO_API_URL = "https://api.xiaomimimo.com/v1/chat/completions"
    private const val MIMO_MODEL = "mimo-v2-flash"

    suspend fun generateTauntMessage(
        apiKey: String,
        screenContent: String?,
        characterName: String,
        useAi: Boolean
    ): Pair<String, String> = withContext(Dispatchers.IO) {

        if (!useAi || apiKey.isEmpty()) return@withContext getDefaultTaunt(characterName)

        val contextInfo = if (!screenContent.isNullOrBlank()) {
            "用户当前屏幕内容：「${screenContent.take(200)}」，请结合内容生成提醒。"
        } else { "用户可能在玩手机或刷视频。" }

        val systemPrompt = """你是「$characterName」，风格傲娇毒舌但关心用户学习。
只输出JSON格式：{"message":"15字以内挑衅提醒","emoji":"一个emoji"}
不要输出其他任何内容。"""

        try {
            val url = URL(MIMO_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 12000

            val body = JSONObject().apply {
                put("model", MIMO_MODEL)
                put("max_tokens", 100)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                    put(JSONObject().put("role", "user").put("content", contextInfo))
                })
            }
            conn.outputStream.write(body.toString().toByteArray(Charsets.UTF_8))

            val code = conn.responseCode
            val text = if (code == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val errBody = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null }
                android.util.Log.w("AiHelper", "HTTP $code: $errBody")
                conn.disconnect()
                return@withContext getDefaultTaunt(characterName)
            }
            conn.disconnect()

            val content = JSONObject(text)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()
            val result = JSONObject(content)
            Pair(result.optString("message", getDefaultTaunt(characterName).first),
                 result.optString("emoji", "😤"))
        } catch (e: Exception) {
            android.util.Log.e("AiHelper", "MiMo error: ${e.message}")
            getDefaultTaunt(characterName)
        }
    }

    private fun getDefaultTaunt(name: String) = listOf(
        Pair("${name}：还在玩？快去学习！", "😤"),
        Pair("学习时间到了，放下手机！", "📚"),
        Pair("再不学习我生气了！", "💢"),
        Pair("任务还没完成！", "😠"),
        Pair("${name}催你了！快！", "⏰"),
        Pair("别装了，去学习！！", "🔥"),
        Pair("偷懒被我发现了！", "👀"),
    ).random()
}
