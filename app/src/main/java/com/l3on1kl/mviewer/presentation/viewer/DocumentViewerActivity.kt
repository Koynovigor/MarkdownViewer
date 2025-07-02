package com.l3on1kl.mviewer.presentation.viewer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.l3on1kl.mviewer.R
import com.l3on1kl.mviewer.databinding.ActivityDocumentViewerBinding
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DocumentViewerActivity : AppCompatActivity(R.layout.activity_document_viewer) {

    private val viewModel: DocumentViewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityDocumentViewerBinding.bind(findViewById(R.id.viewer_root))
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_open_with) {
                intent.getStringExtra(EXTRA_URI)?.let {
                    val openIntent = Intent(Intent.ACTION_VIEW, it.toUri())
                    startActivity(Intent.createChooser(openIntent, null))
                }
                true
            } else false
        }

        val doc: MarkdownDocument? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DOCUMENT, MarkdownDocument::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DOCUMENT)
        }

        if (doc != null) {
            viewModel.load(doc)
        } else {
            finish()
        }

        binding.nextButton.setOnClickListener { viewModel.nextPage() }
        binding.prevButton.setOnClickListener { viewModel.prevPage() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DocumentViewerViewModel.UiState.Text -> {
                            binding.scrollView.isVisible = true
                            binding.imageView.isVisible = false
                            binding.pageControls.isVisible = false
                            binding.contentText.text = state.text
                        }

                        is DocumentViewerViewModel.UiState.Pdf -> {
                            binding.scrollView.isVisible = false
                            binding.imageView.isVisible = true
                            binding.pageControls.isVisible = true
                            binding.imageView.setImageBitmap(state.bitmap)
                            binding.pageIndicator.text =
                                getString(R.string.page_x_of_y, state.page, state.pageCount)
                            binding.prevButton.isEnabled = state.page > 1
                            binding.nextButton.isEnabled = state.page < state.pageCount
                        }

                        is DocumentViewerViewModel.UiState.Error -> {
                            binding.scrollView.isVisible = true
                            binding.imageView.isVisible = false
                            binding.pageControls.isVisible = false
                            binding.contentText.text = state.throwable.message
                        }

                        DocumentViewerViewModel.UiState.Loading -> {}
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_DOCUMENT = "extra_document"
        const val EXTRA_URI = "extra_uri"
    }
}