import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Station {
    // ConcurrentHashMaps to manage socket channel and interface name mappings
    private static final ConcurrentHashMap<SocketChannel, Vector<String>> socketFdToIfaceName = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SocketChannel> ifaceNameTosocketFd = new ConcurrentHashMap<>();

    private static final Scanner scanner = new Scanner(System.in);
    public static List<SocketChannel> activeStations = new ArrayList<>();
    public static Vector<Vector<String>> ifaceData;
    public static Vector<Vector<String>> rtableData;
    public static Map<String, Vector<String>> hostData;
    private static String ifaceName;
    private static Thread receivingThread;
    private static Selector selector;
    private static int tryCount = 5;
    private final EthernetFrame ethernetFrame;
    private final PendingQueue pq;
    String comp_name, iface, rtable, htable;

    // Constructor to initialize station properties
    public Station(String name, String i_face, String r_table, String h_table) {
        comp_name = name;
        iface = i_face;
        rtable = r_table;
        htable = h_table;
        pq = new PendingQueue();
        ethernetFrame = new EthernetFrame();
        Arp.myTimer();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("Usage: java Station <name> <i_face> <r_table> <h_table>");
            System.exit(1); // Exit with an error code
        }
        Station s1 = new Station(args[0], args[1], args[2], args[3]);
        s1.load_files(); // Load configuration files

        // Initialize selector and handle connections
        Selector selector;
        try {
            selector = Selector.open();
            handleConnections(selector);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Create and start the receiving thread for user input
        receivingThread = new Thread(() -> {
            System.out.println("You can start chatting now. Press CTRL + C to quit.");
            String userInput;
            if(Objects.equals(s1.comp_name, "-no")){
                System.out.println("STATION>");
            }else{
                System.out.println("ROUTER>");
            }
            while ((userInput = scanner.nextLine()) != null) {
                try {
                    if(Objects.equals(s1.comp_name, "-no")){
                        System.out.println("STATION>");
                    }else{
                        System.out.println("ROUTER>");
                    }
                    List<String> userInputVector = Arrays.asList(userInput.trim().split("\\s+"));
                    if (!userInputVector.isEmpty()) {
                        String firstElement = userInputVector.get(0);
                        if (firstElement.equals("quit")) {
                            System.exit(0);
                        } else if (firstElement.equals("show")) {
                            String secondElement = userInputVector.get(1);
                            if (secondElement.equals("host")) {
                                Map<String, Vector<String>> hosts = loadAndPrintHosts(("hosts"));
                            } else if (secondElement.equals("pq")) {
                                s1.pq.printPendingQueue();
                            } else if (secondElement.equals("iface")) {
                                Vector<Vector<String>> iface = loadAndPrintIfaces(s1.iface);
                            } else if (secondElement.equals("rtable")) {
                                Vector<Vector<String>> rtable = loadAndPrintRtables((s1.rtable));
                            } else if (secondElement.equals("arp")) {
                                Arp.printArpCache();
                            }
                        }
                        else if (firstElement.equals("send") && s1.comp_name.equals("-no")) {
                            if (userInputVector.size() >= 3) {
                                String destinationStation = userInputVector.get(1);
                                if (hostData.containsKey(destinationStation)) {
                                    String destinationIP = hostData.get(destinationStation).get(1);
                                    Map<String, String> nextHopNdNextIface = s1.findNextHop(destinationIP, rtableData, ifaceData);
                                    if (nextHopNdNextIface != null) {
                                        System.out.println("# Next Hop IP: " + nextHopNdNextIface.get("nextHop"));
                                        System.out.println("# Next iface: " + nextHopNdNextIface.get("nextIface"));
                                        SocketChannel next_fd = ifaceNameTosocketFd.get(nextHopNdNextIface.get("nextIface"));
                                        Vector<String> nextIface = socketFdToIfaceName.get(next_fd);
                                        String srcIP = nextIface.get(1);
                                        String srcMac = nextIface.get(3);

                                        StringBuilder messageBuilder = new StringBuilder();
                                        for (int i = 2; i < userInputVector.size(); i++) {
                                            messageBuilder.append(userInputVector.get(i)).append(" ");
                                        }
                                        String completeMessage = messageBuilder.toString().trim();
                                        Message outgoingMessage = new Message(srcIP, destinationIP, completeMessage);
                                        if (Arp.getMac(nextHopNdNextIface.get("nextHop")).isEmpty()) {
                                            System.out.println("Entry not in ARP cache");
                                            System.out.println("Sending an ARP request through interface " + nextHopNdNextIface.get("nextIface"));
                                            System.out.println("Adding packet to pending queue " + nextHopNdNextIface.get("nextHop"));
                                            s1.pq.addPendingPacket(nextHopNdNextIface.get("nextHop"), outgoingMessage);
                                            s1.ethernetFrame.createArp("ARP_REQUEST", srcIP, nextHopNdNextIface.get("nextHop"), srcMac, "FF:FF:FF:FF");
                                            byte[] serializedFrame = s1.ethernetFrame.serialize();
                                            ByteBuffer frameBuffer = ByteBuffer.wrap(serializedFrame);
                                            next_fd.write(frameBuffer);
                                        } else {
                                            System.out.println("Entry in ARP table \nSending ethernet frame to next hop");
                                            Arp.resetTTl(nextHopNdNextIface.get("nextHop"));
                                            System.out.println("******IPPacket details*****");
                                            System.out.println("Message: " + outgoingMessage.getMessage());
                                            System.out.println("Destination IP: " + destinationIP);
                                            System.out.println("Source IP: " + srcIP);
                                            System.out.println("***************************");
                                            s1.ethernetFrame.createDF("DATAFRAME", outgoingMessage, srcIP, nextHopNdNextIface.get("nextHop"), srcMac, Arp.getMac(nextHopNdNextIface.get("nextHop")));
                                            byte[] serializedFrame = s1.ethernetFrame.serialize();
                                            ByteBuffer frameBuffer = ByteBuffer.wrap(serializedFrame);
                                            next_fd.write(frameBuffer);
                                            System.out.println("Message sent to " + destinationStation);
                                        }
                                    } else {
                                        System.out.println("Next hop not found for " + destinationStation);
                                    }
                                } else {
                                    System.out.println("Station not found: " + destinationStation);
                                }
                            }
                        } else {
                            System.out.println("Invalid command: " + firstElement);
                        }
                    } else {
                        System.out.println("Empty input vector");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        receivingThread.start();
        // Continuously handle selected keys
        while (true) {
            selector.select();
            if (true) {
                selector.selectedKeys().forEach((socket) -> {
                    if (socket.isReadable()) {
                        try {
                            // Handle incoming data from stations
                            handleStation(s1, socket);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                });
                selector.selectedKeys().clear();
            }
        }
    }

    private static void handleConnections(Selector selector) throws IOException, InterruptedException {
        List<String> successCon = new ArrayList<>();
        while (successCon.size() != ifaceData.size() && tryCount-- > 0) {
            System.out.println("trying " + (5 - tryCount) + " for connections");
            for (Vector<String> line : ifaceData) {
                if (!successCon.contains(line.firstElement())) {
                    try {
                        Path addressLink = Paths.get("." + line.lastElement() + ".addr");
                        String ipAddress = Files.readSymbolicLink(addressLink).toString();

                        Path portLink = Paths.get("." + line.lastElement() + ".port");
                        String portNumber = Files.readSymbolicLink(portLink).toString();
                        SocketChannel socket = SocketChannel.open(new InetSocketAddress(ipAddress, Integer.parseInt(portNumber)));

                        socket.configureBlocking(false);
                        socket.register(selector, SelectionKey.OP_READ);

                        activeStations.add(socket);
                        socketFdToIfaceName.put(socket, line);
                        ifaceNameTosocketFd.put(line.get(0), socket);
                    } catch (IOException ignore) {
                    }
                }
            }
            Thread.sleep(2000);
            selector.selectNow();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (SelectionKey key : selectionKeys) {
                if (key.isReadable()) {
                    ByteBuffer bytes = ByteBuffer.allocate(100);
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    if (socketChannel.read(bytes) > 0) {
                        System.out.println("Accept\nConnected to " + socketFdToIfaceName.get(socketChannel).lastElement());
                        successCon.add(socketFdToIfaceName.get(socketChannel).firstElement());
                    }
                }
            }
            selectionKeys.clear();
        }
        for (Vector<String> line : ifaceData) {
            if (!successCon.contains(line.firstElement())) {
                System.out.println("Reject\nConnection rejected by bridge " + line.lastElement());
            }
        }
    }

    // Method to handle incoming data from stations
    private static void handleStation(Station s1, SelectionKey socketKey) throws ClassNotFoundException {
        SocketChannel socket = (SocketChannel) socketKey.channel();
        Vector<String> interFace = socketFdToIfaceName.get(socket);
        if(Objects.equals(s1.comp_name, "-no")){
            System.out.println("\nSTATION: " + interFace.get(0) + ">");
        }else{
            System.out.println("\nROUTER: " + interFace.get(0) + ">");
        }

        try {
            ByteBuffer bytes = ByteBuffer.allocate(4096);
            int bytesRead = socket.read(bytes);
            if (bytesRead <= 0) {
                System.out.println("Server Closed!");
                socketKey.cancel();
                quit(socket);
            }

            bytes.flip();
            System.out.println("Received ethernet frame ");
            EthernetFrame ethernetFrame = EthernetFrame.deserialize(bytes);
//            System.out.println(ethernetFrame.getDestinationIP() + interFace.get(1));
            // Arp.addArpCache(ethernetFrame.getSourceIP(), ethernetFrame.getSourceMac());
            System.out.println(ethernetFrame.getDestinationIP() + interFace.get(1));
//            Arp.addArpCache(ethernetFrame.getSourceIP(), ethernetFrame.getSourceMac());
            if (ethernetFrame.getType().equals("ARP_REQUEST")) {
                System.out.println("Received arp request ");
                if (ethernetFrame.getDestinationIP().equals(interFace.get(1))) {
                    System.out.println("The ARP request is for me!");
                    System.out.println("Sending back ARP response");
                    Arp.addArpCache(ethernetFrame.getSourceIP(), ethernetFrame.getSourceMac());
                    s1.ethernetFrame.createArp("ARP_RESPONSE", ethernetFrame.getDestinationIP(), ethernetFrame.getSourceIP(), interFace.get(3), ethernetFrame.getSourceMac());
                    byte[] serializedFrame = s1.ethernetFrame.serialize();
                    ByteBuffer frameBuffer = ByteBuffer.wrap(serializedFrame);
                    socket.write(frameBuffer);
                } else {
                    System.out.println("Not mine. My mac is " + interFace.get(3));
                }
            } else if (ethernetFrame.getType().equals("ARP_RESPONSE")) {
                System.out.println("Received an arp response to " +ethernetFrame.getDestinationMac() + " My MAC Address is: "+interFace.get(3));
                if (ethernetFrame.getDestinationMac().equals(interFace.get(3))) {
                    System.out.println("The ARP response is for me!");
                   Arp.addArpCache(ethernetFrame.getSourceIP(), ethernetFrame.getSourceMac());
                    List<Message> packets = s1.pq.getPendingPacket(ethernetFrame.getSourceIP());
                    for (Message packet : packets) {
                        System.out.println("Sending dataframe to destination " + ethernetFrame.getDestinationMac());
                        System.out.println("Packet details \n Message: " + packet.getMessage() + "\n Source IP: "+packet.getSourceIP() +"\n Destination IP: "+ packet.getDestinationIP());
//                        System.out.println("===> "+ ethernetFrame.getDestinationMac());
                        s1.ethernetFrame.createDF("DATAFRAME", packet, ethernetFrame.getDestinationIP(), ethernetFrame.getSourceIP(), ethernetFrame.getDestinationMac(), ethernetFrame.getSourceMac());
                        byte[] serializedFrame = s1.ethernetFrame.serialize();
                        ByteBuffer frameBuffer = ByteBuffer.wrap(serializedFrame);
                        socket.write(frameBuffer);
                        Thread.sleep(2000);
                    }
                    s1.pq.removePendingPacket(ethernetFrame.getSourceIP());
                }
            } else if (ethernetFrame.getType().equals("DATAFRAME")) {
                System.out.println("Received dataframe to " + ethernetFrame.getDestinationMac() + ", My Mac is " + interFace.get(3));
                if (Objects.equals(ethernetFrame.getDestinationMac(), interFace.get(3))) {
                    Message packet = ethernetFrame.getPacket();
                    if (ethernetFrame.getDestinationIP().equals(interFace.get(1)) && !s1.comp_name.equals("-route")) {
                        System.out.println("Message: " + packet.getMessage());
                        System.out.println("Src IP: " + packet.getSourceIP());
                        System.out.println("Dest IP: " + packet.getDestinationIP());
                    } else {
                        System.out.println("Received dataframe but this is not for me");
                        System.out.println("Going for nexthop");
                        Map<String, String> nextHopNdNextIface = s1.findNextHop(packet.getDestinationIP(), rtableData, ifaceData);
                        SocketChannel nextFd = ifaceNameTosocketFd.get(nextHopNdNextIface.get("nextIface"));
                        Vector<String> nextIface = socketFdToIfaceName.get(nextFd);
                        System.out.println("# Next Hop IP: " + nextHopNdNextIface.get("nextHop"));
                        System.out.println("# Next iface: " + nextHopNdNextIface.get("nextIface"));
                        if (Arp.getMac(nextHopNdNextIface.get("nextHop")).equals("")) {
                            System.out.println("Entry not in ARP cache \nSending an ARP request from interface " + interFace.get(0));
                            System.out.println("Adding packet to pending queue " + nextHopNdNextIface.get("nextHop"));
                            s1.pq.addPendingPacket(nextHopNdNextIface.get("nextHop"), packet);
                            s1.ethernetFrame.createArp("ARP_REQUEST", nextIface.get(1), nextHopNdNextIface.get("nextHop"), nextIface.get(3), "FF:FF:FF:FF:FF:FF");
                            byte[] serializedFrame = s1.ethernetFrame.serialize();
                            ByteBuffer frameBuffer = ByteBuffer.wrap(serializedFrame);
                            nextFd.write(frameBuffer);
                        } else {
                            System.out.println("Entry in ARP table \nSending ethernet frame to next hop");
                            // update arp timer
                            Arp.resetTTl(nextHopNdNextIface.get("nextHop"));
                            System.out.println("******IPPacket details*****");
                            System.out.println("Message: " + packet.getMessage());
                            System.out.println("Destination IP: " + packet.getDestinationIP());
                            System.out.println("Source IP: " + packet.getSourceIP());
                            System.out.println("***************************");
                            System.out.println(":::" + Arp.getMac(nextHopNdNextIface.get("nextHop")));
                            s1.ethernetFrame.createDF("DATAFRAME", packet, interFace.get(1), nextHopNdNextIface.get("nextHop"), interFace.get(2), Arp.getMac(nextHopNdNextIface.get("nextHop")));
                            byte[] serializedFrame = s1.ethernetFrame.serialize();
                            ByteBuffer frameBuffer = ByteBuffer.wrap(serializedFrame);
                            nextFd.write(frameBuffer);
                        }
                    }
                }
            }
            bytes.clear();

        } catch (IOException ignore) {
            ignore.printStackTrace();
            System.out.println("Server Closed!");
            socketKey.cancel();
            quit(socket);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to load and print interface data from a file
    private static Vector<Vector<String>> loadAndPrintIfaces(String ifaceFileName) {
        Vector<Vector<String>> ifaceData = new Vector<>();
        try {
            File matchingFile = new File(ifaceFileName);
            System.out.println("reading iface... ");
            System.out.println();
            assert matchingFile != null;
            Scanner myReader = new Scanner(matchingFile);
            while (myReader.hasNextLine()) {
                String[] ifaceLine = myReader.nextLine().trim().split("\\s+");
                Vector<String> line = new Vector<>();
                Collections.addAll(line, ifaceLine);
                if (!line.isEmpty()) {
                    ifaceData.add(line);
                }
            }
            myReader.close();
            for (Vector<String> line : ifaceData) {
                System.out.println("=> " + line);
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return ifaceData;
    }

    // Method to load and print routing table data from a file
    private static Vector<Vector<String>> loadAndPrintRtables(String rtableFileName) {
        Vector<Vector<String>> rtableData = new Vector<>();
        try {
            File matchingFile = new File(rtableFileName);
            System.out.println();
            System.out.println("reading rtable...");
            System.out.println();
            Scanner myReader = new Scanner(matchingFile);
            while (myReader.hasNextLine()) {
                String[] rtableLine = myReader.nextLine().trim().split("\\s+");
                Vector<String> line = new Vector<>();
                Collections.addAll(line, rtableLine);
                if (!line.isEmpty()) {
                    rtableData.add(line);
                }
            }
            myReader.close();
            for (Vector<String> line : rtableData) {
                System.out.println("=> " + line);
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return rtableData;
    }

    // Method to load and print host data from a file
    private static Map<String, Vector<String>> loadAndPrintHosts(String fileName) {
        Map<String, Vector<String>> hostData = new HashMap<>();
        try {
            File hosts = new File(fileName);
            Scanner myReader = new Scanner(hosts);
            while (myReader.hasNextLine()) {
                String[] hostLine = myReader.nextLine().trim().split("\\s+");
                Vector<String> line = new Vector<>();
                Collections.addAll(line, hostLine);
                hostData.put(line.get(0), line);
            }
            myReader.close();
            for (Map.Entry<String, Vector<String>> entry : hostData.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return hostData;
    }

    // Method to quit the station
    public static void quit(SocketChannel socket) {
        System.out.println("in quit");
        try {
            socket.close();
            receivingThread.interrupt();
        } catch (IOException e) {
            System.out.println("Cannot close socket!");
        }
        System.out.println("Exiting ...");
        System.exit(0);
    }

    // Method to find the next hop in the routing table
    private Map<String, String> findNextHop(String destinationIP, Vector<Vector<String>> rtable, Vector<Vector<String>> iface) {
        Map<String, String> hopAndIface = new HashMap<>();
        for (Vector<String> line : rtable) {
            String nextHop = line.get(1);
            String subnetMask = line.get(2);
            String tableIP = line.get(0);
            String nextIface = line.get(3);

            String[] destParts = destinationIP.split("\\.");
            String[] tableIPParts = tableIP.split("\\.");
            String[] subnetParts = subnetMask.split("\\.");

            boolean match = true;
            for (int i = 0; i < 4; i++) {
                int destValue = Integer.parseInt(destParts[i]);
                int tableIPValueInt = Integer.parseInt(tableIPParts[i]);
                int subnetValue = Integer.parseInt(subnetParts[i]);

                if ((destValue & subnetValue) != (tableIPValueInt)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                if (!nextHop.equals("0.0.0.0")) {
                    hopAndIface.put("nextHop", nextHop);
                } else {
                    hopAndIface.put("nextHop", destinationIP);
                }
                hopAndIface.put("nextIface", nextIface);
                return hopAndIface;
            }
        }

        for (Vector<String> line : rtable) {
            String subnetMask = line.get(2);
            if (subnetMask.equals("0.0.0.0")) {
                hopAndIface.put("nextHop", line.get(1));
                hopAndIface.put("nextIface", line.get(3));
                return hopAndIface;
            }
        }
        return null;
    }

    // Method to load configuration files and initialize the station
    public void load_files() {
        System.out.println();
        System.out.println("initializing..");
        System.out.println();
        ifaceData = loadAndPrintIfaces(this.iface);
        rtableData = loadAndPrintRtables(this.rtable);
        System.out.println();
        System.out.println("reading hosts...");
        hostData = loadAndPrintHosts("hosts");
    }
}
