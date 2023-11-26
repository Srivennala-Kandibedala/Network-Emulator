import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

class Arp implements Serializable {
    private static Map<String, Map<String, Object>> arpCache = new HashMap<>();

    public void myTimer(){
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);
    }

    static void resetTTl(String key){
            Map<String, Object> entry = arpCache.get(key);
            if (entry != null) {
                entry.put("ttl", 60);
                arpCache.put(key, entry);
            }
    }

    private static void updateTimer() {
        List<Map.Entry<String, Map<String, Object>>> entriesToRemove = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : arpCache.entrySet()) {
            int currentTTL = (int) entry.getValue().get("ttl");
            if (currentTTL > 0) {
                entry.getValue().put("ttl", currentTTL - 1);
            } else {
                System.out.println("Entry with IP " + entry.getKey() + " has expired and will be removed.");
                entriesToRemove.add(entry);
            }
        }

        for (Map.Entry<String, Map<String, Object>> entryToRemove : entriesToRemove) {
            arpCache.remove(entryToRemove.getKey());
            System.out.println("Entry with IP " + entryToRemove.getKey() + " has been removed.");
        }
    }
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

