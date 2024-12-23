package network;

import foundation.MainPanel;
import foundation.input.InputEvent;
import foundation.input.InputType;
import level.Level;
import level.objects.PhysicsBlock;
import level.objects.Player;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    public static final int TCP_PORT = 37001, UDP_SERVER_PORT = 37002, UDP_CLIENT_PORT = 37003;
    public ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    public ConcurrentHashMap<InetAddress, ClientHandler> clientsByAddress = new ConcurrentHashMap<>();
    public static DatagramSocket udpSocket;

    public HashSet<Integer> getClientIDs() {
        HashSet<Integer> ids = new HashSet<>();
        clients.forEach((id, c) -> ids.add(id));
        return ids;
    }

    public Server() {
        try {
            udpSocket = new DatagramSocket(UDP_SERVER_PORT);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        new Thread(() -> {
            byte[] receiveBuffer = new byte[4096];
            while (true) {
                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                try {
                    udpSocket.receive(packet);
                    ClientHandler client = clientsByAddress.get(packet.getAddress());
                    if (client != null) {
                        ByteArrayInputStream bytes = new ByteArrayInputStream(packet.getData());
                        client.readStream(new DataInputStream(bytes));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
                while (true) {
                    int id = clientIDCounter++;
                    ClientHandler client = new ClientHandler(serverSocket.accept(), id).start();
                    clients.put(id, client);
                    clientsByAddress.put(client.socket.getInetAddress(), client);
                    MainPanel.updatePlayers.set(true);
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
        clients.remove(client.clientID);
        clientsByAddress.remove(client.socket.getInetAddress());
        MainPanel.updatePlayers.set(true);
    }

    public void sendLevelPacket(HashMap<Integer, Level> levels, HashMap<Integer, Boolean> finalised) {
        clients.forEach((id, c) -> c.queuePacket(new PacketWriter(PacketType.LEVEL_UPDATE, false, w -> {
            try {
                w.writeLong(System.currentTimeMillis());
                w.writeInt(levels.size());
                levels.forEach((i, l) -> {
                    try {
                        w.writeInt(l.levelIndex);
                        w.writeLong(l.seed);
                        w.writeBoolean(finalised.get(i));
                        w.writeLong(l.uiProgressTracker == null ? 0 : l.uiProgressTracker.startTime);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                w.writeInt(c.levelIndex);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        })));
    }

    public void sendPhysicsUpdate(HashMap<Integer, Level> loadedLevels) {
        clients.forEach((id, c) -> {
            Level l = loadedLevels.get(c.levelIndex);
            if (l == null)
                return;
            c.queuePacket(new PacketWriter(PacketType.PHYSICS_UPDATE, true, w -> {
                AtomicInteger count = new AtomicInteger();
                l.dynamicBlocks.forEach(d -> {
                    if (d instanceof PhysicsBlock)
                        count.getAndIncrement();
                });
                try {
                    w.writeLong(System.currentTimeMillis());
                    w.writeInt(l.levelIndex);
                    w.writeInt(count.get());
                    Player player = l.players.get(id);
                    if (player == null)
                        w.writeInt(-1);
                    else {
                        w.writeInt(player.index);
                        w.writeBoolean(player.space);
                        w.writeBoolean(player.left);
                        w.writeBoolean(player.right);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                l.dynamicBlocks.forEach(d -> {
                    if (d instanceof PhysicsBlock b) {
                        try {
                            w.writeInt(b.index);
                            w.writeUTF(b.name);
                            b.pos.write(w);
                            b.velocity.write(w);
                            b.prevPos.write(w);
                            b.previousVelocity.write(w);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }));
        });
    }

    public static int clientIDCounter = 1;

    public static class ClientHandler {
        public final Socket socket;
        private final DataInputStream reader;
        private final DataOutputStream writer;
        private final HashSet<PacketWriter> packetQueue = new HashSet<>();
        public final InetAddress inetAddress;

        public int levelIndex = -1;

        public final int clientID;

        public ClientHandler(Socket socket, int clientID) {
            inetAddress = socket.getInetAddress();
            for (InputEvent event : InputEvent.values()) {
                lastPlayerMovementUpdate.put(event, 0L);
            }
            this.socket = socket;
            this.clientID = clientID;
            try {
                reader = new DataInputStream(socket.getInputStream());
                writer = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            queuePacket(new PacketWriter(PacketType.CLIENT_ID, false, w -> {
                try {
                    w.writeInt(clientID);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        public synchronized void setLevel(Level l) {
            levelIndex = l.levelIndex;
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

        private final HashMap<InputEvent, Long> lastPlayerMovementUpdate = new HashMap<>();

        public void readStream(DataInputStream stream) throws IOException {
            switch (PacketType.values()[stream.readInt()]) {
                case PLAYER_MOVEMENT -> {
                    long timeStamp = stream.readLong();
                    InputEvent event = PacketReceiver.readEnum(InputEvent.class, stream);
                    InputType inputType = InputType.read(stream);
                    if (lastPlayerMovementUpdate.get(event) > timeStamp)
                        break;
                    lastPlayerMovementUpdate.put(event, timeStamp);
                    MainPanel.addTask(() -> MainPanel.handleClientPlayerInput(
                            event,
                            inputType,
                            clientID, levelIndex
                    ));
                    MainPanel.sendLevelPacketTimer = 1;
                }
            }
        }

        public void runReader() {
            while (!closed) {
                try {
                    readStream(reader);
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
                            if (p.udp()) {
                                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                DataOutputStream out = new DataOutputStream(bytes);
                                try {
                                    PacketWriter.writeEnum(p.type(), out);
                                    p.writer().accept(out);
                                    udpSocket.send(new DatagramPacket(bytes.toByteArray(), bytes.size(), inetAddress, UDP_CLIENT_PORT));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                try {
                                    PacketWriter.writeEnum(p.type(), writer);
                                } catch (SocketException e) {
                                    closed = true;
                                    return;
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                p.writer().accept(writer);
                            }
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
