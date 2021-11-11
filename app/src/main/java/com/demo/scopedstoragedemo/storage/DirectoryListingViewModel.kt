package com.demo.scopedstoragedemo.storage

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executors


enum class DocumentActions {
    SelectFileAndReturnUri,
    OpenAndViewFile,
    OpenAndViewDirectory,
    NoAction
}

sealed class DocumentAction{
    data class SelectFileAndReturnUri(val documentFile: CachingDocumentFile = CachingDocumentFile(null)): DocumentAction()
    data class OpenAndViewFile(val documentFile: CachingDocumentFile = CachingDocumentFile(null)): DocumentAction()
    data class OpenAndViewDirectory(val documentFile: CachingDocumentFile = CachingDocumentFile(null)): DocumentAction()
    object NoAction : DocumentAction()
}

/**
 * ViewModel for the [DirectoryFragment].
 */
class DirectoryListingViewModel(application: Application) : AndroidViewModel(application) {
    private val _documents = MutableLiveData<List<CachingDocumentFile>>()
    val documents = _documents

    private val _openTree = MutableLiveData<Event<CachingDocumentFile>>()
    val openTree = _openTree

    private val _openDocument = MutableLiveData<Event<DocumentAction>>()
    val openDocument = _openDocument

    private lateinit var currentDocumentAction: DocumentAction

    private val executor = Executors.newSingleThreadExecutor()

    init {
        setDocumentEventAction(DocumentAction.NoAction)
    }

    fun getDirectoryDocumentFile(directoryUri: Uri): CachingDocumentFile? {
        val documentFile = DocumentFile.fromTreeUri(getApplication(), directoryUri)
        if(documentFile != null) {
            return CachingDocumentFile(documentFile)
        }
        return null
    }

    fun loadDirectory(directoryUri: Uri) {
        val documentsTree = DocumentFile.fromTreeUri(getApplication(), directoryUri) ?: return
        val childDocuments = documentsTree.listFiles().toCachingList()

        // It's much nicer when the documents are sorted by something, so we'll sort the documents
        // we got by name. Unfortunate there may be quite a few documents, and sorting can take
        // some time, so we'll take advantage of coroutines to take this work off the main thread.
        executor.submit {
            val sortedDocuments =
                childDocuments.toMutableList().apply {
                    sortBy { it.name }
                }
            _documents.postValue(sortedDocuments)
        }
    }

    fun setDocumentEventAction(action: DocumentAction) {
        currentDocumentAction = action
    }

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }

    /**
     * Method to dispatch between clicking on a document (which should be opened), and
     * a directory (which the user wants to navigate into).
     */
    fun documentClicked(clickedDocument: CachingDocumentFile) {
        if (clickedDocument.isDirectory) {
            openTree.postValue(Event(clickedDocument))
        } else {
            if(currentDocumentAction is DocumentAction.SelectFileAndReturnUri) {
                currentDocumentAction = (currentDocumentAction as DocumentAction.SelectFileAndReturnUri).copy(
                    documentFile = clickedDocument
                )
            } else if(currentDocumentAction is DocumentAction.OpenAndViewFile) {
                currentDocumentAction =
                    (currentDocumentAction as DocumentAction.OpenAndViewFile).copy(
                        documentFile = clickedDocument
                    )
            } else if(currentDocumentAction is DocumentAction.NoAction) {

            }

            openDocument.postValue(Event(currentDocumentAction))
        }
    }
}