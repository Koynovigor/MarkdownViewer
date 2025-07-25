package com.l3on1kl.mviewer.data.repository

import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.l3on1kl.mviewer.domain.model.HistoryEntry
import com.l3on1kl.mviewer.domain.repository.HistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val applicationContext: Context
) : HistoryRepository {

    private val historySharedPreferences by lazy {
        applicationContext.getSharedPreferences(
            "history",
            Context.MODE_PRIVATE
        )
    }
    private val flow = MutableStateFlow(load())

    override fun flow(): Flow<List<HistoryEntry>> = flow

    override suspend fun add(entry: HistoryEntry) {
        val list = load().filterNot {
            it.path == entry.path
        } + entry

        save(
            list.takeLast(50)
        )

        flow.value = list
    }

    override suspend fun remove(path: String) {
        val list = load().filterNot {
            it.path == path
        }

        save(list)
        flow.value = list
    }

    private fun load(): List<HistoryEntry> =
        historySharedPreferences.getString(
            "items",
            "[]"
        )
            ?.let {
                JSONArray(it)
            }
            ?.let { entriesJson ->
                (0 until entriesJson.length())
                    .mapNotNull { index ->
                        entriesJson.optJSONObject(index)?.run {
                            HistoryEntry(
                                getString("path"),
                                getString("name"),
                                getLong("openedAt")
                            )
                        }
                    }
            } ?: emptyList()

    private fun save(
        list: List<HistoryEntry>
    ) {
        val array = JSONArray().apply {
            list.forEach {
                put(
                    JSONObject().apply {
                        put("path", it.path)
                        put("name", it.name)
                        put("openedAt", it.openedAt)
                    }
                )
            }
        }
        historySharedPreferences.edit {
            putString(
                "items",
                array.toString()
            )
        }
    }

    private val launchPreferences by lazy {
        applicationContext.getSharedPreferences(
            "prefs",
            Context.MODE_PRIVATE
        )
    }

    private fun isFirstLaunch(): Boolean =
        launchPreferences.getBoolean(
            "is_first_launch",
            true
        )

    private fun markFirstLaunchComplete() {
        launchPreferences.edit {
            putBoolean(
                "is_first_launch",
                false
            )
        }
    }

    override suspend fun initDefaultHistoryEntryIfNeeded() {
        if (!isFirstLaunch()) return

        val fileName = "example.md"
        val file = File(
            applicationContext.filesDir,
            fileName
        )

        if (!file.exists()) {
            applicationContext.assets.open(fileName).use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val uri = FileProvider.getUriForFile(
            applicationContext,
            "${applicationContext.packageName}.provider",
            file
        )

        add(
            HistoryEntry(
                path = uri.toString(),
                name = "example.md",
                openedAt = System.currentTimeMillis()
            )
        )
        markFirstLaunchComplete()
    }
}
