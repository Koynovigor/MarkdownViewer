package com.l3on1kl.mviewer.presentation.main

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.l3on1kl.mviewer.R
import com.l3on1kl.mviewer.databinding.ActivityMainBinding
import com.l3on1kl.mviewer.presentation.history.HistoryAdapter
import com.l3on1kl.mviewer.presentation.model.MainUiState
import com.l3on1kl.mviewer.presentation.model.toUi
import com.l3on1kl.mviewer.presentation.util.applySystemBarsPadding
import com.l3on1kl.mviewer.presentation.util.getFileName
import com.l3on1kl.mviewer.presentation.viewer.DocumentViewerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val viewModel: MainViewModel by viewModels()

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { fileUri ->
                if (contentResolver.persistedUriPermissions.none { permissionEntry ->
                        permissionEntry.uri == fileUri
                    }) {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    try {
                        contentResolver.takePersistableUriPermission(
                            fileUri,
                            flags
                        )
                    } catch (securityException: SecurityException) {
                        Toast.makeText(
                            this,
                            securityException.localizedMessage
                                ?: getString(R.string.permission_denied),
                            Toast.LENGTH_SHORT
                        ).show()

                        return@let
                    }
                }

                val name = getFileName(contentResolver, fileUri)
                    ?: getString(R.string.unknown_file)

                viewModel.onLocalFileSelected(
                    fileUri,
                    name
                )
            }
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
            val clipboardText = (getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager)
                .primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.text?.toString()

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

        val historyListAdapter = HistoryAdapter(
            onClick = { item ->
                if (item.path.startsWith("http://")
                    || item.path.startsWith("https://")
                ) {
                    viewModel.onUrlEntered(item.path)
                } else {
                    val uri = item.path.toUri()
                    try {
                        contentResolver.openInputStream(uri)?.use {}

                        if (contentResolver.persistedUriPermissions.none {
                                it.uri == uri
                            }
                        ) {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }

                        val name = getFileName(
                            contentResolver,
                            uri
                        ) ?: getString(R.string.unknown_file)

                        viewModel.onLocalFileSelected(
                            uri,
                            name
                        )
                    } catch (_: Exception) {
                        Toast.makeText(
                            this,
                            R.string.file_deleted_or_moved,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDelete = { filePath ->
                viewModel.deleteHistory(
                    filePath.path
                )
            }
        )

        binding.historyList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyListAdapter
        }


        lifecycleScope.launch {
            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.history.collect { list ->
                    val uiList = list.reversed().mapIndexed { index, entry ->
                        entry.toUi(index)
                    }
                    historyListAdapter.submitList(uiList)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.uiState.value is MainUiState.Loading) {
            viewModel.resetUiState()
        }
    }
}
