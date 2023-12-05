/*
    Authors names: Sri Vennala Kandibedala
                   Rahul Mallela
*/
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Bridge {
    private static final Scanner scanner = new Scanner(System.in);
    public static List<SocketChannel> activeClients = new ArrayList<>();
    static int num_ports;
    private static Thread receivingThread;
    private static ServerSocketChannel serverChannel;
    private static Selector selector;
    private static boolean serverRunning = true;

    String lan_name;
    private SlTable sl;
    public Bridge(String l_name, String n_ports) {
        lan_name = l_name;
        num_ports = Integer.parseInt(n_ports);
        this.sl = new SlTable();
        this.sl.myTimer();
    }

    // Creates a symbolic link for the LAN's IP address.
    private static void createAddressSymbolicLink(String lanName, String ipAddress) {
        try {
            Path linkPath = Paths.get("." + lanName + ".addr");
            Path targetPath = Paths.get(ipAddress);
            Files.createSymbolicLink(linkPath, targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Error creating address symbolic link: " + e.getMessage());
        }
    }

    // Creates a symbolic link for the LAN's port number.
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

            String ipAddress = serverChannel.socket().getInetAddress().getHostName();
            createAddressSymbolicLink(b1.lan_name, ipAddress);

            int portNumber = serverChannel.socket().getLocalPort();
            createPortSymbolicLink(b1.lan_name, portNumber);

            // Start a thread for receiving user input
            receivingThread = new Thread(() -> {
                String userInput;
                System.out.println("BRIDGE>");
                while ((userInput = scanner.nextLine()) != null) {
                    try {
                        System.out.println("BRIDGE>");
                        List<String> userInputVector = Arrays.asList(userInput.trim().split("\\s+"));
                        if (!userInputVector.isEmpty()) {

                            if (userInputVector.get(0).equals("quit")) {
                                // Disconnect all stations and exit the server
                                ServerSocketChannel mainChannel = serverChannel;
                                List<SocketChannel> connectedStations = activeClients;
                                for (SocketChannel connectedStation : connectedStations
                                ) {
                                    System.out.println("Disconnecting station " + connectedStation.getRemoteAddress().toString().split("/")[1]);
                                    connectedStation.close();
                                }
                                mainChannel.close();
                                System.exit(0);
                            } else if (userInputVector.get(0).equals("show") && userInputVector.get(1).equals("sl")) {
                                // Display the SL table
                                b1.sl.printSl();
                            } else {
                                System.out.println("Not a correct command");
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            receivingThread.start();
            // Main loop for handling server events
            while (serverRunning) {
                selector.select();
                if (serverRunning) {
                    selector.selectedKeys().forEach((socket) -> {
                        if (socket.isAcceptable()) {
                            if (num_ports > 0) {
                                try {
                                    // Accept incoming connection, configure the client, and register for reading
                                    ServerSocketChannel server = (ServerSocketChannel) socket.channel();
                                    SocketChannel client = server.accept();
                                    client.configureBlocking(false);
                                    client.register(selector, SelectionKey.OP_READ);
                                    activeClients.add(client);
                                    client.write(ByteBuffer.wrap("accept".getBytes()));
                                    num_ports--;
                                    System.out.println("Connected from: " + client.getRemoteAddress().toString().split("/")[1]);
                                } catch (IOException e) {
                                    System.out.println("Cannot accept connection");
                                }
                            }
                        } else if (socket.isReadable()) {
                            try {
                                // Handle incoming data from a connected client
                                handleClient(b1, socket);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("here");
                        }
                    });
                    selector.selectedKeys().clear();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Handles communication with a connected client.
    private static void handleClient(Bridge bridge, SelectionKey socket) throws ClassNotFoundException {
        System.out.println("BRIDGE>");
        try {
            SocketChannel client = (SocketChannel) socket.channel();
            ByteBuffer bytes = ByteBuffer.allocate(1024);
            int read = client.read(bytes);
            if (read < 0) {
                System.out.println("Disconnected from: " + client.getRemoteAddress().toString().split("/")[1]);
                activeClients.remove(client);
                bridge.sl.remove(client);
                client.close();
                socket.cancel();
                num_ports++;
                return;
            }

            bytes.flip();
            byte[] serializedFrame = new byte[read];
            bytes.get(serializedFrame);

            try {
                EthernetFrame frame = deserializeFrame(serializedFrame);
                bridge.sl.addEntry(frame.getSourceMac(), client, activeClients.indexOf(client)); // define sl table properly [next implementation]
                byte[] serialized = serializedFrame(frame);
                if (bridge.sl.isKey(frame.getDestinationMac())) {
                    bridge.sl.resetTTl(frame.getDestinationMac());
                    SocketChannel destFD = bridge.sl.getEntry(frame.getDestinationMac());
                    System.out.println("Forwarding frame....." + activeClients.indexOf(destFD));
                    destFD.write(ByteBuffer.wrap(serialized));
                } else {
                    System.out.println("Found no entry in SL table. Broadcasting frame.....");
                    for (SocketChannel otherClient : activeClients) {
                        if (client != otherClient) {
                            otherClient.write(ByteBuffer.wrap(serialized));
                        }
                    }
                }
            } catch (EOFException e) {
                e.printStackTrace();
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