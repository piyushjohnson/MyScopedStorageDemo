package com.demo.scopedstoragedemo.storage

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.lang.Exception

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
data class CachingDocumentFile(private val documentFile: DocumentFile?) {
    val name: String? by lazy { documentFile?.name }
    val type: String? by lazy { documentFile?.type }

    val isDirectory: Boolean by lazy { documentFile?.isDirectory ?: false }
    private val canWrite by lazy { documentFile?.canWrite() ?: false }
    private val canRead by lazy { documentFile?.canRead() ?: false }

    val uri get() = documentFile?.uri ?: Uri.EMPTY

    fun renameFile(newName: String): CachingDocumentFile {
        documentFile?.renameTo(newName)
        return CachingDocumentFile(documentFile)
    }

    fun createNewFile(fileName: String, mimeType: String): Either<Exception, CachingDocumentFile> {
        if (!isDirectory) {
            Either.Left(Exception("DocumentFile should be a directory to find files"))
        }

        if (!canWrite) {
            Either.Left(Exception("DocumentFile should be have write permissions"))
        }
        return if (documentFile?.findFile(fileName) == null) {
            val newlyCreatedFile = documentFile?.createFile(mimeType, fileName)
            if (newlyCreatedFile !== null)
                Either.Right(CachingDocumentFile(newlyCreatedFile))
            else
                Either.Left(Exception("Failed creating DocumentFile for $fileName"))
        } else
            Either.Left(Exception("Unable to create file with $fileName, as it is already taken"))
    }

    fun createDirectory(
        directoryName: String,
        mimeType: String = DocumentsContract.Document.MIME_TYPE_DIR
    ): Either<Exception,CachingDocumentFile> {
        if (!isDirectory) {
            Either.Left(Exception("DocumentFile should be a directory to find files"))
        }

        if (!canWrite) {
            Either.Left(Exception("DocumentFile should be have write permissions"))
        }


        val newlyCreatedDirectory = documentFile?.createDirectory(directoryName)
        return if (newlyCreatedDirectory !== null)
            Either.Right(CachingDocumentFile(newlyCreatedDirectory))
        else
            Either.Left(Exception(""))
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

    fun readBytesFromFile(app: Application): ByteArray? {
        val inputStream = app.contentResolver.openInputStream(uri)
        return inputStream?.use { stream ->
            stream.buffered().readBytes()
        }
    }

    fun writeBytesToFile(app: Application, byteArray: ByteArray) {
        val outputStream = app.contentResolver.openOutputStream(uri, "w")
        outputStream?.let { stream ->
            stream.buffered().use {
                it.write(byteArray)
            }
        }
    }

    fun findFileInDirectory(fileName: String): Either<CachingDocumentFile, Exception> {
        if (!isDirectory) {
            Either.Right(Exception("DocumentFile should be a directory to find files"))
        }

        if (!canRead) {
            Either.Right(Exception("DocumentFile should be have read permissions"))
        }
        val docFile = documentFile?.findFile(fileName)
        return if (docFile != null)
            Either.Left(CachingDocumentFile(docFile))
        else Either.Right(Exception("No matching document found for display name:${fileName}"))
    }
}


fun Array<DocumentFile>.toCachingList(): List<CachingDocumentFile> {
    val list = mutableListOf<CachingDocumentFile>()
    for (document in this) {
        list += CachingDocumentFile(document)
    }
    return list
}