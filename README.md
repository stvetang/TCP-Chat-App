# TCP Chat Client/Server with File Transfer

A multi-threaded TCP chat application written in Java that supports real-time messaging and file transfers between clients. This project demonstrates concepts in socket programming, client-server architecture, threading, and simple messaging protocols.

## Features
- Connect multiple clients to a central server
- Real-time text-based messaging between users
- File transfer support using dedicated TCP sockets
- Thread-per-client model for concurrent processing
- Basic command support (`/sendfile`, `/acceptfile`, `/rejectfile`, `/quit`)
- Informative terminal outputs for chat and file status

## Compile & Run Server
```bash
javac server/tcpcss.java
java server.tcpcss
```

## Compile & Run Client
```bash
javac client/tcpccs.java
java client.tcpccs <server_ip> <username>
```
Example:
java client.tcpccs 127.0.0.1 alice

## Commands
Inside the client, use:
```bash
/sendfile <recipient> <filename> – Request to send a file

/acceptfile <sender> – Accept a pending file

/rejectfile <sender> – Reject a pending file

/quit – Leave the chat
```
