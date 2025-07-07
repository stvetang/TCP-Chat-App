import java.io.*;
import java.net.*;
import java.util.Scanner;

public class tcpccs {
    private Socket socket;
    private String username;
    private PrintWriter out;
    private BufferedReader in;
    private String pendingFilename;
    private String pendingRecipient;
    private boolean fileTransferPrinted = false;
    public tcpccs(String serverAddress, int port, String username) {
        try {
            this.socket = new Socket(serverAddress, port);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
            System.out.println("Connected to the server. You can start sending messages.");
            out.println(username); 
            new Thread(new IncomingMessageHandler()).start();
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String message = scanner.nextLine();
                if (message.startsWith("/sendfile")) {
                    handleSendFileCommand(message);
                } else if (message.startsWith("/acceptfile")) {
                    handleAcceptFileCommand(message);
                } else if (message.startsWith("/rejectfile")) {
                    handleRejectFileCommand(message);
                } else {
                    out.println(message);
                    if (message.equalsIgnoreCase("/quit")) {
                        
                        break;
                    }
                }
            }
            closeConnection();
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
    private void handleSendFileCommand(String message) {
        String[] parts = message.split(" ", 3);
        if (parts.length < 3) {
            System.out.println("Usage: /sendfile <recipient> <filename>");
            return;
        }
        String recipient = parts[1];
        String filename = parts[2];
        File file = new File(filename);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + filename);
            return;
        }
        long fileSizeBytes = file.length();
        long sizeKB = fileSizeBytes / 1024;
        out.println(message);
        System.out.println("[File transfer initiated from " + username + " to " + recipient + " " + filename + " (" + sizeKB + " KB)]");
    }
    private void handleAcceptFileCommand(String message) {
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            System.out.println("Usage: /acceptfile <sender>");
            return;
        }
        String sender = parts[1];
        out.println(message);
    }
    private void handleRejectFileCommand(String message) {
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            System.out.println("Usage: /rejectfile <sender>");
            return;
        }
        String sender = parts[1];
        out.println("[File transfer rejected by " + username + "]");
    }
    private void closeConnection() {
        try {
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
            System.out.println("Disconnected from server.");
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    private class IncomingMessageHandler implements Runnable {
        public void run() {
            try {
                String message; 
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("[File transfer initiated from " + username)) {
                        continue;
                    }
                    if (message.startsWith("[File transfer")) {
                            System.out.println(message);
                    } else {
                        System.out.println(message);
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection closed.");
            }
        }
    }
    private void handleIncomingFileRequest(String message) {
        String[] parts = message.split(":", 4);
        if (parts.length < 4) return;
        String sender = parts[1];
        String filename = parts[2];
        long filesize = Long.parseLong(parts[3]);
        System.out.println("[" + sender + " wants to send you a file: " + filename + " (" + (filesize / 1024) + " KB)]");
        System.out.println("Type /acceptfile " + sender + " to receive it or /rejectfile " + sender + " to decline.");
    }
    private void handleFileAccepted(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length < 2) return;
        String receiver = parts[1];
        System.out.println("[File transfer accepted from " + receiver + "]");
    }
    private void handleFileRejected(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length < 2) return;
        String receiver = parts[1];
        System.out.println("[File transfer rejected by " + receiver + "]");
    }
    private void handleFilePort(String message) {
        String[] parts = message.split(":", 3);
        if (parts.length < 3) return;
        String sender = parts[1];
        int port = Integer.parseInt(parts[2]);
        if (sender.equals(username)) {
            System.out.println("[Starting file transfer to recipient on port " + port + "]");
            startFileSendThread(pendingRecipient, pendingFilename, port);
        } else {
            System.out.println("[Starting file receiver on port " + port + "]");
            startFileReceiveThread(sender, port);
        }
    }
    private void startFileSendThread(String recipient, String filename, int port) {
        new Thread(() -> {
            try (Socket fileSocket = new Socket(socket.getInetAddress(), port);
                 FileInputStream fis = new FileInputStream(filename);
                 OutputStream os = fileSocket.getOutputStream()) {  
                 byte[] buffer = new byte[4096];
                 int bytesRead;
                 while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();

                String completionMessage = "[File transfer complete from " + username + " to " + recipient + " " + filename + " (" + (new File(filename).length() / 1024) + " KB)]";
                out.println(completionMessage);
                System.out.println(completionMessage);
            } catch (IOException e) {
                System.err.println("Error sending file: " + e.getMessage());
            }
        }).start();
    }
    private void startFileReceiveThread(String sender, int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port);
                 Socket connection = serverSocket.accept();
                 InputStream is = connection.getInputStream()) {
                String filename = "received_from_" + sender + "_" + System.currentTimeMillis();
                File receivedFile = new File(filename);
                try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    System.out.println("[File received from " + sender + " and saved as " + filename + "]");
                }
            } catch (IOException e) {
                System.err.println("Error receiving file: " + e.getMessage());
            }
        }).start();
    }
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java tcpccs <server_address> <username>");
            return;
        }    
        String serverAddress = args[0];
        String username = args[1];
        new tcpccs(serverAddress, 12345, username);
    }
}


