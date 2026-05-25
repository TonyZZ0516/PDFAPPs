package com.swiftpdf.app.data.document

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.swiftpdf.app.domain.document.DocumentItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.documentHistoryDataStore by preferencesDataStore(
    name = "document_history",
)

interface DocumentHistoryDataSource {
    val recentDocuments: Flow<List<DocumentItem>>

    suspend fun saveRecentDocuments(documents: List<DocumentItem>)
}

class DocumentHistoryStore(
    private val appContext: Context,
) : DocumentHistoryDataSource {
    override val recentDocuments: Flow<List<DocumentItem>> = appContext.documentHistoryDataStore.data
        .catch {
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { preferences ->
            decodeDocuments(preferences[RecentDocumentsKey].orEmpty())
        }

    override suspend fun saveRecentDocuments(documents: List<DocumentItem>) {
        appContext.documentHistoryDataStore.edit { preferences ->
            preferences[RecentDocumentsKey] = encodeDocuments(documents.take(MaxRecentDocuments))
        }
    }

    private fun encodeDocuments(documents: List<DocumentItem>): String {
        val array = JSONArray()
        documents.forEach { document ->
            array.put(
                JSONObject()
                    .put("uri", document.uri.toString())
                    .put("displayName", document.displayName)
                    .put("sizeBytes", document.sizeBytes)
                    .put("importedAtMillis", document.importedAtMillis),
            )
        }
        return array.toString()
    }

    private fun decodeDocuments(rawValue: String): List<DocumentItem> {
        if (rawValue.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(rawValue)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        DocumentItem(
                            uri = Uri.parse(item.getString("uri")),
                            displayName = item.optString("displayName", "Untitled PDF"),
                            sizeBytes = item.optLongOrNull("sizeBytes"),
                            importedAtMillis = item.optLong("importedAtMillis", 0L),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val MaxRecentDocuments = 30
        val RecentDocumentsKey = stringPreferencesKey("recent_documents")
    }
}

private fun JSONObject.optLongOrNull(name: String): Long? {
    return if (has(name) && !isNull(name)) optLong(name) else null
}
