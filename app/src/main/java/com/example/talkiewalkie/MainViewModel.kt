package com.example.talkiewalkie

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    private val _headphones = mutableStateListOf<BluetoothDevice>()
    val headphones: List<BluetoothDevice> get() = _headphones.toList()

    fun addHeadphone(headphone: BluetoothDevice) {
        _headphones.add(headphone)
    }

    private var _selectedThings = mutableStateOf(Pair("This Device", "This Device"))
    val selectedThings: Pair<String, String> get() = _selectedThings.value
    val firstThing: String get() = _selectedThings.value.first
    val secondThing: String get() = _selectedThings.value.second

    fun updateFirstHeadphone(newHeadphone: String) {
        _selectedThings.value = _selectedThings.value.copy(first = newHeadphone)
    }

    fun updateSecondHeadphone(newHeadphone: String) {
        _selectedThings.value = _selectedThings.value.copy(second = newHeadphone)
    }
}