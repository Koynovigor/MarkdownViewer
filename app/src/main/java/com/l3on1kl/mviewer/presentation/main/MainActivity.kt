package com.l3on1kl.mviewer.presentation.main

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.l3on1kl.mviewer.R
import com.l3on1kl.mviewer.databinding.ActivityMainBinding
import com.l3on1kl.mviewer.presentation.model.MainUiState
import com.l3on1kl.mviewer.presentation.util.applySystemBarsPadding
import com.l3on1kl.mviewer.presentation.viewer.DocumentViewerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val viewModel: MainViewModel by viewModels()

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::onLocalFileSelected)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.bind(findViewById(R.id.main))

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.main.applySystemBarsPadding()

        binding.openFileButton.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("text/*"))
        }
        binding.loadUrlButton.setOnClickListener {
            val url = binding.urlInput.text.toString()
            if (url.isNotBlank()) viewModel.onUrlEntered(url)
        }

        binding.urlInputLayout.setEndIconOnClickListener {
            val clipboard = getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager

            val clipboardText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()

            if (!clipboardText.isNullOrBlank()) {
                binding.urlInput.setText(clipboardText)
            } else {
                Toast.makeText(
                    this,
                    R.string.clipboard_empty,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            MainUiState.Loading -> {
                                binding.progressBar.isVisible = true
                                binding.contentGroup.isVisible = false
                            }

                            is MainUiState.Error -> {
                                binding.progressBar.isVisible = false
                                binding.contentGroup.isVisible = true
                                Toast.makeText(
                                    this@MainActivity,
                                    state.error.getMessage(this@MainActivity),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            MainUiState.None -> {
                                binding.progressBar.isVisible = false
                                binding.contentGroup.isVisible = true
                            }
                        }
                    }
                }

                launch {
                    viewModel.events.collect { navigationEvent ->
                        when (navigationEvent) {
                            is MainNavEvent.OpenDocument -> {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        DocumentViewerActivity::class.java
                                    ).apply {
                                        putExtra(
                                            DocumentViewerActivity.EXTRA_DOCUMENT,
                                            navigationEvent.doc
                                        )
                                        putExtra(
                                            DocumentViewerActivity.EXTRA_URI,
                                            navigationEvent.uri?.toString()
                                        )
                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
