package com.demo.scopedstoragedemo

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Fragment that shows a list of documents in a directory.
 */
class DirectoryFragment : Fragment() {
    private lateinit var directoryUri: Uri

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DirectoryEntryAdapter

    private lateinit var viewModel: DirectoryFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        directoryUri = arguments?.getString(ARG_DIRECTORY_URI)?.toUri()
            ?: throw IllegalArgumentException("Must pass URI of directory to open")

        viewModel = ViewModelProviders.of(this)
            .get(DirectoryFragmentViewModel::class.java)

        val view = inflater.inflate(R.layout.fragment_directory, container, false)
        recyclerView = view.findViewById(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)

        adapter = DirectoryEntryAdapter(object : ClickListeners {
            override fun onDocumentClicked(clickedDocument: CachingDocumentFile) {
                viewModel.documentClicked(clickedDocument)
            }

            override fun onDocumentLongClicked(clickedDocument: CachingDocumentFile) {
                renameDocument(clickedDocument)
            }
        })

        recyclerView.adapter = adapter

        viewModel.documents.observe(viewLifecycleOwner, Observer { documents ->
            documents?.let { adapter.setEntries(documents) }
        })

        viewModel.openDirectory.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { directory ->
                (activity as? MainActivity)?.showDirectoryContents(directory.uri)
            }
        })

        viewModel.openDocument.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { document ->
                openDocument(document)
            }
        })

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        /*
        * DocumentFile.fromTreeUri(activity.applicationContext,directoryUri).findFile("internal").listFiles()
        * */
        viewModel.loadDirectory(directoryUri)
    }

    private fun openDocument(document: CachingDocumentFile) {
        try {
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                data = document.uri
            }
            startActivity(openIntent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                "No app found to open this file ${document.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("InflateParams")
    private fun renameDocument(document: CachingDocumentFile) {
        // Normally we don't want to pass `null` in as the parent, but the dialog doesn't exist,
        // so there isn't a parent layout to use yet.
        /*val dialogView = layoutInflater.inflate(R.layout.rename_layout, null)
        val editText = dialogView.findViewById<EditText>(R.id.file_name)
        editText.setText(document.name)

        // Use a lambda so that we have access to the [EditText] with the new name.
        val buttonCallback: (DialogInterface, Int) -> Unit = { _, buttonId ->
            when (buttonId) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val newName = editText.text.toString()
                    if (newName.isNotBlank()) {
                        document.rename(newName)

                        // The easiest way to refresh the UI is to load the directory again.
                        viewModel.loadDirectory(directoryUri)
                    }
                }
            }
        }

        val renameDialog = AlertDialog.Builder(requireActivity())
            .setTitle(R.string.rename_title)
            .setView(dialogView)
            .setPositiveButton(R.string.rename_okay, buttonCallback)
            .setNegativeButton(R.string.rename_cancel, buttonCallback)
            .create()

        // When the dialog is shown, select the name so it can be easily changed.
        renameDialog.setOnShowListener {
            editText.requestFocus()
            editText.selectAll()
        }

        renameDialog.show()*/
    }

    companion object {

        /**
         * Convenience method for constructing a [DirectoryFragment] with the directory uri
         * to display.
         */
        @JvmStatic
        fun newInstance(directoryUri: Uri) =
            DirectoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DIRECTORY_URI, directoryUri.toString())
                }
            }
    }
}

private const val ARG_DIRECTORY_URI = "com.example.android.directoryselection.ARG_DIRECTORY_URI"