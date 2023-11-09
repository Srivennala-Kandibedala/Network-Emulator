import java.io.*;
import java.util.*;
public class Station {
    String comp_name, iface, rtable, htable;

    public Station(String name, String i_face, String r_table, String h_table) {
        comp_name = name;
        iface = i_face;
        rtable = r_table;
        htable = h_table;
    }

    public static void main(String[] args) {
        Station s1 = new Station(args[0], args[1], args[2], args[3]);
        // System.out.println(s1.comp_name+","+ s1.iface+","+ s1.rtable+","+s1.htable);
        System.out.println();
        System.out.println("initializing..");
        System.out.println();
        //Loading and Printing ifaces
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
            Vector<Vector<String>> ifaceData = new Vector<>();
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
    }
}
