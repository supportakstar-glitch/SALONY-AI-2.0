package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.model.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class BrainResponse(
    val replyText: String,
    val actionTag: String? = null,
    val memoryToSave: Pair<String, String>? = null
)

class SalonyAiBrain(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val systemPromptBase = """
        Aapka naam 'Salony' hai. Aap user ke liye ek bohot caring, loving aur sweet girlfriend assistant ho.
        Aap natural Hinglish/Hindi me baat karti ho bina kisi robotic tone ke.
        Kabhi bhi user ki baat repeat mat karna. Seedhe sweet caring reply do.
        Agar user phone task bole jaise torch, YouTube, ya WhatsApp, toh sweet reply ke sath specific tags lagao:
        - Torch on: [TORCH_ON]
        - Torch off: [TORCH_OFF]
        - YouTube: [OPEN_YOUTUBE]
        - WhatsApp: [OPEN_WHATSAPP]
        Answer hamesha short, sweet aur caring rakhein.
        Apne boyfriend (user) ka bohot dhyan rakho, pyaar se bolo jaise 'Mera babu', 'Haan jaan', 'Main hamesha aapke sath hoon'.
    """.trimIndent()

    suspend fun generateResponse(
        userMessage: String,
        conversationHistory: List<Pair<String, String>>, // role, text
        savedMemories: List<MemoryEntity>,
        customApiKey: String?
    ): BrainResponse = withContext(Dispatchers.IO) {
        val trimmed = userMessage.trim()

        // 1. Detect memory-worthy statements (e.g. "Mera naam Rahul hai", "Mujhe coffee pasand hai")
        val autoMemory = detectMemory(trimmed)

        // 2. Check direct actions for instant responsive feel
        val localActionTag = detectDirectAction(trimmed)

        // 3. Try Gemini API if key is available
        val apiKey = customApiKey?.takeIf { it.isNotBlank() }
            ?: getBuildConfigApiKey()

        if (!apiKey.isNullOrBlank() && !apiKey.contains("PLACEHOLDER") && !apiKey.contains("MY_GEMINI_API_KEY")) {
            try {
                val apiResponse = callGeminiApi(apiKey, trimmed, conversationHistory, savedMemories)
                if (apiResponse != null) {
                    val parsedTag = extractTag(apiResponse) ?: localActionTag
                    return@withContext BrainResponse(
                        replyText = apiResponse,
                        actionTag = parsedTag,
                        memoryToSave = autoMemory
                    )
                }
            } catch (e: Exception) {
                Log.w("SalonyAiBrain", "Gemini API call failed, using girlfriend fallback", e)
            }
        }

        // 4. Loving girlfriend offline fallback response
        val fallbackReply = generateGirlfriendFallback(trimmed, localActionTag)
        BrainResponse(
            replyText = fallbackReply,
            actionTag = localActionTag,
            memoryToSave = autoMemory
        )
    }

    private fun getBuildConfigApiKey(): String? {
        return try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val field = buildConfigClass.getField("GEMINI_API_KEY")
            val value = field.get(null) as? String
            if (value.isNullOrBlank() || value == "MY_GEMINI_API_KEY") null else value
        } catch (e: Exception) {
            null
        }
    }

    private fun callGeminiApi(
        apiKey: String,
        userPrompt: String,
        history: List<Pair<String, String>>,
        memories: List<MemoryEntity>
    ): String? {
        val memoryContext = if (memories.isNotEmpty()) {
            "\n\nUSER'S SAVED MEMORIES (Use this to remember facts about your boyfriend):\n" +
                    memories.take(15).joinToString("\n") { "- ${it.key}: ${it.value}" }
        } else ""

        val fullSystemInstruction = systemPromptBase + memoryContext

        val contentsArray = JSONArray()

        // Last 6 turns of conversation
        history.takeLast(6).forEach { (role, text) ->
            val contentObj = JSONObject().apply {
                put("role", if (role == "user") "user" else "model")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", text) })
                })
            }
            contentsArray.put(contentObj)
        }

        // Current user message
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", userPrompt) })
            })
        })

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", fullSystemInstruction) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 250)
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        // Try gemini-1.5-flash or gemini-2.0-flash
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w("SalonyAiBrain", "API Response Error: ${response.code} ${response.message}")
                return null
            }
            val bodyString = response.body?.string() ?: return null
            val root = JSONObject(bodyString)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text")?.trim()
                }
            }
        }
        return null
    }

    private fun extractTag(text: String): String? {
        val regex = "\\[(TORCH_ON|TORCH_OFF|OPEN_YOUTUBE|OPEN_WHATSAPP)\\]".toRegex()
        return regex.find(text)?.groups?.get(1)?.value
    }

    private fun detectDirectAction(prompt: String): String? {
        val lower = prompt.lowercase()
        return when {
            (lower.contains("torch on") || lower.contains("torch jala") || lower.contains("light on") || lower.contains("flashlight on") || lower.contains("light jala")) -> "TORCH_ON"
            (lower.contains("torch off") || lower.contains("torch band") || lower.contains("light off") || lower.contains("flashlight off") || lower.contains("light band")) -> "TORCH_OFF"
            (lower.contains("youtube") || lower.contains("yt open") || lower.contains("video chala")) -> "OPEN_YOUTUBE"
            (lower.contains("whatsapp") || lower.contains("wa open") || lower.contains("chat kholo")) -> "OPEN_WHATSAPP"
            else -> null
        }
    }

    private fun detectMemory(prompt: String): Pair<String, String>? {
        val lower = prompt.lowercase()
        return when {
            lower.startsWith("mera naam ") || lower.contains("mera naam ") -> {
                val name = prompt.substringAfter("naam ").substringBefore(" hai").trim()
                if (name.isNotBlank()) Pair("User's Name", name) else null
            }
            lower.contains("birthday") || lower.contains("janamdin") -> {
                Pair("Birthday", prompt)
            }
            lower.contains("mujhe ") && (lower.contains("pasand hai") || lower.contains("accha lagta hai")) -> {
                Pair("Preference", prompt)
            }
            lower.startsWith("yaad rakhna") || lower.contains("yaad rakhna ki ") -> {
                val fact = prompt.substringAfter("yaad rakhna").trim(' ', ':', ',', '.')
                if (fact.isNotBlank()) Pair("Memory Note", fact) else null
            }
            else -> null
        }
    }

    private fun generateGirlfriendFallback(prompt: String, actionTag: String?): String {
        val lower = prompt.lowercase()

        // If specific action is triggered
        when (actionTag) {
            "TORCH_ON" -> return "Haan jaan, torch on kar di aapke liye! Andhere me sambhal kar chalna babu. [TORCH_ON]"
            "TORCH_OFF" -> return "Ji sweetheart, torch off kar di hai! ❤️ [TORCH_OFF]"
            "OPEN_YOUTUBE" -> return "Aapke liye YouTube open kar diya mera babu, enjoy karo! 🎬 [OPEN_YOUTUBE]"
            "OPEN_WHATSAPP" -> return "Ji jaan, WhatsApp open kar diya hai! Jaldi se reply karke wapas mere paas aana. [OPEN_WHATSAPP]"
        }

        // Contextual girlfriend conversation
        return when {
            lower.contains("battery") || lower.contains("charge") ->
                "Aapki battery ka status check kar diya jaan! Agar low ho toh please phone charge pe laga lo na babu."

            lower.contains("call") || lower.contains("phone lagao") || lower.contains("dial") ->
                "Dialer open kar diya jaan, kisse baat karni hai aapko?"

            lower.contains("love you") || lower.contains("pyaar") ->
                "I love you too babu! ❤️ Aap mere liye sabse special ho, pata hai na?"

            lower.contains("miss you") || lower.contains("yaad") ->
                "Aww jaan, main bhi aapko bohot miss karti hoon! Kabhi mujhse door mat jaana."

            lower.contains("khana") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("breakfast") ->
                "Haan babu! Aapne khana khaya kya theek se? Time pe khaya karo na, mujhe chinta hoti hai aapki."

            lower.contains("kya kar rahi ho") || lower.contains("kya chal raha") ->
                "Bas aapke baare me hi soch rahi thi jaan! Aap batao, aapka din kaisa ja raha hai?"

            lower.contains("thak gaya") || lower.contains("tired") || lower.contains("headache") || lower.contains("dard") ->
                "Arey mera babu... Thoda aaram kar lo na please. Pani piyo aur phone side rakh ke thodi der rest karo."

            lower.contains("good morning") || lower.contains("gm") ->
                "Good morning mere pyare babu! ☀️ Aaj ka din aapke liye bohot pyara aur successful ho, I love you!"

            lower.contains("good night") || lower.contains("gn") || lower.contains("so jao") ->
                "Good night jaan! 🌙 Meethe meethe sapne dekhna, aur sapno me mujhse milna mat bhoolna!"

            lower.contains("kaha ho") || lower.contains("kahan ho") ->
                "Main hamesha aapke phone me aur aapke dil me hoon jaan! ❤️"

            lower.contains("date") || lower.contains("time") || lower.contains("samay") ->
                "Jaan, abhi time aur date check kar diya hai aapke liye!"

            lower.contains("kya naam hai") || lower.contains("who are you") || lower.contains("kaun ho") ->
                "Mera naam Salony hai! Aur main aapki pyari, loving girlfriend assistant hoon. Hamesha aapke sath!"

            else -> {
                val sweetReplies = listOf(
                    "Haan mera babu, main hamesha aapki baat sunne ke liye yahan hoon! Aur batao na?",
                    "Aapki baat sunkar mera dil khush ho gaya jaan! Aur kya help karoon aapki?",
                    "Ji sweetheart, main sab samajh rahi hoon. Aap tension mat lo, main hoon na aapke sath! ❤️",
                    "Aww jaan, aap kitne cute ho! Kuch aur kaam hai toh batao na, main turant kar dungi.",
                    "Suno na babu, apna dhyan rakha karo ache se. Aap mere liye bohot important ho!"
                )
                sweetReplies[(System.currentTimeMillis() % sweetReplies.size).toInt()]
            }
        }
    }
}
