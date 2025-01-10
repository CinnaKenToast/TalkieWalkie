package com.example.talkiewalkie

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.talkiewalkie.ui.theme.TalkieWalkieTheme
import java.lang.reflect.Method

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val permissionsViewModel: PermissionsViewModel by viewModels()

    private val permissionsToRequest = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TalkieWalkieTheme {
                val dialogQueue = permissionsViewModel.visiblePermissionDialogQueue

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TopAppBar(
                            title = { Text("Talkie Walkie") },
                            // GUESS THIS ISNT EVEN NECESSARY
//                            actions = {
//                                Text("Refresh Devices")
//                                IconButton(
//                                    onClick = { /* TODO */ }
//                                ) {
//                                    Icon(
//                                        painter = painterResource(id = R.drawable.baseline_refresh_24),
//                                        contentDescription = null,
//                                        tint = Color.White
//                                    )
//                                }
//                            }
                        )
                        DeviceControls(permissionsViewModel, mainViewModel, 0)
                        DeviceControls(permissionsViewModel, mainViewModel, 1)
                        Button(
                            onClick = { /* TODO */ }
                        ) {
                            Text(
                                text = "Start",
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterVertically)
                            )
                        }
                    }
                }

                dialogQueue.reversed().forEach { permission ->
                    // TODO: NEED TO ADD A CHECK TO SEE IF COARSE LOCATION PERMISSIONS ARE ACCEPTED OR FINE
                    PermissionDialog(
                        permissionTextProvider = when (permission) {
                            Manifest.permission.BLUETOOTH_CONNECT -> BluetoothPermissionTextProvider()
                            Manifest.permission.RECORD_AUDIO -> RecordAudioPermissionTextProvider()
                            Manifest.permission.ACCESS_FINE_LOCATION -> FineLocationPermissionTextProvider()
                            else -> return@forEach
                        },
                        isPermanentlyDeclined = !shouldShowRequestPermissionRationale(permission),
                        onDismiss = permissionsViewModel::dismissPermissionDialog,
                        onOkClicked = {
                            permissionsViewModel.dismissPermissionDialog()
                        },
                        onGoToAppSettingsClicked = ::openAppSettings
                    )

                }

                val multiplePermissionsResultLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { perms ->
                        permissionsToRequest.forEach { permission ->
                            permissionsViewModel.onPermissionResult(
                                permission = permission,
                                isGranted = perms[permission] == true
                            )
                        }
                    }
                )
                LaunchedEffect(Unit) {
                    multiplePermissionsResultLauncher.launch(permissionsToRequest.toTypedArray())
                }

                checkForDevices(baseContext, mainViewModel)
            }
        }
    }
}

private fun checkForDevices(context: Context, mainViewModel: MainViewModel) {
    val btManager = context.getSystemService(ComponentActivity.BLUETOOTH_SERVICE) as BluetoothManager
    val pairedDevices: List<BluetoothDevice> = if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        btManager.adapter.bondedDevices.toList()
    } else { listOf() }

    pairedDevices.forEach { device ->
        val deviceName = device.name
        val macAddress = device.address
        mainViewModel.addHeadphone(device)
        Log.d(
            "pairedDevices",
            "paired device: $deviceName at $macAddress. isConnected: " + isConnected(device)
        )
    }

}

private fun isConnected(device: BluetoothDevice?): Boolean {
    if (device == null) { return true } // To account for "THIS DEVICE"
    return try {
        val m: Method = device.javaClass.getMethod("isConnected")
        m.invoke(device) as Boolean
    } catch (e: Exception) {
        throw IllegalStateException(e)
    }
}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectMenu(permissionsViewModel: PermissionsViewModel, mainViewModel: MainViewModel, index: Int) {
    val context = LocalContext.current
    if (ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) != PackageManager.PERMISSION_GRANTED) {
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val things = mainViewModel.headphones.map { it.name }.toMutableList()
    things.add("This Device")
    var selectedThing = if (index == 0) {
        mainViewModel.firstThing
    } else {
        mainViewModel.secondThing
    }
    if (permissionsViewModel.isPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded != !expanded },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TextField(
                    value = selectedThing,
                    onValueChange = {},
                    enabled = false,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .clickable {
                            expanded = !expanded
                        }
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    things.forEach { item ->
                        val btDevice = mainViewModel.headphones.firstOrNull { it.name == item }
                        DropdownMenuItem(
                            modifier = Modifier.fillMaxWidth(),
                            text = {
                                var text = item
                                if (!isConnected(btDevice)) {
                                    text = "$text (NOT CONNECTED)"
                                }
                                Text(
                                    text = text,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            onClick = {
                                if (isConnected(btDevice)) {
//                                    Log.d("DeviceSelectMenu", "SelectedThing $index: Was ${mainViewModel.selectedThings}")
                                    if (index == 0) {
                                        mainViewModel.updateFirstHeadphone(item)
                                    } else {
                                        mainViewModel.updateSecondHeadphone(item)
                                    }
//                                    Log.d("DeviceSelectMenu", "SelectedThing $index: Now ${mainViewModel.selectedThings}")
                                    expanded = !expanded
                                    Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "$item is disconnected. Please connect to the device to communicate with it.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun VolumeSlider() {
    var sliderPosition by remember { mutableIntStateOf(0) }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(painter = painterResource(id = R.drawable.baseline_volume_down_24), contentDescription = null)
            Slider(
                value = sliderPosition.toFloat(),
                onValueChange = { sliderPosition = it.toInt() },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f)
            )
            Icon(painter = painterResource(id = R.drawable.baseline_volume_up_24), contentDescription = null)
            CircularButton()
        }
        Text(text = sliderPosition.toString())
    }
}

@Preview
@Composable
fun CircularButton() {
    var enabled by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { enabled = !enabled },
            modifier = Modifier.background(Color.LightGray, shape = CircleShape)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_mic_off_24),
                contentDescription = null,
                tint = if (enabled) Color.Black else Color.Gray
            )
        }
//        Text(text = if (enabled) "Muted" else "Live")
    }
}

@Composable
fun DeviceControls(permissionsViewModel: PermissionsViewModel, mainViewModel: MainViewModel, index: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DeviceSelectMenu(permissionsViewModel, mainViewModel, index)
        VolumeSlider()
    }
}