File Transfer Using Socket Communication

Project Overview

This project implements a file transfer system using direct socket communication between two devices. The goal is to transfer files without relying on external servers, demonstrating skills in Android development, concurrency handling, and maintaining UI responsiveness.

The application consists of two parts:
1.	Host (Server): Accepts incoming connections and receives files.
2.	Client: Sends selected files to the host.

Features
1.	Socket Communication:
•	Establishes direct communication between two devices using ServerSocket and Socket.
2.	File Transfer:
•	Supports files up to 50 MB in size.
•	Displays file transfer progress using a progress bar.
•	Handles connection drops and incomplete transfers gracefully.
3.	Concurrency:
•	Ensures UI responsiveness using coroutines and Dispatchers.IO.
4.	User Interface:
•	Simple and intuitive interface for starting the server, selecting files, and displaying transfer status.

Technology Stack
•	Programming Language: Kotlin
•	Concurrency: Coroutines
•	Socket Communication: Socket and ServerSocket from Java’s networking library.
•	UI Framework: Android XML layouts and components.

Getting Started

Prerequisites
•	Android Studio installed on your system.
•	Two Android devices connected to the same network (e.g., Wi-Fi).

Installation
1.	Clone the repository or download the project zip file.
2.	Open the project in Android Studio.
3.	Build the project and run it on two devices:
•	Device 1: Start as the Host (Server).
•	Device 2: Start as the Client and select a file to send.

Usage Instructions

Host (Server)
1.	Launch the application on the first device.
2.	Click “Start Server” to begin listening for client connections.
3.	Observe the status updates:
•	“Server started on port …”
•	“Waiting for client…”
4.	Upon a successful connection, the server receives the file and saves it in the device’s local storage.

Client
1.	Launch the application on the second device.
2.	Select a file using the file picker dialog.
3.	The file transfer begins automatically after selection.
4.	Observe the file transfer progress and success status.

File Transfer Workflow
1.	Server Side:
•	Starts a ServerSocket on the specified port.
•	Waits for a client connection using accept().
•	Reads the file in chunks and saves it locally.
•	Updates the progress bar and displays a success message upon completion.
2.	Client Side:
•	Automatically starts sending the selected file after it is chosen.
•	Reads the file and sends it in chunks.
•	Updates the progress bar and displays a success message upon completion.

Challenges and Solutions
1.	Thread-Safe UI Updates:
•	Used Dispatchers.Main with coroutines to handle UI updates from background threads.
2.	Connection Management:
•	Ensured proper resource management by closing sockets and streams after each operation.
3.	File Size Handling:
•	Processed files in chunks to handle large files efficiently.

Future Enhancements
1.	Retry Mechanism:
•	Automatically retry file transfers if the connection is dropped.
2.	Foreground Service:
•	Enable transfers to continue even when the app is in the background.
3.	Notification Integration:
•	Notify the user of transfer success or failure via system notifications.