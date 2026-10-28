package com.example.googletranslateclone

import android.text.Html
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class TranslationResult {
    data class Success(val text: String) : TranslationResult()
    data class Error(val messageId: Int, val formatArg: Int? = null) : TranslationResult()
}

class TranslationClient(
    private val urlProvider: (String, String) -> URL = ::defaultUrlProvider,
    private val htmlDecoder: (String) -> String = { text -> 
        @Suppress("DEPRECATION")
        Html.fromHtml(text).toString() 
    }
) {

    suspend fun translate(text: String, sourceLang: String, targetLang: String): TranslationResult {
        return withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                // Encode input exactly once
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val langPair = URLEncoder.encode("$sourceLang|$targetLang", "UTF-8")
                val url = urlProvider(encodedText, langPair)

                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.requestMethod = "GET"

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseStr)
                    
                    if (!json.has("responseStatus")) {
                        TranslationResult.Error(R.string.error_api_response)
                    } else {
                        val responseStatus = json.getInt("responseStatus")
                        if (responseStatus == 200) {
                            val responseData = json.optJSONObject("responseData")
                            val rawText = responseData?.optString("translatedText")
                            
                            if (rawText.isNullOrBlank()) {
                                TranslationResult.Error(R.string.error_api_response)
                            } else {
                                // Decode HTML entities returned by the translation API.
                                val decodedText = htmlDecoder(rawText)
                                TranslationResult.Success(decodedText)
                            }
                        } else if (responseStatus == 429 || responseStatus == 403) {
                            TranslationResult.Error(R.string.error_api_limit)
                        } else {
                            TranslationResult.Error(R.string.error_api_response)
                        }
                    }
                } else {
                    conn.errorStream?.close()
                    if (responseCode == 429 || responseCode == 403) {
                        TranslationResult.Error(R.string.error_api_limit)
                    } else {
                        TranslationResult.Error(R.string.msg_connection_error, responseCode)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
                when (e) {
                    is JSONException -> TranslationResult.Error(R.string.error_api_response)
                    is IOException -> TranslationResult.Error(R.string.error_network)
                    else -> TranslationResult.Error(R.string.error_network)
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    companion object {
        fun defaultUrlProvider(encodedText: String, langPair: String): URL {
            // Unverified email address removed. Anonymous limit applies.
            return URL("https://api.mymemory.translated.net/get?q=$encodedText&langpair=$langPair")
        }
    }
}
