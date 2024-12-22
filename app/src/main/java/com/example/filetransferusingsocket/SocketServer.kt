package com.example.filetransferusingsocket

// SocketServer.kt
import android.os.Environment
import android.view.View
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket


class SocketServer(
    private val port: Int = 8888,
    private val onProgressUpdate: (Int) -> Unit,
    private val onStatusUpdate: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var isRunning = false



    fun startServer() = CoroutineScope(Dispatchers.IO).launch {
        try {
            serverSocket = ServerSocket(port)
            withContext(Dispatchers.Main) {
                onStatusUpdate("Server started on port $port")
            }
            isRunning = true

            while (isRunning) {
                withContext(Dispatchers.Main) {
                    onStatusUpdate("Waiting for client...")
                }

                clientSocket = serverSocket?.accept()
                withContext(Dispatchers.Main) {
                    onStatusUpdate("Client connected: ${clientSocket?.inetAddress}")
                }

                handleClient(clientSocket!!)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onStatusUpdate("Server error: ${e.message}")
            }
        } finally {
            serverSocket?.close()
            withContext(Dispatchers.Main) {
                onStatusUpdate("Server stopped")
            }
        }
    }

    private suspend fun handleClient(client: Socket) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = client.getInputStream()
                val dataInputStream = DataInputStream(inputStream)

                // Read file metadata
                val fileName = dataInputStream.readUTF()
                val fileSize = dataInputStream.readLong()

                // Create file
                val file = File(getOutputDirectory(), fileName)
                val fileOutputStream = FileOutputStream(file)
                val buffer = ByteArray(8192)
                var bytesReceived: Long = 0

                // Receive file data
                while (bytesReceived < fileSize) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    fileOutputStream.write(buffer, 0, bytesRead)
                    bytesReceived += bytesRead

                    val progress = ((bytesReceived.toFloat() / fileSize) * 100).toInt()
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(progress)
                    }
                }

                fileOutputStream.close()
                withContext(Dispatchers.Main) {
                    onStatusUpdate("File received successfully")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onStatusUpdate("Error receiving file: ${e.message}")
                }
            }
        }
    }

    fun stopServer() {
        isRunning = false
        clientSocket?.close()
        serverSocket?.close()
    }

    private fun getOutputDirectory(): File {
        // Implement based on your requirements
        return File(Environment.getExternalStorageDirectory(), "FileTransfer")
    }
}

// SocketClient.kt
class SocketClient(
    private val onProgressUpdate: (Int) -> Unit,
    private val onStatusUpdate: (String) -> Unit
) {
    private var socket: Socket? = null

    fun connectToServer(serverIp: String, port: Int) = CoroutineScope(Dispatchers.IO).launch {
        try {
            socket = Socket(serverIp, port)
            withContext(Dispatchers.Main) {
                onStatusUpdate("Connected to server")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onStatusUpdate("Connection error: ${e.message}")
            }
        }
    }

    fun sendFile(filePath: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                withContext(Dispatchers.Main) {
                    onStatusUpdate("File not found")
                }
                return@launch
            }

            val outputStream = socket?.getOutputStream()
            val dataOutputStream = DataOutputStream(outputStream)

            // Send file metadata
            dataOutputStream.writeUTF(file.name)
            dataOutputStream.writeLong(file.length())

            // Send file data
            val fileInputStream = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesSent: Long = 0

            while (true) {
                val bytesRead = fileInputStream.read(buffer)
                if (bytesRead == -1) break

                outputStream?.write(buffer, 0, bytesRead)
                bytesSent += bytesRead

                val progress = ((bytesSent.toFloat() / file.length()) * 100).toInt()
                withContext(Dispatchers.Main) {
                    onProgressUpdate(progress)
                }
            }

            fileInputStream.close()
            withContext(Dispatchers.Main) {
                onStatusUpdate("File sent successfully")
            }
            outputStream?.close()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onStatusUpdate("Error sending file: ${e.message}")
            }
        }
    }

    fun disconnect() {
        socket?.close()
    }
}