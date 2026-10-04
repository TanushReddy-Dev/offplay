package com.offlineplayer.provider.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafFolderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("saf_folders", Context.MODE_PRIVATE)
    
    private val _folders = MutableStateFlow<List<Uri>>(emptyList())
    val folders: StateFlow<List<Uri>> = _folders.asStateFlow()

    init {
        loadFolders()
    }

    private fun loadFolders() {
        val uris = prefs.getStringSet("folder_uris", emptySet()) ?: emptySet()
        val validUris = mutableListOf<Uri>()
        
        for (uriString in uris) {
            try {
                val uri = Uri.parse(uriString)
                // Check if we still have permission
                val hasPermission = context.contentResolver.persistedUriPermissions.any {
                    it.uri == uri && it.isReadPermission
                }
                if (hasPermission) {
                    validUris.add(uri)
                } else {
                    Log.w("SafFolderManager", "Lost permission for $uri")
                }
            } catch (e: Exception) {
                Log.e("SafFolderManager", "Invalid URI string: $uriString", e)
            }
        }
        
        // Save cleaned up list if it changed
        if (validUris.size != uris.size) {
            prefs.edit().putStringSet("folder_uris", validUris.map { it.toString() }.toSet()).apply()
        }
        
        _folders.value = validUris
    }

    fun addFolder(uri: Uri) {
        try {
            // Take persistable URI permission
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            val current = prefs.getStringSet("folder_uris", emptySet()) ?: emptySet()
            val updated = current + uri.toString()
            prefs.edit().putStringSet("folder_uris", updated).apply()
            
            loadFolders()
        } catch (e: Exception) {
            Log.e("SafFolderManager", "Failed to take persistable permission for $uri", e)
        }
    }

    fun removeFolder(uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            Log.e("SafFolderManager", "Failed to release permission for $uri", e)
        }
        
        val current = prefs.getStringSet("folder_uris", emptySet()) ?: emptySet()
        val updated = current - uri.toString()
        prefs.edit().putStringSet("folder_uris", updated).apply()
        
        loadFolders()
    }
}
