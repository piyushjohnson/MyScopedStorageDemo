package com.demo.scopedstoragedemo

import android.app.Application
import android.content.UriPermission
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class GrantedURIAccessViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _persistedUriPermissions = MutableLiveData<List<UriPermission>>()
    val persistedUriPermissions = _persistedUriPermissions

    init {
        _persistedUriPermissions.value = app.contentResolver.persistedUriPermissions
    }

    fun arePersistedUrisPermissionPresent() = _persistedUriPermissions.value?.isNotEmpty() ?: false

    fun takePersistedUriPermission(uri: Uri, modeFlags: Int) {
        app.contentResolver.takePersistableUriPermission(uri, modeFlags)
        _persistedUriPermissions.value = app.contentResolver.persistedUriPermissions
    }

    fun releasePersistableUriPermission(uri: Uri, modeFlags: Int) {
        app.contentResolver.releasePersistableUriPermission(uri, modeFlags)
        _persistedUriPermissions.value = app.contentResolver.persistedUriPermissions
    }
}