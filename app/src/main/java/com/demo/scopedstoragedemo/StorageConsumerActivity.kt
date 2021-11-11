package com.demo.scopedstoragedemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import com.demo.scopedstoragedemo.storage.*
import com.google.android.material.snackbar.Snackbar

const val SELECT_A_FILE_URI = 100
const val REQUEST_TREEURI_GRANT = 200
class StorageConsumerActivity : AppCompatActivity() {
    private lateinit var readFromFileBtn: Button
    private lateinit var saveToFileBtn: Button
    private lateinit var fileContentsField: EditText
    private lateinit var fileNameField: EditText
    private lateinit var showGrantedDirectoriesBtn: Button
    private lateinit var rootLayout: ConstraintLayout
    private val grantedURIListingViewModel: GrantedURIListingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(StorageConsumerActivity::class.simpleName,"Test log")
        setContentView(R.layout.activity_storage_consumer)

        rootLayout = findViewById(R.id.rootLayout)
        readFromFileBtn = findViewById(R.id.readFromFileBtn)
        saveToFileBtn = findViewById(R.id.saveToFileBtn)
        fileNameField = findViewById(R.id.fileNameField)
        fileContentsField = findViewById(R.id.fileContentsField)
        showGrantedDirectoriesBtn = findViewById(R.id.showGrantedDirectoriesBtn)
    }


    fun readFromFile(view: View) {
        if (!grantedURIListingViewModel.arePersistedUrisPermissionPresent()) {
            startActivity(Intent(this, StorageURIPermissionDialogScreen::class.java).apply {
                putExtra(GRANT_TREEURI_PERMISSION, true)
            })
        }
        val fileName = fileNameField.text.toString()
        if (fileName.isBlank() || fileName.isEmpty()) {
            Snackbar.make(rootLayout, "Enter filename of file to read", Snackbar.LENGTH_LONG).show()
            return
        }
        /*val externalFile = File(getExternalFilesDir(null), fileName)
        val fileContents = externalFile.bufferedReader().readText()
        fileContentsField.setText(fileContents)*/
    }

    fun saveToFile(view: View) {
        if (!grantedURIListingViewModel.arePersistedUrisPermissionPresent()) {
            startActivity(Intent(this, StorageURIPermissionDialogScreen::class.java).apply {
                putExtra(GRANT_TREEURI_PERMISSION, true)
            })

            val fileContents = fileContentsField.text.toString()
            val fileName = fileNameField.text.toString()
            if (fileContents.isBlank() || fileContents.isEmpty()) {
                Snackbar.make(
                    rootLayout,
                    "Enter some text for file contents",
                    Snackbar.LENGTH_LONG
                ).show()
                return
            }

            if (fileName.isBlank() || fileName.isEmpty()) {
                Snackbar.make(rootLayout, "Enter filename for file to save", Snackbar.LENGTH_LONG)
                    .show()
                return
            }
            /*val externalFile = File(applicationContext.getExternalFilesDir(null), fileName)
        externalFile.printWriter().use { out ->
            out.println(fileContents)
        }*/

        }
    }

    fun showGrantedDirectories(view: View) {
        val selectedDirectoryUri = grantedURIListingViewModel.filterPersistedUris("myapp")
        /*startActivity(
            Intent(
                this,
                StorageURIPermissionDialogScreen::class.java
            ).apply {
                action = DocumentActions.SelectFileAndReturnUri.name
                putExtra(FORCE_SHOW_URI_LISTING, true)
                data = if(selectedDirectoryUri.isNotEmpty()) selectedDirectoryUri[0] else null
            }
        )*/
        startActivityForResult(Intent(
            this,
            StorageURIPermissionDialogScreen::class.java
        ).apply {
            putExtra(GRANT_TREEURI_PERMISSION, true)
            putExtra(FORCE_SHOW_URI_LISTING, true)
        },REQUEST_TREEURI_GRANT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_A_FILE_URI) {
            when (resultCode) {
                RESULT_OK -> {
                    Log.i("SELECT_A_FILE_URI",data?.data.toString())
                }
                RESULT_CANCELED -> {
                    Log.i("SELECT_A_FILE_URI","No file uri selected")
                }
            }
        }

        if(requestCode == REQUEST_TREEURI_GRANT) {
            when (resultCode) {
                RESULT_OK -> {
                    Log.i("REQUEST_TREEURI_GRANT",data?.data.toString())
                }
                RESULT_CANCELED -> {
                    Log.i("REQUEST_TREEURI_GRANT","No directory uri selected")
                }
            }
        }
    }

    fun selectedFileAndRead() {

    }
}