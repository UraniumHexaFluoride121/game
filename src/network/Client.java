package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    public final String address;
    public final Socket socket;
    private final DataInputStream reader;
    private final DataOutputStream writer;
    private final HashSet<PacketWriter> packetQueue = new HashSet<>();
    public int clientID = -1;

    public Client(String address) {
        this.address = address;
        try {
            socket = new Socket(address, Server.PORT);
            reader = new DataInputStream(socket.getInputStream());
            writer = new DataOutputStream(socket.getOutputStream());
            new Thread(this::runReader).start();
            new Thread(this::runWriter).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void queuePacket(PacketWriter packet) {
        packetQueue.add(packet);
    }

    public void runReader() {
        while (true) {
            try {
                switch (PacketType.values()[reader.readInt()]) {
                    case CLIENT_ID -> {
                        clientID = reader.readInt();
                    }
                }
            } catch (EOFException | SocketException e) {
                break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void runWriter() {
        AtomicBoolean close = new AtomicBoolean(false);
        try  {
            while (!close.get()) {
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
