/**
 * Bridge
 */
public class Bridge {
    String lan_name;
    String num_ports;
    public Bridge(String l_name, String n_ports){
        lan_name = l_name;
        num_ports = n_ports;
    }
    public static void main(String args[]){
        Bridge b1 = new Bridge(args[0],args[1]);
        System.out.println(b1.lan_name + ',' + b1.num_ports);
    }
}