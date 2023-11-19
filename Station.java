import java.io.*;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.*;

//import sun.misc.Signal;
//import sun.misc.SignalHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Station {
    String comp_name, iface, rtable, htable;
    //    private static final Signal SIGNAL = new Signal("INT");
//    private static final SignalHandler signalHandler = signal -> {
//        quit();
//    };
    private static SocketChannel socket;
    private static String ifaceName;
    private static Thread receivingThread;
    private static Map<SocketChannel, String> socketFdToIfaceName = new HashMap<>();
    private static Map<String, SocketChannel> ifaceNameTosocketFd = new HashMap<>();
    private Arp arpCache;
    private EthernetFrame ethernetFrame;

    private PendingQueue pq;

    public Station(String name, String i_face, String r_table, String h_table) {
//        super();
        comp_name = name;
        iface = i_face;
        rtable = r_table;
        htable = h_table;
        arpCache = new Arp();
        pq = new PendingQueue();
        ethernetFrame = new EthernetFrame();
    }

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
//                for (Vector<String> ifaceLine : iface) {
//                    if (ifaceLine.get(0).equals(nextIface)) {
//                        hopAndIface.put("nextIface", ifaceLine.get(4));
//                        break;
//                    }
//                }
                hopAndIface.put("nextIface", nextIface);
                return hopAndIface;
            }
        }

        for (Vector<String> line : rtable) {
            String subnetMask = line.get(2);
            if (subnetMask.equals("0.0.0.0")) {
                hopAndIface.put("nextHop", line.get(1));
//                for (Vector<String> ifaceLine : iface) {
//                    if (ifaceLine.get(0).equals(line.get(3))) {
//                        hopAndIface.put("nextIface", ifaceLine.get(4));
//                        break;
//                    }
//                }
                hopAndIface.put("nextIface", line.get(3));
                return hopAndIface;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        // [potti] - implement args len validation for user cmd line args
        Station s1 = new Station(args[0], args[1], args[2], args[3]);
        System.out.println();
        System.out.println("initializing..");
        System.out.println();
        Vector<Vector<String>> ifaceData = loadAndPrintIfaces(s1.iface);
        Vector<Vector<String>> rtableData = loadAndPrintRtables(s1.rtable);
        System.out.println();
        System.out.println("reading hosts...");
        Map<String, Vector<String>> hostData = loadAndPrintHosts("hosts");
//
////        try {
//        if (args.length < 2) {
//            System.out.println("Usage: java chatclient <host name> <port>");
//            return; // should close the program -[potti]
//        }
        String ipAddress = "", portNumber = "";
        for (Vector<String> line : ifaceData) {
            try {
                Path addressLink = Paths.get("." + line.lastElement() + ".addr");
                ipAddress = Files.readSymbolicLink(addressLink).toString();

                Path portLink = Paths.get("." + line.lastElement() + ".port");
                portNumber = Files.readSymbolicLink(portLink).toString();

                socket = SocketChannel.open(new InetSocketAddress(ipAddress, Integer.parseInt(portNumber)));
//                System.out.println("---------------------" + socket);
//            for (Vector<String> line : ifaceData) {
//                if() {
                String ip = line.get(1);
                socketFdToIfaceName.put(socket, line.get(0));
                ifaceNameTosocketFd.put(line.get(0), socket);
//                    break;
//                }
//            }
                Scanner scanner = new Scanner(System.in);
//                Signal.handle(SIGNAL, signalHandler);
                System.out.println("Connected to the Chat Server at: " + socket.socket().getRemoteSocketAddress() + " through port " + socket.socket().getLocalPort());

                receivingThread = new Thread(() -> {
                    System.out.println("You can start chatting now. Press CTRL + C to quit.");
                    String userInput;
                    while ((userInput = scanner.nextLine()) != null) {
                        try {
                            List<String> userInputVector = Arrays.asList(userInput.trim().split("\\s+"));
                            if (!userInputVector.isEmpty()) {
                                String firstElement = userInputVector.get(0);
                                if (firstElement.equals("show")) {
                                    String secondElement = userInputVector.get(1);
                                    if (secondElement.equals(secondElement)) {
                                        Map<String, Vector<String>> hosts = loadAndPrintHosts("hosts");
                                    }
                                } else if (firstElement.equals("send")) {
                                    if (userInputVector.size() >= 3) {
                                        String destinationStation = userInputVector.get(1);
                                        if (hostData.containsKey(destinationStation)) {
                                            String destinationIP = hostData.get(destinationStation).get(1);
                                            Map<String, String> nextHopNdNextIface = s1.findNextHop(destinationIP, rtableData, ifaceData);
                                            if (nextHopNdNextIface != null) {
                                                System.out.println("# Next Hop IP: " + nextHopNdNextIface.get("nextHop"));
                                                System.out.println("# Next iface: " + nextHopNdNextIface.get("nextIface"));
                                                String srcIP = hostData.get(socketFdToIfaceName.get(socket)).get(1);
                                                Message outgoingMessage = new Message(srcIP, destinationIP, userInputVector.get(2));

                                                String srcMac = null;
                                                for (Vector<String> ifaceRow : ifaceData) {
                                                    String ifaceIP = ifaceRow.get(1);
                                                    if (srcIP.equals(ifaceIP)) {
                                                        srcMac = ifaceRow.get(3);
                                                        break;
                                                    }
                                                }
//                                                    ArpRequest arpRequest = new ArpRequest("ARP_REQUEST", srcIP, srcMac, destinationIP);
//                                                    System.out.println("arp req=> "+arpRequest);
                                                SocketChannel fd = ifaceNameTosocketFd.get(nextHopNdNextIface.get("nextIface"));
                                                System.out.println("socket=> " + fd);
//                                                    byte[] serializedArp = arpRequest.serialize();
//                                                    ByteBuffer buffer = ByteBuffer.wrap(serializedArp);
                                                if (Objects.equals(s1.arpCache.getMac(destinationIP), "")) {
                                                    System.out.println("Entry not in ARP cache \nSending an ARP request from interface " + line.get(0));
                                                    System.out.println("Adding packet to pending queue " + nextHopNdNextIface.get("nextHop"));
                                                    s1.pq.addPendingPacket(nextHopNdNextIface.get("nextHop"), outgoingMessage);
                                                    s1.ethernetFrame.createArp("ARP_REQUEST", srcIP, destinationIP, srcMac, "FF:FF:FF:FF");
                                                    byte[] serializedFrame = s1.ethernetFrame.serialize();
                                                    ByteBuffer frameBuffer = ByteBuffer.wrap(serializedFrame);
                                                    fd.write(frameBuffer);
                                                } else {
                                                    System.out.println("Entry in ARP table \nSending ethernet frame to next hop");
                                                    System.out.println("******IPPacket details*****");
                                                    System.out.println("Message: " + outgoingMessage.getMessage());
                                                    System.out.println("Destination IP: " + destinationIP);
                                                    System.out.println("Source IP: " + srcIP);
                                                    System.out.println("***************************");
                                                    byte[] serializedMessage = outgoingMessage.serialize();
                                                    ByteBuffer byteBuffer = ByteBuffer.wrap(serializedMessage);
                                                    socket.write(byteBuffer);
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

            } catch (IOException e) {
                System.out.println("Unable to create socket!");
            }

            try {
                ByteBuffer bytes = ByteBuffer.allocate(4096);
                while (socket.read(bytes) > 0) {
                    bytes.flip();
                    System.out.println("Received ethernet frame ");
                    EthernetFrame ethernetFrame = EthernetFrame.deserialize(bytes);
                    if (Objects.equals(ethernetFrame.getType(), "ARP_REQUEST")) {
                        System.out.println("Received arp request");
                        if (Objects.equals(ethernetFrame.getDestinationIP(), line.get(1))) {
                            System.out.println("The ARP request is for me!");
                            System.out.println("Sending back ARP response");
                            s1.ethernetFrame.createArp("ARP_RESPONSE", ethernetFrame.getDestinationIP(), ethernetFrame.getSourceIP(), line.get(3), ethernetFrame.getSourceMac());
                            byte[] serializedFrame = s1.ethernetFrame.serialize();
                            ByteBuffer frameBuffer = ByteBuffer.wrap(serializedFrame);
                            socket.write(frameBuffer);
                        }
                    }
                    else if (Objects.equals(ethernetFrame.getType(), "ARP_RESPONSE")) {
                        System.out.println("Received an arp response");
                        if (Objects.equals(ethernetFrame.getDestinationMac(), line.get(3))) {
                            System.out.println("The ARP response is for me!");
                            // get packet from pending queue
                            List<Message> packets = s1.pq.getPendingPacket(ethernetFrame.getSourceIP());
//                            System.out.println("packets "+packets);
                            for (Message packet : packets) {
                                System.out.println("Sending dataframe to destination");
                                s1.ethernetFrame.createDF("DATAFRAME", packet, ethernetFrame.getDestinationIP(), ethernetFrame.getSourceIP(), ethernetFrame.getDestinationMac(), ethernetFrame.getSourceMac());
                                byte[] serializedFrame = s1.ethernetFrame.serialize();
                                ByteBuffer frameBuffer = ByteBuffer.wrap(serializedFrame);
                                socket.write(frameBuffer);
                            }
                        }
                    }
                    else if (Objects.equals(ethernetFrame.getType(), "DATAFRAME")) {
                        System.out.println("Received dataframe");
                        if (Objects.equals(ethernetFrame.getDestinationMac(), line.get(3))) {
                            if(Objects.equals(ethernetFrame.getDestinationIP(), line.get(1))){
                                Message packet = ethernetFrame.getPacket();
                                System.out.println("Message: " + packet.getMessage());
                                System.out.println("Src IP: " + packet.getSourceIP());
                                System.out.println("Dest IP: " + packet.getDestinationIP());
                            }
                            else {
                                System.out.println("Received dataframe but this is not for me");
                                System.out.println("Going for nexthop");
                            }
                        }
                    }
                    bytes.clear();
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
                System.out.println("Server Closed!");
                quit();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Vector<Vector<String>> loadAndPrintIfaces(String ifaceFileName) {
        Vector<Vector<String>> ifaceData = new Vector<>();
        try {
            File ifacePath = new File("ifaces");
            File ifaceList[] = ifacePath.listFiles();
            File matchingFile = null;
            for (File ifaceFile : ifaceList) {
                if (ifaceFile.getName().equals(ifaceFileName)) {
                    matchingFile = ifaceFile;
                    break;
                }
            }
            System.out.println("reading ifaces... ");
            System.out.println();
            Scanner myReader = new Scanner(matchingFile);
            while (myReader.hasNextLine()) {
                String[] ifaceLine = myReader.nextLine().trim().split("\\s+");
                Vector<String> line = new Vector<>();
                for (String element : ifaceLine) {
                    line.add(element);
                }
                ifaceData.add(line);
            }
            myReader.close();
            for (Vector<String> line : ifaceData) {
                System.out.println(line);
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return ifaceData;
    }

    private static Vector<Vector<String>> loadAndPrintRtables(String rtableFileName) {
        Vector<Vector<String>> rtableData = new Vector<>();
        try {
            File rtable = new File("rtables");
            File rtableList[] = rtable.listFiles();
            File matchingFile = null;
            for (File rtableFile : rtableList) {
                if (rtableFile.getName().equals(rtableFileName)) {
                    matchingFile = rtableFile;
                    break;
                }
            }
            System.out.println();
            System.out.println("reading rtables...");
            System.out.println();
            Scanner myReader = new Scanner(matchingFile);
            while (myReader.hasNextLine()) {
                String[] rtableLine = myReader.nextLine().trim().split("\\s+");
                Vector<String> line = new Vector<>();
                for (String element : rtableLine) {
                    line.add(element);
                }
                rtableData.add(line);
            }
            myReader.close();
            for (Vector<String> line : rtableData) {
                System.out.println(line);
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return rtableData;
    }

    private static Map<String, Vector<String>> loadAndPrintHosts(String fileName) {
        Map<String, Vector<String>> hostData = new HashMap<>();
        try {
            File hosts = new File(fileName);
            Scanner myReader = new Scanner(hosts);
            while (myReader.hasNextLine()) {
                String[] hostLine = myReader.nextLine().trim().split("\\s+");
                Vector<String> line = new Vector<>();
                for (String element : hostLine) {
                    line.add(element);
                }
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

    public static void quit() {
        try {
            socket.close();
            receivingThread.interrupt();
        } catch (IOException e) {
            System.out.println("Cannot close socket!");
        }
        System.out.println("Exiting ...");
        System.exit(0);
    }
}
