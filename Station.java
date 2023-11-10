import java.io.*;
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
public class Station {
    String comp_name, iface, rtable, htable;
    private static final Signal SIGNAL = new Signal("INT");
    private static final SignalHandler signalHandler = signal -> {
        quit();
    };
    private static SocketChannel socket;
    private static Thread receivingThread;

    public Station(String name, String i_face, String r_table, String h_table) {
        comp_name = name;
        iface = i_face;
        rtable = r_table;
        htable = h_table;
    }

    public static void main(String[] args) {
        Station s1 = new Station(args[0], args[1], args[2], args[3]);
        System.out.println();
        System.out.println("initializing..");
        System.out.println();
        //Loading and Printing ifaces
        Vector<Vector<String>> ifaceData = new Vector<>();
        try{
            File ifacePath = new File("ifaces");
            File ifaceList[] = ifacePath.listFiles();
            File matchingFile = null;
            for (File ifaceFile : ifaceList) {
                if (ifaceFile.getName().equals(s1.iface)) {
                    matchingFile = ifaceFile;
                    break;
                }
            }
            System.out.println("reading ifaces... ");
            System.out.println();
            Scanner myReader = new Scanner(matchingFile);
            // Vector<Vector<String>> ifaceData = new Vector<>();
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
        }
        catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        // Loading and Printing rtables
        try{
            File rtable = new File("rtables");
            File rtableList[] = rtable.listFiles();
            File matchingFile = null;
            for (File rtableFile : rtableList) {
                if (rtableFile.getName().equals(s1.rtable)) {
                    matchingFile = rtableFile;
                    break;
                }
            }
            System.out.println();
            System.out.println("reading rtables...");
            System.out.println();
            Scanner myReader = new Scanner(matchingFile);
            Vector<Vector<String>> rtableData = new Vector<>();
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
        }
        catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("reading hosts...");
        //loading and printing hosts
        try{
            File hosts = new File("hosts");
            Scanner myReader = new Scanner(hosts);
            Vector<Vector<String>> hostData = new Vector<>();
              while (myReader.hasNextLine()) {
                String[] hostLine = myReader.nextLine().trim().split("\\s+");
             Vector<String> line = new Vector<>();
                for (String element : hostLine) {
                    line.add(element);
                  }
                hostData.add(line);
            }
            myReader.close();
            for (Vector<String> line : hostData) {
                System.out.println(line);
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        try {
            if (args.length < 2) {
                System.out.println("Usage: java chatclient <host name> <port>");
                return;
            }
            String ipAddress = "", portNumber = "";
            for (Vector<String> line : ifaceData){
                Path addressLink = Paths.get("." + line.lastElement()+ ".addr");
                ipAddress = Files.readSymbolicLink(addressLink).toString();

                Path portLink = Paths.get("." + line.lastElement() + ".port");
                portNumber = Files.readSymbolicLink(portLink).toString();
            }
            socket = SocketChannel.open(new InetSocketAddress(ipAddress, Integer.parseInt(portNumber)));
            Scanner scanner = new Scanner(System.in);

            Signal.handle(SIGNAL, signalHandler);

            System.out.println("Connected to the Chat Server at: " + socket.socket().getRemoteSocketAddress() + " through port " + socket.socket().getLocalPort());

            receivingThread = new Thread(() -> {
                try {
                    ByteBuffer bytes = ByteBuffer.allocate(1024);
                    while (socket.read(bytes) > 0) {
                        bytes.flip();
                        System.out.print("Received -> ");
                        System.out.println(StandardCharsets.UTF_8.decode(bytes));
                        bytes.clear();
                    }

                    System.out.println("Server Closed!");
                    quit();
                } catch (IOException ignore) {

                }
            });
            receivingThread.start();

            System.out.println("You can start chatting now. Press CTRL + C to quit.");
            String userInput;
            while ((userInput = scanner.nextLine()) != null) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(userInput.getBytes());
                socket.write(byteBuffer);
            }

        } catch (IOException e) {
            System.out.println("Unable to create socket!");
        }
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
