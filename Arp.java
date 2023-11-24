import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

class Arp implements Serializable {
    private static Map<String, Map<String, Object>> arpCache = new HashMap<>();

    static void addArpCache(String destIP, String destMac) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("destMac", destMac);
        entry.put("ttl", 60);
        arpCache.put(destIP, entry);
    }

    static String getMac(String destIP) {
        String mac = "";
        if (arpCache.containsKey(destIP)){
            mac = (String) arpCache.get(destIP).get("destMac");
        }

        if (mac != "") {
            return mac;
        } else {
            System.out.println("Mac not found for " + destIP);
            return ""; // or any default value you want to return
        }
    }

    static void printArpCache() {
        for (Map.Entry<String, Map<String, Object>> entry : arpCache.entrySet()) {
            String destIP = entry.getKey();
            Map<String, Object> entryDetails = entry.getValue();

            String destMac = entryDetails.get("destMac").toString();
            int ttl = (int) entryDetails.get("ttl");

            System.out.println("Destination IP: " + destIP + ", Destination Mac: " + destMac + ", TTL: " + ttl);
        }
        System.out.println("End of Arp Cache Table");
    }
}

