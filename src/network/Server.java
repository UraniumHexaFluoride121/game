package network;

import foundation.Main;
import foundation.MainPanel;
import foundation.input.InputEvent;
import foundation.input.InputType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class Server {
    public static final int PORT = 37001;
    public Vector<ClientHandler> clients = new Vector<>();

    public HashSet<Integer> getClientIDs() {
        HashSet<Integer> ids = new HashSet<>();
        clients.forEach(c -> ids.add(c.clientID));
        return ids;
    }

    public Server() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    ClientHandler client = new ClientHandler(serverSocket.accept()).start();
                    clients.add(client);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void closeClient(ClientHandler client) {
        client.closeClient();
        removeClient(client);
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        MainPanel.updatePlayers.set(true);
    }

    public static int clientIDCounter = 1;

    public static class ClientHandler {
        public final Socket socket;
        private final DataInputStream reader;
        private final DataOutputStream writer;
        private final HashSet<PacketWriter> packetQueue = new HashSet<>();

        public final int clientID = clientIDCounter++;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                reader = new DataInputStream(socket.getInputStream());
                writer = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            queuePacket(new PacketWriter(PacketType.CLIENT_ID, w -> {
                try {
                    w.writeInt(clientID);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        public synchronized void queuePacket(PacketWriter packet) {
            packetQueue.add(packet);
        }

        public ClientHandler start() {
            new Thread(this::runReader).start();
            new Thread(this::runWriter).start();
            return this;
        }

        public boolean closed = false;

        public void closeClient() {
            closed = true;
        }

        public void runReader() {
            while (!closed) {
                try {
                    switch (PacketType.values()[reader.readInt()]) {
                        case PLAYER_MOVEMENT -> {
                            Main.window.handleClientPlayerInput(
                                    PacketReceiver.readEnum(InputEvent.class, reader),
                                    InputType.read(reader),
                                    clientID
                            );
                        }
                    }
                } catch (EOFException | SocketException e) {
                    break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            MainPanel.server.closeClient(this);
        }

        public void runWriter() {
            try {
                while (!closed) {
                    TimeUnit.MILLISECONDS.sleep(5);
                    synchronized (this) {
                        packetQueue.forEach(p -> {
                            PacketWriter.writeEnum(p.type(), writer);
                            p.writer().accept(writer);
                        });
                        packetQueue.clear();
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
