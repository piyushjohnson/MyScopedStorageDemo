package com.demo.scopedstoragedemo.storage

import android.content.UriPermission
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.demo.scopedstoragedemo.R

class GrantedURIListingAdapter(private val clickListeners: ClickListeners) :
        RecyclerView.Adapter<GrantedURIListingAdapter.ViewHolder>() {

    private val uriEntries = mutableListOf<UriPermission>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.granted_uri_listing_item, parent, false)
        return ViewHolder(view)
    }

    private fun CheckedTextView.changeCheckState(checked: Boolean) {
        isChecked = checked
        setCheckMarkDrawable(
                if (isChecked)
                    R.drawable.ic_check_black_24
                else
                    R.drawable.ic_check_gray_24
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        with(viewHolder) {
            val item = uriEntries[position]
            fileName.text = item.uri.lastPathSegment?.substringAfter(":")
            read.changeCheckState(item.isReadPermission)
            write.changeCheckState(item.isWritePermission)

            val itemDrawableRes = if (true) {
                R.drawable.ic_folder_black_24
            } else {
                R.drawable.ic_file_black_24
            }
            entryImg.setImageResource(itemDrawableRes)



            this.itemView.setOnClickListener {
                clickListeners.onURIClicked(item)
            }
            this.itemView.setOnLongClickListener {
                clickListeners.onURILongClicked(item)
                true
            }
        }
    }

    override fun getItemCount() = uriEntries.size

    fun setEntries(newList: List<UriPermission>) {
        synchronized(uriEntries) {
            uriEntries.clear()
            uriEntries.addAll(newList)
            notifyDataSetChanged()
        }
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.uri)
        val entryImg: ImageView = view.findViewById(R.id.entry_image)
        val read: CheckedTextView = view.findViewById(R.id.read)
        val write: CheckedTextView = view.findViewById(R.id.write)
    }

    companion object {
        interface ClickListeners {
            fun onURIClicked(clickedUriPermission: UriPermission)
            fun onURILongClicked(clickedUriPermission: UriPermission)
        }
    }
}