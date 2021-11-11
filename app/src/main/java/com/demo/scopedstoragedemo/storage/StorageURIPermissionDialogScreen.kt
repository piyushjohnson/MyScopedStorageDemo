package com.demo.scopedstoragedemo.storage

import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.demo.scopedstoragedemo.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow


const val OPEN_DIRECTORY_REQUEST_CODE = 0xf11e
private const val OPEN_FILE_REQUEST_CODE = 0xf12e

const val GRANT_TREEURI_PERMISSION =
    "com.storagepath.StoragePermissionActivity.GRANT_TREEURI_PERMISSION"
const val FORCE_SHOW_URI_LISTING =
    "com.storagepath.StoragePermissionActivity.FORCE_SHOW_URI_LISTING"
const val FORCE_SHOW_USER_HELP_SLIDES =
    "com.storagepath.StoragePermissionActivity.FORCE_SHOW_USER_HELP_SLIDES"

/*
* Show granted URI listing only if one or more URI permissions are present
* Else
* 1. Show user help slides if no URI permissions are present
* 2. From show user help slides open default DoucmentProvider for user to grant permission for a directory URI
* 3. If user selects a valid directory, then pop show user help slides and show URI listing
* 4. If user doesn't select a directory, then show user help slides and also ask if he wants to opt-out of this
* 5.
* */

class StorageURIPermissionDialogScreen : AppCompatActivity() {
    private val grantedURIListingViewModel: GrantedURIListingViewModel by viewModels()
    private val directoryListingViewModel: DirectoryListingViewModel by viewModels()
    private lateinit var primaryExternalStorage: File

    private lateinit var mainActivityRoot: ConstraintLayout

    private val grantTreeUriPermission: Boolean by lazy {
        intent.getBooleanExtra(
            GRANT_TREEURI_PERMISSION,
            false
        )
    }
    private val forceShowUriListing: Boolean by lazy {
        intent.getBooleanExtra(
            FORCE_SHOW_URI_LISTING,
            false
        )
    }
    private val forceShowUserHelpSlides: Boolean by lazy {
        intent.getBooleanExtra(
            FORCE_SHOW_USER_HELP_SLIDES,
            false
        )
    }
    private val finishActivityWhenEmptyBackStack = {
        if (supportFragmentManager.backStackEntryCount == 0 && forceActivityFinishWhenEmptyBackStack) {
            finish()
        }
    }
    private var forceActivityFinishWhenEmptyBackStack = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.addOnBackStackChangedListener(finishActivityWhenEmptyBackStack)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setContentView(R.layout.dialog_storage_uri_permission)

            mainActivityRoot = findViewById(R.id.mainActivityRoot)

