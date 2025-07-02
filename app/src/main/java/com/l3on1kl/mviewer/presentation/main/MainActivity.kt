package com.l3on1kl.mviewer.presentation.main

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.l3on1kl.mviewer.R
import com.l3on1kl.mviewer.databinding.ActivityMainBinding
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

        binding.openFileButton.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("text/*"))
        }

        binding.loadUrlButton.setOnClickListener {
            val url = binding.urlInput.text.toString()
            if (url.isNotBlank()) viewModel.onUrlEntered(url)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        MainViewModel.UiState.Idle -> {
                            // показать пустой экран или скрыть лоадер
                        }

                        MainViewModel.UiState.Loading -> {
                            // показать лоадер
                        }

                        is MainViewModel.UiState.Error -> {
                            // показать ошибку
                        }

                        is MainViewModel.UiState.Success -> {
                            // Переход на экран просмотра документа
                        }
                    }
                }
            }
        }

    }
}
