package com.demo.scopedstoragedemo

import android.content.Intent
import android.content.UriPermission
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView


/**
 * Fragment that shows list of granted URI for app
 */
class GrantedURIAccessFragment : Fragment() {
    private val viewModel: GrantedURIAccessViewModel by activityViewModels()
    private lateinit var grantedUris: List<UriPermission>

    private lateinit var recyclerView: RecyclerView
    private lateinit var noDataTv: MaterialTextView
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
        noDataTv = view.findViewById(R.id.noDataPlaceholder)
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)

        // Inflate the layout for this fragment
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val parentActivity = activity
        if (parentActivity is MainActivity) {
            adapter = GrantedURIAdapter(object : GrantedURIAdapter.Companion.ClickListeners {

                override fun onURIClicked(clickedUriPermission: UriPermission) {
                    parentActivity.showDirectoryContents(clickedUriPermission.uri)
                }

                override fun onURILongClicked(clickedUriPermission: UriPermission) {
                    val modeFlag: Int =
                            if (clickedUriPermission.isReadPermission && !clickedUriPermission.isWritePermission) {
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            } else if (clickedUriPermission.isWritePermission && !clickedUriPermission.isReadPermission) {
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            } else {
                                0
                            }
                    viewModel.releasePersistableUriPermission(clickedUriPermission.uri, modeFlag)
                }
            })
            recyclerView.adapter = adapter
            observePersistedUris(parentActivity)
        }
    }

    private fun observePersistedUris(parentActivity: MainActivity) {
        viewModel.persistedUriPermissions.observe(parentActivity, Observer { uriEntries ->
            if (uriEntries.isNullOrEmpty()) {
                noDataTv.show()
                recyclerView.hide()
                return@Observer
            }
            noDataTv.hide()
            recyclerView.show()
            adapter.setEntries(uriEntries)
        })
    }

    companion object {

        @JvmStatic
        fun newInstance() =
                GrantedURIAccessFragment()
    }
}

private const val ARG_DIRECTORY_URI_LIST =
        "com.example.android.directoryselection.ARG_DIRECTORY_URI"