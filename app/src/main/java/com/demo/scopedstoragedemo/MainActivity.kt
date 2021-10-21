package com.demo.scopedstoragedemo

import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow


private const val OPEN_DIRECTORY_REQUEST_CODE = 0xf11e
private const val OPEN_FILE_REQUEST_CODE = 0xf12e

class MainActivity : AppCompatActivity() {
    private val viewModel: GrantedURIAccessViewModel by viewModels()
    private val directoryFragmentViewModel: DirectoryFragmentViewModel by viewModels()
    private lateinit var primaryExternalStorage: File
    private lateinit var readFromFileBtn: Button
    private lateinit var saveToFileBtn: Button
    private lateinit var fileContentsField: EditText
    private lateinit var fileNameField: EditText
    private lateinit var mainActivityRoot: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showUriContents()

        mainActivityRoot = findViewById(R.id.mainActivityRoot)
        readFromFileBtn = findViewById(R.id.readFromFileBtn)
        saveToFileBtn = findViewById(R.id.saveToFileBtn)
        fileNameField = findViewById(R.id.fileNameField)
        fileContentsField = findViewById(R.id.fileContentsField)

        if (!isExternalStorageReadable()) {
            Snackbar.make(
                    mainActivityRoot,
                    "External storage data directory is not readable",
                    Snackbar.LENGTH_LONG
            ).show()
        }

        if (!isExternalStorageWritable()) {
            Snackbar.make(
                    mainActivityRoot,
                    "External storage data directory is not writable",
                    Snackbar.LENGTH_LONG
            ).show()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val directoryOpen = supportFragmentManager.backStackEntryCount > 0
            supportActionBar?.let { actionBar ->
                actionBar.setDisplayHomeAsUpEnabled(directoryOpen)
                actionBar.setDisplayShowHomeEnabled(directoryOpen)
            }
        }

        getExternalStorageVolume()
        if (viewModel.arePersistedUrisPermissionPresent()) {
            showUriContents()
        }
    }

    override fun onBackPressed() {
        supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name?.let {
            if (it.contains("GrantedURIAccessFragment")) {
                finish()
            }
        }
        supportFragmentManager.popBackStack()
    }

    fun readFromFile(view: View) {
        val fileName = fileNameField.text.toString()
        if (fileName.isBlank() || fileName.isEmpty()) {
            Snackbar.make(mainActivityRoot, "Enter filename of file to read", Snackbar.LENGTH_LONG).show()
            return
        }
        val externalFile = File(getExternalFilesDir(null), fileName)
        val fileContents = externalFile.bufferedReader().readText()
        fileContentsField.setText(fileContents)
    }

    fun saveToFile(view: View) {
        val fileContents = fileContentsField.text.toString()
        val fileName = fileNameField.text.toString()
        if (fileContents.isBlank() || fileContents.isEmpty()) {
            Snackbar.make(
                    mainActivityRoot,
                    "Enter some text for file contents",
                    Snackbar.LENGTH_LONG
            ).show()
            return
        }

        if (fileName.isBlank() || fileName.isEmpty()) {
            Snackbar.make(mainActivityRoot, "Enter filename for file to save", Snackbar.LENGTH_LONG).show()
            return
        }
        val externalFile = File(applicationContext.getExternalFilesDir(null), fileName)
        externalFile.printWriter().use { out ->
            out.println(fileContents)
        }
    }


    // Find and selects a external storage volume i.e. either SD Card or Internal
    // shows its usable space
    private fun getExternalStorageVolume() {
        val externalStorageVolumes: Array<File> = ContextCompat.getExternalFilesDirs(
                applicationContext,
                null
        )
        primaryExternalStorage = externalStorageVolumes[0]
        val usableSpace = primaryExternalStorage.usableSpace
        if (usableSpace != 0L) {
            Snackbar.make(
                    mainActivityRoot, "External storage (${
                primaryExternalStorage.absolutePath.split(
                        "/"
                )[2]
            }) space available ${getFileSize(usableSpace)}", Snackbar.LENGTH_LONG
            ).show()
        }
    }

    // Checks if a volume containing external storage is available
    // for read and write.
    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
    }

    // Checks if a volume containing external storage is available to at least read.
    private fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)
    }

    private fun getFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())).toString() + " " + units[digitGroups]
    }

    fun grantFilePermission(view: View) {
        // Choose a file using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            // can select any MIME type file
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            // adding Read URI permission
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when it loads.
        /*val uriToLoad: Uri? = null
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad)*/
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE)
    }


    fun grantDirectoryPermission(view: View) {
        val sm: StorageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val volumes = sm.storageVolumes
            val totalVolumes = volumes.size
            val volumeState = volumes[0].state
            if (totalVolumes < 0 || (volumeState == Environment.MEDIA_UNMOUNTED || volumeState == Environment.MEDIA_EJECTING || volumeState == Environment.MEDIA_UNKNOWN)) {
                Toast.makeText(this, "no volumes found", Toast.LENGTH_LONG).show()
                return
            }

            val intent = volumes[0].createOpenDocumentTreeIntent()

            startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
        }
    }

    fun showDirectoryContents(directoryUri: Uri, newlyGranted: Boolean = false) {
        if (newlyGranted) {
            (1 until supportFragmentManager.backStackEntryCount).forEach { _ ->
                supportFragmentManager.popBackStack()
            }
        }
        supportFragmentManager.beginTransaction().apply {
            val directoryTag = directoryUri.hashCode()
            val directoryFragment = DirectoryFragment.newInstance(directoryUri)
            replace(R.id.fragment_container, directoryFragment, directoryTag.toString())
            addToBackStack("DirectoryFragment$directoryTag")
        }.commit()
    }

    private fun showUriContents(uriPermissions: List<UriPermission> = emptyList()) {
        supportFragmentManager.beginTransaction().apply {
            val uriTag = uriPermissions.hashCode()
            val directoryFragment = GrantedURIAccessFragment.newInstance()
            replace(R.id.fragment_container, directoryFragment, uriTag.toString())
            addToBackStack("GrantedURIAccessFragment$uriTag")
        }.commit()
    }

    override fun onActivityResult(
            requestCode: Int, resultCode: Int,
            resultData: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == RESULT_OK) {
            // The result data contains a URI for the directory that the user selected.
            if (resultData != null) {
                val directoryUri = resultData.data!!
                //  Once taken, the permission grant will be remembered across device reboots.
                viewModel.takePersistedUriPermission(directoryUri, Intent.FLAG_GRANT_READ_URI_PERMISSION + Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                showDirectoryContents(directoryUri, newlyGranted = true)
            }
        }
        if (requestCode == OPEN_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            // The result data contains a URI for the document that the user selected.
            if (resultData != null) {
                val fileUri = resultData.data!!

                Log.d("File URI", fileUri.toString())
            }
        }
    }
}