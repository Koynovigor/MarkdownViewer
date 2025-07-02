package com.l3on1kl.mviewer.presentation.viewer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var binding: ActivityDocumentViewerBinding
    private var document: MarkdownDocument? = null
    private var pendingContent: String? = null
    private var lastState: DocumentViewerViewModel.UiState? = null

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
            val content = pendingContent ?: return@registerForActivityResult
            pendingContent = null
            if (uri != null) {
                lifecycleScope.launch {
                    val result = viewModel.saveDocument(content, uri)
                    if (result.isSuccess) {
                        document = document?.copy(content = content, path = uri.toString())
                        binding.tabLayout.getTabAt(0)?.select()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDocumentViewerBinding.bind(findViewById(R.id.viewer_root))
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
            document = doc
            viewModel.load(doc)
        } else {
            finish()
            return
        }

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_preview))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_edit))
        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                val index = tab.position
                if (index == 0) {
                    binding.previewLayout.isVisible = true
                    binding.editorLayout.isVisible = false
                    binding.pageControls.isVisible =
                        lastState is DocumentViewerViewModel.UiState.Pdf
                } else {
                    binding.previewLayout.isVisible = false
                    binding.pageControls.isVisible = false
                    binding.editorLayout.isVisible = true
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        binding.nextButton.setOnClickListener { viewModel.nextPage() }
        binding.prevButton.setOnClickListener { viewModel.prevPage() }

        binding.saveButton.setOnClickListener {
            val content = binding.editText.text.toString()
            val doc = document ?: return@setOnClickListener
            if (doc.path.startsWith("http://") || doc.path.startsWith("https://")) {
                pendingContent = content
                createDocumentLauncher.launch("${doc.id}.md")
            } else {
                lifecycleScope.launch {
                    val result = viewModel.saveDocument(content)
                    if (result.isSuccess) {
                        document = document?.copy(content = content)
                        binding.tabLayout.getTabAt(0)?.select()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    lastState = state
                    when (state) {
                        is DocumentViewerViewModel.UiState.Text -> {
                            binding.scrollView.isVisible = true
                            binding.imageView.isVisible = false
                            binding.pageControls.isVisible = false
                            binding.contentText.text = state.text
                            binding.editText.setText(state.text)
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