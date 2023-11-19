import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

class Arp implements Serializable {
    private static Map<String, Map<String, Object>> arpCache = new HashMap<>();

    private static void addArpCache(String destIP, String destMac) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("destMac", destMac);
        entry.put("ttl", 60);
        arpCache.put(destIP, entry);
    }

    static String getMac(String destIP) {
        Object macObject = arpCache.get(destIP);

        if (macObject != null) {
            String mac = macObject.toString();
            System.out.println("Fetching mac => " + mac);
            return mac;
        } else {
            System.out.println("Mac not found for " + destIP);
            return ""; // or any default value you want to return
        }
    }
}

