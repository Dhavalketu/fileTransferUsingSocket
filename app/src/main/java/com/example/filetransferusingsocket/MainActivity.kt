package com.example.filetransferusingsocket

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.filetransferusingsocket.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// MainActivity.kt
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var server: SocketServer? = null
    private var client: SocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        runOnUiThread {
            // Stuff that updates the UI
            setupUI()
            checkPermissions()
        }
    }

    private fun setupUI() {
        binding.apply {
            // Server controls
            btnStartServer.setOnClickListener {
                server = SocketServer(
                    onProgressUpdate = { progress ->
                        progressBar.progress = progress
                    },
                    onStatusUpdate = { status ->
                        tvStatus.text = status
                    }
                )
                lifecycleScope.launch {
                    server?.startServer()
                }
            }

            // Client controls
            btnConnect.setOnClickListener {

                runOnUiThread {
                    val serverIp = etServerIp.text.toString()
                    client = SocketClient(
                        onProgressUpdate = { progress ->
                            progressBar.progress = progress
                        },
                        onStatusUpdate = { status ->
                            tvStatus.text = status
                        }
                    )
                    lifecycleScope.launch {
                        client?.connectToServer(serverIp, 8888)
                    }
                }

            }

            btnSelectFile.setOnClickListener {
                selectFile()
            }
        }
    }

    // Define permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle the permission results
        if (permissions.entries.all { it.value }) {
            // All permissions granted
            binding.tvStatus.text = "Permissions granted"
        } else {
            // Some permissions denied
            binding.tvStatus.text = "Required permissions not granted"
            // You might want to show a dialog explaining why permissions are needed
        }
    }

    private fun selectFile() {
        filePickerLauncher.launch("*/*")
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = mutableListOf<String>()

            // Check for storage permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13 and above
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                // For Android 12 and below
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            // Check if we need to request permissions
            val permissionsToRequest = permissions.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }


    private fun getMediaFilePathFromUri(uri: Uri): String? {
        // Handle MediaStore Video URI
        if (uri.authority == "com.android.providers.media.documents") {
            try {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                val contentUri: Uri = when (type) {
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> return null
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(contentUri, selection, selectionArgs)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return null
    }

    private fun getDataColumn(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )

            if (cursor?.moveToFirst() == true) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }

    // Update your file handling to use this new method
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                // First try to get the file path
                val filePath = getMediaFilePathFromUri(selectedUri)

                if (filePath != null) {
                    // Use the file path
                    lifecycleScope.launch {
                        client?.sendFile(filePath)
                    }
                } else {
                    // Fallback to streaming the content directly
                    lifecycleScope.launch {
                        contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                            // Get file name from URI
                            val fileName = getFileNameFromUri(selectedUri)
                            // Create temporary file
                            val tempFile = File(cacheDir, fileName)
                            FileOutputStream(tempFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            client?.sendFile(tempFile.absolutePath)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvStatus.text = "Error processing file: ${e.message}"
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "temp_file"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }

        if (fileName == "temp_file") {
            fileName = "temp_file_${System.currentTimeMillis()}.mp4"  // Added .mp4 extension for videos
        }

        return fileName
    }

    companion object {
        private const val REQUEST_FILE_PICK = 1
        private const val PERMISSION_REQUEST_CODE = 2
    }
}