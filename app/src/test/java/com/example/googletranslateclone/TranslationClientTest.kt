package com.example.googletranslateclone

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLStreamHandler
import java.net.URLConnection
import java.io.ByteArrayInputStream
import java.io.IOException

class TranslationClientTest {

    @Test
    fun testValidTranslation() = runTest {
        val mockResponse = """{"responseData":{"translatedText":"Hello"},"responseStatus":200}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Success)
        assertEquals("Hello", (result as TranslationResult.Success).text)
    }

    @Test
    fun testValidTranslationStartingWithLoi() = runTest {
        // Legitimate translation starting with "Lỗi" must remain valid
        val mockResponse = """{"responseData":{"translatedText":"Lỗi hệ thống"},"responseStatus":200}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("System error", "en", "vi")
        assertTrue(result is TranslationResult.Success)
        assertEquals("Lỗi hệ thống", (result as TranslationResult.Success).text)
    }

    @Test
    fun testMalformedJson() = runTest {
        val mockResponse = """{ bad json """
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_response, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testHttpError() = runTest {
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(500, "") },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.msg_connection_error, (result as TranslationResult.Error).messageId)
        assertEquals(500, result.formatArg)
    }

    @Test
    fun testHttp429QuotaError() = runTest {
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(429, "") },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_limit, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testHttp403AccessDeniedError() = runTest {
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(403, "") },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_access_denied, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testApiLimitErrorViaJsonResponseStatus() = runTest {
        val mockResponse = """{"responseDetails":"Quota Exceeded","responseStatus":429}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_limit, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testApiLimitErrorViaResponseDetails() = runTest {
        val mockResponse = """{"responseDetails":"QUOTA EXCEEDED","responseStatus":403}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(403, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_limit, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testInvalidResponseStatusType() = runTest {
        val mockResponse = """{"responseData":{"translatedText":"Hello"},"responseStatus":"200"}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_response, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testHttp403WithSuccessJsonStillFails() = runTest {
        val mockResponse = """{"responseData":{"translatedText":"Hello"},"responseStatus":200}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(403, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_access_denied, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testHttp429WithSuccessJsonStillFails() = runTest {
        val mockResponse = """{"responseData":{"translatedText":"Hello"},"responseStatus":200}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(429, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_limit, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testHttp500WithEmptyErrorStream() = runTest {
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(500, "") },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.msg_connection_error, (result as TranslationResult.Error).messageId)
        assertEquals(500, result.formatArg)
    }

    @Test
    fun testHttp200WithResponseStatus403AndQuotaDetails() = runTest {
        val mockResponse = """{"responseDetails":"QUOTA EXCEEDED","responseStatus":403}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_limit, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testHttp200WithResponseStatus403AccessDenied() = runTest {
        val mockResponse = """{"responseDetails":"FORBIDDEN","responseStatus":403}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_access_denied, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testNullStringTranslatedText() = runTest {
        val mockResponse = """{"responseStatus":200, "responseData":{"translatedText":"null"}}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Le bonheur...", "fr", "vi")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_response, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testLiteralNullTranslatedText() = runTest {
        val mockResponse = """{"responseStatus":200, "responseData":{"translatedText":null}}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Le bonheur...", "fr", "vi")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_response, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testMissingResponseStatus() = runTest {
        val mockResponse = """{"responseData":{"translatedText":"Hello"}}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_response, (result as TranslationResult.Error).messageId)
    }

    @Test
    fun testMissingTranslatedText() = runTest {
        val mockResponse = """{"responseStatus":200, "responseData":{}}"""
        val client = TranslationClient(
            urlProvider = { _, _ -> createMockUrl(200, mockResponse) },
            htmlDecoder = { it }
        )
        
        val result = client.translate("Xin chào", "vi", "en")
        assertTrue(result is TranslationResult.Error)
        assertEquals(R.string.error_api_response, (result as TranslationResult.Error).messageId)
    }

    private fun createMockUrl(statusCode: Int, responseBody: String): URL {
        val handler = object : URLStreamHandler() {
            override fun openConnection(u: URL?): URLConnection {
                return object : HttpURLConnection(u) {
                    override fun connect() {}
                    override fun disconnect() {}
                    override fun usingProxy(): Boolean = false
                    override fun getResponseCode(): Int = statusCode
                    override fun getInputStream() = if (statusCode == 200) ByteArrayInputStream(responseBody.toByteArray()) else throw IOException("Server returned HTTP response code: $statusCode")
                    override fun getErrorStream() = if (statusCode != 200) ByteArrayInputStream(responseBody.toByteArray()) else null
                }
            }
        }
        return URL("http", "mock", 80, "", handler)
    }
}