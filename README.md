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

## File Transfer Details
File transfers are initiated via CLI commands.

When using /sendfile <recipient> <filename>, the specified file must be located in the same directory from which the client (tcpccs) is being run.

The file is transferred over a separate TCP connection to avoid blocking the main chat.

The recipient will be prompted in their terminal to /acceptfile or /rejectfile the incoming file.

```bash
java client.tcpccs 127.0.0.1 alice

/sendfile bob index.html
```
In the example above, index.html must be in the current working directory of the client terminal. If the file is missing or named incorrectly, the client will show a “File not found” error.
