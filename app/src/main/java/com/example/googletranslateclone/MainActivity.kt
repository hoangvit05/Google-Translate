package com.example.googletranslateclone

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.googletranslateclone.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TranslationUIState {
    IDLE, LOADING, SUCCESS, ERROR
}

// Hosts the translation screen and its user interactions.
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var translateJob: Job? = null
    private val translationClient = TranslationClient()
    
    private var currentState = TranslationUIState.IDLE
    private var currentTranslationResult = ""
    private var isSwapping = false

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

        // Default source language: English (index 1); target language: Vietnamese (index 0).
        binding.spSourceLang.setSelection(1, false)
        binding.spTargetLang.setSelection(0, false)

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSwapping) {
                    invalidateCurrentRequest()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spSourceLang.onItemSelectedListener = spinnerListener
        binding.spTargetLang.onItemSelectedListener = spinnerListener
    }

    private fun setupEvents() {
        // Show the clear button only when the source field contains text.
        binding.edtSource.addTextChangedListener { text ->
            binding.btnClear.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
            // Invalidate pending request if user edits text
            if (!isSwapping) {
                invalidateCurrentRequest()
            }
        }

        // Clear the source text and reset the translation result.
        binding.btnClear.setOnClickListener {
            binding.edtSource.text?.clear()
            invalidateCurrentRequest()
            binding.tvTarget.text = getString(R.string.default_result)
        }

        // Validate the input before starting a translation request.
        binding.btnTranslate.setOnClickListener {
            val text = binding.edtSource.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_input_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check UTF-8 byte limit (500 bytes for MyMemory free API 'q' parameter)
            if (!InputValidator.isValidLength(text)) {
                Toast.makeText(this, getString(R.string.msg_input_too_long), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            callTranslateApi(text, currentSourceLang(), currentTargetLang())
        }

        // Swap the selected languages and exchange the source and result text.
        binding.btnSwap.setOnClickListener {
            if (currentState != TranslationUIState.SUCCESS) {
                isSwapping = true
                val tempPos = binding.spSourceLang.selectedItemPosition
                binding.spSourceLang.setSelection(binding.spTargetLang.selectedItemPosition)
                binding.spTargetLang.setSelection(tempPos)
                isSwapping = false
                invalidateCurrentRequest()
                return@setOnClickListener
            }
            
            val previousSource = binding.edtSource.text.toString().trim()
            val previousTarget = currentTranslationResult
            
            isSwapping = true
            
            val tempPos = binding.spSourceLang.selectedItemPosition
            binding.spSourceLang.setSelection(binding.spTargetLang.selectedItemPosition)
            binding.spTargetLang.setSelection(tempPos)

            binding.edtSource.setText(previousTarget)
            binding.edtSource.setSelection(previousTarget.length)
            
            isSwapping = false
            
            // Mark new state as success with swapped texts
            translateJob?.cancel()
            translateJob = null
            currentState = TranslationUIState.SUCCESS
            currentTranslationResult = previousSource
            binding.tvTarget.text = previousSource
        }

        // Copy a valid translation result to the system clipboard.
        binding.btnCopy.setOnClickListener {
            if (currentState != TranslationUIState.SUCCESS) {
                Toast.makeText(this, getString(R.string.msg_nothing_to_copy), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Translated Text", currentTranslationResult))
            Toast.makeText(this, getString(R.string.msg_copied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun invalidateCurrentRequest() {
        translateJob?.cancel()
        translateJob = null
        if (currentState != TranslationUIState.IDLE) {
            currentState = TranslationUIState.IDLE
            currentTranslationResult = ""
            binding.tvTarget.text = getString(R.string.default_result)
        }
    }

    private fun callTranslateApi(text: String, sourceLang: String, targetLang: String) {
        // Cancel the previous request so an older response cannot overwrite a newer one.
        translateJob?.cancel()

        currentState = TranslationUIState.LOADING
        binding.tvTarget.text = getString(R.string.translating)

        translateJob = lifecycleScope.launch {
            val result = translationClient.translate(text, sourceLang, targetLang)
            
            if (isActive) {
                when (result) {
                    is TranslationResult.Success -> {
                        currentState = TranslationUIState.SUCCESS
                        currentTranslationResult = result.text
                        binding.tvTarget.text = result.text
                    }
                    is TranslationResult.Error -> {
                        currentState = TranslationUIState.ERROR
                        currentTranslationResult = ""
                        val errorMessage = if (result.formatArg != null) {
                            getString(result.messageId, result.formatArg)
                        } else {
                            getString(result.messageId)
                        }
                        binding.tvTarget.text = errorMessage
                    }
                }
            }
        }
    }

    private fun currentSourceLang() = langMap[binding.spSourceLang.selectedItem.toString()] ?: "en"
    private fun currentTargetLang() = langMap[binding.spTargetLang.selectedItem.toString()] ?: "vi"
}
