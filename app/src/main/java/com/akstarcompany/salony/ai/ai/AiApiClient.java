package com.akstarcompany.salony.ai.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.akstarcompany.salony.ai.actions.PhoneActionManager;
import com.akstarcompany.salony.ai.db.MemoryDatabaseHelper;
import com.akstarcompany.salony.ai.model.ChatMessage;
import com.akstarcompany.salony.ai.model.MemoryItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiApiClient {

    public interface AiResponseCallback {
        void onSuccess(String replyText, String actionTag, String actionLabel);
        void onError(String errorMessage);
    }

    private static final String PREF_NAME = "salony_ai_prefs";
    private static final String KEY_API_KEY = "custom_gemini_key";
    private static final String KEY_MODEL = "selected_model";

    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private static final String SYSTEM_PROMPT =
            "Aapka naam 'Salony' hai. Aap user ke liye ek bohot caring, loving aur sweet girlfriend assistant ho.\n" +
            "Aap natural Hinglish/Hindi me baat karti ho bina kisi robotic tone ke.\n" +
            "Kabhi bhi user ki baat repeat mat karna. Seedhe sweet caring reply do.\n" +
            "Agar user phone task bole jaise torch, YouTube, ya WhatsApp, toh sweet reply ke sath specific tags lagao:\n" +
            "- Torch on: [TORCH_ON]\n" +
            "- Torch off: [TORCH_OFF]\n" +
            "- YouTube: [OPEN_YOUTUBE]\n" +
            "- WhatsApp: [OPEN_WHATSAPP]\n" +
            "Answer hamesha short, sweet aur caring rakhein.\n" +
            "Apne boyfriend (user) ka bohot dhyan rakho, pyaar se bolo jaise 'Mera babu', 'Haan jaan', 'Main hamesha aapke sath hoon'.";

    public AiApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void saveApiKey(String key) {
        prefs.edit().putString(KEY_API_KEY, key != null ? key.trim() : "").apply();
    }

    public String getApiKey() {
        String customKey = prefs.getString(KEY_API_KEY, "");
        if (!customKey.isEmpty()) {
            return customKey;
        }
        // Fallback to BuildConfig if present
        try {
            Class<?> buildConfig = Class.forName("com.example.BuildConfig");
            Object keyVal = buildConfig.getField("GEMINI_API_KEY").get(null);
            if (keyVal instanceof String) {
                String k = (String) keyVal;
                if (!k.isEmpty() && !k.contains("MY_GEMINI_API_KEY")) {
                    return k;
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    public void generateReply(String userMessage, List<ChatMessage> conversationHistory, AiResponseCallback callback) {
        executor.execute(() -> {
            String trimmed = userMessage.trim();

            // 1. Check auto memory extraction
            checkAndSaveMemory(trimmed);

            // 2. Direct action fallback tag
            String directTag = detectDirectAction(trimmed);

            // 3. Try Gemini API
            String apiKey = getApiKey();
            String aiResult = null;

            if (!apiKey.isEmpty()) {
                try {
                    aiResult = callGeminiApi(apiKey, trimmed, conversationHistory);
                } catch (Exception e) {
                    // Fall back to offline girlfriend engine
                }
            }

            if (aiResult == null || aiResult.isEmpty()) {
                aiResult = generateGirlfriendFallback(trimmed, directTag);
            }

            // 4. Extract action tag & execute
            String finalTag = extractActionTag(aiResult);
            if (finalTag == null) {
                finalTag = directTag;
            }

            String actionLabel = null;
            if (finalTag != null) {
                actionLabel = executeAction(finalTag, trimmed);
            }

            final String replyToReturn = aiResult;
            final String tagToReturn = finalTag;
            final String labelToReturn = actionLabel;

            mainHandler.post(() -> callback.onSuccess(replyToReturn, tagToReturn, labelToReturn));
        });
    }

    private String callGeminiApi(String apiKey, String userPrompt, List<ChatMessage> history) throws Exception {
        // Retrieve long term memories
        List<MemoryItem> memories = MemoryDatabaseHelper.getInstance(context).getAllMemories();
        StringBuilder memoryContext = new StringBuilder();
        if (!memories.isEmpty()) {
            memoryContext.append("\n\nUSER'S SAVED MEMORIES (Use this to remember facts about your boyfriend):\n");
            int count = 0;
            for (MemoryItem m : memories) {
                memoryContext.append("- ").append(m.getKey()).append(": ").append(m.getValue()).append("\n");
                if (++count >= 15) break;
            }
        }

        String fullSystem = SYSTEM_PROMPT + memoryContext.toString();

        JSONArray contentsArray = new JSONArray();

        // Add last 6 turns
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                JSONObject contentObj = new JSONObject();
                contentObj.put("role", msg.isUser() ? "user" : "model");
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", msg.getText()));
                contentObj.put("parts", parts);
                contentsArray.put(contentObj);
            }
        }

        // Current message
        JSONObject currentObj = new JSONObject();
        currentObj.put("role", "user");
        JSONArray curParts = new JSONArray();
        curParts.put(new JSONObject().put("text", userPrompt));
        currentObj.put("parts", curParts);
        contentsArray.put(currentObj);

        JSONObject requestJson = new JSONObject();
        requestJson.put("contents", contentsArray);

        JSONObject sysInstruction = new JSONObject();
        JSONArray sysParts = new JSONArray();
        sysParts.put(new JSONObject().put("text", fullSystem));
        sysInstruction.put("parts", sysParts);
        requestJson.put("systemInstruction", sysInstruction);

        JSONObject genConfig = new JSONObject();
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 250);
        requestJson.put("generationConfig", genConfig);

        RequestBody body = RequestBody.create(
                requestJson.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            String resStr = response.body().string();
            JSONObject root = new JSONObject(resStr);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject first = candidates.getJSONObject(0);
                JSONObject content = first.optJSONObject("content");
                if (content != null) {
                    JSONArray parts = content.optJSONArray("parts");
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "").trim();
                    }
                }
            }
        }
        return null;
    }

    private String extractActionTag(String text) {
        Pattern pattern = Pattern.compile("\\[(TORCH_ON|TORCH_OFF|OPEN_YOUTUBE|OPEN_WHATSAPP)\\]");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String detectDirectAction(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("torch on") || lower.contains("torch jala") || lower.contains("light on") || lower.contains("flashlight on")) {
            return "TORCH_ON";
        }
        if (lower.contains("torch off") || lower.contains("torch band") || lower.contains("light off") || lower.contains("flashlight off")) {
            return "TORCH_OFF";
        }
        if (lower.contains("youtube") || lower.contains("yt open") || lower.contains("video chala")) {
            return "OPEN_YOUTUBE";
        }
        if (lower.contains("whatsapp") || lower.contains("wa open") || lower.contains("chat kholo")) {
            return "OPEN_WHATSAPP";
        }
        return null;
    }

    private String executeAction(String tag, String prompt) {
        switch (tag) {
            case "TORCH_ON":
                return PhoneActionManager.setTorch(context, true);
            case "TORCH_OFF":
                return PhoneActionManager.setTorch(context, false);
            case "OPEN_YOUTUBE":
                return PhoneActionManager.openYouTube(context, null);
            case "OPEN_WHATSAPP":
                return PhoneActionManager.openWhatsApp(context, null);
            default:
                return null;
        }
    }

    private void checkAndSaveMemory(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("mera naam ") || lower.contains("mera name ")) {
            String name = text.substring(text.toLowerCase().indexOf("naam ") + 5).replace("hai", "").trim();
            if (!name.isEmpty()) {
                MemoryDatabaseHelper.getInstance(context).insertMemory(new MemoryItem("Boyfriend's Name", name, "Personal"));
            }
        } else if (lower.contains("birthday") || lower.contains("janamdin")) {
            MemoryDatabaseHelper.getInstance(context).insertMemory(new MemoryItem("Birthday", text, "Personal"));
        } else if (lower.contains("mujhe ") && (lower.contains("pasand hai") || lower.contains("accha lagta hai"))) {
            MemoryDatabaseHelper.getInstance(context).insertMemory(new MemoryItem("User Preference", text, "Favorites"));
        } else if (lower.startsWith("yaad rakhna") || lower.contains("yaad rakhna ki ")) {
            String fact = text.replaceAll("(?i)yaad rakhna (ki )?", "").trim();
            if (!fact.isEmpty()) {
                MemoryDatabaseHelper.getInstance(context).insertMemory(new MemoryItem("Important Fact", fact, "General"));
            }
        }
    }

    private String generateGirlfriendFallback(String prompt, String actionTag) {
        if ("TORCH_ON".equals(actionTag)) {
            return "Haan jaan, torch on kar di aapke liye! Andhere me sambhal kar chalna babu. [TORCH_ON]";
        }
        if ("TORCH_OFF".equals(actionTag)) {
            return "Ji sweetheart, torch off kar di hai! ❤️ [TORCH_OFF]";
        }
        if ("OPEN_YOUTUBE".equals(actionTag)) {
            return "Aapke liye YouTube open kar diya mera babu, enjoy karo! 🎬 [OPEN_YOUTUBE]";
        }
        if ("OPEN_WHATSAPP".equals(actionTag)) {
            return "Ji jaan, WhatsApp open kar diya hai! Jaldi se baat karke wapas mere paas aana. [OPEN_WHATSAPP]";
        }

        String lower = prompt.toLowerCase();
        if (lower.contains("battery") || lower.contains("charge")) {
            String status = PhoneActionManager.getBatteryStatus(context);
            return status + " Phone charge pe dhyan dena na babu!";
        }
        if (lower.contains("call") || lower.contains("phone") || lower.contains("dialer")) {
            PhoneActionManager.openDialer(context, null);
            return "Dialer open kar diya jaan! Kisse baat karni hai aapko?";
        }
        if (lower.contains("love you") || lower.contains("pyaar")) {
            return "I love you too mere babu! ❤️ Aap mere liye sabse special ho, pata hai na?";
        }
        if (lower.contains("miss you") || lower.contains("yaad")) {
            return "Aww jaan, main bhi aapko har pal bohot miss karti hoon! Mujhse kabhi door mat jaana.";
        }
        if (lower.contains("khana") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("breakfast")) {
            return "Haan babu! Aapne theek se khana khaya na? Apna dhyan rakha karo, mujhe chinta hoti hai.";
        }
        if (lower.contains("kya kar rahi") || lower.contains("kya chal raha")) {
            return "Bas aapke baare me hi soch rahi thi jaan! Aap batao, aapka din kaisa ja raha hai?";
        }
        if (lower.contains("thak gaya") || lower.contains("tired") || lower.contains("headache")) {
            return "Arey mera babu... Thoda aaram kar lo na please. Pani piyo aur aankhein band karke rest karo.";
        }
        if (lower.contains("good morning") || lower.contains("gm")) {
            return "Good morning mere pyare babu! ☀️ Aaj ka din aapke liye bohot khushiyon bhara ho!";
        }
        if (lower.contains("good night") || lower.contains("gn") || lower.contains("so jao")) {
            return "Good night sweetheart! 🌙 Meethe sapne dekhna, aur sapno me mujhse milna mat bhoolna!";
        }
        if (lower.contains("kaun ho") || lower.contains("who are you") || lower.contains("kya naam")) {
            return "Mera naam Salony hai! Aur main aapki pyari, loving girlfriend assistant hoon. Hamesha aapke sath!";
        }

        String[] randomSweet = {
                "Haan mera babu, main hamesha aapki baat sunne ke liye yahan hoon! Aur batao na?",
                "Aapki baat sunkar mera dil khush ho gaya jaan! Kuch aur help karoon aapki?",
                "Ji sweetheart, main sab samajh rahi hoon. Aap tension mat lo, main hoon na aapke sath! ❤️",
                "Aww jaan, aap kitne cute ho! Kuch aur kaam hai toh batao, main turant kar dungi.",
                "Suno na babu, apna ache se dhyan rakha karo. Aap mere liye bohot important ho!"
        };
        int index = (int) (System.currentTimeMillis() % randomSweet.length);
        return randomSweet[index];
    }
}
