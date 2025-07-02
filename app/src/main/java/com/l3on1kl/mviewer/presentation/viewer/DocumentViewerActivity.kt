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
import com.l3on1kl.mviewer.presentation.model.DocumentArgs
import com.l3on1kl.mviewer.presentation.model.DocumentViewerUiState
import com.l3on1kl.mviewer.presentation.model.toDomain
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DocumentViewerActivity : AppCompatActivity(R.layout.activity_document_viewer) {

    private val viewModel: DocumentViewerViewModel by viewModels()
    private lateinit var binding: ActivityDocumentViewerBinding
    private var document: MarkdownDocument? = null
    private var pendingContent: String? = null

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

        val args: DocumentArgs? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DOCUMENT, DocumentArgs::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DOCUMENT)
        }

        val doc = args?.toDomain()
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
                } else {
                    binding.previewLayout.isVisible = false
                    binding.editorLayout.isVisible = true
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

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
                    when (state) {
                        is DocumentViewerUiState.Text -> {
                            binding.scrollView.isVisible = true
                            binding.contentText.text = state.text
                            binding.editText.setText(state.text)
                        }

                        is DocumentViewerUiState.Error -> {
                            binding.scrollView.isVisible = true
                            binding.contentText.text = state.throwable.message
                        }

                        DocumentViewerUiState.Loading -> {}
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
