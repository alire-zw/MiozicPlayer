package com.rero.miozicplayer.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

data class BluetoothPlaybackInfo(
    val isConnected: Boolean = false,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val albumArtUri: Uri? = null,
)

class BluetoothAvrcpManager(private val context: Context) {

    private val _playbackInfo = MutableStateFlow(BluetoothPlaybackInfo())
    val playbackInfo: StateFlow<BluetoothPlaybackInfo> = _playbackInfo.asStateFlow()

    private var avrcpController: Any? = null
    private var connectedDevice: BluetoothDevice? = null
    private var started = false

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == PROFILE_AVRCP_CONTROLLER) {
                avrcpController = proxy
                refreshConnectedDevice()
                refreshRemoteState()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == PROFILE_AVRCP_CONTROLLER) {
                avrcpController = null
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                ACTION_TRACK_EVENT -> handleTrackEvent(intent)
                ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED,
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                -> handleConnectionChange(intent)
            }
        }
    }

    fun start() {
        if (started) return
        started = true

        val filter = IntentFilter().apply {
            addAction(ACTION_TRACK_EVENT)
            addAction(ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }

        try {
            bluetoothAdapter?.getProfileProxy(context, profileListener, PROFILE_AVRCP_CONTROLLER)
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot connect AVRCP profile", e)
        }

        refreshConnectedDevice()
        refreshRemoteState()
    }

    fun stop() {
        if (!started) return
        started = false
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
        try {
            avrcpController?.let { controller ->
                bluetoothAdapter?.closeProfileProxy(PROFILE_AVRCP_CONTROLLER, controller as BluetoothProfile)
            }
        } catch (_: Exception) {
        }
        avrcpController = null
    }

    fun play() = sendPassThrough(PASSTHROUGH_PLAY)

    fun pause() = sendPassThrough(PASSTHROUGH_PAUSE)

    fun togglePlayPause() {
        if (_playbackInfo.value.isPlaying) pause() else play()
    }

    fun skipToNext() = sendPassThrough(PASSTHROUGH_FORWARD)

    fun skipToPrevious() = sendPassThrough(PASSTHROUGH_BACKWARD)

    fun seekForward15() = sendPassThrough(PASSTHROUGH_FF)

    fun seekBackward15() = sendPassThrough(PASSTHROUGH_REWIND)

    private fun handleTrackEvent(intent: Intent) {
        val metadata = intent.getParcelableExtra<MediaMetadata>(EXTRA_METADATA)
        val playbackState = intent.getParcelableExtra<PlaybackState>(EXTRA_PLAYBACK)
        updateFromRemote(metadata, playbackState)
    }

    private fun handleConnectionChange(intent: Intent) {
        when (intent.action) {
            ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                val device = getDeviceExtra(intent)
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    connectedDevice = device
                } else if (state == BluetoothProfile.STATE_DISCONNECTED &&
                    device?.address == connectedDevice?.address
                ) {
                    connectedDevice = null
                    clearRemotePlayback()
                }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                connectedDevice = getDeviceExtra(intent) ?: connectedDevice
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = getDeviceExtra(intent)
                if (device == null || device.address == connectedDevice?.address) {
                    connectedDevice = null
                    clearRemotePlayback()
                }
            }
        }
        refreshConnectedDevice()
        refreshRemoteState()
    }

    private fun refreshConnectedDevice() {
        val device = findActiveDevice()
        connectedDevice = device
        _playbackInfo.value = _playbackInfo.value.copy(
            isConnected = device != null,
            deviceName = device?.name,
            deviceAddress = device?.address,
        )
    }

    private fun refreshRemoteState() {
        val device = connectedDevice ?: findActiveDevice() ?: return
        val controller = avrcpController ?: return

        try {
            val getMetadata = controller.javaClass.getMethod("getMetadata", BluetoothDevice::class.java)
            val metadata = getMetadata.invoke(controller, device) as? MediaMetadata
            val getPlaybackState = controller.javaClass.getMethod("getPlaybackState", BluetoothDevice::class.java)
            val playbackState = getPlaybackState.invoke(controller, device) as? PlaybackState
            updateFromRemote(metadata, playbackState)
        } catch (e: Exception) {
            Log.d(TAG, "Could not refresh remote state: ${e.message}")
        }
    }

    private fun updateFromRemote(metadata: MediaMetadata?, playbackState: PlaybackState?) {
        val device = connectedDevice ?: findActiveDevice()
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState?.position ?: _playbackInfo.value.positionMs
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val artUri = metadata?.let { cacheAlbumArt(it) } ?: _playbackInfo.value.albumArtUri

        _playbackInfo.value = _playbackInfo.value.copy(
            isConnected = device != null,
            deviceName = device?.name ?: _playbackInfo.value.deviceName,
            deviceAddress = device?.address ?: _playbackInfo.value.deviceAddress,
            title = title.ifBlank { _playbackInfo.value.title },
            artist = artist.ifBlank { _playbackInfo.value.artist },
            album = album.ifBlank { _playbackInfo.value.album },
            isPlaying = playbackState?.let { isPlaying } ?: _playbackInfo.value.isPlaying,
            positionMs = if (playbackState != null) position.coerceAtLeast(0L) else _playbackInfo.value.positionMs,
            durationMs = if (duration > 0) duration else _playbackInfo.value.durationMs,
            albumArtUri = artUri,
        )
    }

    private fun clearRemotePlayback() {
        _playbackInfo.value = BluetoothPlaybackInfo(
            isConnected = false,
            deviceName = null,
            deviceAddress = null,
        )
    }

    private fun findActiveDevice(): BluetoothDevice? {
        connectedDevice?.let { return it }
        val controller = avrcpController
        if (controller != null) {
            try {
                val getConnectedDevices = controller.javaClass.getMethod("getConnectedDevices")
                @Suppress("UNCHECKED_CAST")
                val devices = getConnectedDevices.invoke(controller) as? List<BluetoothDevice>
                if (!devices.isNullOrEmpty()) return devices.first()
            } catch (_: Exception) {
            }
        }
        return try {
            bluetoothAdapter?.bondedDevices?.firstOrNull()
        } catch (_: SecurityException) {
            null
        }
    }

    private fun sendPassThrough(keyCode: Int) {
        val device = connectedDevice ?: findActiveDevice() ?: return
        val controller = avrcpController ?: return
        try {
            val method = controller.javaClass.getMethod(
                "sendPassThroughCmd",
                BluetoothDevice::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            method.invoke(controller, device, keyCode, KEY_STATE_PRESS)
            method.invoke(controller, device, keyCode, KEY_STATE_RELEASE)
        } catch (e: Exception) {
            Log.w(TAG, "Pass-through command failed: ${e.message}")
        }
    }

    private fun cacheAlbumArt(metadata: MediaMetadata): Uri? {
        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: return null
        return try {
            val file = File(context.cacheDir, "bluetooth_album_art.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
            }
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }

    private fun getDeviceExtra(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    companion object {
        private const val TAG = "BluetoothAvrcpManager"

        const val PROFILE_AVRCP_CONTROLLER = 18

        private const val ACTION_TRACK_EVENT =
            "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT"
        private const val EXTRA_METADATA =
            "android.bluetooth.avrcp-controller.profile.extra.METADATA"
        private const val EXTRA_PLAYBACK =
            "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK"
        private const val ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED =
            "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED"

        private const val PASSTHROUGH_PLAY = 0x44
        private const val PASSTHROUGH_PAUSE = 0x46
        private const val PASSTHROUGH_FORWARD = 0x4B
        private const val PASSTHROUGH_BACKWARD = 0x4C
        private const val PASSTHROUGH_FF = 0x49
        private const val PASSTHROUGH_REWIND = 0x48
        private const val KEY_STATE_PRESS = 0
        private const val KEY_STATE_RELEASE = 1
    }
}
