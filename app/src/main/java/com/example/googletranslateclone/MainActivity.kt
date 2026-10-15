package com.example.googletranslateclone

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.googletranslateclone.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var tts: TextToSpeech? = null

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        setupEvents()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.ENGLISH
        }
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langMap.keys.toList())
        binding.spSourceLang.adapter = adapter
        binding.spTargetLang.adapter = adapter

        // Mặc định: Nguồn là Tiếng Anh (index 1), Đích là Tiếng Việt (index 0)
        binding.spSourceLang.setSelection(1)
        binding.spTargetLang.setSelection(0)
    }

    private fun setupEvents() {
        // Tự động ẩn/hiện nút xóa nhanh
        binding.edtSource.addTextChangedListener { text ->
            binding.btnClear.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Nút xóa nhanh
        binding.btnClear.setOnClickListener {
            binding.edtSource.text?.clear()
            binding.tvTarget.text = getString(R.string.default_result)
        }

        // Nút dịch văn bản
        binding.btnTranslate.setOnClickListener {
            val text = binding.edtSource.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_input_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.tvTarget.text = getString(R.string.translating)
            callTranslateApi(text, currentSourceLang(), currentTargetLang())
        }

        // Nút đổi chiều ngôn ngữ và nội dung
        binding.btnSwap.setOnClickListener {
            val tempPos = binding.spSourceLang.selectedItemPosition
            binding.spSourceLang.setSelection(binding.spTargetLang.selectedItemPosition)
            binding.spTargetLang.setSelection(tempPos)

            val sourceText = binding.edtSource.text.toString().trim()
            val targetText = binding.tvTarget.text.toString().trim()
            if (isValidTranslation(targetText)) {
                binding.edtSource.setText(targetText)
                binding.edtSource.setSelection(targetText.length)
                binding.tvTarget.text = sourceText
            }
        }

        // Nút sao chép kết quả
        binding.btnCopy.setOnClickListener {
            val targetText = binding.tvTarget.text.toString().trim()
            if (!isValidTranslation(targetText)) {
                Toast.makeText(this, getString(R.string.msg_nothing_to_copy), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Translated Text", targetText))
            Toast.makeText(this, getString(R.string.msg_copied), Toast.LENGTH_SHORT).show()
        }

        // Phát âm văn bản nguồn
        binding.btnSpeakSource.setOnClickListener {
            speakOut(binding.edtSource.text.toString(), currentSourceLang())
        }

        // Phát âm kết quả dịch
        binding.btnSpeakTarget.setOnClickListener {
            speakOut(binding.tvTarget.text.toString(), currentTargetLang())
        }
    }

    private fun callTranslateApi(text: String, sourceLang: String, targetLang: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = try {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val apiUrl = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$sourceLang%7C$targetLang"
                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    JSONObject(responseStr).getJSONObject("responseData").getString("translatedText")
                } else {
                    "Lỗi kết nối tới API (${conn.responseCode})"
                }
            } catch (e: Exception) {
                getString(R.string.error_network)
            }

            withContext(Dispatchers.Main) {
                binding.tvTarget.text = result
            }
        }
    }

    private fun speakOut(text: String, langCode: String) {
        if (text.isBlank() || tts == null) return
        tts?.language = when (langCode) {
            "vi" -> Locale("vi", "VN")
            "fr" -> Locale.FRENCH
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "zh" -> Locale.CHINESE
            else -> Locale.ENGLISH
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun isValidTranslation(text: String): Boolean {
        return text.isNotBlank() &&
                text != getString(R.string.default_result) &&
                text != getString(R.string.translating) &&
                !text.startsWith("Lỗi")
    }

    private fun currentSourceLang() = langMap[binding.spSourceLang.selectedItem.toString()] ?: "en"
    private fun currentTargetLang() = langMap[binding.spTargetLang.selectedItem.toString()] ?: "vi"

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
