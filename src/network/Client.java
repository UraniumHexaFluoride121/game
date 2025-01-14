package network;

import foundation.MainPanel;
import foundation.VelocityHandler;
import foundation.input.InputEvent;
import foundation.math.ObjPos;
import level.Level;
import level.objects.BlockLike;
import level.objects.PhysicsBlock;
import level.objects.Player;
import loader.AssetManager;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    public final String address;
    public InetAddress inetAddress;
    public Socket socket;
    public DatagramSocket udpSocketReceive, udpSocketSend;
    private DataInputStream reader;
    private DataOutputStream writer;
    private final HashSet<PacketWriter> packetQueue = new HashSet<>();
    public int clientID = -1;

    public boolean failed = false;

    public Client(String address) {
        this.address = address;
        try {
            inetAddress = InetAddress.getByName(address);
            udpSocketReceive = new DatagramSocket(Server.UDP_CLIENT_PORT);
            udpSocketSend = new DatagramSocket(Server.UDP_SERVER_PORT);
            socket = new Socket(address, Server.TCP_PORT);
            reader = new DataInputStream(socket.getInputStream());
            writer = new DataOutputStream(socket.getOutputStream());
            new Thread(this::runReader).start();
            new Thread(this::runWriter).start();
            new Thread(() -> {
                try {
                    byte[] receiveBuffer = new byte[4096];
                    while (true) {
                        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        udpSocketReceive.receive(packet);
                        ByteArrayInputStream bytes = new ByteArrayInputStream(packet.getData());
                        readStream(new DataInputStream(bytes));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        } catch (ConnectException e) {
            failed = true;
        } catch (IOException e) {
            failed = true;
            System.out.println("[WARNING] Client port already in use");
        }
    }

    public synchronized void queuePacket(PacketWriter packet) {
        packetQueue.add(packet);
    }

    private long lastLevelUpdate = 0, lastPhysicsUpdate = 0;

    public void readStream(DataInputStream stream) throws IOException {
        switch (PacketType.values()[stream.readInt()]) {
            case CLIENT_ID -> {
                clientID = stream.readInt();
            }
            case LEVEL_UPDATE -> {
                long timeStamp = stream.readLong();
                int levelCount = stream.readInt();
                int[] indices = new int[levelCount];
                HashSet<Integer> indexSet = new HashSet<>();
                long[] seeds = new long[levelCount];
                boolean[] finalise = new boolean[levelCount];
                long[] startTimes = new long[levelCount];
                for (int i = 0; i < levelCount; i++) {
                    indices[i] = stream.readInt();
                    indexSet.add(indices[i]);
                    seeds[i] = stream.readLong();
                    finalise[i] = stream.readBoolean();
                    startTimes[i] = stream.readLong();
                }
                int active = stream.readInt();
                if (lastLevelUpdate >= timeStamp)
                    break;
                lastLevelUpdate = timeStamp;
                MainPanel.addTask(() -> {
                    MainPanel.loadedLevels.forEach((i, l) -> {
                        indexSet.remove(i);
                    });
                    indexSet.forEach(MainPanel::deleteLevel);
                    for (int i = 0; i < levelCount; i++) {
                        int index = indices[i];
                        if (MainPanel.loadedLevels.containsKey(index)) {
                            Level level = MainPanel.loadedLevels.get(index);
                            if (finalise[i] && !MainPanel.finalisedLevels.get(index)) {
                                level.finalise();
                                MainPanel.finalisedLevels.put(index, true);
                            }
                            if (level.uiProgressTracker != null) {
                                level.uiProgressTracker.startTime = startTimes[i];
                            }
                        } else {
                            MainPanel.addNewLevel(seeds[i], finalise[i], index);
                        }
                    }
                    MainPanel.setActiveLevel(active);
                });
            }
            case PHYSICS_UPDATE -> {
                long timeStamp = stream.readLong();
                int levelIndex = stream.readInt(), blockCount = stream.readInt();
                int playerIndex = stream.readInt();
                boolean space, left, right;
                if (playerIndex == -1) {
                    space = false;
                    left = false;
                    right = false;
                } else {
                    space = stream.readBoolean();
                    left = stream.readBoolean();
                    right = stream.readBoolean();
                }
                int[] indices = new int[blockCount];
                HashMap<Integer, Integer> indexSet = new HashMap<>();
                String[] names = new String[blockCount];
                ObjPos[] positions = new ObjPos[blockCount], prevPositions = new ObjPos[blockCount];
                VelocityHandler[] velocities = new VelocityHandler[blockCount], prevVelocities = new VelocityHandler[blockCount];
                for (int i = 0; i < blockCount; i++) {
                    indices[i] = stream.readInt();
                    indexSet.put(indices[i], i);
                    names[i] = stream.readUTF();
                    positions[i] = ObjPos.read(stream);
                    velocities[i] = VelocityHandler.read(stream);
                    prevPositions[i] = ObjPos.read(stream);
                    prevVelocities[i] = VelocityHandler.read(stream);
                }
                if (lastPhysicsUpdate >= timeStamp)
                    break;
                lastPhysicsUpdate = timeStamp;
                MainPanel.addTask(() -> {
                    if (MainPanel.currentLevelIndex != levelIndex)
                        return;

                    Level l = MainPanel.getLevel(levelIndex);
                    if (l == null)
                        return;
                    HashSet<PhysicsBlock> removeBlocks = new HashSet<>();
                    l.dynamicBlocks.forEach(d -> {
                        if (d instanceof PhysicsBlock b) {
                            Integer i = indexSet.remove(b.index);
                            if (i != null) {
                                b.serverPos = positions[i];
                                b.velocity = velocities[i];
                                //b.prevPos = prevPositions[i];
                                b.previousVelocity = prevVelocities[i];
                                if (b.index == playerIndex && b instanceof Player p) {
                                    l.cameraPlayer = p;
                                    p.addInput(l.inputHandler);
                                    long time = System.currentTimeMillis();
                                    if (p.space != space && time - p.timeSpace > 50) {
                                        p.sendMovementPacketUpdate(InputEvent.MOVEMENT_UP);
                                    }
                                    if (p.left != left && time - p.timeLeft > 50) {
                                        p.sendMovementPacketUpdate(InputEvent.MOVEMENT_LEFT);
                                    }
                                    if (p.right != right && time - p.timeRight > 50) {
                                        p.sendMovementPacketUpdate(InputEvent.MOVEMENT_RIGHT);
                                    }
                                }
                            } else {
                                removeBlocks.add(b);
                            }
                        }
                    });
                    removeBlocks.forEach(b -> l.removeBlocks(true, b));
                    indexSet.forEach((index, i) -> {
                        BlockLike b = AssetManager.createBlock(names[i], positions[i], l);
                        l.addBlocks(true, false, b);
                        ((PhysicsBlock) b).velocity = velocities[i];
                        ((PhysicsBlock) b).index = index;
                        ((PhysicsBlock) b).serverPos = positions[i];
                    });
                });
            }
        }
    }

    public void runReader() {
        while (true) {
            try {
                readStream(reader);
            } catch (EOFException | SocketException e) {
                break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void runWriter() {
        AtomicBoolean close = new AtomicBoolean(false);
        try {
            while (!close.get()) {
                TimeUnit.MILLISECONDS.sleep(5);
                synchronized (this) {
                    packetQueue.forEach(p -> {
                        if (p.udp()) {
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            DataOutputStream out = new DataOutputStream(bytes);
                            try {
                                PacketWriter.writeEnum(p.type(), out);
                                p.writer().accept(out);
                                udpSocketSend.send(new DatagramPacket(bytes.toByteArray(), bytes.size(), inetAddress, Server.UDP_SERVER_PORT));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            try {
                                PacketWriter.writeEnum(p.type(), writer);
                            } catch (SocketException e) {
                                close.set(true);
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