            if (!isExternalStorageReadable()) {
                Snackbar.make(
                    mainActivityRoot,
                    "External storage data directory is not readable",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
                setResult(RESULT_CANCELED)
                return
            }

            if (!isExternalStorageWritable()) {
                Snackbar.make(
                    mainActivityRoot,
                    "External storage data directory is not writable",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
                setResult(RESULT_CANCELED)
                return
            }

            supportFragmentManager.addOnBackStackChangedListener {
                val directoryOpen = supportFragmentManager.backStackEntryCount > 1
                supportActionBar?.let { actionBar ->
                    actionBar.setDisplayHomeAsUpEnabled(directoryOpen)
                    actionBar.setDisplayShowHomeEnabled(directoryOpen)
                }
            }

            if (grantTreeUriPermission) {
                getExternalStorageVolume(storageIsAvailableListener = {
                    grantDirectoryPermission(View(this))
                }, storageIsNotAvailableListener = {
                    lifecycleScope.launch {
                        delay(4000)
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                })
                return
            }

            if ((grantedURIListingViewModel.arePersistedUrisPermissionPresent() || forceShowUriListing)) {
                showUriContents()
                val directoryUri = intent.data
                if (directoryUri != null) {
                    showDirectoryContents(directoryUri)
                }
                return
            }
        } else {
            Snackbar.make(
                mainActivityRoot,
                "This kind of storage permission is not needed on API level 29(Q) or less",
                Snackbar.LENGTH_LONG
            ).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onBackPressed() {
        // TODO: Check if backstack is empty or not berfore proceeding
        supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name?.let {
            if (it.contains("GrantedURIAccessFragment")) {
                finish()
            }
        }
        supportFragmentManager.popBackStack()
    }

    /**
     * Finds a external storage volume i.e. either SD Card or Internal, currently chooses first one (Internal)
     * Shows usable space of that external storage if available
     * */
    private fun getExternalStorageVolume(
        storageIsAvailableListener: () -> Unit,
        storageIsNotAvailableListener: () -> Unit
    ) {
        val externalStorageVolumes: Array<File> = ContextCompat.getExternalFilesDirs(
            applicationContext,
            null
        )
        val message: String = if (externalStorageVolumes.isNotEmpty()) {
            primaryExternalStorage = externalStorageVolumes[0]
            val storageName = primaryExternalStorage.absolutePath.split(
                "/"
            )[2]
            val usableSpace = primaryExternalStorage.usableSpace
//        primaryExternalStorage.absolutePath.split("/").subList(1,4).joinToString(separator = "/")
            if (usableSpace != 0L) {
                storageIsAvailableListener()
                "External storage ($storageName) space available ${getFileSize(usableSpace)}"
            } else {
                storageIsNotAvailableListener()
                "No space available on external storage ($storageName)"
            }
        } else {
            storageIsNotAvailableListener()
            "No external storage found to use"
        }

        Snackbar.make(
            mainActivityRoot,
            message,
            Snackbar.LENGTH_LONG
        ).show()
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
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble()))
            .toString() + " " + units[digitGroups]
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
        if (!grantedURIListingViewModel.arePersistedUrisPermissionPresent() || forceShowUserHelpSlides) {
            showUserHelpSlides()
        } else {
            requestDirectoryURIAccess()
        }
    }

    fun requestDirectoryURIAccess(view: View = View(this)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val sm: StorageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val volumes = sm.storageVolumes
            val totalVolumes = volumes.size
            val volumeState = volumes[0].state
            if (totalVolumes < 0 || (volumeState == Environment.MEDIA_UNMOUNTED || volumeState == Environment.MEDIA_EJECTING || volumeState == Environment.MEDIA_UNKNOWN)) {
                Toast.makeText(this, "no volumes found", Toast.LENGTH_LONG).show()
                return
            }

            val intent = volumes[0].createOpenDocumentTreeIntent()

            intent.putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMLeads")
            )
            intent.putExtra(DocumentsContract.EXTRA_PROMPT, "ScopedStorageDemo")

            startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
        }
    }

    private fun isFragmentVisible(fragmentSimpleName: String): Fragment? {
        if (supportFragmentManager.backStackEntryCount != 0) {
            val fragment =
                supportFragmentManager.fragments[supportFragmentManager.backStackEntryCount - 1]
            return if (fragment::class.simpleName == fragmentSimpleName) {
                if (fragment.isVisible) fragment else null
            } else {
                null
            }
        } else return null
    }

    fun showDirectoryContents(directoryUri: Uri, newlyGranted: Boolean = false) {
        if (newlyGranted) {
            clearAllFragments()
        }
        supportFragmentManager.beginTransaction().apply {
            val directoryTag = directoryUri.hashCode()
            val directoryFragment = DirectoryFragment.newInstance(directoryUri)
            replace(R.id.fragment_container, directoryFragment, directoryTag.toString())
            addToBackStack("DirectoryFragment$directoryTag")
        }.commit()
        forceActivityFinishWhenEmptyBackStack = true
    }

    fun showUriContents(uriPermissions: List<UriPermission> = emptyList()) {
        supportFragmentManager.beginTransaction().apply {
            val uriTag = uriPermissions.hashCode()
            val directoryFragment = GrantedURIAccessFragment.newInstance()
            replace(R.id.fragment_container, directoryFragment, uriTag.toString())
            addToBackStack("GrantedURIAccessFragment")
        }.commit()
        forceActivityFinishWhenEmptyBackStack = true
    }

    private fun showUserHelpSlides() {
        supportFragmentManager.beginTransaction().apply {
            val directoryFragment = StorageUserHelpSlidesScreen.newInstance()
            replace(R.id.fragment_container, directoryFragment)
            addToBackStack("StorageUserHelpSlidesScreen")
        }.commit()
        forceActivityFinishWhenEmptyBackStack = true
    }

    fun clearAllFragments() {
        (1 until supportFragmentManager.backStackEntryCount).forEach { _ ->
            supportFragmentManager.popBackStack()
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        resultData: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE) {
            val storageUserHelpSlidesScreen = isFragmentVisible("StorageUserHelpSlidesScreen")
            when (resultCode) {
                RESULT_OK -> {
                    if (storageUserHelpSlidesScreen != null) {
                        supportFragmentManager.popBackStack("StorageUserHelpSlidesScreen", 0)
                    }

                    if (resultData == null) {
                        setResult(RESULT_CANCELED)
                        return
                    }

                    val directoryUri = resultData.data
                    if (directoryUri == null) {
                        setResult(RESULT_CANCELED)
                        return
                    }
                    //  Once taken, the permission grant will be remembered across device reboots.
                    grantedURIListingViewModel.takePersistedUriPermission(
                        directoryUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION + Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    if (grantTreeUriPermission) {
                        val resultIntent = Intent().apply {
                            data = directoryUri
                        }
                        setResult(RESULT_OK, resultIntent)
                        // TODO: Can remove this,extra functionality for default usage
                        // TODO: Make distinct variables flag for post and pre variables for this,such as preForceShowUriListing && postForceShowUriListing
                        if (forceShowUriListing) {
                            showUriContents()
                        } else {
                            finish()
                        }
                    } else {
                        val grantedURIAccessFragment = isFragmentVisible("GrantedURIAccessFragment")
                        if (grantedURIAccessFragment == null) {
                            showUriContents()
                        }
                        showDirectoryContents(directoryUri, newlyGranted = true)
                    }
                }

                RESULT_CANCELED -> {
                    if (grantTreeUriPermission) {
                        // TODO: Can remove this,extra functionality for default usage
                        // TODO: Make distinct variables flag for post and pre variables for this also
                        if (forceShowUriListing) {
                            forceActivityFinishWhenEmptyBackStack = false
                            showUriContents()
                        } else {
                            // TODO: Make distinct variables flag for post and pre variables for this also
                            if (forceShowUserHelpSlides) {
                                if (storageUserHelpSlidesScreen == null) {
                                    forceActivityFinishWhenEmptyBackStack = false
                                    showUserHelpSlides()
                                }
                            } else {
                                finish()
                            }
                        }
                    }
                    setResult(RESULT_CANCELED)
                }
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