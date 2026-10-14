package com.example.googletranslateclone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var edtSource: EditText
    private lateinit var tvTarget: TextView
    private lateinit var spSourceLang: Spinner
    private lateinit var spTargetLang: Spinner
    private lateinit var btnTranslate: Button
    private lateinit var btnMic: ImageButton
    private lateinit var btnSpeakSource: ImageButton
    private lateinit var btnSpeakTarget: ImageButton
    private lateinit var btnSwap: ImageButton

    private var tts: TextToSpeech? = null
    private val REQ_CODE_SPEECH_INPUT = 100

    // Không cần dùng SERVER_URL Node.js nữa vì đã tích hợp gọi API trực tiếp
    // private val SERVER_URL = "http://10.0.2.2:5000/api/translate"

    private val langMap = mapOf(
        "Tiếng Việt" to "vi",
        "Tiếng Anh" to "en",
        "Tiếng Pháp" to "fr",
        "Tiếng Nhật" to "ja",
        "Tiếng Hàn" to "ko",
        "Tiếng Trung" to "zh"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupSpinners()
        setupEvents()

        // Khởi tạo Text-to-Speech
        tts = TextToSpeech(this, this)
    }

    private fun initViews() {
        edtSource = findViewById(R.id.edtSource)
        tvTarget = findViewById(R.id.tvTarget)
        spSourceLang = findViewById(R.id.spSourceLang)
        spTargetLang = findViewById(R.id.spTargetLang)
        btnTranslate = findViewById(R.id.btnTranslate)
        btnMic = findViewById(R.id.btnMic)
        btnSpeakSource = findViewById(R.id.btnSpeakSource)
        btnSpeakTarget = findViewById(R.id.btnSpeakTarget)
        btnSwap = findViewById(R.id.btnSwap)
    }

    private fun setupSpinners() {
        val languages = langMap.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spSourceLang.adapter = adapter
        spTargetLang.adapter = adapter

        // Mặc định: Nguồn là Tiếng Anh (index 1), Đích là Tiếng Việt (index 0)
        spSourceLang.setSelection(1)
        spTargetLang.setSelection(0)
    }

    private fun setupEvents() {
        // Nút dịch văn bản qua API Node.js
        btnTranslate.setOnClickListener {
            val text = edtSource.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập văn bản!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sourceLangCode = langMap[spSourceLang.selectedItem.toString()] ?: "en"
            val targetLangCode = langMap[spTargetLang.selectedItem.toString()] ?: "vi"

            tvTarget.text = "Đang dịch..."
            callTranslateApi(text, sourceLangCode, targetLangCode)
        }

        // Nút đổi chiều ngôn ngữ (Swap)
        btnSwap.setOnClickListener {
            val temp = spSourceLang.selectedItemPosition
            spSourceLang.setSelection(spTargetLang.selectedItemPosition)
            spTargetLang.setSelection(temp)
        }

        // Nút ghi âm giọng nói (Speech-to-Text)
        btnMic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            } else {
                startVoiceInput()
            }
        }

        // Nút phát âm văn bản nguồn
        btnSpeakSource.setOnClickListener {
            speakOut(edtSource.text.toString(), langMap[spSourceLang.selectedItem.toString()] ?: "en")
        }

        // Nút phát âm kết quả dịch
        btnSpeakTarget.setOnClickListener {
            speakOut(tvTarget.text.toString(), langMap[spTargetLang.selectedItem.toString()] ?: "vi")
        }
    }

    private fun callTranslateApi(text: String, sourceLang: String, targetLang: String) {
        thread {
            try {
                // Đổi sang dùng MyMemory API (Miễn phí, không bị chặn)
                val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
                val apiUrl = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$sourceLang|$targetLang"
                
                val url = URL(apiUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    
                    // Phân tích cú pháp JSON của MyMemory API
                    val jsonResponse = org.json.JSONObject(responseStr)
                    val responseData = jsonResponse.getJSONObject("responseData")
                    val translatedText = responseData.getString("translatedText")

                    runOnUiThread {
                        tvTarget.text = translatedText
                    }
                } else {
                    runOnUiThread {
                        tvTarget.text = "Lỗi kết nối tới API ($responseCode)"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvTarget.text = "Lỗi: Không thể kết nối. Hãy kiểm tra Internet!"
                }
            }
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói câu bạn muốn dịch...")
        }
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                edtSource.setText(result[0])
                btnTranslate.performClick()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH
        }
    }

    private fun speakOut(text: String, langCode: String) {
        if (text.isEmpty() || tts == null) return
        val locale = when (langCode) {
            "vi" -> Locale("vi", "VN")
            "fr" -> Locale.FRENCH
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "zh" -> Locale.CHINESE
            else -> Locale.ENGLISH
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
