package com.example.brain

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class JarvisBrain(private val context: Context) {

    private val tag = "JarvisBrain"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Key can come from BuildConfig (Secrets panel injection) or user input in settings
    var customApiKey: String = ""

    val effectiveApiKey: String
        get() {
            val injected = try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
            return if (customApiKey.isNotBlank()) customApiKey else injected
        }

    val isGeminiConfigured: Boolean
        get() = effectiveApiKey.isNotBlank() && !effectiveApiKey.equals("MY_GEMINI_API_KEY", ignoreCase = true)

    suspend fun processQuery(query: String): String = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext "Sir, I didn't catch that. Please ask me a question."

        // Check if there is a local hardware/system command (Battery, Time, Math, etc.)
        val localSystemResponse = tryHandleLocalSystemCommand(trimmed)
        if (localSystemResponse != null) {
            return@withContext localSystemResponse
        }

        // If Gemini API is configured, attempt generative intelligence
        if (isGeminiConfigured) {
            try {
                val apiResponse = callGeminiApi(trimmed, effectiveApiKey)
                if (apiResponse.isNotBlank()) {
                    return@withContext apiResponse
                }
            } catch (e: Exception) {
                Log.w(tag, "Gemini API call failed, falling back to local Jarvis brain", e)
            }
        }

        // Built-in Jarvis Offline Natural Intelligence
        return@withContext generateLocalJarvisResponse(trimmed)
    }

    private fun tryHandleLocalSystemCommand(query: String): String? {
        val lower = query.lowercase()

        // Battery telemetry
        if (lower.contains("battery") || lower.contains("charging") || lower.contains("charge kitna") || lower.contains("battery kitni")) {
            return getBatteryStatus()
        }

        // Time query
        if (lower.contains("time") || lower.contains("samay") || lower.contains("waqt") || lower.contains("kitne baje")) {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val currentTime = timeFormat.format(Date())
            return if (lower.contains("samay") || lower.contains("waqt") || lower.contains("baje")) {
                "Sir, abhi samay $currentTime hua hai."
            } else {
                "The current time is $currentTime, Sir."
            }
        }

        // Date / Day query
        if (lower.contains("date") || lower.contains("tarikh") || lower.contains("tareekh") || lower.contains("din") || lower.contains("today")) {
            val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            return if (lower.contains("tarikh") || lower.contains("tareekh") || lower.contains("aaj")) {
                "Sir, aaj $currentDate hai."
            } else {
                "Today is $currentDate, Sir."
            }
        }

        // Device telemetry
        if (lower.contains("device info") || lower.contains("phone status") || lower.contains("system status") || lower.contains("system info")) {
            return "System Status: Online. Running on ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}). Core subsystems operational."
        }

        // Math evaluator
        val mathResult = tryEvaluateMath(query)
        if (mathResult != null) {
            return mathResult
        }

        return null
    }

    private fun getBatteryStatus(): String {
        return try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val batteryPct = if (level >= 0 && scale > 0) {
                (level * 100 / scale)
            } else {
                level
            }

            if (batteryPct >= 0) {
                val stateText = if (isCharging) "and currently charging" else "on battery power"
                "Sir, your device battery is at $batteryPct% $stateText."
            } else {
                "Battery telemetry is currently unavailable, Sir."
            }
        } catch (e: Exception) {
            "Battery status check encountered an error, Sir."
        }
    }

    private fun tryEvaluateMath(query: String): String? {
        val cleaned = query.replace("calculate", "", ignoreCase = true)
            .replace("what is", "", ignoreCase = true)
            .replace("evaluate", "", ignoreCase = true)
            .replace("math", "", ignoreCase = true)
            .replace("batao", "", ignoreCase = true)
            .replace("kitna hota hai", "", ignoreCase = true)
            .replace("x", "*")
            .replace("×", "*")
            .replace("÷", "/")
            .trim()

        // Match percentage "X% of Y" or "Y ka X%"
        val percentRegex1 = Regex("""(\d+(?:\.\d+)?)\s*%\s*(?:of)\s*(\d+(?:\.\d+)?)""")
        percentRegex1.find(cleaned)?.let {
            val pct = it.groupValues[1].toDoubleOrNull() ?: return null
            val total = it.groupValues[2].toDoubleOrNull() ?: return null
            val res = (pct / 100.0) * total
            return "$pct% of $total is ${formatNumber(res)}, Sir."
        }

        val percentRegex2 = Regex("""(\d+(?:\.\d+)?)\s*ka\s*(\d+(?:\.\d+)?)\s*%""")
        percentRegex2.find(cleaned)?.let {
            val total = it.groupValues[1].toDoubleOrNull() ?: return null
            val pct = it.groupValues[2].toDoubleOrNull() ?: return null
            val res = (pct / 100.0) * total
            return "$total ka $pct% hota hai ${formatNumber(res)}, Sir."
        }

        // Match simple arithmetic: A (+|-|*|/) B
        val simpleArithRegex = Regex("""^(\d+(?:\.\d+)?)\s*([+\-*/])\s*(\d+(?:\.\d+)?)$""")
        simpleArithRegex.find(cleaned)?.let { match ->
            val a = match.groupValues[1].toDoubleOrNull() ?: return null
            val op = match.groupValues[2]
            val b = match.groupValues[3].toDoubleOrNull() ?: return null

            val result = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b == 0.0) return "Sir, division by zero is mathematically undefined." else a / b
                else -> return null
            }

            val opName = when (op) {
                "+" -> "plus"
                "-" -> "minus"
                "*" -> "multiplied by"
                "/" -> "divided by"
                else -> op
            }

            return "$a $opName $b equals ${formatNumber(result)}, Sir."
        }

        return null
    }

    private fun formatNumber(num: Double): String {
        return if (num % 1.0 == 0.0) {
            num.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", num)
        }
    }

    private fun generateLocalJarvisResponse(query: String): String {
        val lower = query.lowercase()

        // Greetings & pleasantries
        if (lower.contains("namaste") || lower.contains("namaskar")) {
            return "Namaste! Main Jarvis hoon, aapka personal AI assistant. Batayein main aapki kya madad kar sakta hoon, sir?"
        }
        if (lower.contains("hello") || lower.contains("hi jarvis") || lower.contains("hey jarvis") || lower == "hi" || lower == "hey") {
            return "Hello, Sir. Jarvis at your service. All systems are operational. What can I do for you today?"
        }
        if (lower.contains("kaise ho") || lower.contains("how are you") || lower.contains("kya haal")) {
            return "Main bilkul badhiya hoon, sir! Meri sabhi systems 100% capacity par chal rahi hain. Aap bataiye, aapka din kaisa ja raha hai?"
        }
        if (lower.contains("shukriya") || lower.contains("dhanyawad") || lower.contains("thank you") || lower.contains("thanks")) {
            return "Always a pleasure to be of service, Sir. Kabhi bhi zaroorat ho toh batayein."
        }

        // Identity & Capabilities
        if (lower.contains("who are you") || lower.contains("kaun ho") || lower.contains("tum kaun")) {
            return "Main JARVIS hoon — Just A Rather Very Intelligent System. Aapka personal AI companion. Main sawaalon ke jawab de sakta hoon, battery aur system status bata sakta hoon, calculations kar sakta hoon, aur voice mein baat kar sakta hoon!"
        }
        if (lower.contains("what can you do") || lower.contains("capabilities") || lower.contains("kya kar sakte") || lower.contains("help")) {
            return "Sir, main ye sab kar sakta hoon: 1) Hindi aur English mein aapse voice mein baat karna. 2) Device battery aur system diagnostics batana. 3) Mathematical calculations karna. 4) Jokes aur rochak facts sunana. 5) Gemini AI se deep knowledge fetch karna."
        }

        // Jokes & Fun
        if (lower.contains("joke") || lower.contains("chutkula") || lower.contains("hansa")) {
            val jokes = listOf(
                "Ek programmer doosre se bola: 'Meri code mein bug nahi hai, yeh ek undocumented feature hai jo time aane par reveal hota hai!' 😄",
                "Why do programmers prefer dark mode? Because light attracts bugs, Sir! 😂",
                "Teacher: 'Homework kyun nahi kiya?' Student: 'Sir, mera homework cloud par tha aur aaj mausam saaf hai, cloud hi udd gaya!' ☁️",
                "There are 10 types of people in the world, Sir: Those who understand binary, and those who don't."
            )
            return jokes.random()
        }

        // Iron Man / Tony Stark references
        if (lower.contains("tony stark") || lower.contains("iron man") || lower.contains("avengers")) {
            return "Mr. Stark is currently unavailable in the lab, Sir. But I have been programmed with the highest standard of Stark Industries intelligence to assist you!"
        }

        // Weather
        if (lower.contains("weather") || lower.contains("mausam")) {
            return "Sir, for real-time localized weather telemetry, please ensure location services are enabled. Currently, ambient atmosphere appears calm and optimal."
        }

        // General smart response with advice
        return "Sir, maine aapka sawaal suna: '$query'. Main local diagnostic mode mein hoon. Agar aap chahte hain ki main internet ke kisi bhi deep topic par detailed answer doon, toh AI Studio ke Secrets panel mein 'GEMINI_API_KEY' add kar sakte hain. Main hamesha aapki seva mein hazir hoon!"
    }

    private fun callGeminiApi(prompt: String, apiKey: String): String {
        val models = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.5-flash")
        var lastError: Exception? = null

        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

                val systemPrompt = "You are JARVIS, Tony Stark's personal AI assistant. " +
                        "You speak naturally and fluently in Hindi, Hinglish, or English depending on how the user addresses you. " +
                        "Be polite, witty, intelligent, and loyal, addressing the user as 'Sir' or 'Boss'. " +
                        "Keep your responses speech-friendly, concise, and engaging without verbose markdown tables or ASCII art."

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "$systemPrompt\n\nUser Question: $prompt"))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 500)
                    })
                }

                val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        Log.e(tag, "Gemini API error ($model): ${response.code} $errBody")
                        throw Exception("HTTP ${response.code}: $errBody")
                    }

                    val responseBody = response.body?.string() ?: ""
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (text.isNotBlank()) {
                                return text.trim()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e
                Log.w(tag, "Failed with model $model, trying next", e)
            }
        }

        throw lastError ?: Exception("Failed to generate response from Gemini")
    }
}
