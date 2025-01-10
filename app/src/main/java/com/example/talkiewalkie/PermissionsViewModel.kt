package com.example.talkiewalkie

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel

class PermissionsViewModel: ViewModel() {

    val visiblePermissionDialogQueue = mutableStateListOf<String>()

    fun dismissPermissionDialog() {
        visiblePermissionDialogQueue.removeAt(0)
    }

    fun onPermissionResult(permission: String, isGranted: Boolean) {
        if (!isGranted && !visiblePermissionDialogQueue.contains(permission)) {
            visiblePermissionDialogQueue.add(permission)
        }
        setPermissionState(permission, isGranted)
    }

    private val _permissionStates = mutableStateMapOf<String, Boolean>()
    val permissionStates: Map<String, Boolean> get() = _permissionStates

    private fun setPermissionState(permission: String, isGranted: Boolean) {
        _permissionStates[permission] = isGranted
    }

    fun isPermissionGranted(permission: String): Boolean {
        return _permissionStates[permission] ?: false
    }
}