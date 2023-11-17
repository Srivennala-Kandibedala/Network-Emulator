import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SlTable implements Serializable {   
    private static Map<String, Map<String, Object>> slCache = new HashMap<>();

    private static void add(String destIP, String destMac) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("destMac", destMac);
        entry.put("ttl", 60);

        slCache.put(destIP, entry);
    }
}
public class Bridge {
    String lan_name;
    String num_ports;
    public Bridge(String l_name, String n_ports){
        lan_name = l_name;
        num_ports = n_ports;
    }
    private static final Signal SIGNAL = new Signal("INT");
    public static List<SocketChannel> activeClients = new ArrayList<>();
    private static ServerSocketChannel serverChannel;
    private static Selector selector;
    private static boolean serverRunning = true;
    private static final SignalHandler signalHandler = signal -> {
        try {
            serverRunning = false;
            selector.keys().forEach(SelectionKey::cancel);
            selector.close();
            serverChannel.close();
            System.out.println("Server Exiting...");
            System.exit(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };
    private static void createAddressSymbolicLink(String lanName, String ipAddress) {
        try {
            Path linkPath = Paths.get("." + lanName + ".addr");
            Path targetPath = Paths.get(ipAddress);
            Files.createSymbolicLink(linkPath, targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Error creating address symbolic link: " + e.getMessage());
        }
    }

    private static void createPortSymbolicLink(String lanName, int portNumber) {
        try {
            Path linkPath = Paths.get("." + lanName + ".port");
            Path targetPath = Paths.get(String.valueOf(portNumber));
            Files.createSymbolicLink(linkPath, targetPath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating port symbolic link: " + e.getMessage());
        }
    }
    public static void main(String args[]){
        Bridge b1 = new Bridge(args[0],args[1]);
        System.out.println(b1.lan_name + ',' + b1.num_ports);
        try {
            serverChannel = ServerSocketChannel.open();
            selector = Selector.open();
            serverChannel.configureBlocking(false);
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 0);
            serverChannel.bind(address);

            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            Signal.handle(SIGNAL, signalHandler);

            String ipAddress = serverChannel.socket().getInetAddress().getHostName();
            createAddressSymbolicLink(b1.lan_name, ipAddress);
    
            int portNumber = serverChannel.socket().getLocalPort();
            createPortSymbolicLink(b1.lan_name, portNumber);

            while (serverRunning) {
                selector.select();

                if (serverRunning) {
                    selector.selectedKeys().forEach((socket) -> {
                        if (socket.isAcceptable()) {
                            try {
                                ServerSocketChannel server = (ServerSocketChannel) socket.channel();
                                SocketChannel client = server.accept();
                                client.configureBlocking(false);
                                client.register(selector, SelectionKey.OP_READ);
                                activeClients.add(client);
                                System.out.println("Connected from: " + client.getRemoteAddress().toString().split("/")[1]);
                            } catch (IOException e) {
                                System.out.println("Cannot accept connection");
                            }
                        } else if (socket.isReadable()) {
                            try {
                                handleClient(socket);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    selector.selectedKeys().clear();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static void handleClient(SelectionKey socket) throws ClassNotFoundException {
        try {
            SocketChannel client = (SocketChannel) socket.channel();
            ByteBuffer bytes = ByteBuffer.allocate(1024);

            int read = client.read(bytes);
            if (read < 0) {
                System.out.println("Disconnected from: " + client.getRemoteAddress().toString().split("/")[1]);
                activeClients.remove(client);
                client.close();
                socket.cancel();
                return;
            }

            bytes.flip();
            byte[] serializedMessage = new byte[read];
            bytes.get(serializedMessage);
            bytes.clear();
            try{
            // Deserialize the received message
            Message str = deserializeMessage(serializedMessage);   
            System.out.println(str.getMessage());       
            System.out.println(str.getSourceIP());
            System.out.println(str.getDestinationIP());
            // String str = StandardCharsets.UTF_8.decode(bytes).toString();
            // bytes.clear();

            for (SocketChannel otherClient : activeClients) {
                if (client != otherClient) {
                    // ByteBuffer byteBuffer = ByteBuffer.wrap(str.getBytes());
                    byte[] serialized = serializeMessage(str);
                    ByteBuffer byteBuffer = ByteBuffer.wrap(serialized);
                    otherClient.write(byteBuffer);
                } else {
                    String messageStr = otherClient.getRemoteAddress().toString().split("/")[1] + " -> " + str;
                    System.out.println(messageStr);
                }
            }
            } catch (EOFException e) {
                e.printStackTrace();
            // Handle the EOFException gracefully (e.g., close the socket)
            System.out.println("Connection closed by the client: " + client.getRemoteAddress().toString().split("/")[1]);
            activeClients.remove(client);
            client.close();
            socket.cancel();
        } catch (IOException | ClassNotFoundException e) {
            // Handle other exceptions
            e.printStackTrace();
        }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static byte[] serializeMessage(Message message) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(message);
            oos.flush();
            return baos.toByteArray();
        }
    }

    private static Message deserializeMessage(byte[] serializedMessage) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(serializedMessage);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Message) ois.readObject();
        }
    }
}