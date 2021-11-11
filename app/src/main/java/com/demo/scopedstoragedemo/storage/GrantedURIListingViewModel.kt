package com.demo.scopedstoragedemo.storage

import android.app.Application
import android.content.UriPermission
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class GrantedURIListingViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _persistedUriPermissions = MutableLiveData<List<UriPermission>>()
    val persistedUriPermissions = _persistedUriPermissions

    init {
        refreshPersistedUriPermissions()
    }

    private fun refreshPersistedUriPermissions() {
        _persistedUriPermissions.value = app.contentResolver.persistedUriPermissions
    }

    fun arePersistedUrisPermissionPresent(): Boolean {
        refreshPersistedUriPermissions()
        return _persistedUriPermissions.value?.isNotEmpty() ?: false
    }

    fun takePersistedUriPermission(uri: Uri, modeFlags: Int) {
        refreshPersistedUriPermissions()
        app.contentResolver.takePersistableUriPermission(uri, modeFlags)
        _persistedUriPermissions.value = app.contentResolver.persistedUriPermissions
    }

    fun releasePersistableUriPermission(uri: Uri, modeFlags: Int) {
        refreshPersistedUriPermissions()
        app.contentResolver.releasePersistableUriPermission(uri, modeFlags)
        _persistedUriPermissions.value = app.contentResolver.persistedUriPermissions
    }

    fun filterPersistedUris(directoryName: String): List<Uri> {
        refreshPersistedUriPermissions()
        return _persistedUriPermissions.value?.filter {
            it.uri.lastPathSegment?.substringAfter(":").equals(directoryName, ignoreCase = true)
        }?.map { it.uri } ?: emptyList()
    }

    fun hasReadPermissionForPath() {

    }

    fun hasWritePermissionForPath() {

    }
}