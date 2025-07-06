package com.l3on1kl.mviewer.presentation.viewer

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.l3on1kl.mviewer.R
import com.l3on1kl.mviewer.databinding.ActivityDocumentViewerBinding
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.presentation.ImageLoader
import com.l3on1kl.mviewer.presentation.model.DocumentArgs
import com.l3on1kl.mviewer.presentation.model.DocumentViewerUiState
import com.l3on1kl.mviewer.presentation.model.toDomain
import com.l3on1kl.mviewer.presentation.util.applySystemBarsPadding
import com.l3on1kl.mviewer.presentation.util.getFileName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.max

@AndroidEntryPoint
class DocumentViewerActivity :
    AppCompatActivity(R.layout.activity_document_viewer) {

    private val adapter by lazy { MarkdownAdapter() }
    private val viewModel: DocumentViewerViewModel by viewModels()
    private lateinit var binding: ActivityDocumentViewerBinding
    private var document: MarkdownDocument? = null
    private var pendingContent: String? = null

    private val createDocumentLauncher =
        registerForActivityResult(
            ActivityResultContracts
                .CreateDocument("text/markdown")
        ) { uri ->
            val content = pendingContent ?: return@registerForActivityResult
            pendingContent = null
            if (uri != null) {
                lifecycleScope.launch {
                    val result = viewModel.saveDocument(content, uri)
                    if (result.isSuccess) {
                        document = document?.copy(
                            content = content,
                            path = uri.toString()
                        )

                        intent.putExtra(
                            EXTRA_URI,
                            uri.toString()
                        )

                        binding.tabLayout.getTabAt(0)?.select()
                    } else {
                        Toast.makeText(
                            this@DocumentViewerActivity,
                            getString(R.string.save_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentViewerBinding
            .bind(findViewById(R.id.viewer_root))

        WindowCompat.setDecorFitsSystemWindows(window, false)


        ViewCompat.setOnApplyWindowInsetsListener(binding.editText) { view, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val bottom = max(ime, bars)
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottom)
            binding.saveButton.translationY = -bottom.toFloat()
            insets
        }

        binding.viewerRoot.applySystemBarsPadding()

        binding.rvMarkdown.adapter = adapter
        binding.rvMarkdown.layoutManager = LinearLayoutManager(this)

        binding.swipeRefresh.setOnRefreshListener {
            ImageLoader.clear()
            viewModel.refresh()
            binding.rvMarkdown.post {
                @SuppressLint("NotifyDataSetChanged")
                adapter.notifyDataSetChanged()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state is DocumentViewerUiState.Loading

                    when (state) {
                        is DocumentViewerUiState.Success -> {
                            adapter.submitList(state.items)
                            binding.editText.setText(state.content)
                        }

                        is DocumentViewerUiState.Error -> {
                            Toast.makeText(
                                this@DocumentViewerActivity,
                                state.error.getMessage(
                                    this@DocumentViewerActivity
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        DocumentViewerUiState.Loading -> Unit
                    }
                }
            }
        }

        ImageLoader.listener = object : ImageLoader.Listener {
            override fun onError(ex: Throwable) {
                Toast.makeText(
                    this@DocumentViewerActivity,
                    getString(R.string.no_internet),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val args: DocumentArgs? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    EXTRA_DOCUMENT,
                    DocumentArgs::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_DOCUMENT)
            }

        val doc = args?.toDomain() ?: run {
            finish()
            return
        }
        document = doc
        viewModel.tryInit(doc)

        val uri = intent.getStringExtra(EXTRA_URI)?.toUri()

        val fileName = if (uri != null && uri.scheme == "content") {
            getFileName(contentResolver, uri)
        } else {
            doc.path.substringAfterLast('/')
        }

        binding.toolbar.title = fileName

        binding.tabLayout.addTab(
            binding.tabLayout.newTab().setText(R.string.tab_preview)
        )
        binding.tabLayout.addTab(
            binding.tabLayout.newTab().setText(R.string.tab_edit)

        )
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) = when (tab?.position) {
                0 -> {
                    binding.swipeRefresh.isVisible = true
                    binding.editorLayout.isVisible = false
                }

                1 -> {
                    binding.swipeRefresh.isVisible = false
                    binding.editorLayout.isVisible = true
                    ViewCompat.requestApplyInsets(binding.editText)
                }

                else -> Unit
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.saveButton.setOnClickListener {
            val content = binding.editText.text.toString()
            val current = document ?: return@setOnClickListener

            if (current.path.startsWith("http://") ||
                current.path.startsWith("https://")
            ) {
                pendingContent = content
                createDocumentLauncher.launch("${current.id}.md")
            } else {
                lifecycleScope.launch {
                    val result = viewModel.saveDocument(content)
                    if (result.isSuccess) {
                        document = document?.copy(content = content)
                        binding.tabLayout.getTabAt(0)?.select()
                    } else {
                        Toast.makeText(
                            this@DocumentViewerActivity,
                            getString(R.string.save_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) ImageLoader.listener = null
    }

    companion object {
        const val EXTRA_DOCUMENT = "extra_document"
        const val EXTRA_URI = "extra_uri"
    }
}
