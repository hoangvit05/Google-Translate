package com.example.googletranslateclone

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.googletranslateclone.databinding.ActivityMainBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var translateJob: Job? = null

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
        // Tự động ẩn/hiện nút xóa nhanh khi người dùng nhập văn bản
        binding.edtSource.addTextChangedListener { text ->
            binding.btnClear.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Nút xóa nhanh văn bản nguồn và reset kết quả
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

        // Nút đổi chiều ngôn ngữ và hoán đổi nội dung văn bản
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

        // Nút sao chép kết quả dịch
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
    }

    private fun callTranslateApi(text: String, sourceLang: String, targetLang: String) {
        // Hủy yêu cầu dịch trước đó nếu người dùng bấm dịch liên tục
        translateJob?.cancel()

        translateJob = lifecycleScope.launch(Dispatchers.IO) {
            val result = try {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val langPair = URLEncoder.encode("$sourceLang|$targetLang", "UTF-8")
                val apiUrl = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$langPair&de=googletranslateclone@app.com"

                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val rawText = JSONObject(responseStr).getJSONObject("responseData").getString("translatedText")
                    // Giải mã các ký tự HTML Entity (ví dụ: &#39; thành ')
                    Html.fromHtml(rawText, Html.FROM_HTML_MODE_LEGACY).toString()
                } else {
                    getString(R.string.msg_connection_error, conn.responseCode)
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                e.printStackTrace()
                getString(R.string.error_network)
            }

            withContext(Dispatchers.Main) {
                binding.tvTarget.text = result
            }
        }
    }

    private fun isValidTranslation(text: String): Boolean {
        return text.isNotBlank() &&
                text != getString(R.string.default_result) &&
                text != getString(R.string.translating) &&
                !text.startsWith("Lỗi")
    }

    private fun currentSourceLang() = langMap[binding.spSourceLang.selectedItem.toString()] ?: "en"
    private fun currentTargetLang() = langMap[binding.spTargetLang.selectedItem.toString()] ?: "vi"
}
