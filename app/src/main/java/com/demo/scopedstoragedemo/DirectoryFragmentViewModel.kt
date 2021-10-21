package com.demo.scopedstoragedemo

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.storagepath.CachingDocumentFile
import com.storagepath.toCachingList
import java.util.concurrent.Executors

/**
 * ViewModel for the [DirectoryFragment].
 */
class DirectoryFragmentViewModel(application: Application) : AndroidViewModel(application) {
    private val _documents = MutableLiveData<List<CachingDocumentFile>>()
    val documents = _documents

    private val _openDirectory = MutableLiveData<Event<CachingDocumentFile>>()
    val openDirectory = _openDirectory

    private val _openDocument = MutableLiveData<Event<CachingDocumentFile>>()
    val openDocument = _openDocument

    private val executor = Executors.newSingleThreadExecutor()

    fun getDirectoryDocumentFile(directoryUri: Uri): CachingDocumentFile? {
        val documentFile = DocumentFile.fromTreeUri(getApplication(), directoryUri)
        if (documentFile != null) {
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
        executor.submit(Runnable {
            val sortedDocuments =
                    childDocuments.toMutableList().apply {
                        sortBy { it.name }
                    }
            _documents.postValue(sortedDocuments)
        })
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
            openDirectory.postValue(Event(clickedDocument))
        } else {
            openDocument.postValue(Event(clickedDocument))
        }
    }
}