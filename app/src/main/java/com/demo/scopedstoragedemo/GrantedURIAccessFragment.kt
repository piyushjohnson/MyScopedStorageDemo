package com.demo.scopedstoragedemo

import android.content.Intent
import android.content.UriPermission
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


/**
 * Fragment that shows list of granted URI for app
 */
class GrantedURIAccessFragment : Fragment() {
    private lateinit var grantedUris: List<UriPermission>

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GrantedURIAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            grantedUris = it.getParcelableArrayList<UriPermission>(ARG_DIRECTORY_URI_LIST)?.toList()
                    ?: emptyList()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_granted_uri_access, container, false)
        recyclerView = view.findViewById(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)

        // Inflate the layout for this fragment
        return view;
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val parentActivity = activity
        if(parentActivity is MainActivity) {
            adapter = GrantedURIAdapter(object : GrantedURIAdapter.Companion.ClickListeners {

                override fun onURIClicked(clickedUriPermission: UriPermission) {
                    parentActivity.showDirectoryContents(clickedUriPermission.uri)
                }

                override fun onURILongClicked(clickedUriPermission: UriPermission) {
                    val modeFlag: Int = if(clickedUriPermission.isReadPermission && !clickedUriPermission.isWritePermission) {
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    } else if(clickedUriPermission.isWritePermission && !clickedUriPermission.isReadPermission) {
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    } else {
                        0
                    }
                    parentActivity.releasePersistedURIPermission(clickedUriPermission.uri,modeFlag)
                    Toast.makeText(parentActivity,"Release ${clickedUriPermission.uri}",Toast.LENGTH_LONG).show()
                    loadUris(parentActivity)
                }
            })
            recyclerView.adapter = adapter
            loadUris(parentActivity)
        }
//        viewModel.loadDirectory(directoryUri)
    }

    private fun loadUris(parentActivity: MainActivity) {
        parentActivity.getPersistedURIPermissions().let { persistedUriPermissions ->
            val uriEntries: List<UriPermission> = persistedUriPermissions;
            adapter.setEntries(uriEntries)
        }
    }

    companion object {
        /**
         * Convenience method for constructing a [GrantedURIAccessFragment] with the directory uri
         * to display.
         */
        @JvmStatic
        fun newInstance(uriPermissions: ArrayList<UriPermission>) =
                GrantedURIAccessFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList(ARG_DIRECTORY_URI_LIST, uriPermissions)
                    }
                }
    }
}

private const val ARG_DIRECTORY_URI_LIST = "com.example.android.directoryselection.ARG_DIRECTORY_URI"