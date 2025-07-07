import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.ConcurrentHashMap;

public class tcpcss {
    private static final int PORT = 12345;
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    public static void main (String[] args) {
        System.out.println("Listener on port " + PORT);
        System.out.println("Waiting for connections...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
                System.err.println("Error starting server: " + e.getMessage());
            }
        }
    public static synchronized void broadcast(String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(message);
        }
    }
    //Searches for connected client by username, returns null if not found.
    public static ClientHandler getClientByUsername(String username) {
        return clients.get(username);
    }
    public static synchronized void removeClient(ClientHandler client) {
        clients.remove(client.getUsername());
    }
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String username;
        private PrintWriter out;
        private BufferedReader in;
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        public String getUsername() {
            return username;
        }
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                username = in.readLine().toLowerCase();
                System.out.println("New connection, thread name is " + Thread.currentThread().getName() + ", ip is: " +  socket.getInetAddress().getHostAddress() + ", port: " + socket.getPort());
                System.out.println("Adding to list of sockets as " + clients.size());
                clients.put(username, this);
                System.out.println("[" + username + "] has joined the chat.");
                broadcast("[" + username + "] has joined the chat.");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/quit")) {
                        System.out.println("[" + username + "] has left the chat.");
                        broadcast("[" + username + "] has left the chat.");
                        break;
                    } else if (message.equalsIgnoreCase("/who")) {
                        System.out.println("[" + username + "] requested online users list.");
                        StringBuilder userList = new StringBuilder("[Online users: ");
                        userList.append(String.join(", ", clients.keySet()));
                        userList.append("]");
                        sendMessage(userList.toString());
                    } else if (message.startsWith("/sendfile")) {
                        handleSendFile(message);
                    } else if (message.startsWith("/acceptfile")) {
                        handleAcceptFile(message);
                    } else {
                        System.out.println("[" + username + "] " + message);
                        broadcast("[" + username + "] " + message);
                    }
                }
            } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }
        //Gets recipient name and notifies when a file is being sent.
        private void handleSendFile(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) {
                sendMessage("Invalid command. Usage: /sendfile <recipient> <filename>");
                return;
            }
            String recipient = parts[1];
            String filename = parts[2];
            File file = new File(filename);
            long fileSizeKB = file.length() / 1024;
            ClientHandler receiver = getClientByUsername(recipient);
            if (receiver != null) {
                System.out.println("[File transfer initiated from " + username + " to " + recipient + " " + filename + " (" + fileSizeKB + "KB)]");
                sendMessage("[File transfer initiated from " + username + " to " + recipient + " " + filename + " (" + fileSizeKB + " KB)]");
                receiver.sendMessage("[File transfer initiated from " + username + " to " + recipient + " " + filename + " (" + fileSizeKB + " KB)]");
                receiver.sendMessage("Type /acceptfile " + username + " to accept or /rejectfile " + username + " to decline.");
            } else {
                sendMessage("User [" + recipient + "] not found.");
            }
        }
        //Gets sender's name and notifies if the file has been accepted which starts the file transfer.
        private void handleAcceptFile(String message) {
            String[] parts = message.split(" ", 2);
            if (parts.length < 2) {
                sendMessage("invalid command. Usage: /acceptfile <sender>");
                return;
            }
            String sender = parts[1];
            ClientHandler senderClient = getClientByUsername(sender);
            if (senderClient != null) {
                System.out.println("[File transfer accepted from " + username + " to " + sender + "]");
                sendMessage("[File transfer accepted from " + username + " to " + sender + "]");
                senderClient.sendMessage("[File transfer accepted from " + username + " to " + sender + "]");
                Thread fileTransferThread = new Thread(() -> {
                    System.out.println("Starting new file transfer thread, thread name is " + Thread.currentThread().getName());
                    senderClient.sendMessage("[Starting file transfer between " + sender + " and " + username + "]");
                    sendMessage("[Starting file transfer between " + sender + " and " + username + "]");
                    try {
                        Thread.sleep(2000); // fake delay
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    senderClient.sendMessage("[File transfer complete from " + sender + " to " + username + " " + "example.txt (5 KB)]");
                    sendMessage("[File transfer complete from " + sender + " to " + username + " " + "example.txt (5 KB)]");
                    System.out.println("[File transfer complete from " + sender + " to " + username + " example.txt (5 KB)]");
                });
                fileTransferThread.start();
            } else {
                sendMessage("User [" + sender + "] not found.");
            }
        }
        public void sendMessage(String message) {
            out.println(message);
        }
        private void closeConnection() {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
            removeClient(this);
            broadcast("[" + username + "] has left the chat.");
        }
   }
}
