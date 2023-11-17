import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.*;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class Message implements Serializable {
    private String sourceIP;
    private String destinationIP;
    private String message;

    public Message(String sourceIP, String destinationIP, String message) {
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.message = message;
    }

    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
            return bos.toByteArray();
        }
    }

    // Static method to deserialize a byte array into a Message object
    public static Message deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return (Message) in.readObject();
        }
    }

    public String getMessage() {
        return this.message;
    }

    public String getSourceIP() {
        return this.sourceIP;
    }

    public String getDestinationIP() {
        return this.destinationIP;
    }
}

class Arp implements Serializable {
    private static Map<String, Map<String, Object>> arpCache = new HashMap<>();

    private static void addArpCache(String destIP, String destMac) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("destMac", destMac);
        entry.put("ttl", 60);

        arpCache.put(destIP, entry);
    }
}

class ArpRequest implements Serializable {
    private String srcIP;
    private String srcMac;
    private String destIP;
    private String destMac;

    public ArpRequest(String srcIP, String srcMac, String destIP) {
        this.srcIP = srcIP;
        this.srcMac = srcMac;
        this.destIP = destIP;
        this.destMac = (destMac == null) ? "FF:FF:FF:FF:FF:FF" : destMac;
    }
}

class Ethernet implements Serializable {
    private String srcIP;
    private String srcMac;
    private String destIP;
    private String destMac;
    private Message ipPacket;

    public Ethernet(String srcIP, String srcMac, String destIP, Message ipPacket) {
        this.srcIP = srcIP;
        this.srcMac = srcMac;
        this.destIP = destIP;
        this.destMac = (destMac == null) ? "FF:FF:FF:FF:FF:FF" : destMac;
        this.ipPacket = ipPacket;
    }
}

public class Station {
    String comp_name, iface, rtable, htable;
    private static final Signal SIGNAL = new Signal("INT");
    private static final SignalHandler signalHandler = signal -> {
        quit();
    };
    private static SocketChannel socket;
    private static Thread receivingThread;
    private static Map<SocketChannel, String> socketFdToIP = new HashMap<>();

    public Station(String name, String i_face, String r_table, String h_table) {
        comp_name = name;
        iface = i_face;
        rtable = r_table;
        htable = h_table;
        Arp arpCache = new Arp();
    }

    private String findNextHop(String destinationIP, Vector<Vector<String>> rtable) {
        for (Vector<String> line : rtable) {
            String nextHop = line.get(1);
            String subnetMask = line.get(2);
            String rtableIP = line.get(0);

            String[] destParts = destinationIP.split("\\.");
            String[] rtableIPParts = rtableIP.split("\\.");
            String[] subnetParts = subnetMask.split("\\.");

            boolean match = true;
            for (int i = 0; i < 4; i++) {
                int destValue = Integer.parseInt(destParts[i]);
                int rtableIPValueInt = Integer.parseInt(rtableIPParts[i]);
                int subnetValue = Integer.parseInt(subnetParts[i]);

                if ((destValue & subnetValue) != (rtableIPValueInt & subnetValue)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                if (!nextHop.equals("0.0.0.0")) {
                    return nextHop;
                } else {
                    return destinationIP;
                }
            }
        }

        for (Vector<String> line : rtable) {
            String subnetMask = line.get(3);
            if (subnetMask.equals("0.0.0.0")) {
                return line.get(1);
            }
        }
        return null;
    }

    public static void main(String[] args) {
        Station s1 = new Station(args[0], args[1], args[2], args[3]);
        System.out.println();
        System.out.println("initializing..");
        System.out.println();
        Vector<Vector<String>> ifaceData = loadAndPrintIfaces(s1.iface);
        Vector<Vector<String>> rtableData = loadAndPrintRtables(s1.rtable);
        System.out.println();
        System.out.println("reading hosts...");
        Map<String, Vector<String>> hostData = loadAndPrintHosts("hosts");

        try {
            if (args.length < 2) {
                System.out.println("Usage: java chatclient <host name> <port>");
                return;
            }
            String ipAddress = "", portNumber = "";
            for (Vector<String> line : ifaceData) {
                Path addressLink = Paths.get("." + line.lastElement() + ".addr");
                ipAddress = Files.readSymbolicLink(addressLink).toString();

                Path portLink = Paths.get("." + line.lastElement() + ".port");
                portNumber = Files.readSymbolicLink(portLink).toString();
            }
            socket = SocketChannel.open(new InetSocketAddress(ipAddress, Integer.parseInt(portNumber)));
            for (Vector<String> line : ifaceData) {
                if (line.get(0).equals(socket)) {
                    String ip = line.get(1);
                    socketFdToIP.put(socket, ip);
                    // ifacetofd(ifcae,socket)
                    break;
                }
            }
            Scanner scanner = new Scanner(System.in);
            Signal.handle(SIGNAL, signalHandler);
            System.out.println("Connected to the Chat Server at: " + socket.socket().getRemoteSocketAddress()
                    + " through port " + socket.socket().getLocalPort());

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
                                        String nextHop = s1.findNextHop(destinationIP, rtableData);
                                        if (nextHop != null) {
                                            Message outgoingMessage = new Message(socketFdToIP.get(socket),
                                                    destinationIP, userInputVector.get(2));
                                            System.out.println(outgoingMessage.getMessage());
                                            System.out.println(destinationIP);
                                            System.out.println(socketFdToIP.get(socket));
                                            byte[] serializedMessage = outgoingMessage.serialize();
                                            ByteBuffer byteBuffer = ByteBuffer.wrap(serializedMessage);
                                            socket.write(byteBuffer);
                                            System.out.println("Message sent to " + destinationStation);
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
            // ObjectInputStream objectInputStream = new
            // ObjectInputStream(Channels.newInputStream(socket));
            // while (true) {
            // try {

            // byte[] serializedMessage = (byte[]) objectInputStream.readObject();
            // // String receivedMessage = (String) objectInputStream.readObject();
            // // System.out.println("Received -> " + receivedMessage);
            // Message receivedMessage = Message.deserialize(serializedMessage);
            // System.out.println("Received -> " + receivedMessage.getMessage());
            // } catch (ClassNotFoundException e) {
            // e.printStackTrace();
            // }
            // }
            ByteBuffer bytes = ByteBuffer.allocate(1024);
            while (socket.read(bytes) > 0) {
                bytes.flip();
                System.out.print("Received -> ");
                System.out.println(StandardCharsets.UTF_8.decode(bytes));
                bytes.clear();
            }
        } catch (IOException ignore) {
            ignore.printStackTrace();
            System.out.println("Server Closed!");
            quit();
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
