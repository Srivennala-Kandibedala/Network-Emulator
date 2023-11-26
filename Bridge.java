//import sun.misc.Signal;
//import sun.misc.SignalHandler;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Bridge {
    String lan_name;
    String num_ports;

    private SlTable sl;

    private static final Scanner scanner = new Scanner(System.in);
    private static Thread receivingThread;

    public Bridge(String l_name, String n_ports) {
        lan_name = l_name;
        num_ports = n_ports;
        this.sl = new SlTable();
        this.sl.myTimer();
    }

    //    private static final Signal SIGNAL = new Signal("INT");
    public static List<SocketChannel> activeClients = new ArrayList<>();

//    public static List<SocketChannel> activeClients = Collections.synchronizedList(nrmlList);

    private static ServerSocketChannel serverChannel;
    private static Selector selector;
    private static boolean serverRunning = true;
//    private static final SignalHandler signalHandler = signal -> {
//        try {
//            serverRunning = false;
//            selector.keys().forEach(SelectionKey::cancel);
//            selector.close();
//            serverChannel.close();
//            System.out.println("Server Exiting...");
//            System.exit(0);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    };

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

    public static void main(String args[]) {
        Bridge b1 = new Bridge(args[0], args[1]);
        System.out.println(b1.lan_name + ',' + b1.num_ports);
        try {
            serverChannel = ServerSocketChannel.open();
            selector = Selector.open();
            serverChannel.configureBlocking(false);
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 0);
            serverChannel.bind(address);

            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

//            Signal.handle(SIGNAL, signalHandler);

            String ipAddress = serverChannel.socket().getInetAddress().getHostName();
            createAddressSymbolicLink(b1.lan_name, ipAddress);

            int portNumber = serverChannel.socket().getLocalPort();
            createPortSymbolicLink(b1.lan_name, portNumber);

            receivingThread = new Thread(() -> {
                String userInput;
                while ((userInput = scanner.nextLine()) != null) {
                    try {
                        List<String> userInputVector = Arrays.asList(userInput.trim().split("\\s+"));
                        if (!userInputVector.isEmpty()) {
                            System.out.println("userINput: " + userInputVector);

                            if (userInputVector.get(0).equals("quit")) {
                                ServerSocketChannel mainChannel = serverChannel;
                                List<SocketChannel> connectedStations = activeClients;
                                for (SocketChannel connectedStation : connectedStations
                                ) {
                                    System.out.println("Disconnecting station " + connectedStation.getRemoteAddress().toString().split("/")[1]);
//                                    connectedStations.remove(connectedStation);
                                    connectedStation.close();
                                }
                                mainChannel.close();
                                System.exit(0);
                            }
                            else if (userInputVector.get(0).equals("show") && userInputVector.get(1).equals("sl")) {
                                b1.sl.printSl();
                            }
                            else{
                                System.out.println("Not a correct command");
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            receivingThread.start();
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
                                handleClient(b1, socket);
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

    private static void handleClient(Bridge bridge, SelectionKey socket) throws ClassNotFoundException {
        System.out.println("BRIDGE>");
        try {
            SocketChannel client = (SocketChannel) socket.channel();
            ByteBuffer bytes = ByteBuffer.allocate(1024);
//            System.out.println("size: "+activeClients.size());
            int read = client.read(bytes);
            if (read < 0) {
                System.out.println("Disconnected from: " + client.getRemoteAddress().toString().split("/")[1]);
                activeClients.remove(client);
                client.close();
                socket.cancel();
                return;
            }

            bytes.flip();
            byte[] serializedFrame = new byte[read];
            bytes.get(serializedFrame);

            try {
                EthernetFrame frame = deserializeFrame(serializedFrame);
                bridge.sl.addEntry(frame.getSourceMac(), client, activeClients.indexOf(client)); // define sl table properly [next implementation]
                byte[] serialized = serializedFrame(frame);
//                if (Objects.equals(frame.getType(), "DATAFRAME")) {
                    if (bridge.sl.isKey(frame.getDestinationMac())) {
                        bridge.sl.resetTTl(frame.getDestinationMac());
                        SocketChannel destFD = bridge.sl.getEntry(frame.getDestinationMac());
                        System.out.println("Forwarding frame....."+activeClients.indexOf(destFD));
                        destFD.write(ByteBuffer.wrap(serialized));
                    } else {
                        System.out.println("Found no entry in SL table. Broadcasting frame.....");
                        for (SocketChannel otherClient : activeClients) {
                            if (client != otherClient) {
                                otherClient.write(ByteBuffer.wrap(serialized));
                            }
                        }
                    }
//                }
//                else if (Objects.equals(frame.getType(), "ARP_REQUEST")) {
//                    if (bridge.sl.isKey(frame.getDestinationMac())) {
//                        bridge.sl.resetTTl(frame.getDestinationMac());
//                        SocketChannel destFD = bridge.sl.getEntry(frame.getDestinationMac());
//                        destFD.write(ByteBuffer.wrap(serialized));
//                    } else {
//                        for (SocketChannel otherClient : activeClients) {
//                            if (client != otherClient) {
//                                System.out.println("Sending ARP request....." + activeClients.indexOf(otherClient));
//                                otherClient.write(ByteBuffer.wrap(serialized));
//                            }
//                        }
//                    }
//                }
//                else if (Objects.equals(frame.getType(), "ARP_RESPONSE")) {
//                    System.out.println("Received arp response");
//                    SocketChannel destFD = bridge.sl.getEntry(frame.getDestinationMac());
//                    destFD.write(ByteBuffer.wrap(serialized));
//                }
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

    private static byte[] serializedFrame(EthernetFrame frame) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(frame);
            oos.flush();
            return baos.toByteArray();
        }
    }

    private static EthernetFrame deserializeFrame(byte[] serializedFrame) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(serializedFrame);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (EthernetFrame) ois.readObject();
        }
    }
}