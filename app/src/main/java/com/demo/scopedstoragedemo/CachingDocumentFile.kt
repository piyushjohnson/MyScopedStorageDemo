package com.storagepath

import android.app.Application
import android.content.ContentResolver
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

/**
 * Caching version of a [DocumentFile].
 *
 * A [DocumentFile] will perform a lookup (via the system [ContentResolver]), whenever a
 * property is referenced. This means that a request for [DocumentFile.getName] is a *lot*
 * slower than one would expect.
 *
 * To improve performance in the app, where we want to be able to sort a list of [DocumentFile]s
 * by name, we wrap it like this so the value is only looked up once.
 */
data class CachingDocumentFile(private val documentFile: DocumentFile) {
    val name: String? by lazy { documentFile.name }
    val type: String? by lazy { documentFile.type }

    val isDirectory: Boolean by lazy { documentFile.isDirectory }
    private val canWrite by lazy { documentFile.canWrite() }
    private val canRead by lazy { documentFile.canRead() }

    val uri get() = documentFile.uri

    fun renameFile(newName: String): CachingDocumentFile {
        documentFile.renameTo(newName)
        return CachingDocumentFile(documentFile)
    }

    fun createNewFile(fileName: String, mimeType: String): Boolean {
        if (isDirectory && canWrite) {
            return if (documentFile.findFile(fileName) == null) {
                documentFile.createFile(mimeType, fileName)
                true
            } else
                false
        }
        return false
    }

    fun createDirectory(fileName: String, mimeType: String = DocumentsContract.Document.MIME_TYPE_DIR): Boolean {
        if (isDirectory && canWrite) {
            documentFile.createDirectory(fileName)
            return true
        }
        return false
    }

    fun readTextFromFile(app: Application): String? {
        val inputStream = app.contentResolver.openInputStream(uri)
        return inputStream?.use { stream ->
            stream.bufferedReader().readText()
        }
    }

    fun writeTextToFile(app: Application, text: String) {
        val outputStream = app.contentResolver.openOutputStream(uri, "w")
        outputStream?.let { stream ->
            stream.bufferedWriter().use {
                it.write(text)
            }
        }
    }

    fun findFileInDirectory(fileName: String): CachingDocumentFile? {
        if (isDirectory && canRead) {
            val docFile = documentFile.findFile(fileName)
            return if (docFile != null)
                CachingDocumentFile(docFile)
            else null
        }
        return null
    }
}


fun Array<DocumentFile>.toCachingList(): List<CachingDocumentFile> {
    val list = mutableListOf<CachingDocumentFile>()
    for (document in this) {
        list += CachingDocumentFile(document)
    }
    return list
}