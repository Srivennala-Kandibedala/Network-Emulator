import java.io.*;
import java.util.*;



/// A iface.a => cs1. 

public class Station {
    String comp_name, iface, rtable, htable;

    public Station(String name, String i_face, String r_table, String h_table) {
        comp_name = name;
        iface = i_face;
        rtable = r_table;
        htable = h_table;
    }

    public static void main(String args[]) {
        Station s1 = new Station(args[0], args[1], args[2], args[3]);
        // System.out.println(s1.comp_name+","+ s1.iface+","+ s1.rtable+","+s1.htable);
        try{
            File directoryPath = new File("ifaces");
            File filesList[] = directoryPath.listFiles();
            StringBuffer sb = new StringBuffer();
            for(File file : filesList) {
                System.out.println("File name: "+file.getName());
                Scanner myReader = new Scanner(file);
                Vector<Vector<String>> file_data = new Vector<>();
                while (myReader.hasNextLine()) {
                    String[] sline = myReader.nextLine().trim().split("\\s+");
                    Vector<String> line = new Vector<>();
                    for (String element : sline) {
                        line.add(element);
                    }
                    file_data.add(line);
                }
                myReader.close();
                for (Vector<String> line : file_data) {
                    System.out.println(line);
                }
            } 
        }catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
