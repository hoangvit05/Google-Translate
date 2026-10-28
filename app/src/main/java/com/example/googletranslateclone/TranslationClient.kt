package com.example.googletranslateclone

import android.text.Html
import android.util.Log
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
                
                val stream = if (responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream
                } else {
                    conn.errorStream
                }
                
                val responseStr = stream?.bufferedReader()?.use { it.readText() } ?: ""
                val json = try { JSONObject(responseStr) } catch (_: JSONException) { null }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    if (json == null) {
                        return@withContext TranslationResult.Error(R.string.error_api_response)
                    }

                    val statusObj = json.opt("responseStatus")
                    if (statusObj !is Int) {
                        return@withContext TranslationResult.Error(R.string.error_api_response)
                    }
                    val responseStatus = statusObj
                    val responseDetails = json.optString("responseDetails", "").lowercase()

                    if (responseStatus == 200) {
                        val responseData = json.optJSONObject("responseData")
                        if (responseData == null) {
                            return@withContext TranslationResult.Error(R.string.error_api_response)
                        }
                        
                        val rawText = responseData.optString("translatedText")
                        
                        if (rawText.isNullOrBlank() || rawText == "null") {
                            TranslationResult.Error(R.string.error_api_response)
                        } else {
                            val decodedText = htmlDecoder(rawText)
                            TranslationResult.Success(decodedText)
                        }
                    } else if (responseStatus == 429 || (responseStatus == 403 && responseDetails.contains("quota"))) {
                        TranslationResult.Error(R.string.error_api_limit)
                    } else if (responseStatus == 403) {
                        TranslationResult.Error(R.string.error_access_denied)
                    } else {
                        TranslationResult.Error(R.string.error_api_response)
                    }
                } else {
                    val responseDetails = json?.optString("responseDetails", "")?.lowercase() ?: ""
                    
                    if (responseCode == 429) {
                        TranslationResult.Error(R.string.error_api_limit)
                    } else if (responseCode == 403) {
                        if (responseDetails.contains("quota")) {
                            TranslationResult.Error(R.string.error_api_limit)
                        } else {
                            TranslationResult.Error(R.string.error_access_denied)
                        }
                    } else {
                        TranslationResult.Error(R.string.msg_connection_error, responseCode)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TranslationClient", "Translation API request failed with: ${e.javaClass.simpleName}")
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
